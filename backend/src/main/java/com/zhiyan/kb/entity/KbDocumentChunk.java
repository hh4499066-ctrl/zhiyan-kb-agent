package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("kb_document_chunk")
@EqualsAndHashCode(callSuper = true)
public class KbDocumentChunk extends BaseEntity {
    @TableId
    private Long id;
    private Long documentId;
    private Long spaceId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private String embeddingText;
    private String vectorId;
    private String status;
}
