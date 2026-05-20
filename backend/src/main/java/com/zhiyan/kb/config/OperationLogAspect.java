package com.zhiyan.kb.config;

import cn.hutool.json.JSONUtil;
import com.zhiyan.kb.common.LoginUser;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.OperationLog;
import com.zhiyan.kb.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Aspect
@Component
public class OperationLogAspect {
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");
    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of("password", "token", "authorization", "apiKey", "api_key");
    private final OperationLogMapper operationLogMapper;

    public OperationLogAspect(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @AfterReturning("within(com.zhiyan.kb.controller..*)")
    public void logWriteOperation(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        if (!WRITE_METHODS.contains(request.getMethod()) || request.getRequestURI().contains("/auth/login")) {
            return;
        }
        OperationLog log = new OperationLog();
        LoginUser user = safeUser();
        if (user != null) {
            log.setUserId(user.getId());
        }
        log.setModuleName(resolveModule(request.getRequestURI()));
        log.setOperation(request.getMethod() + " " + request.getRequestURI());
        log.setDetail(JSONUtil.toJsonStr(Arrays.stream(joinPoint.getArgs())
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .map(this::safeArg)
                .toList()));
        log.setIp(request.getRemoteAddr());
        operationLogMapper.insert(log);
    }

    private LoginUser safeUser() {
        try {
            return UserContext.get();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object safeArg(Object arg) {
        if (arg == null) {
            return null;
        }
        String type = arg.getClass().getName();
        if (type.contains("MultipartFile") || type.contains("Servlet")) {
            return type;
        }
        if (arg instanceof String || arg instanceof Number || arg instanceof Boolean || arg instanceof Enum<?>) {
            return arg;
        }
        if (arg instanceof Map<?, ?> map) {
            Map<Object, Object> sanitized = new LinkedHashMap<>();
            map.forEach((key, value) -> sanitized.put(key, isSensitive(String.valueOf(key)) ? "***" : value));
            return sanitized;
        }
        if (!type.startsWith("com.zhiyan.kb.")) {
            return type;
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (java.lang.reflect.Field field : arg.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                sanitized.put(field.getName(), isSensitive(field.getName()) ? "***" : field.get(arg));
            } catch (IllegalAccessException ignored) {
                sanitized.put(field.getName(), "<unavailable>");
            }
        }
        if (!sanitized.isEmpty()) {
            return sanitized;
        }
        return arg;
    }

    private boolean isSensitive(String fieldName) {
        String normalized = fieldName == null ? "" : fieldName.trim();
        return SENSITIVE_FIELD_NAMES.stream().anyMatch(item -> item.equalsIgnoreCase(normalized));
    }

    private String resolveModule(String uri) {
        String[] parts = uri.split("/");
        return parts.length >= 3 ? parts[2] : "system";
    }
}
