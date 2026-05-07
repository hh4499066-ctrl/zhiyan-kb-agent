package com.zhiyan.kb.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class HybridRetrievalServiceImpl implements HybridRetrievalService {
    private final VectorStoreService vectorStoreService;
    private final KeywordSearchService keywordSearchService;
    private final VectorSearchService vectorSearchService;
    private final double defaultAlpha;
    private final double defaultMinScore;

    public HybridRetrievalServiceImpl(VectorStoreService vectorStoreService, KeywordSearchService keywordSearchService, VectorSearchService vectorSearchService,
                                      @Value("${zhiyan.rag.alpha:0.5}") double defaultAlpha,
                                      @Value("${zhiyan.rag.min-score:0.08}") double defaultMinScore) {
        this.vectorStoreService = vectorStoreService;
        this.keywordSearchService = keywordSearchService;
        this.vectorSearchService = vectorSearchService;
        this.defaultAlpha = defaultAlpha;
        this.defaultMinScore = defaultMinScore;
    }

    @Override
    public List<RetrievalResult> search(Long spaceId, String question, int topK, double alpha, double minScore) {
        double[] queryVector = vectorSearchService.embed(question);
        double a = alpha <= 0 ? defaultAlpha : alpha;
        double threshold = minScore <= 0 ? defaultMinScore : minScore;
        return vectorStoreService.listBySpaceId(spaceId).stream().map(item -> {
            RetrievalResult result = new RetrievalResult();
            result.setChunkId(item.getChunkId());
            result.setDocumentId(item.getDocumentId());
            result.setSpaceId(item.getSpaceId());
            result.setDocumentTitle(item.getDocumentTitle());
            result.setContent(item.getContent());
            result.setKeywordScore(keywordSearchService.score(question, item.getContent()));
            result.setVectorScore(vectorSearchService.cosine(queryVector, item.getVector()));
            result.setFinalScore(a * result.getVectorScore() + (1 - a) * result.getKeywordScore());
            return result;
        }).filter(r -> r.getFinalScore() >= threshold)
                .sorted(Comparator.comparingDouble(RetrievalResult::getFinalScore).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }
}
