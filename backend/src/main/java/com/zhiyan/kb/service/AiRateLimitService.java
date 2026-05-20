package com.zhiyan.kb.service;

import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class AiRateLimitService {
    private final StringRedisTemplate redisTemplate;
    private final int maxCallsPerMinute;

    public AiRateLimitService(StringRedisTemplate redisTemplate,
                              @Value("${zhiyan.ai.rate-limit-per-minute:5}") int maxCallsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.maxCallsPerMinute = maxCallsPerMinute;
    }

    public void assertAllowed(String action) {
        Long userId = UserContext.userId();
        String key = "ai_rate:" + userId + ":" + action;
        try {
            Long current = redisTemplate.opsForValue().increment(key);
            if (current != null && current == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            if (current != null && current > maxCallsPerMinute) {
                throw new BusinessException(429, "Too many AI requests. Try again later");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("AI rate-limit check failed, allowing request", ex);
        }
    }
}
