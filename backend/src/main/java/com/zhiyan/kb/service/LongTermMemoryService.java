package com.zhiyan.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.entity.UserLongTermMemory;
import com.zhiyan.kb.mapper.UserLongTermMemoryMapper;
import com.zhiyan.kb.rag.KeywordSearchService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class LongTermMemoryService {
    private final UserLongTermMemoryMapper memoryMapper;
    private final KeywordSearchService keywordSearchService;

    public LongTermMemoryService(UserLongTermMemoryMapper memoryMapper, KeywordSearchService keywordSearchService) {
        this.memoryMapper = memoryMapper;
        this.keywordSearchService = keywordSearchService;
    }

    public List<UserLongTermMemory> list(Long userId) {
        return memoryMapper.selectList(new LambdaQueryWrapper<UserLongTermMemory>()
                .eq(UserLongTermMemory::getUserId, userId)
                .eq(UserLongTermMemory::getStatus, "NORMAL")
                .orderByDesc(UserLongTermMemory::getCreateTime));
    }

    public List<String> recall(Long userId, String question, int topK) {
        return list(userId).stream()
                .sorted(Comparator.comparingDouble((UserLongTermMemory m) -> keywordSearchService.score(question, m.getContent())).reversed())
                .limit(topK)
                .map(UserLongTermMemory::getContent)
                .toList();
    }
}
