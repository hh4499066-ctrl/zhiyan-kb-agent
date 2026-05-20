package com.zhiyan.kb.service;

import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.mapper.UnresolvedQuestionMapper;
import org.springframework.stereotype.Service;

@Service
public class UnresolvedQuestionService {
    private final UnresolvedQuestionMapper mapper;

    public UnresolvedQuestionService(UnresolvedQuestionMapper mapper) {
        this.mapper = mapper;
    }

    public long pendingCount() {
        return mapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.zhiyan.kb.entity.UnresolvedQuestion>()
                .eq(com.zhiyan.kb.entity.UnresolvedQuestion::getStatus, StatusConstants.PENDING));
    }
}
