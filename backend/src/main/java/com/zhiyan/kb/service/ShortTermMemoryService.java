package com.zhiyan.kb.service;

import com.zhiyan.kb.dto.ChatMemoryMessage;

import java.util.List;

public interface ShortTermMemoryService {
    void addMessage(String sessionId, Long userId, String role, String content);

    List<ChatMemoryMessage> getContext(String sessionId, Long userId);

    void summarizeOldMessages(String sessionId, Long userId);

    void clearSession(String sessionId, Long userId);
}
