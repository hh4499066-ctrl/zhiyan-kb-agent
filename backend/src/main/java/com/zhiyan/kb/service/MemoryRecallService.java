package com.zhiyan.kb.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemoryRecallService {
    private final LongTermMemoryService longTermMemoryService;

    public MemoryRecallService(LongTermMemoryService longTermMemoryService) {
        this.longTermMemoryService = longTermMemoryService;
    }

    public List<String> recall(Long userId, String question, int topK) {
        return longTermMemoryService.recall(userId, question, topK);
    }
}
