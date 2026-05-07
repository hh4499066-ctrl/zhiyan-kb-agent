package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.kb.common.PageResult;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.entity.OperationLog;
import com.zhiyan.kb.mapper.OperationLogMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operation-logs")
@RequireRole(RoleNames.ADMIN)
public class OperationLogController {
    private final OperationLogMapper operationLogMapper;

    public OperationLogController(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @GetMapping
    public Result<PageResult<OperationLog>> list(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "30") long size,
                                                 @RequestParam(required = false) String moduleName) {
        Page<OperationLog> result = operationLogMapper.selectPage(Page.of(page, size), new LambdaQueryWrapper<OperationLog>()
                .eq(moduleName != null && !moduleName.isBlank(), OperationLog::getModuleName, moduleName)
                .orderByDesc(OperationLog::getCreateTime));
        return Result.ok(new PageResult<>(result.getTotal(), page, size, result.getRecords()));
    }
}
