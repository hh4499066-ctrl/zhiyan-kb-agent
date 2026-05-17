package com.zhiyan.kb.ai;

import com.zhiyan.kb.dto.ChatMemoryMessage;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class DelegatingLLMClient implements LLMClient {
    private final AiProperties properties;
    private final MockLLMClient mockLLMClient;
    private final DeepSeekLLMClient deepSeekLLMClient;

    public DelegatingLLMClient(AiProperties properties, MockLLMClient mockLLMClient, DeepSeekLLMClient deepSeekLLMClient) {
        this.properties = properties;
        this.mockLLMClient = mockLLMClient;
        this.deepSeekLLMClient = deepSeekLLMClient;
    }

    @Override
    public String complete(String prompt) {
        if (properties.realMode()) {
            return deepSeekLLMClient.complete(prompt);
        }
        return mockLLMClient.complete(prompt);
    }

    @Override
    public String complete(String prompt, List<ChatMemoryMessage> context) {
        if (properties.realMode()) {
            return deepSeekLLMClient.complete(prompt, context);
        }
        return mockLLMClient.complete(prompt, context);
    }

    @Override
    public String complete(String prompt, List<ChatMemoryMessage> context, String model) {
        if (properties.realMode()) {
            return deepSeekLLMClient.complete(prompt, context, model);
        }
        return mockLLMClient.complete(prompt, context, model);
    }
}
