package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.PageResult;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.dto.CreateSpaceRequest;
import com.zhiyan.kb.dto.UpdateSpaceRequest;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.mapper.KbSpaceMapper;
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
@RequestMapping("/api/spaces")
public class SpaceController {
    private final KbSpaceMapper spaceMapper;
    private final ResourceAccessService accessService;

    public SpaceController(KbSpaceMapper spaceMapper, ResourceAccessService accessService) {
        this.spaceMapper = spaceMapper;
        this.accessService = accessService;
    }

    @GetMapping
    public Result<PageResult<KbSpace>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "100") long size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        List<Long> accessibleSpaceIds = accessService.accessibleNormalSpaceIds();
        if (accessibleSpaceIds.isEmpty()) {
            return Result.ok(new PageResult<>(0, page, size, List.of()));
        }
        Page<KbSpace> result = spaceMapper.selectPage(Page.of(page, size), new LambdaQueryWrapper<KbSpace>()
                .in(KbSpace::getId, accessibleSpaceIds)
                .like(keyword != null && !keyword.isBlank(), KbSpace::getName, keyword)
                .ne(KbSpace::getStatus, StatusConstants.DELETED)
                .orderByDesc(KbSpace::getCreateTime));
        return Result.ok(new PageResult<>(result.getTotal(), page, size, result.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<KbSpace> detail(@PathVariable Long id) {
        accessService.requireSpaceAccess(id);
        KbSpace space = spaceMapper.selectById(id);
        if (space == null || StatusConstants.DELETED.equals(space.getStatus())) {
            throw new BusinessException(404, "Space not found");
        }
        return Result.ok(space);
    }

    @PostMapping
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<KbSpace> create(@Valid @RequestBody CreateSpaceRequest request) {
        KbSpace space = new KbSpace();
        space.setName(request.getName());
        space.setCode(request.getCode());
        space.setDescription(request.getDescription());
        space.setOwnerId(request.getOwnerId());
        space.setVisibility(request.getVisibility() == null ? "PUBLIC" : request.getVisibility());
        space.setDepartmentId(request.getDepartmentId());
        space.setStatus(StatusConstants.NORMAL);
        space.setDocumentCount(0);
        space.setQaCount(0);
        spaceMapper.insert(space);
        return Result.ok(space);
    }

    @PutMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateSpaceRequest request) {
        accessService.requireSpaceManage(id);
        KbSpace update = new KbSpace();
        update.setId(id);
        update.setName(request.getName());
        update.setDescription(request.getDescription());
        update.setVisibility(request.getVisibility());
        update.setDepartmentId(request.getDepartmentId());
        spaceMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleNames.ADMIN, RoleNames.KB_MANAGER})
    public Result<Void> delete(@PathVariable Long id) {
        accessService.requireSpaceManage(id);
        KbSpace space = new KbSpace();
        space.setId(id);
        space.setStatus(StatusConstants.DELETED);
        spaceMapper.updateById(space);
        return Result.ok();
    }
}
