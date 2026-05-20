package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMemoryRequest {
    @NotBlank
    @Pattern(regexp = "IDENTITY|PREFERENCE|PROJECT|GENERAL")
    private String memoryType;

    @NotBlank
    @Size(max = 1000)
    private String content;
}
