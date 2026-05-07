package com.zhiyan.kb.rag;

import com.zhiyan.kb.entity.KbDocumentChunk;

import java.util.List;

public interface VectorStoreService {
    void upsert(KbDocumentChunk chunk, double[] vector, String documentTitle);

    void removeByDocumentId(Long documentId);

    List<VectorStoreItem> listBySpaceId(Long spaceId);
}
