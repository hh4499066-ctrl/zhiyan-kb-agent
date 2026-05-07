package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@TableName("chat_record")
@EqualsAndHashCode(callSuper = true)
public class ChatRecord extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private Long spaceId;
    private String sessionId;
    private String question;
    private String rewrittenQuestion;
    private String answer;
    private String referencesJson;
    private BigDecimal confidence;
    private Boolean unresolved;
    private Boolean favorite;
}
