package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.kb.common.PageResult;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.dto.CreateUserRequest;
import com.zhiyan.kb.dto.ResetPasswordRequest;
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
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
                .like(keyword != null && !keyword.isBlank(), SysUser::getUsername, keyword)
                .or(keyword != null && !keyword.isBlank()).like(keyword != null && !keyword.isBlank(), SysUser::getRealName, keyword)
                .orderByDesc(SysUser::getCreateTime);
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
        user.setStatus(request.getStatus() == null ? "ENABLED" : request.getStatus());
        userMapper.insert(user);
        user.setPassword(null);
        return Result.ok(user);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
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
        userMapper.deleteById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestParam String status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
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
}
