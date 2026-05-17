package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {
    @NotBlank
    @Pattern(regexp = "ENABLED|DISABLED")
    private String status;
}
