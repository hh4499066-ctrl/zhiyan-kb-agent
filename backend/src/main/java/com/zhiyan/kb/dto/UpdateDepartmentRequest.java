package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDepartmentRequest {
    @NotBlank
    @Size(max = 80)
    private String name;

    @PositiveOrZero
    private Long parentId;

    private Long leaderId;

    @Size(max = 255)
    private String description;

    @Pattern(regexp = "ENABLED|DISABLED")
    private String status;
}
