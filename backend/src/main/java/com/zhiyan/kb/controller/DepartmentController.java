package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.dto.CreateDepartmentRequest;
import com.zhiyan.kb.dto.UpdateDepartmentRequest;
import com.zhiyan.kb.entity.SysDepartment;
import com.zhiyan.kb.entity.SysUser;
import com.zhiyan.kb.mapper.SysDepartmentMapper;
import com.zhiyan.kb.mapper.SysUserMapper;
import jakarta.validation.Valid;
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
    public Result<SysDepartment> create(@Valid @RequestBody CreateDepartmentRequest request) {
        SysDepartment department = new SysDepartment();
        department.setName(request.getName());
        department.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        department.setLeaderId(request.getLeaderId());
        department.setDescription(request.getDescription());
        department.setStatus(request.getStatus() == null ? StatusConstants.ENABLED : request.getStatus());
        departmentMapper.insert(department);
        return Result.ok(department);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateDepartmentRequest request) {
        SysDepartment update = new SysDepartment();
        update.setId(id);
        update.setName(request.getName());
        update.setParentId(request.getParentId());
        update.setLeaderId(request.getLeaderId());
        update.setDescription(request.getDescription());
        update.setStatus(request.getStatus());
        departmentMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long users = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDepartmentId, id));
        if (users != null && users > 0) {
            throw new BusinessException(409, "Department has users and cannot be deleted");
        }
        SysDepartment update = new SysDepartment();
        update.setId(id);
        update.setStatus(StatusConstants.DELETED);
        departmentMapper.updateById(update);
        return Result.ok();
    }

    @GetMapping("/{id}/users")
    public Result<List<SysUser>> users(@PathVariable Long id) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDepartmentId, id));
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }
}
