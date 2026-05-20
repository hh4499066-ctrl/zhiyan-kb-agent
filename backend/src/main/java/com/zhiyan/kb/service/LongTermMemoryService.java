package com.zhiyan.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.entity.UserLongTermMemory;
import com.zhiyan.kb.mapper.UserLongTermMemoryMapper;
import com.zhiyan.kb.rag.KeywordSearchService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
                .eq(UserLongTermMemory::getStatus, StatusConstants.NORMAL)
                .orderByDesc(UserLongTermMemory::getCreateTime));
    }

    public List<String> recall(Long userId, String question, int topK) {
        return memoryMapper.selectList(new LambdaQueryWrapper<UserLongTermMemory>()
                        .eq(UserLongTermMemory::getUserId, userId)
                        .eq(UserLongTermMemory::getStatus, StatusConstants.NORMAL)
                        .orderByDesc(UserLongTermMemory::getUpdateTime)
                        .last("LIMIT 200"))
                .stream()
                .map(memory -> new ScoredMemory(memory, score(question, memory)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingDouble(ScoredMemory::score).reversed())
                .limit(topK)
                .map(scored -> "[" + scored.memory().getMemoryType() + "] " + scored.memory().getContent())
                .toList();
    }

    private double score(String question, UserLongTermMemory memory) {
        double score = keywordSearchService.score(question, memory.getContent());
        String type = memory.getMemoryType() == null ? "" : memory.getMemoryType().toUpperCase(Locale.ROOT);
        String normalizedQuestion = question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
        if ("IDENTITY".equals(type) && normalizedQuestion.matches(".*(我是谁|我叫什么|我的名字|你知道我是谁|who am i|my name).*")) {
            score = Math.max(score, 1.0);
        }
        if ("PREFERENCE".equals(type) && normalizedQuestion.matches(".*(我喜欢|我偏好|我的偏好|我的习惯|适合我|prefer|preference).*")) {
            score = Math.max(score, 0.8);
        }
        if ("PROJECT".equals(type) && normalizedQuestion.matches(".*(我的项目|当前项目|这个项目|项目背景|project).*")) {
            score = Math.max(score, 0.8);
        }
        return score;
    }

    private record ScoredMemory(UserLongTermMemory memory, double score) {
    }
}
