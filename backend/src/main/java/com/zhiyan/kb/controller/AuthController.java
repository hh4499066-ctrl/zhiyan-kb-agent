package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.crypto.SecureUtil;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.LoginUser;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.config.TokenUtil;
import com.zhiyan.kb.dto.LoginRequest;
import com.zhiyan.kb.entity.SysUser;
import com.zhiyan.kb.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenUtil tokenUtil;

    public AuthController(SysUserMapper userMapper, BCryptPasswordEncoder passwordEncoder, TokenUtil tokenUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenUtil = tokenUtil;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !"ENABLED".equals(user.getStatus()) || !passwordMatches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        LoginUser loginUser = toLoginUser(user);
        return Result.ok(Map.of("token", tokenUtil.createToken(loginUser), "user", loginUser));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            tokenUtil.removeToken(header.substring(7));
        }
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<LoginUser> me() {
        return Result.ok(UserContext.get());
    }

    private LoginUser toLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setRole(user.getRole());
        loginUser.setDepartmentId(user.getDepartmentId());
        return loginUser;
    }

    private boolean passwordMatches(String raw, String stored) {
        if (stored == null) {
            return false;
        }
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(raw, stored);
        }
        return SecureUtil.sha256(raw).equalsIgnoreCase(stored);
    }
}
