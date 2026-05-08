package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.entity.KbFaq;
import com.zhiyan.kb.mapper.KbFaqMapper;
import com.zhiyan.kb.service.ResourceAccessService;
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
        List<KbFaq> faqs = faqMapper.selectList(new LambdaQueryWrapper<KbFaq>()
                .eq(spaceId != null, KbFaq::getSpaceId, spaceId)
                .eq(KbFaq::getStatus, "NORMAL")
                .orderByDesc(KbFaq::getCreateTime));
        return Result.ok(faqs.stream().filter(faq -> accessService.canAccessSpace(faq.getSpaceId())).toList());
    }

    @PostMapping
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<KbFaq> create(@RequestBody KbFaq faq) {
        accessService.requireSpaceManage(faq.getSpaceId());
        faq.setStatus(faq.getStatus() == null ? "NORMAL" : faq.getStatus());
        faq.setCreateType(faq.getCreateType() == null ? "MANUAL" : faq.getCreateType());
        faqMapper.insert(faq);
        return Result.ok(faq);
    }

    @PutMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> update(@PathVariable Long id, @RequestBody KbFaq faq) {
        KbFaq existing = existingFaq(id);
        accessService.requireSpaceManage(existing.getSpaceId());
        faq.setId(id);
        faqMapper.updateById(faq);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> delete(@PathVariable Long id) {
        KbFaq existing = existingFaq(id);
        accessService.requireSpaceManage(existing.getSpaceId());
        KbFaq faq = new KbFaq();
        faq.setId(id);
        faq.setStatus("DELETED");
        faqMapper.updateById(faq);
        return Result.ok();
    }

    private KbFaq existingFaq(Long id) {
        KbFaq faq = faqMapper.selectById(id);
        if (faq == null || "DELETED".equals(faq.getStatus())) {
            throw new BusinessException(404, "FAQ not found");
        }
        return faq;
    }
}
