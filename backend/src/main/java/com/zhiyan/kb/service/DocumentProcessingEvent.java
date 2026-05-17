package com.zhiyan.kb.service;

import java.nio.file.Path;

public record DocumentProcessingEvent(Long documentId, Path file, String fileType) {
}
