package com.zhiyan.kb.controller;

import com.zhiyan.kb.ai.LLMClient;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.GlobalExceptionHandler;
import com.zhiyan.kb.common.LoginUser;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbFaqMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import com.zhiyan.kb.service.ChunkService;
import com.zhiyan.kb.service.DocumentParseService;
import com.zhiyan.kb.service.ResourceAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
        DocumentParseService parseService = mock(DocumentParseService.class);
        ChunkService chunkService = mock(ChunkService.class);
        LLMClient llmClient = mock(LLMClient.class);
        ResourceAccessService accessService = mock(ResourceAccessService.class);
        when(parseService.parse(any(File.class), eq("txt"))).thenReturn("hello");
        when(chunkService.rebuildChunks(any(KbDocument.class))).thenReturn(List.of());
        MockMvc mvc = mockMvc(new DocumentController(documentMapper, chunkMapper, faqMapper, spaceMapper,
                parseService, chunkService, llmClient, accessService, tempDir.toString()));
        login();

        mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes()))
                        .param("spaceId", "1"))
                .andExpect(jsonPath("$.code").value(200));

        verify(documentMapper).insert(any(KbDocument.class));
        verify(chunkService).rebuildChunks(any(KbDocument.class));
    }

    @Test
    void uploadRejectsUserWithoutManagePermission() throws Exception {
        ResourceAccessService accessService = mock(ResourceAccessService.class);
        doThrow(new BusinessException(403, "No permission to manage this space"))
                .when(accessService).requireSpaceManage(1L);
        MockMvc mvc = mockMvc(new DocumentController(mock(KbDocumentMapper.class), mock(KbDocumentChunkMapper.class),
                mock(KbFaqMapper.class), mock(KbSpaceMapper.class), mock(DocumentParseService.class),
                mock(ChunkService.class), mock(LLMClient.class), accessService, tempDir.toString()));

        mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes()))
                        .param("spaceId", "1"))
                .andExpect(jsonPath("$.code").value(403));
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
        return mockMvc(new DocumentController(mock(KbDocumentMapper.class), mock(KbDocumentChunkMapper.class),
                mock(KbFaqMapper.class), mock(KbSpaceMapper.class), mock(DocumentParseService.class),
                mock(ChunkService.class), mock(LLMClient.class), mock(ResourceAccessService.class), tempDir.toString()));
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
