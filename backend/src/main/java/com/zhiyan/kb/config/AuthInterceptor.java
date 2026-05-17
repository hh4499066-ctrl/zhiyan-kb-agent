package com.zhiyan.kb.config;

import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.LoginUser;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final TokenUtil tokenUtil;

    public AuthInterceptor(TokenUtil tokenUtil) {
        this.tokenUtil = tokenUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : request.getHeader("X-Token");
        LoginUser loginUser = token == null ? null : tokenUtil.getLoginUser(token);
        if (loginUser == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        UserContext.set(loginUser);
        try {
            checkRole(handler, loginUser);
        } catch (RuntimeException ex) {
            UserContext.clear();
            throw ex;
        }
        return true;
    }

    private void checkRole(Object handler, LoginUser loginUser) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null || requireRole.value().length == 0) {
            return;
        }
        boolean allowed = Arrays.asList(requireRole.value()).contains(loginUser.getRole());
        if (!allowed) {
            throw new BusinessException(403, "无权限访问该接口");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
