package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.mapper.KbSpaceMapper;
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
@RequestMapping("/api/spaces")
public class SpaceController {
    private final KbSpaceMapper spaceMapper;
    private final ResourceAccessService accessService;

    public SpaceController(KbSpaceMapper spaceMapper, ResourceAccessService accessService) {
        this.spaceMapper = spaceMapper;
        this.accessService = accessService;
    }

    @GetMapping
    public Result<List<KbSpace>> list(@RequestParam(required = false) String keyword) {
        List<KbSpace> spaces = spaceMapper.selectList(new LambdaQueryWrapper<KbSpace>()
                .like(keyword != null && !keyword.isBlank(), KbSpace::getName, keyword)
                .ne(KbSpace::getStatus, "DELETED")
                .orderByDesc(KbSpace::getCreateTime));
        return Result.ok(spaces.stream().filter(space -> accessService.canAccessSpace(space.getId())).toList());
    }

    @GetMapping("/{id}")
    public Result<KbSpace> detail(@PathVariable Long id) {
        accessService.requireSpaceAccess(id);
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
        accessService.requireSpaceManage(id);
        space.setId(id);
        spaceMapper.updateById(space);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> delete(@PathVariable Long id) {
        accessService.requireSpaceManage(id);
        KbSpace space = new KbSpace();
        space.setId(id);
        space.setStatus("DELETED");
        spaceMapper.updateById(space);
        return Result.ok();
    }
}
