package com.zhiyan.kb.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhiyan.kb.ai.EmbeddingClient;
import com.zhiyan.kb.entity.KbDocumentChunk;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.rag.VectorStoreService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        KbDocumentChunk chunk = new KbDocumentChunk();
        chunk.setId(10L);
        chunk.setDocumentId(20L);
        chunk.setStatus("NORMAL");
        when(chunkMapper.selectList(anyChunkWrapper())).thenReturn(List.of(chunk));
        ChunkService service = new ChunkService(chunkMapper, embeddingClient, vectorStoreService);

        service.disableDocumentChunks(20L);

        assertThat(chunk.getStatus()).isEqualTo("DISABLED");
        verify(chunkMapper).updateById(chunk);
        verify(vectorStoreService).removeByDocumentId(20L);
    }

    @SuppressWarnings("unchecked")
    private Wrapper<KbDocumentChunk> anyChunkWrapper() {
        return any(Wrapper.class);
    }
}
