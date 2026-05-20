package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSpaceRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @Pattern(regexp = "PUBLIC|PRIVATE|DEPARTMENT")
    private String visibility;

    @Positive
    private Long departmentId;
}
