package com.zhiyan.kb.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkServiceTest {
    @Test
    void splitShouldUseFixedWindowWithOverlap() {
        ChunkService service = new ChunkService(null, null, null);
        String text = "a".repeat(1100);

        var chunks = service.split(text);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(500);
        assertThat(chunks.get(1)).hasSize(500);
        assertThat(chunks.get(2)).hasSize(200);
    }
}
