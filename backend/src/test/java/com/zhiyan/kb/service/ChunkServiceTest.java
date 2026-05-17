package com.zhiyan.kb.service;

import com.zhiyan.kb.ai.EmbeddingClient;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.rag.VectorStoreService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChunkServiceTest {
    @Test
    void splitShouldPreserveParagraphBoundariesWhenPossible() {
        ChunkService service = new ChunkService(null, null, null);
        String text = "Title\n\nFirst paragraph.\n\nSecond paragraph.";

        var chunks = service.split(text);

        assertThat(chunks).containsExactly("Title\n\nFirst paragraph.\n\nSecond paragraph.");
    }

    @Test
    void splitLongBlockShouldUseFixedWindowWithOverlap() {
        ChunkService service = new ChunkService(null, null, null);
        String text = "a".repeat(1100);

        var chunks = service.split(text);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(500);
        assertThat(chunks.get(1)).hasSize(500);
        assertThat(chunks.get(2)).hasSize(200);
    }

    @Test
    void disableDocumentChunksDisablesDbChunksAndRemovesVectors() {
        KbDocumentChunkMapper chunkMapper = mock(KbDocumentChunkMapper.class);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        ChunkService service = new ChunkService(chunkMapper, embeddingClient, vectorStoreService);

        service.disableDocumentChunks(20L);

        verify(chunkMapper).update(any(), any());
        verify(vectorStoreService).removeByDocumentId(20L);
    }
}
