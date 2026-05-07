package com.zhiyan.kb.rag;

import lombok.Data;

@Data
public class RetrievalResult {
    private Long chunkId;
    private Long documentId;
    private Long spaceId;
    private String documentTitle;
    private String content;
    private double keywordScore;
    private double vectorScore;
    private double finalScore;
}
