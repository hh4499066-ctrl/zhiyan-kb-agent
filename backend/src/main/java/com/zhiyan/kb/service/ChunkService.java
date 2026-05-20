package com.zhiyan.kb.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zhiyan.kb.ai.EmbeddingClient;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.entity.KbDocumentChunk;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.rag.VectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkService {
    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 50;
    private final KbDocumentChunkMapper chunkMapper;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;
    private final TransactionTemplate transactionTemplate;

    public ChunkService(KbDocumentChunkMapper chunkMapper, EmbeddingClient embeddingClient,
                        VectorStoreService vectorStoreService) {
        this(chunkMapper, embeddingClient, vectorStoreService, null);
    }

    @Autowired
    public ChunkService(KbDocumentChunkMapper chunkMapper, EmbeddingClient embeddingClient,
                        VectorStoreService vectorStoreService, TransactionTemplate transactionTemplate) {
        this.chunkMapper = chunkMapper;
        this.embeddingClient = embeddingClient;
        this.vectorStoreService = vectorStoreService;
        this.transactionTemplate = transactionTemplate;
    }

    public List<KbDocumentChunk> rebuildChunks(KbDocument document) {
        List<KbDocumentChunk> chunks = persistChunks(document);
        upsertVectorsWithCompensation(document, chunks);
        return chunks;
    }

    private List<KbDocumentChunk> persistChunks(KbDocument document) {
        if (transactionTemplate == null) {
            return persistChunksInCurrentThread(document);
        }
        return transactionTemplate.execute(status -> persistChunksInCurrentThread(document));
    }

    private List<KbDocumentChunk> persistChunksInCurrentThread(KbDocument document) {
        disableDocumentChunks(document.getId());
        List<String> parts = split(document.getContentText());
        List<KbDocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            KbDocumentChunk chunk = new KbDocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setSpaceId(document.getSpaceId());
            chunk.setChunkIndex(i);
            chunk.setContent(parts.get(i));
            chunk.setTokenCount(estimateTokens(parts.get(i)));
            chunk.setEmbeddingText(parts.get(i));
            chunk.setStatus(StatusConstants.NORMAL);
            chunkMapper.insert(chunk);
            chunk.setVectorId("chunk-" + chunk.getId());
            KbDocumentChunk update = new KbDocumentChunk();
            update.setId(chunk.getId());
            update.setVectorId(chunk.getVectorId());
            chunkMapper.updateById(update);
            chunks.add(chunk);
        }
        return chunks;
    }

    private void upsertVectorsWithCompensation(KbDocument document, List<KbDocumentChunk> chunks) {
        try {
            for (KbDocumentChunk chunk : chunks) {
                vectorStoreService.upsert(chunk, embeddingClient.embed(chunk.getContent()), document.getTitle());
            }
        } catch (RuntimeException ex) {
            disableDocumentChunks(document.getId());
            throw ex;
        }
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int asciiWords = text.replaceAll("[^\\p{Alnum}]+", " ").trim().isBlank()
                ? 0
                : text.replaceAll("[^\\p{Alnum}]+", " ").trim().split("\\s+").length;
        int nonAsciiChars = (int) text.chars().filter(ch -> ch > 127 && !Character.isWhitespace(ch)).count();
        return Math.max(1, asciiWords + (int) Math.ceil(nonAsciiChars / 1.6d));
    }

    public void disableDocumentChunks(Long documentId) {
        chunkMapper.update(null, new UpdateWrapper<KbDocumentChunk>()
                .eq("document_id", documentId)
                .set("status", StatusConstants.DISABLED));
        removeVectorsAfterCommit(documentId);
    }

    private void removeVectorsAfterCommit(Long documentId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    vectorStoreService.removeByDocumentId(documentId);
                }
            });
            return;
        }
        vectorStoreService.removeByDocumentId(documentId);
    }

    public List<String> split(String text) {
        String normalized = normalize(text);
        List<String> blocks = structuralBlocks(normalized);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String block : blocks) {
            if (block.length() > CHUNK_SIZE) {
                flush(current, chunks);
                chunks.addAll(splitLongBlock(block));
                continue;
            }
            if (!current.isEmpty() && current.length() + block.length() + 2 > CHUNK_SIZE) {
                flush(current, chunks);
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(block);
        }
        flush(current, chunks);
        return chunks;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    private List<String> structuralBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        if (text.isBlank()) {
            return blocks;
        }
        for (String raw : text.split("\\n{2,}")) {
            String block = raw.strip();
            if (!block.isBlank()) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    private List<String> splitLongBlock(String block) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < block.length()) {
            int end = Math.min(block.length(), start + CHUNK_SIZE);
            result.add(block.substring(start, end).trim());
            if (end == block.length()) {
                break;
            }
            start = Math.max(0, end - OVERLAP);
        }
        return result;
    }

    private void flush(StringBuilder current, List<String> chunks) {
        String value = current.toString().trim();
        if (!value.isBlank()) {
            chunks.add(value);
        }
        current.setLength(0);
    }
}
