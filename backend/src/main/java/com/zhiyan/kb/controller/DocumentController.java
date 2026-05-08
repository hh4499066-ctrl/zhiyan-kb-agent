package com.zhiyan.kb.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.ai.LLMClient;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.entity.KbDocumentChunk;
import com.zhiyan.kb.entity.KbFaq;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbFaqMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import com.zhiyan.kb.service.ChunkService;
import com.zhiyan.kb.service.DocumentParseService;
import com.zhiyan.kb.service.ResourceAccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of("txt", "md", "pdf", "docx");
    private static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;

    private final KbDocumentMapper documentMapper;
    private final KbDocumentChunkMapper chunkMapper;
    private final KbFaqMapper faqMapper;
    private final KbSpaceMapper spaceMapper;
    private final DocumentParseService parseService;
    private final ChunkService chunkService;
    private final LLMClient llmClient;
    private final ResourceAccessService accessService;
    private final String uploadDir;

    public DocumentController(KbDocumentMapper documentMapper, KbDocumentChunkMapper chunkMapper,
                              KbFaqMapper faqMapper, KbSpaceMapper spaceMapper,
                              DocumentParseService parseService, ChunkService chunkService,
                              LLMClient llmClient, ResourceAccessService accessService,
                              @Value("${zhiyan.upload-dir:uploads}") String uploadDir) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.faqMapper = faqMapper;
        this.spaceMapper = spaceMapper;
        this.parseService = parseService;
        this.chunkService = chunkService;
        this.llmClient = llmClient;
        this.accessService = accessService;
        this.uploadDir = uploadDir;
    }

    @GetMapping
    public Result<List<KbDocument>> list(@RequestParam(required = false) Long spaceId,
                                         @RequestParam(required = false) String keyword) {
        List<KbDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .eq(spaceId != null, KbDocument::getSpaceId, spaceId)
                .like(keyword != null && !keyword.isBlank(), KbDocument::getTitle, keyword)
                .eq(KbDocument::getStatus, "NORMAL")
                .orderByDesc(KbDocument::getCreateTime));
        return Result.ok(documents.stream().filter(d -> accessService.canAccessSpace(d.getSpaceId())).toList());
    }

    @PostMapping("/upload")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<KbDocument> upload(@RequestParam Long spaceId, @RequestParam(required = false) String title,
                                     @RequestPart("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new BusinessException(400, "Uploaded file is empty");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BusinessException(400, "Uploaded file exceeds 20MB");
        }
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
        Path target = base.resolve(UUID.randomUUID() + "." + ext).normalize();
        if (!target.startsWith(base)) {
            throw new BusinessException(400, "Invalid upload path");
        }
        Files.createDirectories(base);
        file.transferTo(target);

        KbDocument document = new KbDocument();
        document.setSpaceId(spaceId);
        document.setTitle(title == null || title.isBlank() ? FileUtil.mainName(originalFilename) : title);
        document.setOriginalFilename(originalFilename);
        document.setFileType(ext);
        document.setFileSize(file.getSize());
        document.setFileUrl(target.toString());
        document.setParseStatus("PARSING");
        document.setVectorStatus("PROCESSING");
        document.setStatus("NORMAL");
        document.setUploaderId(UserContext.userId());
        documentMapper.insert(document);

        try {
            document.setContentText(parseService.parse(target.toFile(), ext));
            chunkService.rebuildChunks(document);
            document.setParseStatus("SUCCESS");
            document.setVectorStatus("SUCCESS");
            documentMapper.updateById(document);
            incrementDocumentCount(spaceId);
        } catch (Exception ex) {
            document.setParseStatus("FAILED");
            document.setVectorStatus("FAILED");
            documentMapper.updateById(document);
            throw ex;
        }
        return Result.ok(document);
    }

    @GetMapping("/{id}")
    public Result<KbDocument> detail(@PathVariable Long id) {
        return Result.ok(accessService.requireDocument(id));
    }

    @PutMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> update(@PathVariable Long id, @RequestBody KbDocument document) {
        accessService.requireDocumentManage(id);
        document.setId(id);
        documentMapper.updateById(document);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> delete(@PathVariable Long id) {
        KbDocument document = accessService.requireDocumentManage(id);
        document.setStatus("DELETED");
        documentMapper.updateById(document);
        chunkMapper.selectList(new LambdaQueryWrapper<KbDocumentChunk>().eq(KbDocumentChunk::getDocumentId, id))
                .forEach(c -> {
                    c.setStatus("DISABLED");
                    chunkMapper.updateById(c);
                });
        return Result.ok();
    }

    @PostMapping("/{id}/parse")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> parse(@PathVariable Long id) {
        KbDocument document = accessService.requireDocumentManage(id);
        try {
            chunkService.rebuildChunks(document);
            document.setParseStatus("SUCCESS");
            document.setVectorStatus("SUCCESS");
            documentMapper.updateById(document);
        } catch (Exception ex) {
            document.setParseStatus("FAILED");
            document.setVectorStatus("FAILED");
            documentMapper.updateById(document);
            throw ex;
        }
        return Result.ok();
    }

    @GetMapping("/{id}/chunks")
    public Result<List<KbDocumentChunk>> chunks(@PathVariable Long id) {
        accessService.requireDocument(id);
        return Result.ok(chunkMapper.selectList(new LambdaQueryWrapper<KbDocumentChunk>()
                .eq(KbDocumentChunk::getDocumentId, id)
                .eq(KbDocumentChunk::getStatus, "NORMAL")
                .orderByAsc(KbDocumentChunk::getChunkIndex)));
    }

    @PostMapping("/{id}/ai-summary")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Map<String, Object>> summary(@PathVariable Long id) {
        KbDocument document = accessService.requireDocumentManage(id);
        String text = llmClient.complete("Summarize this document and extract keywords, audience and reading tips:\n"
                + document.getContentText());
        document.setSummary(text);
        document.setKeywords("AI summary,knowledge base,document");
        documentMapper.updateById(document);
        return Result.ok(Map.of(
                "summary", text,
                "keywords", document.getKeywords(),
                "audience", "Enterprise users",
                "readingTips", "Read the summary first, then inspect the cited sections."
        ));
    }

    @PostMapping("/{id}/generate-faq")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<List<KbFaq>> generateFaq(@PathVariable Long id) {
        KbDocument document = accessService.requireDocumentManage(id);
        String faqText = llmClient.complete("Generate FAQ items from this document:\n" + document.getContentText());
        KbFaq faq = new KbFaq();
        faq.setSpaceId(document.getSpaceId());
        faq.setDocumentId(document.getId());
        faq.setQuestion("What problem does this document mainly solve?");
        faq.setAnswer(faqText);
        faq.setTags("AI_GENERATED,DOCUMENT_FAQ");
        faq.setCreateType("AI_GENERATED");
        faq.setStatus("NORMAL");
        faqMapper.insert(faq);
        return Result.ok(List.of(faq));
    }

    private void validateFileSignature(MultipartFile file, String ext) throws IOException {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(8);
            if ("pdf".equals(ext) && !startsWith(header, "%PDF".getBytes())) {
                throw new BusinessException(400, "Invalid PDF file");
            }
            if ("docx".equals(ext) && !(header.length >= 4 && header[0] == 'P' && header[1] == 'K')) {
                throw new BusinessException(400, "Invalid DOCX file");
            }
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
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

    private void incrementDocumentCount(Long spaceId) {
        KbSpace space = spaceMapper.selectById(spaceId);
        if (space == null) {
            return;
        }
        int current = space.getDocumentCount() == null ? 0 : space.getDocumentCount();
        space.setDocumentCount(current + 1);
        spaceMapper.updateById(space);
    }
}
