package com.zhiyan.kb.service;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.DocumentProcessingTask;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.mapper.DocumentProcessingTaskMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class DocumentUploadService {
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of("txt", "md", "pdf", "docx");
    private static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;
    private static final int MAX_DOCX_ENTRY_COUNT = 3000;
    private static final int MAX_DOCX_ENTRY_NAME_LENGTH = 255;
    private static final long MAX_DOCX_UNCOMPRESSED_BYTES = 100L * 1024 * 1024;

    private final KbDocumentMapper documentMapper;
    private final DocumentProcessingTaskMapper taskMapper;
    private final KbSpaceMapper spaceMapper;
    private final ResourceAccessService accessService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final String uploadDir;

    public DocumentUploadService(KbDocumentMapper documentMapper, DocumentProcessingTaskMapper taskMapper,
                                 KbSpaceMapper spaceMapper,
                                 ResourceAccessService accessService, ApplicationEventPublisher eventPublisher,
                                 TransactionTemplate transactionTemplate,
                                 @Value("${zhiyan.upload-dir:uploads}") String uploadDir) {
        this.documentMapper = documentMapper;
        this.taskMapper = taskMapper;
        this.spaceMapper = spaceMapper;
        this.accessService = accessService;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.uploadDir = uploadDir;
    }

    public KbDocument upload(Long spaceId, String title, MultipartFile file) throws Exception {
        validateUpload(spaceId, file);
        accessService.requireSpaceManage(spaceId);

        String originalFilename = Paths.get(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename())
                .getFileName()
                .toString();
        String ext = FileUtil.extName(originalFilename).toLowerCase(Locale.ROOT);
        if (!ALLOWED_FILE_TYPES.contains(ext)) {
            throw new BusinessException(400, "Unsupported file type");
        }
        validateFileSignature(file, ext);

        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(base);
        String fileKey = UUID.randomUUID() + "." + ext;
        Path target = base.resolve(fileKey).normalize();
        if (!target.startsWith(base)) {
            throw new BusinessException(400, "Invalid upload path");
        }

        Path temp = Files.createTempFile(base, "upload-", ".tmp");
        try {
            file.transferTo(temp);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            UploadedDocument uploaded = createDocument(spaceId, title, originalFilename, ext, file.getSize(), fileKey);
            eventPublisher.publishEvent(new DocumentProcessingEvent(uploaded.task().getId(),
                    uploaded.document().getId(), target, ext));
            return uploaded.document();
        } catch (Exception ex) {
            cleanupFile(temp);
            cleanupFile(target);
            throw ex;
        }
    }

    private UploadedDocument createDocument(Long spaceId, String title, String originalFilename, String ext,
                                            long fileSize, String fileKey) {
        return transactionTemplate.execute(status -> {
            KbDocument document = new KbDocument();
            document.setSpaceId(spaceId);
            document.setTitle(title == null || title.isBlank() ? FileUtil.mainName(originalFilename) : title);
            document.setOriginalFilename(originalFilename);
            document.setFileType(ext);
            document.setFileSize(fileSize);
            document.setFileUrl(fileKey);
            document.setParseStatus("UPLOADED");
            document.setVectorStatus("PENDING");
            document.setStatus("NORMAL");
            document.setUploaderId(UserContext.userId());
            documentMapper.insert(document);
            DocumentProcessingTask task = createTask(document.getId(), fileKey, ext);
            taskMapper.insert(task);
            incrementDocumentCount(spaceId);
            return new UploadedDocument(document, task);
        });
    }

    private DocumentProcessingTask createTask(Long documentId, String fileKey, String fileType) {
        DocumentProcessingTask task = new DocumentProcessingTask();
        task.setDocumentId(documentId);
        task.setFileUrl(fileKey);
        task.setFileType(fileType);
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetries(3);
        return task;
    }

    private void validateUpload(Long spaceId, MultipartFile file) {
        if (spaceId == null || spaceId <= 0) {
            throw new BusinessException(400, "Invalid spaceId");
        }
        if (file.isEmpty()) {
            throw new BusinessException(400, "Uploaded file is empty");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BusinessException(400, "Uploaded file exceeds 20MB");
        }
    }

    public static void validateFileSignature(MultipartFile file, String ext) throws IOException {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(8);
            if ("pdf".equals(ext) && !startsWith(header, "%PDF".getBytes())) {
                throw new BusinessException(400, "Invalid PDF file");
            }
            if ("docx".equals(ext) && !(header.length >= 4 && header[0] == 'P' && header[1] == 'K')) {
                throw new BusinessException(400, "Invalid DOCX file");
            }
        }
        if ("docx".equals(ext) && !hasRequiredDocxEntries(file.getInputStream())) {
            throw new BusinessException(400, "Invalid DOCX file");
        }
    }

    public static boolean hasRequiredDocxEntries(InputStream input) throws IOException {
        boolean hasContentTypes = false;
        boolean hasDocumentXml = false;
        int entryCount = 0;
        long totalUncompressedBytes = 0;
        byte[] buffer = new byte[8192];
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_DOCX_ENTRY_COUNT) {
                    throw new BusinessException(400, "DOCX file is too complex");
                }
                String name = entry.getName();
                if (name.length() > MAX_DOCX_ENTRY_NAME_LENGTH) {
                    throw new BusinessException(400, "DOCX entry name is too long");
                }
                if ("[Content_Types].xml".equals(name)) {
                    hasContentTypes = true;
                } else if ("word/document.xml".equals(name)) {
                    hasDocumentXml = true;
                }
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    totalUncompressedBytes += read;
                    if (totalUncompressedBytes > MAX_DOCX_UNCOMPRESSED_BYTES) {
                        throw new BusinessException(400, "DOCX file expands too large");
                    }
                }
            }
        }
        return hasContentTypes && hasDocumentXml;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private void cleanupFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort compensation; the original failure is more important.
        }
    }

    private void incrementDocumentCount(Long spaceId) {
        spaceMapper.update(null, new LambdaUpdateWrapper<KbSpace>()
                .eq(KbSpace::getId, spaceId)
                .setSql("document_count = COALESCE(document_count, 0) + 1"));
    }

    private record UploadedDocument(KbDocument document, DocumentProcessingTask task) {
    }
}
