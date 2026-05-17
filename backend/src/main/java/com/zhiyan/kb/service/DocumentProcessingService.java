package com.zhiyan.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.entity.DocumentProcessingTask;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.mapper.DocumentProcessingTaskMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class DocumentProcessingService {
    private static final int DEFAULT_MAX_RETRIES = 3;

    private final KbDocumentMapper documentMapper;
    private final DocumentProcessingTaskMapper taskMapper;
    private final DocumentParseService parseService;
    private final ChunkService chunkService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final String uploadDir;

    public DocumentProcessingService(KbDocumentMapper documentMapper, DocumentProcessingTaskMapper taskMapper,
                                     DocumentParseService parseService, ChunkService chunkService,
                                     ApplicationEventPublisher eventPublisher,
                                     TransactionTemplate transactionTemplate,
                                     @Value("${zhiyan.upload-dir:uploads}") String uploadDir) {
        this.documentMapper = documentMapper;
        this.taskMapper = taskMapper;
        this.parseService = parseService;
        this.chunkService = chunkService;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.uploadDir = uploadDir;
    }

    @Async
    @EventListener
    public void process(DocumentProcessingEvent event) {
        if (event.taskId() != null) {
            processTask(event.taskId());
            return;
        }
        processDocument(event.documentId(), event.file(), event.fileType());
    }

    public void requestReprocess(KbDocument document) {
        if (document.getFileUrl() == null || document.getFileUrl().isBlank()
                || document.getFileType() == null || document.getFileType().isBlank()) {
            throw new BusinessException(400, "Document source file is missing");
        }
        Path file = resolveUploadPath(document.getFileUrl());
        if (!Files.exists(file)) {
            throw new BusinessException(404, "Document source file is missing");
        }
        DocumentProcessingTask task = transactionTemplate.execute(status -> {
            KbDocument update = new KbDocument();
            update.setId(document.getId());
            update.setParseStatus("UPLOADED");
            update.setVectorStatus("PENDING");
            documentMapper.updateById(update);

            DocumentProcessingTask created = new DocumentProcessingTask();
            created.setDocumentId(document.getId());
            created.setFileUrl(document.getFileUrl());
            created.setFileType(document.getFileType());
            created.setStatus("PENDING");
            created.setRetryCount(0);
            created.setMaxRetries(DEFAULT_MAX_RETRIES);
            taskMapper.insert(created);
            return created;
        });
        eventPublisher.publishEvent(new DocumentProcessingEvent(task.getId(), document.getId(), file, document.getFileType()));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOutstandingTasks() {
        taskMapper.update(null, new LambdaUpdateWrapper<DocumentProcessingTask>()
                .eq(DocumentProcessingTask::getStatus, "RUNNING")
                .set(DocumentProcessingTask::getStatus, "PENDING"));
        republishPendingTasks();
    }

    @Scheduled(fixedDelayString = "${zhiyan.document-processing.retry-delay-ms:60000}")
    public void retryPendingTasks() {
        republishPendingTasks();
    }

    public void processTask(Long taskId) {
        DocumentProcessingTask task = taskMapper.selectById(taskId);
        if (task == null || !"PENDING".equals(task.getStatus())) {
            return;
        }
        if (!markTaskRunning(task)) {
            return;
        }
        try {
            processDocument(task.getDocumentId(), resolveUploadPath(task.getFileUrl()), task.getFileType());
            markTaskSuccess(task.getId());
        } catch (Exception ex) {
            markTaskFailure(task, ex);
        }
    }

    private void processDocument(Long documentId, Path file, String fileType) {
        KbDocument document = documentMapper.selectById(documentId);
        if (document == null || "DELETED".equals(document.getStatus())) {
            return;
        }
        try {
            updateStatus(document.getId(), "PARSING", "PENDING");
            document.setContentText(parseService.parse(file.toFile(), fileType));
            document.setParseStatus("SUCCESS");
            document.setVectorStatus("PROCESSING");
            documentMapper.updateById(document);

            chunkService.rebuildChunks(document);
            updateStatus(document.getId(), "SUCCESS", "SUCCESS");
        } catch (Exception ex) {
            log.warn("Document processing failed, documentId={}", documentId, ex);
            chunkService.disableDocumentChunks(documentId);
            updateStatus(documentId, "FAILED", "FAILED");
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(ex);
        }
    }

    private void republishPendingTasks() {
        List<DocumentProcessingTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<DocumentProcessingTask>()
                .eq(DocumentProcessingTask::getStatus, "PENDING")
                .apply("retry_count < max_retries"));
        for (DocumentProcessingTask task : tasks) {
            eventPublisher.publishEvent(new DocumentProcessingEvent(task.getId(), task.getDocumentId(),
                    resolveUploadPath(task.getFileUrl()), task.getFileType()));
        }
    }

    private Path resolveUploadPath(String fileUrl) {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = base.resolve(fileUrl).normalize();
        if (!file.startsWith(base)) {
            throw new BusinessException(400, "Invalid document source file");
        }
        return file;
    }

    private boolean markTaskRunning(DocumentProcessingTask task) {
        DocumentProcessingTask update = new DocumentProcessingTask();
        update.setId(task.getId());
        update.setStatus("RUNNING");
        update.setLastProcessTime(LocalDateTime.now());
        return taskMapper.update(update, new LambdaUpdateWrapper<DocumentProcessingTask>()
                .eq(DocumentProcessingTask::getId, task.getId())
                .eq(DocumentProcessingTask::getStatus, "PENDING")) > 0;
    }

    private void markTaskSuccess(Long taskId) {
        DocumentProcessingTask update = new DocumentProcessingTask();
        update.setId(taskId);
        update.setStatus("SUCCESS");
        update.setLastError(null);
        update.setLastProcessTime(LocalDateTime.now());
        taskMapper.updateById(update);
    }

    private void markTaskFailure(DocumentProcessingTask task, Exception ex) {
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetries = task.getMaxRetries() == null ? DEFAULT_MAX_RETRIES : task.getMaxRetries();
        DocumentProcessingTask update = new DocumentProcessingTask();
        update.setId(task.getId());
        update.setRetryCount(retryCount + 1);
        update.setStatus(retryCount + 1 >= maxRetries ? "FAILED" : "PENDING");
        update.setLastError(trimError(ex));
        update.setLastProcessTime(LocalDateTime.now());
        taskMapper.updateById(update);
    }

    private String trimError(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private void updateStatus(Long documentId, String parseStatus, String vectorStatus) {
        KbDocument update = new KbDocument();
        update.setId(documentId);
        update.setParseStatus(parseStatus);
        update.setVectorStatus(vectorStatus);
        documentMapper.updateById(update);
    }
}
