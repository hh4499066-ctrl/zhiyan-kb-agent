package com.zhiyan.kb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReferenceVO {
    private Long documentId;
    private String documentTitle;
    private Long chunkId;
    private String chunkContent;
    private double score;
}
