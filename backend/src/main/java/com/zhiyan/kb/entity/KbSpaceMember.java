package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("kb_space_member")
@EqualsAndHashCode(callSuper = true)
public class KbSpaceMember extends BaseEntity {
    @TableId
    private Long id;
    private Long spaceId;
    private Long userId;
    private String memberRole;
    private String status;
}
