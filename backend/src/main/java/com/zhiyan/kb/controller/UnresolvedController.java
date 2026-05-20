package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.dto.ResolveUnresolvedRequest;
import com.zhiyan.kb.entity.UnresolvedQuestion;
import com.zhiyan.kb.mapper.UnresolvedQuestionMapper;
import com.zhiyan.kb.service.ResourceAccessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        LambdaQueryWrapper<UnresolvedQuestion> query = new LambdaQueryWrapper<UnresolvedQuestion>()
                .eq(status != null && !status.isBlank(), UnresolvedQuestion::getStatus, status)
                .eq(spaceId != null, UnresolvedQuestion::getSpaceId, spaceId)
                .orderByDesc(UnresolvedQuestion::getCreateTime);
        if (accessService.isAdmin() || accessService.isKbManager()) {
            List<Long> accessibleSpaceIds = accessService.accessibleNormalSpaceIds();
            if (accessibleSpaceIds.isEmpty()) {
                return Result.ok(List.of());
            }
            query.in(UnresolvedQuestion::getSpaceId, accessibleSpaceIds);
        } else {
            query.eq(UnresolvedQuestion::getUserId, UserContext.userId());
        }
        return Result.ok(mapper.selectList(query));
    }

    @PutMapping("/{id}/resolve")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> resolve(@PathVariable Long id, @Valid @RequestBody ResolveUnresolvedRequest request) {
        accessService.requireUnresolvedQuestionManage(id);
        UnresolvedQuestion question = new UnresolvedQuestion();
        question.setId(id);
        question.setStatus(StatusConstants.RESOLVED);
        question.setResolverId(UserContext.userId());
        question.setResolveNote(request.getResolveNote() == null || request.getResolveNote().isBlank()
                ? "Resolved" : request.getResolveNote());
        question.setRelatedDocumentId(request.getRelatedDocumentId());
        mapper.updateById(question);
        return Result.ok();
    }

    @PutMapping("/{id}/ignore")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> ignore(@PathVariable Long id) {
        accessService.requireUnresolvedQuestionManage(id);
        UnresolvedQuestion question = new UnresolvedQuestion();
        question.setId(id);
        question.setStatus(StatusConstants.IGNORED);
        mapper.updateById(question);
        return Result.ok();
    }
}
