package com.zhiyan.kb.ai;

import org.springframework.stereotype.Component;

@Component
public class MockEmbeddingClient implements EmbeddingClient {
    private static final int DIM = 32;

    @Override
    public double[] embed(String text) {
        double[] vector = new double[DIM];
        if (text == null) {
            return vector;
        }
        for (int i = 0; i < text.length(); i++) {
            int bucket = Math.abs((text.charAt(i) * 31 + i) % DIM);
            vector[bucket] += 1.0;
        }
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = vector[i] / norm;
            }
        }
        return vector;
    }
}
