package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("chat_feedback")
@EqualsAndHashCode(callSuper = true)
public class ChatFeedback extends BaseEntity {
    @TableId
    private Long id;
    private Long recordId;
    private Long userId;
    private Boolean helpful;
    private String comment;
}
