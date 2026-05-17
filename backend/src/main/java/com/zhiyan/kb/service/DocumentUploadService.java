package com.zhiyan.kb.service;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import com.zhiyan.kb.rag.VectorStoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class DocumentUploadService {
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of("txt", "md", "pdf", "docx");
    private static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;

    private final KbDocumentMapper documentMapper;
    private final KbSpaceMapper spaceMapper;
    private final DocumentParseService parseService;
    private final ChunkService chunkService;
    private final ResourceAccessService accessService;
    private final VectorStoreService vectorStoreService;
    private final String uploadDir;

    public DocumentUploadService(KbDocumentMapper documentMapper, KbSpaceMapper spaceMapper,
                                 DocumentParseService parseService, ChunkService chunkService,
                                 ResourceAccessService accessService, VectorStoreService vectorStoreService,
                                 @Value("${zhiyan.upload-dir:uploads}") String uploadDir) {
        this.documentMapper = documentMapper;
        this.spaceMapper = spaceMapper;
        this.parseService = parseService;
        this.chunkService = chunkService;
        this.accessService = accessService;
        this.vectorStoreService = vectorStoreService;
        this.uploadDir = uploadDir;
    }

    @Transactional
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
        KbDocument document = new KbDocument();
        try {
            file.transferTo(temp);
            document.setSpaceId(spaceId);
            document.setTitle(title == null || title.isBlank() ? FileUtil.mainName(originalFilename) : title);
            document.setOriginalFilename(originalFilename);
            document.setFileType(ext);
            document.setFileSize(file.getSize());
            document.setFileUrl(fileKey);
            document.setParseStatus("PARSING");
            document.setVectorStatus("PROCESSING");
            document.setStatus("NORMAL");
            document.setUploaderId(UserContext.userId());
            documentMapper.insert(document);

            document.setContentText(parseService.parse(temp.toFile(), ext));
            Files.move(temp, target);
            chunkService.rebuildChunks(document);
            document.setParseStatus("SUCCESS");
            document.setVectorStatus("SUCCESS");
            documentMapper.updateById(document);
            incrementDocumentCount(spaceId);
            return document;
        } catch (Exception ex) {
            cleanupFile(temp);
            cleanupFile(target);
            if (document.getId() != null) {
                vectorStoreService.removeByDocumentId(document.getId());
            }
            throw ex;
        }
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
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if ("[Content_Types].xml".equals(name)) {
                    hasContentTypes = true;
                } else if ("word/document.xml".equals(name)) {
                    hasDocumentXml = true;
                }
                if (hasContentTypes && hasDocumentXml) {
                    return true;
                }
            }
        }
        return false;
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
}
