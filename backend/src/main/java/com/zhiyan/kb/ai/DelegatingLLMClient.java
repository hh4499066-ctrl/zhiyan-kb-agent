package com.zhiyan.kb.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

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
}
