package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OnboardingGenerateRequest {
    @NotBlank
    @Size(max = 30)
    private String roleType;
}
