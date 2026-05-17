package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatAskRequest {
    private Long spaceId;
    private String sessionId;
    @NotBlank
    @Size(max = 2000)
    private String question;
    private Boolean useMemory = true;
    @Min(1)
    @Max(20)
    private Integer topK = 5;
    @Pattern(regexp = "^$|deepseek-v4-flash|deepseek-v4-pro")
    private String model;
}
