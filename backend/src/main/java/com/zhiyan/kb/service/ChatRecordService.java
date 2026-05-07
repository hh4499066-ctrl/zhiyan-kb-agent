package com.zhiyan.kb.service;

import com.zhiyan.kb.mapper.ChatRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class ChatRecordService {
    private final ChatRecordMapper mapper;

    public ChatRecordService(ChatRecordMapper mapper) {
        this.mapper = mapper;
    }

    public long count() {
        return mapper.selectCount(null);
    }
}
