package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("kb_faq")
@EqualsAndHashCode(callSuper = true)
public class KbFaq extends BaseEntity {
    @TableId
    private Long id;
    private Long spaceId;
    private Long documentId;
    private String question;
    private String answer;
    private String tags;
    private String createType;
    private String status;
}
