package com.zhiyan.kb.controller;

import com.zhiyan.kb.ai.LLMClient;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.GlobalExceptionHandler;
import com.zhiyan.kb.common.LoginUser;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.dto.UpdateDocumentRequest;
import com.zhiyan.kb.entity.DocumentProcessingTask;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbFaqMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import com.zhiyan.kb.mapper.DocumentProcessingTaskMapper;
import com.zhiyan.kb.service.ChunkService;
import com.zhiyan.kb.service.DocumentUploadService;
import com.zhiyan.kb.service.DocumentProcessingEvent;
import com.zhiyan.kb.service.DocumentProcessingService;
import com.zhiyan.kb.service.ResourceAccessService;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class DocumentControllerTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void hasRequiredDocxEntriesAcceptsDocxPackageShape() throws Exception {
        byte[] docx = zip("[Content_Types].xml", "word/document.xml");

        boolean valid = DocumentController.hasRequiredDocxEntries(new ByteArrayInputStream(docx));

        assertThat(valid).isTrue();
    }

    @Test
    void hasRequiredDocxEntriesRejectsPlainZipPackage() throws Exception {
        byte[] zip = zip("readme.txt", "word/styles.xml");

        boolean valid = DocumentController.hasRequiredDocxEntries(new ByteArrayInputStream(zip));

        assertThat(valid).isFalse();
    }

    @Test
    void uploadRejectsExecutableFile() throws Exception {
        MockMvc mvc = mockMvc();

        mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "run.exe", "application/octet-stream", "MZ".getBytes()))
                        .param("spaceId", "1"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void uploadRejectsFileWithoutExtension() throws Exception {
        MockMvc mvc = mockMvc();

        mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "upload", "text/plain", "hello".getBytes()))
                        .param("spaceId", "1"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void uploadRejectsFakePdf() throws Exception {
        MockMvc mvc = mockMvc();

        mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a pdf".getBytes()))
                        .param("spaceId", "1"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void uploadRejectsFakeDocxZip() throws Exception {
        MockMvc mvc = mockMvc();

        mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "fake.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", zip("readme.txt")))
                        .param("spaceId", "1"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void uploadAcceptsTxtFileWithManagePermission() throws Exception {
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        KbDocumentChunkMapper chunkMapper = mock(KbDocumentChunkMapper.class);
        KbFaqMapper faqMapper = mock(KbFaqMapper.class);
        KbSpaceMapper spaceMapper = mock(KbSpaceMapper.class);
        ChunkService chunkService = mock(ChunkService.class);
        LLMClient llmClient = mock(LLMClient.class);
        ResourceAccessService accessService = mock(ResourceAccessService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        DocumentProcessingTaskMapper taskMapper = mock(DocumentProcessingTaskMapper.class);
        doAnswer(invocation -> {
            KbDocument document = invocation.getArgument(0);
            document.setId(100L);
            return 1;
        }).when(documentMapper).insert(any(KbDocument.class));
        DocumentUploadService uploadService = new DocumentUploadService(documentMapper, taskMapper, spaceMapper, accessService,
                eventPublisher, transactionTemplate(), tempDir.toString());
        MockMvc mvc = mockMvc(new DocumentController(documentMapper, chunkMapper, faqMapper,
                chunkService, llmClient, accessService, uploadService, mock(DocumentProcessingService.class)));
        login();

        mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes()))
                        .param("spaceId", "1"))
                .andExpect(jsonPath("$.code").value(200));

        verify(documentMapper).insert(any(KbDocument.class));
        verify(taskMapper).insert(any(DocumentProcessingTask.class));
        verify(eventPublisher).publishEvent(any(DocumentProcessingEvent.class));
    }

    @Test
    void uploadRejectsUserWithoutManagePermission() throws Exception {
        ResourceAccessService accessService = mock(ResourceAccessService.class);
        doThrow(new BusinessException(403, "No permission to manage this space"))
                .when(accessService).requireSpaceManage(1L);
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        KbSpaceMapper spaceMapper = mock(KbSpaceMapper.class);
        ChunkService chunkService = mock(ChunkService.class);
        DocumentUploadService uploadService = new DocumentUploadService(documentMapper, mock(DocumentProcessingTaskMapper.class),
                spaceMapper, accessService,
                mock(ApplicationEventPublisher.class), transactionTemplate(), tempDir.toString());
        MockMvc mvc = mockMvc(new DocumentController(documentMapper, mock(KbDocumentChunkMapper.class),
                mock(KbFaqMapper.class), chunkService, mock(LLMClient.class), accessService, uploadService,
                mock(DocumentProcessingService.class)));

        mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes()))
                        .param("spaceId", "1"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void updateOnlyPersistsEditableDocumentFields() {
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        ResourceAccessService accessService = mock(ResourceAccessService.class);
        KbDocument existing = new KbDocument();
        existing.setId(10L);
        existing.setSpaceId(1L);
        when(accessService.requireDocumentManage(10L)).thenReturn(existing);
        ChunkService chunkService = mock(ChunkService.class);
        DocumentController controller = new DocumentController(documentMapper, mock(KbDocumentChunkMapper.class),
                mock(KbFaqMapper.class), chunkService, mock(LLMClient.class), accessService,
                mock(DocumentUploadService.class), mock(DocumentProcessingService.class));
        UpdateDocumentRequest request = new UpdateDocumentRequest();
        request.setTitle("moved");
        request.setSummary("summary");
        request.setKeywords("k1,k2");

        controller.update(10L, request);

        ArgumentCaptor<KbDocument> captor = ArgumentCaptor.forClass(KbDocument.class);
        verify(documentMapper).updateById(captor.capture());
        KbDocument update = captor.getValue();
        assertThat(update.getId()).isEqualTo(10L);
        assertThat(update.getTitle()).isEqualTo("moved");
        assertThat(update.getSummary()).isEqualTo("summary");
        assertThat(update.getKeywords()).isEqualTo("k1,k2");
        assertThat(update.getSpaceId()).isNull();
        assertThat(update.getFileUrl()).isNull();
        assertThat(update.getContentText()).isNull();
        assertThat(update.getParseStatus()).isNull();
        assertThat(update.getVectorStatus()).isNull();
        assertThat(update.getStatus()).isNull();
        assertThat(update.getUploaderId()).isNull();
    }

    @Test
    void parseQueuesFullReprocessInsteadOfRebuildingChunksInline() {
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        ResourceAccessService accessService = mock(ResourceAccessService.class);
        KbDocument existing = new KbDocument();
        existing.setId(10L);
        existing.setFileUrl("doc.txt");
        existing.setFileType("txt");
        when(accessService.requireDocumentManage(10L)).thenReturn(existing);
        ChunkService chunkService = mock(ChunkService.class);
        DocumentProcessingService processingService = mock(DocumentProcessingService.class);
        DocumentController controller = new DocumentController(documentMapper, mock(KbDocumentChunkMapper.class),
                mock(KbFaqMapper.class), chunkService, mock(LLMClient.class), accessService,
                mock(DocumentUploadService.class), processingService);

        controller.parse(10L);

        verify(processingService).requestReprocess(existing);
        verify(chunkService, never()).rebuildChunks(any());
        verify(documentMapper, never()).updateById(any(KbDocument.class));
    }

    private byte[] zip(String... names) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (String name : names) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write("x".getBytes());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private MockMvc mockMvc() {
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        KbSpaceMapper spaceMapper = mock(KbSpaceMapper.class);
        ChunkService chunkService = mock(ChunkService.class);
        ResourceAccessService accessService = mock(ResourceAccessService.class);
        DocumentUploadService uploadService = new DocumentUploadService(documentMapper, mock(DocumentProcessingTaskMapper.class),
                spaceMapper, accessService,
                mock(ApplicationEventPublisher.class), transactionTemplate(), tempDir.toString());
        return mockMvc(new DocumentController(documentMapper, mock(KbDocumentChunkMapper.class),
                mock(KbFaqMapper.class), chunkService, mock(LLMClient.class), accessService, uploadService,
                mock(DocumentProcessingService.class)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TransactionTemplate transactionTemplate() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer((Answer<Object>) invocation -> {
            TransactionCallback callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        return transactionTemplate;
    }

    private MockMvc mockMvc(DocumentController controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void login() {
        LoginUser user = new LoginUser();
        user.setId(1L);
        user.setRole(RoleNames.EMPLOYEE);
        UserContext.set(user);
    }
}
