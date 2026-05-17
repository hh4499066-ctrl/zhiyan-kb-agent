package com.zhiyan.kb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    @Size(max = 50)
    private String realName;

    @Email
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^$|^1\\d{10}$")
    private String phone;

    @NotBlank
    @Pattern(regexp = "admin|kb_manager|employee|newcomer")
    private String role;

    @NotNull
    private Long departmentId;

    @Pattern(regexp = "ENABLED|DISABLED")
    private String status;
}
