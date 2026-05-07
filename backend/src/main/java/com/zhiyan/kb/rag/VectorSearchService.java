package com.zhiyan.kb.rag;

import com.zhiyan.kb.ai.EmbeddingClient;
import org.springframework.stereotype.Service;

@Service
public class VectorSearchService {
    private final EmbeddingClient embeddingClient;

    public VectorSearchService(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public double[] embed(String text) {
        return embeddingClient.embed(text);
    }

    public double cosine(double[] a, double[] b) {
        double dot = 0;
        double an = 0;
        double bn = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot += a[i] * b[i];
            an += a[i] * a[i];
            bn += b[i] * b[i];
        }
        return an == 0 || bn == 0 ? 0 : dot / (Math.sqrt(an) * Math.sqrt(bn));
    }
}
