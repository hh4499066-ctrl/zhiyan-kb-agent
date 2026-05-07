package com.zhiyan.kb.rag;

import java.util.List;

public interface HybridRetrievalService {
    List<RetrievalResult> search(Long spaceId, String question, int topK, double alpha, double minScore);
}
