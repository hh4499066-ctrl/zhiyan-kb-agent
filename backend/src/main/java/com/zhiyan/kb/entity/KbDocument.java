package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("kb_document")
@EqualsAndHashCode(callSuper = true)
public class KbDocument extends BaseEntity {
    @TableId
    private Long id;
    private Long spaceId;
    private String title;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private String contentText;
    private String summary;
    private String keywords;
    private String parseStatus;
    private String vectorStatus;
    private String status;
    private Long uploaderId;
}
