package com.zhiyan.kb.service;

import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DocumentProcessingService {
    private final KbDocumentMapper documentMapper;
    private final DocumentParseService parseService;
    private final ChunkService chunkService;

    public DocumentProcessingService(KbDocumentMapper documentMapper, DocumentParseService parseService,
                                     ChunkService chunkService) {
        this.documentMapper = documentMapper;
        this.parseService = parseService;
        this.chunkService = chunkService;
    }

    @Async
    @EventListener
    public void process(DocumentProcessingEvent event) {
        KbDocument document = documentMapper.selectById(event.documentId());
        if (document == null || "DELETED".equals(document.getStatus())) {
            return;
        }
        try {
            updateStatus(document.getId(), "PARSING", "PENDING");
            document.setContentText(parseService.parse(event.file().toFile(), event.fileType()));
            document.setParseStatus("SUCCESS");
            document.setVectorStatus("PROCESSING");
            documentMapper.updateById(document);

            chunkService.rebuildChunks(document);
            updateStatus(document.getId(), "SUCCESS", "SUCCESS");
        } catch (Exception ex) {
            log.warn("Document processing failed, documentId={}", event.documentId(), ex);
            chunkService.disableDocumentChunks(event.documentId());
            updateStatus(event.documentId(), "FAILED", "FAILED");
        }
    }

    private void updateStatus(Long documentId, String parseStatus, String vectorStatus) {
        KbDocument update = new KbDocument();
        update.setId(documentId);
        update.setParseStatus(parseStatus);
        update.setVectorStatus(vectorStatus);
        documentMapper.updateById(update);
    }
}
