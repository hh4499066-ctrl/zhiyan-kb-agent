package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.kb.ai.LLMClient;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.entity.OnboardingPlan;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.OnboardingPlanMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {
    private final OnboardingPlanMapper planMapper;
    private final KbDocumentMapper documentMapper;
    private final LLMClient llmClient;

    public OnboardingController(OnboardingPlanMapper planMapper, KbDocumentMapper documentMapper, LLMClient llmClient) {
        this.planMapper = planMapper;
        this.documentMapper = documentMapper;
        this.llmClient = llmClient;
    }

    @PostMapping("/generate-plan")
    public Result<OnboardingPlan> generate(@RequestBody Map<String, String> body) {
        String roleType = body.getOrDefault("roleType", "后端");
        List<KbDocument> docs = documentMapper.selectPage(Page.of(1, 6),
                new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getStatus, "NORMAL")).getRecords();
        String planText = llmClient.complete("请为新人生成学习计划，岗位：" + roleType + "，推荐文档：" + docs.stream().map(KbDocument::getTitle).toList());
        OnboardingPlan plan = new OnboardingPlan();
        plan.setUserId(UserContext.userId());
        plan.setRoleType(roleType);
        plan.setTitle(roleType + "新人 7 天学习路径");
        plan.setDescription("AI 根据知识库文档生成的新人入职路径");
        plan.setPlanContent(planText);
        plan.setRecommendedDocuments(String.join("、", docs.stream().map(KbDocument::getTitle).toList()));
        planMapper.insert(plan);
        return Result.ok(plan);
    }

    @GetMapping("/plans")
    public Result<List<OnboardingPlan>> plans() {
        return Result.ok(planMapper.selectList(new LambdaQueryWrapper<OnboardingPlan>()
                .eq(OnboardingPlan::getUserId, UserContext.userId())
                .orderByDesc(OnboardingPlan::getCreateTime)));
    }
}
