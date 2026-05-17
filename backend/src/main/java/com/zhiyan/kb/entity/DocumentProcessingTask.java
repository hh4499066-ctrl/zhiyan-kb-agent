package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("document_processing_task")
@EqualsAndHashCode(callSuper = true)
public class DocumentProcessingTask extends BaseEntity {
    @TableId
    private Long id;
    private Long documentId;
    private String fileUrl;
    private String fileType;
    private String status;
    private Integer retryCount;
    private Integer maxRetries;
    private String lastError;
    private LocalDateTime lastProcessTime;
}
