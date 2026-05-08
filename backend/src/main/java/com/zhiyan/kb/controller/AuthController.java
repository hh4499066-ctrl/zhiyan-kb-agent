package com.zhiyan.kb.controller;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String DEFAULT_DEMO_PASSWORD_SHA256 = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
    private static final Set<String> DEMO_USERNAMES = Set.of("admin", "manager", "zhangsan", "newcomer");

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenUtil tokenUtil;
    private final boolean demoAccountsEnabled;

    public AuthController(SysUserMapper userMapper, BCryptPasswordEncoder passwordEncoder, TokenUtil tokenUtil,
                          @Value("${zhiyan.demo-accounts-enabled:true}") boolean demoAccountsEnabled) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenUtil = tokenUtil;
        this.demoAccountsEnabled = demoAccountsEnabled;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !"ENABLED".equals(user.getStatus()) || !passwordMatches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid username or password");
        }
        if (!demoAccountsEnabled && isDefaultDemoAccount(user)) {
            throw new BusinessException(403, "Default demo account is disabled");
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

    private boolean isDefaultDemoAccount(SysUser user) {
        return DEMO_USERNAMES.contains(user.getUsername())
                && DEFAULT_DEMO_PASSWORD_SHA256.equalsIgnoreCase(user.getPassword());
    }
}
