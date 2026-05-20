package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatFeedbackRequest {
    @NotNull
    private Boolean helpful;

    @Size(max = 500)
    private String comment;
}
