package com.zhiyan.kb.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.ai.EmbeddingClient;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.entity.KbDocumentChunk;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MockVectorStoreService implements VectorStoreService {
    private final Map<Long, VectorStoreItem> store = new ConcurrentHashMap<>();
    private final KbDocumentChunkMapper chunkMapper;
    private final KbDocumentMapper documentMapper;
    private final EmbeddingClient embeddingClient;

    public MockVectorStoreService(KbDocumentChunkMapper chunkMapper, KbDocumentMapper documentMapper, EmbeddingClient embeddingClient) {
        this.chunkMapper = chunkMapper;
        this.documentMapper = documentMapper;
        this.embeddingClient = embeddingClient;
    }

    @PostConstruct
    public void rebuild() {
        List<KbDocumentChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<KbDocumentChunk>().eq(KbDocumentChunk::getStatus, StatusConstants.NORMAL));
        if (chunks.isEmpty()) {
            return;
        }
        List<Long> documentIds = chunks.stream().map(KbDocumentChunk::getDocumentId).distinct().toList();
        Map<Long, KbDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                        .in(KbDocument::getId, documentIds)
                        .eq(KbDocument::getStatus, StatusConstants.NORMAL))
                .stream()
                .collect(Collectors.toMap(KbDocument::getId, Function.identity()));
        for (KbDocumentChunk chunk : chunks) {
            KbDocument document = documents.get(chunk.getDocumentId());
            if (document != null) {
                upsert(chunk, embeddingClient.embed(chunk.getContent()), document.getTitle());
            }
        }
    }

    @Override
    public void upsert(KbDocumentChunk chunk, double[] vector, String documentTitle) {
        store.put(chunk.getId(), new VectorStoreItem(chunk.getId(), chunk.getDocumentId(), chunk.getSpaceId(), documentTitle, chunk.getContent(), vector));
    }

    @Override
    public void removeByDocumentId(Long documentId) {
        store.values().removeIf(item -> item.getDocumentId().equals(documentId));
    }

    @Override
    public List<VectorStoreItem> listBySpaceId(Long spaceId) {
        return store.values().stream().filter(item -> item.getSpaceId().equals(spaceId)).toList();
    }
}
