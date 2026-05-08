package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.UnresolvedQuestion;
import com.zhiyan.kb.mapper.UnresolvedQuestionMapper;
import com.zhiyan.kb.service.ResourceAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/unresolved")
public class UnresolvedController {
    private final UnresolvedQuestionMapper mapper;
    private final ResourceAccessService accessService;

    public UnresolvedController(UnresolvedQuestionMapper mapper, ResourceAccessService accessService) {
        this.mapper = mapper;
        this.accessService = accessService;
    }

    @GetMapping
    public Result<List<UnresolvedQuestion>> list(@RequestParam(required = false) String status,
                                                 @RequestParam(required = false) Long spaceId) {
        List<UnresolvedQuestion> questions = mapper.selectList(new LambdaQueryWrapper<UnresolvedQuestion>()
                .eq(status != null && !status.isBlank(), UnresolvedQuestion::getStatus, status)
                .eq(spaceId != null, UnresolvedQuestion::getSpaceId, spaceId)
                .orderByDesc(UnresolvedQuestion::getCreateTime));
        if (accessService.isAdmin() || accessService.isKbManager()) {
            return Result.ok(questions.stream().filter(q -> accessService.canAccessSpace(q.getSpaceId())).toList());
        }
        return Result.ok(questions.stream()
                .filter(q -> UserContext.userId().equals(q.getUserId()))
                .toList());
    }

    @PutMapping("/{id}/resolve")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> resolve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        accessService.requireUnresolvedQuestionManage(id);
        UnresolvedQuestion question = new UnresolvedQuestion();
        question.setId(id);
        question.setStatus("RESOLVED");
        question.setResolverId(UserContext.userId());
        question.setResolveNote(String.valueOf(body.getOrDefault("resolveNote", "Resolved")));
        if (body.get("relatedDocumentId") != null) {
            question.setRelatedDocumentId(Long.valueOf(String.valueOf(body.get("relatedDocumentId"))));
        }
        mapper.updateById(question);
        return Result.ok();
    }

    @PutMapping("/{id}/ignore")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> ignore(@PathVariable Long id) {
        accessService.requireUnresolvedQuestionManage(id);
        UnresolvedQuestion question = new UnresolvedQuestion();
        question.setId(id);
        question.setStatus("IGNORED");
        mapper.updateById(question);
        return Result.ok();
    }
}
