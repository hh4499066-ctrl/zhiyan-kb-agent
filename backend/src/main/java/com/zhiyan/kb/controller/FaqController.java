package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.dto.CreateFaqRequest;
import com.zhiyan.kb.dto.UpdateFaqRequest;
import com.zhiyan.kb.entity.KbFaq;
import com.zhiyan.kb.mapper.KbFaqMapper;
import com.zhiyan.kb.service.ResourceAccessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
public class FaqController {
    private final KbFaqMapper faqMapper;
    private final ResourceAccessService accessService;

    public FaqController(KbFaqMapper faqMapper, ResourceAccessService accessService) {
        this.faqMapper = faqMapper;
        this.accessService = accessService;
    }

    @GetMapping
    public Result<List<KbFaq>> list(@RequestParam(required = false) Long spaceId) {
        List<Long> accessibleSpaceIds = accessService.accessibleNormalSpaceIds();
        if (accessibleSpaceIds.isEmpty()) {
            return Result.ok(List.of());
        }
        if (spaceId != null && !accessibleSpaceIds.contains(spaceId)) {
            throw new BusinessException(403, "No permission to access this space");
        }
        List<KbFaq> faqs = faqMapper.selectList(new LambdaQueryWrapper<KbFaq>()
                .eq(spaceId != null, KbFaq::getSpaceId, spaceId)
                .in(spaceId == null, KbFaq::getSpaceId, accessibleSpaceIds)
                .eq(KbFaq::getStatus, StatusConstants.NORMAL)
                .orderByDesc(KbFaq::getCreateTime));
        return Result.ok(faqs);
    }

    @PostMapping
    public Result<KbFaq> create(@Valid @RequestBody CreateFaqRequest request) {
        accessService.requireSpaceManage(request.getSpaceId());
        KbFaq faq = new KbFaq();
        faq.setSpaceId(request.getSpaceId());
        faq.setDocumentId(request.getDocumentId());
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setTags(request.getTags());
        faq.setStatus(StatusConstants.NORMAL);
        faq.setCreateType("MANUAL");
        faqMapper.insert(faq);
        return Result.ok(faq);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateFaqRequest request) {
        KbFaq existing = existingFaq(id);
        accessService.requireSpaceManage(existing.getSpaceId());
        KbFaq update = new KbFaq();
        update.setId(id);
        update.setQuestion(request.getQuestion());
        update.setAnswer(request.getAnswer());
        update.setTags(request.getTags());
        faqMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        KbFaq existing = existingFaq(id);
        accessService.requireSpaceManage(existing.getSpaceId());
        KbFaq faq = new KbFaq();
        faq.setId(id);
        faq.setStatus(StatusConstants.DELETED);
        faqMapper.updateById(faq);
        return Result.ok();
    }

    private KbFaq existingFaq(Long id) {
        KbFaq faq = faqMapper.selectById(id);
        if (faq == null || StatusConstants.DELETED.equals(faq.getStatus())) {
            throw new BusinessException(404, "FAQ not found");
        }
        return faq;
    }
}
