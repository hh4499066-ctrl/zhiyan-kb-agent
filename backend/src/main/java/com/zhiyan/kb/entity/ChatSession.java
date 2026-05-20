package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("chat_session")
@EqualsAndHashCode(callSuper = true)
public class ChatSession extends BaseEntity {
    @TableId
    private String id;
    private Long userId;
    private Long spaceId;
    private String title;
    private String status;
}
