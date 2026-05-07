package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sys_department")
@EqualsAndHashCode(callSuper = true)
public class SysDepartment extends BaseEntity {
    @TableId
    private Long id;
    private String name;
    private Long parentId;
    private Long leaderId;
    private String description;
    private String status;
}
