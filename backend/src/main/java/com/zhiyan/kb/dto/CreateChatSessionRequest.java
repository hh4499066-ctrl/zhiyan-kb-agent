package com.zhiyan.kb.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChatSessionRequest {
    @Positive
    private Long spaceId;

    @Size(max = 120)
    private String title;
}
