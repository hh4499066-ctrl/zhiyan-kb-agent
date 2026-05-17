package com.zhiyan.kb.ai;

import com.zhiyan.kb.dto.ChatMemoryMessage;

import java.util.List;

public interface LLMClient {
    String complete(String prompt);

    default String complete(String prompt, List<ChatMemoryMessage> context) {
        return complete(prompt);
    }

    default String complete(String prompt, List<ChatMemoryMessage> context, String model) {
        return complete(prompt, context);
    }
}
