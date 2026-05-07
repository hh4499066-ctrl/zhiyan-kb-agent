package com.zhiyan.kb.config;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.zhiyan.kb.common.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TokenUtil {
    private static final String PREFIX = "auth:token:";
    private final StringRedisTemplate redisTemplate;
    private final long expireHours;

    public TokenUtil(StringRedisTemplate redisTemplate, @Value("${zhiyan.auth.token-expire-hours:2}") long expireHours) {
        this.redisTemplate = redisTemplate;
        this.expireHours = expireHours;
    }

    public String createToken(LoginUser loginUser) {
        String token = UUID.fastUUID().toString(true);
        redisTemplate.opsForValue().set(PREFIX + token, JSONUtil.toJsonStr(loginUser), Duration.ofHours(expireHours));
        return token;
    }

    public LoginUser getLoginUser(String token) {
        String json = redisTemplate.opsForValue().get(PREFIX + token);
        if (json == null || json.isBlank()) {
            return null;
        }
        redisTemplate.expire(PREFIX + token, Duration.ofHours(expireHours));
        return JSONUtil.toBean(json, LoginUser.class);
    }

    public void removeToken(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}
