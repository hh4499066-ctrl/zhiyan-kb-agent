package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {
    private final KbSpaceMapper spaceMapper;

    public SpaceController(KbSpaceMapper spaceMapper) {
        this.spaceMapper = spaceMapper;
    }

    @GetMapping
    public Result<List<KbSpace>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(spaceMapper.selectList(new LambdaQueryWrapper<KbSpace>()
                .like(keyword != null && !keyword.isBlank(), KbSpace::getName, keyword)
                .ne(KbSpace::getStatus, "DELETED")
                .orderByDesc(KbSpace::getCreateTime)));
    }

    @GetMapping("/{id}")
    public Result<KbSpace> detail(@PathVariable Long id) {
        return Result.ok(spaceMapper.selectById(id));
    }

    @PostMapping
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<KbSpace> create(@RequestBody KbSpace space) {
        space.setStatus(space.getStatus() == null ? "NORMAL" : space.getStatus());
        space.setDocumentCount(space.getDocumentCount() == null ? 0 : space.getDocumentCount());
        space.setQaCount(space.getQaCount() == null ? 0 : space.getQaCount());
        spaceMapper.insert(space);
        return Result.ok(space);
    }

    @PutMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> update(@PathVariable Long id, @RequestBody KbSpace space) {
        space.setId(id);
        spaceMapper.updateById(space);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> delete(@PathVariable Long id) {
        KbSpace space = new KbSpace();
        space.setId(id);
        space.setStatus("DELETED");
        spaceMapper.updateById(space);
        return Result.ok();
    }
}
