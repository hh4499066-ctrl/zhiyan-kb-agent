package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.entity.KbFaq;
import com.zhiyan.kb.mapper.KbFaqMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
public class FaqController {
    private final KbFaqMapper faqMapper;

    public FaqController(KbFaqMapper faqMapper) {
        this.faqMapper = faqMapper;
    }

    @GetMapping
    public Result<List<KbFaq>> list(@RequestParam(required = false) Long spaceId) {
        return Result.ok(faqMapper.selectList(new LambdaQueryWrapper<KbFaq>()
                .eq(spaceId != null, KbFaq::getSpaceId, spaceId)
                .eq(KbFaq::getStatus, "NORMAL")
                .orderByDesc(KbFaq::getCreateTime)));
    }

    @PostMapping
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<KbFaq> create(@RequestBody KbFaq faq) {
        faq.setStatus(faq.getStatus() == null ? "NORMAL" : faq.getStatus());
        faq.setCreateType(faq.getCreateType() == null ? "MANUAL" : faq.getCreateType());
        faqMapper.insert(faq);
        return Result.ok(faq);
    }

    @PutMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> update(@PathVariable Long id, @RequestBody KbFaq faq) {
        faq.setId(id);
        faqMapper.updateById(faq);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> delete(@PathVariable Long id) {
        KbFaq faq = new KbFaq();
        faq.setId(id);
        faq.setStatus("DELETED");
        faqMapper.updateById(faq);
        return Result.ok();
    }
}
