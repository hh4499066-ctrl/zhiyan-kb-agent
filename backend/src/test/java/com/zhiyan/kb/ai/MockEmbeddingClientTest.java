package com.zhiyan.kb.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockEmbeddingClientTest {
    @Test
    void embedShouldReturnStableNormalizedVector() {
        MockEmbeddingClient client = new MockEmbeddingClient();

        double[] first = client.embed("Redis 缓存规范");
        double[] second = client.embed("Redis 缓存规范");

        assertThat(first).hasSize(32);
        assertThat(first).containsExactly(second);
        double norm = 0;
        for (double value : first) {
            norm += value * value;
        }
        assertThat(Math.sqrt(norm)).isBetween(0.99, 1.01);
    }
}
