package com.zhiyan.kb.service;

import cn.hutool.json.JSONUtil;
import com.zhiyan.kb.dto.ChatMemoryMessage;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RedisShortTermMemoryServiceImpl implements ShortTermMemoryService {
    private final StringRedisTemplate redisTemplate;
    private final int windowSize;
    private final long ttlMinutes;

    public RedisShortTermMemoryServiceImpl(StringRedisTemplate redisTemplate,
                                           @Value("${zhiyan.memory.window-size:6}") int windowSize,
                                           @Value("${zhiyan.memory.ttl-minutes:120}") long ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.windowSize = windowSize;
        this.ttlMinutes = ttlMinutes;
    }

    @Override
    public void addMessage(String sessionId, Long userId, String role, String content) {
        MemorySession session = loadSession(sessionId, userId);
        session.getMessages().add(new ChatMemoryMessage(role, content, LocalDateTime.now()));
        if (session.getMessages().size() > windowSize * 2) {
            summarize(session);
        }
        saveSession(sessionId, userId, session);
    }

    @Override
    public List<ChatMemoryMessage> getContext(String sessionId, Long userId) {
        MemorySession session = loadSession(sessionId, userId);
        List<ChatMemoryMessage> context = new ArrayList<>();
        if (session.getSummary() != null && !session.getSummary().isBlank()) {
            context.add(new ChatMemoryMessage("SYSTEM", "历史摘要：" + session.getSummary(), LocalDateTime.now()));
        }
        context.addAll(session.getMessages());
        return context;
    }

    @Override
    public void summarizeOldMessages(String sessionId, Long userId) {
        MemorySession session = loadSession(sessionId, userId);
        summarize(session);
        saveSession(sessionId, userId, session);
    }

    @Override
    public void clearSession(String sessionId, Long userId) {
        redisTemplate.delete(key(sessionId, userId));
    }

    private MemorySession loadSession(String sessionId, Long userId) {
        String json = redisTemplate.opsForValue().get(key(sessionId, userId));
        MemorySession session = new MemorySession();
        if (json == null || json.isBlank()) {
            return session;
        }
        if (json.trim().startsWith("[")) {
            session.setMessages(new ArrayList<>(JSONUtil.toList(JSONUtil.parseArray(json), ChatMemoryMessage.class)));
            return session;
        }
        MemorySession parsed = JSONUtil.toBean(json, MemorySession.class);
        if (parsed.getMessages() == null) {
            parsed.setMessages(new ArrayList<>());
        }
        return parsed;
    }

    private void summarize(MemorySession session) {
        List<ChatMemoryMessage> messages = session.getMessages();
        if (messages.size() <= windowSize * 2) {
            return;
        }
        int splitIndex = messages.size() - windowSize * 2;
        List<ChatMemoryMessage> oldMessages = messages.subList(0, splitIndex);
        StringBuilder summary = new StringBuilder(session.getSummary() == null ? "" : session.getSummary());
        if (!summary.isEmpty()) {
            summary.append("；");
        }
        summary.append("此前讨论过");
        oldMessages.stream()
                .filter(m -> "USER".equals(m.getRole()))
                .limit(3)
                .forEach(m -> summary.append("「").append(limit(m.getContent(), 40)).append("」"));
        session.setSummary(limit(summary.toString(), 800));
        session.setMessages(new ArrayList<>(messages.subList(splitIndex, messages.size())));
    }

    private String limit(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...";
    }

    private void saveSession(String sessionId, Long userId, MemorySession session) {
        redisTemplate.opsForValue().set(key(sessionId, userId), JSONUtil.toJsonStr(session), Duration.ofMinutes(ttlMinutes));
    }

    private String key(String sessionId, Long userId) {
        return "memory:short:" + userId + ":" + sessionId;
    }

    @Data
    public static class MemorySession {
        private List<ChatMemoryMessage> messages = new ArrayList<>();
        private String summary = "";
    }
}
