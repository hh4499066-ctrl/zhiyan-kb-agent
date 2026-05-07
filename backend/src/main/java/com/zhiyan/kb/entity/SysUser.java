package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sys_user")
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {
    @TableId
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private String role;
    private Long departmentId;
    private String status;
}
