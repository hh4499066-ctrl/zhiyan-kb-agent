package com.zhiyan.kb.service;

import java.nio.file.Path;

public record DocumentProcessingEvent(Long taskId, Long documentId, Path file, String fileType) {
    public DocumentProcessingEvent(Long documentId, Path file, String fileType) {
        this(null, documentId, file, fileType);
    }
}
