package com.zhiyan.kb.rag;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VectorStoreItem {
    private Long chunkId;
    private Long documentId;
    private Long spaceId;
    private String documentTitle;
    private String content;
    private double[] vector;
}
