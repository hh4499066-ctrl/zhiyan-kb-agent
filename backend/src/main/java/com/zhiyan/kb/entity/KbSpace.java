package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("kb_space")
@EqualsAndHashCode(callSuper = true)
public class KbSpace extends BaseEntity {
    @TableId
    private Long id;
    private String name;
    private String code;
    private String description;
    private Long ownerId;
    private String visibility;
    private Long departmentId;
    private String status;
    private Integer documentCount;
    private Integer qaCount;
}
