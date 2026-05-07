package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("operation_log")
@EqualsAndHashCode(callSuper = true)
public class OperationLog extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String moduleName;
    private String operation;
    private String detail;
    private String ip;
}
