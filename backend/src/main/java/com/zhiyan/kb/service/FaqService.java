package com.zhiyan.kb.service;

import com.zhiyan.kb.mapper.KbFaqMapper;
import org.springframework.stereotype.Service;

@Service
public class FaqService {
    private final KbFaqMapper mapper;

    public FaqService(KbFaqMapper mapper) {
        this.mapper = mapper;
    }

    public long count() {
        return mapper.selectCount(null);
    }
}
