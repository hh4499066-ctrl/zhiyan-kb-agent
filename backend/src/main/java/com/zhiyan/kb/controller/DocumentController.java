package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.kb.ai.LLMClient;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.PageResult;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.dto.UpdateDocumentRequest;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.entity.KbDocumentChunk;
import com.zhiyan.kb.entity.KbFaq;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbFaqMapper;
import com.zhiyan.kb.service.ChunkService;
import com.zhiyan.kb.service.DocumentProcessingService;
import com.zhiyan.kb.service.DocumentUploadService;
import com.zhiyan.kb.service.AiRateLimitService;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final DocumentProcessingService processingService;
    private final AiRateLimitService aiRateLimitService;

    public DocumentController(KbDocumentMapper documentMapper, KbDocumentChunkMapper chunkMapper,
                              KbFaqMapper faqMapper, ChunkService chunkService,
                              LLMClient llmClient, ResourceAccessService accessService,
                              DocumentUploadService uploadService, DocumentProcessingService processingService,
                              AiRateLimitService aiRateLimitService) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.faqMapper = faqMapper;
        this.chunkService = chunkService;
        this.llmClient = llmClient;
        this.accessService = accessService;
        this.uploadService = uploadService;
        this.processingService = processingService;
        this.aiRateLimitService = aiRateLimitService;
    }

    public DocumentController(KbDocumentMapper documentMapper, KbDocumentChunkMapper chunkMapper,
                              KbFaqMapper faqMapper, ChunkService chunkService,
                              LLMClient llmClient, ResourceAccessService accessService,
                              DocumentUploadService uploadService, DocumentProcessingService processingService) {
        this(documentMapper, chunkMapper, faqMapper, chunkService, llmClient, accessService, uploadService,
                processingService, null);
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
                .eq(KbDocument::getStatus, StatusConstants.NORMAL)
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
        document.setStatus(StatusConstants.DELETED);
        documentMapper.updateById(document);
        chunkService.disableDocumentChunks(id);
        return Result.ok();
    }

    @PostMapping("/{id}/parse")
    public Result<Void> parse(@PathVariable Long id) {
        KbDocument document = accessService.requireDocumentManage(id);
        processingService.requestReprocess(document);
        return Result.ok();
    }

    @GetMapping("/{id}/chunks")
    public Result<List<KbDocumentChunk>> chunks(@PathVariable Long id) {
        accessService.requireDocument(id);
        return Result.ok(chunkMapper.selectList(new LambdaQueryWrapper<KbDocumentChunk>()
                .eq(KbDocumentChunk::getDocumentId, id)
                .eq(KbDocumentChunk::getStatus, StatusConstants.NORMAL)
                .orderByAsc(KbDocumentChunk::getChunkIndex)));
    }

    @PostMapping("/{id}/ai-summary")
    public Result<Map<String, Object>> summary(@PathVariable Long id) {
        assertAiAllowed("document-summary");
        KbDocument document = accessService.requireDocumentManage(id);
        String text = llmClient.complete("Summarize this document and extract keywords, audience and reading tips:\n"
                + document.getContentText());
        Map<String, Object> parsed = parseSummary(text);
        document.setSummary(text);
        document.setKeywords((String) parsed.get("keywords"));
        documentMapper.updateById(document);
        return Result.ok(parsed);
    }

    @PostMapping("/{id}/generate-faq")
    public Result<List<KbFaq>> generateFaq(@PathVariable Long id) {
        assertAiAllowed("document-faq");
        KbDocument document = accessService.requireDocumentManage(id);
        String faqText = llmClient.complete("Generate FAQ items from this document:\n" + document.getContentText());
        List<KbFaq> faqs = parseFaqs(faqText).stream().map(item -> {
            KbFaq faq = new KbFaq();
            faq.setSpaceId(document.getSpaceId());
            faq.setDocumentId(document.getId());
            faq.setQuestion(item.question());
            faq.setAnswer(item.answer());
            faq.setTags("AI_GENERATED,DOCUMENT_FAQ");
            faq.setCreateType("AI_GENERATED");
            faq.setStatus(StatusConstants.NORMAL);
            faqMapper.insert(faq);
            return faq;
        }).toList();
        return Result.ok(faqs);
    }

    private Map<String, Object> parseSummary(String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", text);
        result.put("keywords", extractLineValue(text, "keywords", "AI summary,knowledge base,document"));
        result.put("audience", extractLineValue(text, "audience", ""));
        result.put("readingTips", extractLineValue(text, "reading tips", ""));
        return result;
    }

    private void assertAiAllowed(String action) {
        if (aiRateLimitService != null) {
            aiRateLimitService.assertAllowed(action);
        }
    }

    private String extractLineValue(String text, String key, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        String normalizedKey = key.toLowerCase();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            int separator = Math.max(trimmed.indexOf(':'), trimmed.indexOf('\uff1a'));
            if (separator > 0 && trimmed.substring(0, separator).trim().toLowerCase().contains(normalizedKey)) {
                String value = trimmed.substring(separator + 1).trim();
                return value.isBlank() ? fallback : value;
            }
        }
        return fallback;
    }

    private List<FaqItem> parseFaqs(String text) {
        if (text == null || text.isBlank()) {
            return List.of(new FaqItem("What does this document cover?", "No FAQ content was generated."));
        }
        List<FaqItem> items = new ArrayList<>();
        String currentQuestion = null;
        StringBuilder currentAnswer = new StringBuilder();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.matches("(?i)^q\\d*[:：].*")) {
                addFaq(items, currentQuestion, currentAnswer);
                currentQuestion = trimmed.replaceFirst("(?i)^q\\d*[:：]\\s*", "");
                currentAnswer.setLength(0);
            } else if (trimmed.matches("(?i)^a\\d*[:：].*")) {
                currentAnswer.append(trimmed.replaceFirst("(?i)^a\\d*[:：]\\s*", ""));
            } else if (!trimmed.isBlank()) {
                if (!currentAnswer.isEmpty()) {
                    currentAnswer.append('\n');
                }
                currentAnswer.append(trimmed);
            }
        }
        addFaq(items, currentQuestion, currentAnswer);
        if (items.isEmpty()) {
            items.add(new FaqItem("What does this document cover?", text));
        }
        return items.stream().limit(10).toList();
    }

    private void addFaq(List<FaqItem> items, String question, StringBuilder answer) {
        if (question != null && !question.isBlank() && !answer.isEmpty()) {
            items.add(new FaqItem(limit(question, 500), limit(answer.toString(), 5000)));
        }
    }

    private String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record FaqItem(String question, String answer) {
    }
}
