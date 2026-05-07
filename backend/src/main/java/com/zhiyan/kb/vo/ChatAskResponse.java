package com.zhiyan.kb.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChatAskResponse {
    private String answer;
    private String rewrittenQuestion;
    private List<ChatReferenceVO> references;
    private double confidence;
    private String sessionId;
    private Long spaceId;
    private Long recordId;
    private boolean unresolved;
}
