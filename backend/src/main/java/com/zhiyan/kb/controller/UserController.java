package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.PageResult;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.dto.CreateUserRequest;
import com.zhiyan.kb.dto.ResetPasswordRequest;
import com.zhiyan.kb.dto.UpdateUserStatusRequest;
import com.zhiyan.kb.dto.UpdateUserRequest;
import com.zhiyan.kb.entity.SysUser;
import com.zhiyan.kb.mapper.SysUserMapper;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequireRole(RoleNames.ADMIN)
public class UserController {
    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserController(SysUserMapper userMapper, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Result<PageResult<SysUser>> list(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size, @RequestParam(required = false) String keyword) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }
        qw.ne(SysUser::getStatus, StatusConstants.DELETED).orderByDesc(SysUser::getCreateTime);
        Page<SysUser> p = userMapper.selectPage(Page.of(page, size), qw);
        p.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(new PageResult<>(p.getTotal(), page, size, p.getRecords()));
    }

    @PostMapping
    public Result<SysUser> create(@Valid @RequestBody CreateUserRequest request) {
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(blankToNull(request.getEmail()));
        user.setPhone(blankToNull(request.getPhone()));
        user.setRole(request.getRole());
        user.setDepartmentId(request.getDepartmentId());
        user.setStatus(request.getStatus() == null ? StatusConstants.ENABLED : request.getStatus());
        userMapper.insert(user);
        user.setPassword(null);
        return Result.ok(user);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        guardSelfPrivilegeChange(id, request.getRole(), request.getStatus());
        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(request.getRealName());
        user.setEmail(blankToNull(request.getEmail()));
        user.setPhone(blankToNull(request.getPhone()));
        user.setRole(request.getRole());
        user.setDepartmentId(request.getDepartmentId());
        user.setStatus(request.getStatus());
        userMapper.updateById(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (UserContext.userId().equals(id)) {
            throw new BusinessException(400, "Cannot delete current user");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(StatusConstants.DELETED);
        userMapper.updateById(user);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        guardSelfPrivilegeChange(id, null, request.getStatus());
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(request.getStatus());
        userMapper.updateById(user);
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> reset(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.updateById(user);
        return Result.ok();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void guardSelfPrivilegeChange(Long id, String role, String status) {
        if (!UserContext.userId().equals(id)) {
            return;
        }
        if (role != null && !RoleNames.ADMIN.equals(role)) {
            throw new BusinessException(400, "Cannot downgrade current administrator");
        }
        if (StatusConstants.DISABLED.equals(status) || StatusConstants.DELETED.equals(status)) {
            throw new BusinessException(400, "Cannot disable current user");
        }
    }
}
