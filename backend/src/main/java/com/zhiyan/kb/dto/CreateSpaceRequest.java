package com.zhiyan.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSpaceRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Pattern(regexp = "[a-zA-Z0-9_-]{2,80}")
    private String code;

    @Size(max = 500)
    private String description;

    @NotNull
    @Positive
    private Long ownerId;

    @Pattern(regexp = "PUBLIC|PRIVATE|DEPARTMENT")
    private String visibility;

    @Positive
    private Long departmentId;
}
