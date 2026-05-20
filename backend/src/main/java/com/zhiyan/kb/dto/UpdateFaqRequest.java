package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateFaqRequest {
    @NotBlank
    @Size(max = 500)
    private String question;

    @NotBlank
    @Size(max = 5000)
    private String answer;

    @Size(max = 255)
    private String tags;
}
