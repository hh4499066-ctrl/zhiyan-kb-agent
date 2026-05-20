package com.zhiyan.kb.controller;

import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.LoginUser;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.config.TokenUtil;
import com.zhiyan.kb.dto.LoginRequest;
import com.zhiyan.kb.entity.SysUser;
import com.zhiyan.kb.service.AuthService;
import com.zhiyan.kb.service.LoginRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String DEFAULT_DEMO_PASSWORD_SHA256 = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
    private static final Set<String> DEMO_USERNAMES = Set.of("admin", "manager", "zhangsan", "newcomer");

    private final AuthService authService;
    private final TokenUtil tokenUtil;
    private final LoginRateLimitService loginRateLimitService;
    private final boolean demoAccountsEnabled;
    private final Set<String> trustedProxies;

    public AuthController(AuthService authService, TokenUtil tokenUtil,
                          LoginRateLimitService loginRateLimitService,
                          @Value("${zhiyan.demo-accounts-enabled:false}") boolean demoAccountsEnabled,
                          @Value("${zhiyan.auth.trusted-proxies:}") String trustedProxies) {
        this.authService = authService;
        this.tokenUtil = tokenUtil;
        this.loginRateLimitService = loginRateLimitService;
        this.demoAccountsEnabled = demoAccountsEnabled;
        this.trustedProxies = parseTrustedProxies(trustedProxies);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String clientIp = clientIp(servletRequest);
        loginRateLimitService.consumeAttempt(request.getUsername(), clientIp);
        SysUser user = authService.findEnabledUser(request.getUsername());
        if (user == null || !authService.passwordMatches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid username or password");
        }
        if (!demoAccountsEnabled && isDefaultDemoAccount(user)) {
            throw new BusinessException(403, "Default demo account is disabled");
        }
        authService.upgradePasswordIfLegacy(user, request.getPassword());
        loginRateLimitService.clear(request.getUsername(), clientIp);
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

    private boolean isDefaultDemoAccount(SysUser user) {
        return DEMO_USERNAMES.contains(user.getUsername())
                && (DEFAULT_DEMO_PASSWORD_SHA256.equalsIgnoreCase(user.getPassword())
                || authService.passwordMatches("123456", user.getPassword()));
    }

    private String clientIp(HttpServletRequest request) {
        if (trustedProxies.contains(request.getRemoteAddr())) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private Set<String> parseTrustedProxies(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
