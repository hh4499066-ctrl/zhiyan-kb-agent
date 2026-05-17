package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.kb.ai.LLMClient;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.PageResult;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.dto.UpdateDocumentRequest;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.entity.KbDocumentChunk;
import com.zhiyan.kb.entity.KbFaq;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbFaqMapper;
import com.zhiyan.kb.service.ChunkService;
import com.zhiyan.kb.service.DocumentUploadService;
import com.zhiyan.kb.service.ResourceAccessService;
import com.zhiyan.kb.vo.DocumentDetailVO;
import com.zhiyan.kb.vo.DocumentListVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final KbDocumentMapper documentMapper;
    private final KbDocumentChunkMapper chunkMapper;
    private final KbFaqMapper faqMapper;
    private final ChunkService chunkService;
    private final LLMClient llmClient;
    private final ResourceAccessService accessService;
    private final DocumentUploadService uploadService;

    public DocumentController(KbDocumentMapper documentMapper, KbDocumentChunkMapper chunkMapper,
                              KbFaqMapper faqMapper, ChunkService chunkService,
                              LLMClient llmClient, ResourceAccessService accessService,
                              DocumentUploadService uploadService) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.faqMapper = faqMapper;
        this.chunkService = chunkService;
        this.llmClient = llmClient;
        this.accessService = accessService;
        this.uploadService = uploadService;
    }

    @GetMapping
    public Result<PageResult<DocumentListVO>> list(@RequestParam(required = false) Long spaceId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        List<Long> accessibleSpaceIds = accessService.accessibleNormalSpaceIds();
        if (accessibleSpaceIds.isEmpty()) {
            return Result.ok(new PageResult<>(0, page, size, List.of()));
        }
        if (spaceId != null && !accessibleSpaceIds.contains(spaceId)) {
            throw new BusinessException(403, "No permission to access this space");
        }
        LambdaQueryWrapper<KbDocument> query = new LambdaQueryWrapper<KbDocument>()
                .eq(spaceId != null, KbDocument::getSpaceId, spaceId)
                .in(spaceId == null, KbDocument::getSpaceId, accessibleSpaceIds)
                .like(keyword != null && !keyword.isBlank(), KbDocument::getTitle, keyword)
                .eq(KbDocument::getStatus, "NORMAL")
                .orderByDesc(KbDocument::getCreateTime);
        Page<KbDocument> result = documentMapper.selectPage(Page.of(page, size), query);
        List<DocumentListVO> records = result.getRecords().stream().map(DocumentListVO::from).toList();
        return Result.ok(new PageResult<>(result.getTotal(), page, size, records));
    }

    @PostMapping("/upload")
    public Result<DocumentDetailVO> upload(@RequestParam Long spaceId, @RequestParam(required = false) String title,
                                           @RequestPart("file") MultipartFile file) throws Exception {
        return Result.ok(DocumentDetailVO.from(uploadService.upload(spaceId, title, file)));
    }

    @GetMapping("/{id}")
    public Result<DocumentDetailVO> detail(@PathVariable Long id) {
        return Result.ok(DocumentDetailVO.from(accessService.requireDocument(id)));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateDocumentRequest request) {
        accessService.requireDocumentManage(id);
        KbDocument update = new KbDocument();
        update.setId(id);
        update.setTitle(request.getTitle());
        update.setSummary(request.getSummary());
        update.setKeywords(request.getKeywords());
        documentMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        KbDocument document = accessService.requireDocumentManage(id);
        document.setStatus("DELETED");
        documentMapper.updateById(document);
        chunkService.disableDocumentChunks(id);
        return Result.ok();
    }

    @PostMapping("/{id}/parse")
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

    public static boolean hasRequiredDocxEntries(InputStream input) throws IOException {
        return DocumentUploadService.hasRequiredDocxEntries(input);
    }
}
