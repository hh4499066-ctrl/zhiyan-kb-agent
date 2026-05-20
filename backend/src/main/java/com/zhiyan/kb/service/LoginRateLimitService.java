package com.zhiyan.kb.service;

import com.zhiyan.kb.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class LoginRateLimitService {
    private final StringRedisTemplate redisTemplate;
    private final int maxFailures;
    private final Duration lockDuration;

    public LoginRateLimitService(StringRedisTemplate redisTemplate,
                                 @Value("${zhiyan.auth.login-max-failures:5}") int maxFailures,
                                 @Value("${zhiyan.auth.login-lock-minutes:10}") long lockMinutes) {
        this.redisTemplate = redisTemplate;
        this.maxFailures = maxFailures;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public void assertAllowed(String username, String clientIp) {
        try {
            for (String key : keys(username, clientIp)) {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null && Integer.parseInt(value) >= maxFailures) {
                    throw new BusinessException(429, "Too many failed login attempts. Try again later");
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Login rate-limit check failed, allowing login attempt", ex);
        }
    }

    public void consumeAttempt(String username, String clientIp) {
        try {
            for (String key : keys(username, clientIp)) {
                Long attempts = redisTemplate.opsForValue().increment(key);
                if (attempts != null && attempts == 1L) {
                    redisTemplate.expire(key, lockDuration);
                }
                if (attempts != null && attempts > maxFailures) {
                    throw new BusinessException(429, "Too many failed login attempts. Try again later");
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Login rate-limit attempt record failed, allowing login attempt", ex);
        }
    }

    public void recordFailure(String username, String clientIp) {
        try {
            for (String key : keys(username, clientIp)) {
                Long failures = redisTemplate.opsForValue().increment(key);
                if (failures != null && failures == 1L) {
                    redisTemplate.expire(key, lockDuration);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Login rate-limit failure record failed", ex);
        }
    }

    public void clear(String username, String clientIp) {
        try {
            redisTemplate.delete(keys(username, clientIp));
        } catch (RuntimeException ex) {
            log.warn("Login rate-limit clear failed", ex);
        }
    }

    private List<String> keys(String username, String clientIp) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String normalizedIp = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
        return List.of("login_fail:user:" + normalizedUsername,
                "login_fail:user_ip:" + normalizedUsername + ":" + normalizedIp);
    }
}
