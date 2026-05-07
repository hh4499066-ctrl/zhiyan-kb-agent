package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("unresolved_question")
@EqualsAndHashCode(callSuper = true)
public class UnresolvedQuestion extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private Long spaceId;
    private String question;
    private String rewrittenQuestion;
    private String reason;
    private String status;
    private Long resolverId;
    private String resolveNote;
    private Long relatedDocumentId;
}
