package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatAskRequest {
    private Long spaceId;
    private String sessionId;
    @NotBlank
    private String question;
    private Boolean useMemory = true;
    private Integer topK = 5;
    private String model;
}
