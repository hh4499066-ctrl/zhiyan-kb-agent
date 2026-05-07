package com.zhiyan.kb.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginUser implements Serializable {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private Long departmentId;
}
