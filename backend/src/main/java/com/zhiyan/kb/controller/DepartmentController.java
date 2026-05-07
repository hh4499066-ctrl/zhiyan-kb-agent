package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.entity.SysDepartment;
import com.zhiyan.kb.entity.SysUser;
import com.zhiyan.kb.mapper.SysDepartmentMapper;
import com.zhiyan.kb.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequireRole(RoleNames.ADMIN)
public class DepartmentController {
    private final SysDepartmentMapper departmentMapper;
    private final SysUserMapper userMapper;

    public DepartmentController(SysDepartmentMapper departmentMapper, SysUserMapper userMapper) {
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    public Result<List<SysDepartment>> list() {
        return Result.ok(departmentMapper.selectList(new LambdaQueryWrapper<SysDepartment>().orderByAsc(SysDepartment::getParentId, SysDepartment::getId)));
    }

    @PostMapping
    public Result<SysDepartment> create(@RequestBody SysDepartment department) {
        department.setStatus(department.getStatus() == null ? "ENABLED" : department.getStatus());
        departmentMapper.insert(department);
        return Result.ok(department);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysDepartment department) {
        department.setId(id);
        departmentMapper.updateById(department);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/{id}/users")
    public Result<List<SysUser>> users(@PathVariable Long id) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDepartmentId, id));
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }
}
