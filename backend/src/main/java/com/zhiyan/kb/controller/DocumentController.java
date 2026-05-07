package com.zhiyan.kb.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final KbDocumentMapper documentMapper;
    private final KbDocumentChunkMapper chunkMapper;
    private final KbFaqMapper faqMapper;
    private final KbSpaceMapper spaceMapper;
    private final DocumentParseService parseService;
    private final ChunkService chunkService;
    private final LLMClient llmClient;
    private final String uploadDir;

    public DocumentController(KbDocumentMapper documentMapper, KbDocumentChunkMapper chunkMapper, KbFaqMapper faqMapper, KbSpaceMapper spaceMapper,
                              DocumentParseService parseService, ChunkService chunkService, LLMClient llmClient,
                              @Value("${zhiyan.upload-dir:uploads}") String uploadDir) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.faqMapper = faqMapper;
        this.spaceMapper = spaceMapper;
        this.parseService = parseService;
        this.chunkService = chunkService;
        this.llmClient = llmClient;
        this.uploadDir = uploadDir;
    }

    @GetMapping
    public Result<List<KbDocument>> list(@RequestParam(required = false) Long spaceId, @RequestParam(required = false) String keyword) {
        return Result.ok(documentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .eq(spaceId != null, KbDocument::getSpaceId, spaceId)
                .like(keyword != null && !keyword.isBlank(), KbDocument::getTitle, keyword)
                .eq(KbDocument::getStatus, "NORMAL")
                .orderByDesc(KbDocument::getCreateTime)));
    }

    @PostMapping("/upload")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<KbDocument> upload(@RequestParam Long spaceId, @RequestParam(required = false) String title, @RequestPart("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }
        String ext = FileUtil.extName(file.getOriginalFilename());
        String name = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        File target = FileUtil.file(uploadDir, name);
        FileUtil.mkParentDirs(target);
        file.transferTo(target);

        KbDocument document = new KbDocument();
        document.setSpaceId(spaceId);
        document.setTitle(title == null || title.isBlank() ? FileUtil.mainName(file.getOriginalFilename()) : title);
        document.setOriginalFilename(file.getOriginalFilename());
        document.setFileType(ext);
        document.setFileSize(file.getSize());
        document.setFileUrl(target.getPath());
        document.setParseStatus("PARSING");
        document.setVectorStatus("PROCESSING");
        document.setStatus("NORMAL");
        document.setUploaderId(UserContext.userId());
        document.setContentText(parseService.parse(target, ext));
        documentMapper.insert(document);
        chunkService.rebuildChunks(document);
        document.setParseStatus("SUCCESS");
        document.setVectorStatus("SUCCESS");
        documentMapper.updateById(document);
        spaceMapper.update(null, new LambdaUpdateWrapper<KbSpace>().eq(KbSpace::getId, spaceId).setSql("document_count = document_count + 1"));
        return Result.ok(document);
    }

    @GetMapping("/{id}")
    public Result<KbDocument> detail(@PathVariable Long id) {
        return Result.ok(documentMapper.selectById(id));
    }

    @PutMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> update(@PathVariable Long id, @RequestBody KbDocument document) {
        document.setId(id);
        documentMapper.updateById(document);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> delete(@PathVariable Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document != null) {
            document.setStatus("DELETED");
            documentMapper.updateById(document);
            chunkMapper.selectList(new LambdaQueryWrapper<KbDocumentChunk>().eq(KbDocumentChunk::getDocumentId, id))
                    .forEach(c -> {
                        c.setStatus("DISABLED");
                        chunkMapper.updateById(c);
                    });
        }
        return Result.ok();
    }

    @PostMapping("/{id}/parse")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> parse(@PathVariable Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        chunkService.rebuildChunks(document);
        document.setParseStatus("SUCCESS");
        document.setVectorStatus("SUCCESS");
        documentMapper.updateById(document);
        return Result.ok();
    }

    @GetMapping("/{id}/chunks")
    public Result<List<KbDocumentChunk>> chunks(@PathVariable Long id) {
        return Result.ok(chunkMapper.selectList(new LambdaQueryWrapper<KbDocumentChunk>()
                .eq(KbDocumentChunk::getDocumentId, id)
                .eq(KbDocumentChunk::getStatus, "NORMAL")
                .orderByAsc(KbDocumentChunk::getChunkIndex)));
    }

    @PostMapping("/{id}/ai-summary")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Map<String, Object>> summary(@PathVariable Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        String text = llmClient.complete("请总结文档并提取关键词、适用人群、阅读建议：\n" + document.getContentText());
        document.setSummary(text);
        document.setKeywords("研发规范,AI问答,知识库,新人培训");
        documentMapper.updateById(document);
        return Result.ok(Map.of("summary", text, "keywords", document.getKeywords(), "audience", "新人 / 后端 / 前端 / 测试 / 运维", "readingTips", "先看摘要，再看步骤，最后结合 FAQ 复盘"));
    }

    @PostMapping("/{id}/generate-faq")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<List<KbFaq>> generateFaq(@PathVariable Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        String faqText = llmClient.complete("请根据文档生成FAQ：\n" + document.getContentText());
        KbFaq faq = new KbFaq();
        faq.setSpaceId(document.getSpaceId());
        faq.setDocumentId(document.getId());
        faq.setQuestion("这篇文档主要解决什么问题？");
        faq.setAnswer(faqText);
        faq.setTags("AI生成,文档FAQ");
        faq.setCreateType("AI_GENERATED");
        faq.setStatus("NORMAL");
        faqMapper.insert(faq);
        return Result.ok(List.of(faq));
    }
}
