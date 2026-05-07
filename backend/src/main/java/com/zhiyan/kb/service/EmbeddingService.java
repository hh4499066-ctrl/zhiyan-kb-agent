package com.zhiyan.kb.service;

import com.zhiyan.kb.ai.EmbeddingClient;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
    private final EmbeddingClient embeddingClient;

    public EmbeddingService(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public double[] embed(String text) {
        return embeddingClient.embed(text);
    }
}
