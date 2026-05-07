package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.UnresolvedQuestion;
import com.zhiyan.kb.mapper.UnresolvedQuestionMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/unresolved")
public class UnresolvedController {
    private final UnresolvedQuestionMapper mapper;

    public UnresolvedController(UnresolvedQuestionMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public Result<List<UnresolvedQuestion>> list(@RequestParam(required = false) String status, @RequestParam(required = false) Long spaceId) {
        return Result.ok(mapper.selectList(new LambdaQueryWrapper<UnresolvedQuestion>()
                .eq(status != null && !status.isBlank(), UnresolvedQuestion::getStatus, status)
                .eq(spaceId != null, UnresolvedQuestion::getSpaceId, spaceId)
                .orderByDesc(UnresolvedQuestion::getCreateTime)));
    }

    @PutMapping("/{id}/resolve")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> resolve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UnresolvedQuestion q = new UnresolvedQuestion();
        q.setId(id);
        q.setStatus("RESOLVED");
        q.setResolverId(UserContext.userId());
        q.setResolveNote(String.valueOf(body.getOrDefault("resolveNote", "已补充知识")));
        if (body.get("relatedDocumentId") != null) {
            q.setRelatedDocumentId(Long.valueOf(String.valueOf(body.get("relatedDocumentId"))));
        }
        mapper.updateById(q);
        return Result.ok();
    }

    @PutMapping("/{id}/ignore")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> ignore(@PathVariable Long id) {
        UnresolvedQuestion q = new UnresolvedQuestion();
        q.setId(id);
        q.setStatus("IGNORED");
        mapper.updateById(q);
        return Result.ok();
    }
}
