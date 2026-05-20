package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFaqRequest {
    @NotNull
    @Positive
    private Long spaceId;

    @Positive
    private Long documentId;

    @NotBlank
    @Size(max = 500)
    private String question;

    @NotBlank
    @Size(max = 5000)
    private String answer;

    @Size(max = 255)
    private String tags;
}
