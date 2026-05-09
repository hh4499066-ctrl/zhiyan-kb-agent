package com.zhiyan.kb.controller;

import com.zhiyan.kb.ai.LLMClient;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbFaqMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import com.zhiyan.kb.service.ChunkService;
import com.zhiyan.kb.service.DocumentParseService;
import com.zhiyan.kb.service.ResourceAccessService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentControllerTest {
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
    void updateOnlyPersistsEditableDocumentFields() {
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        ResourceAccessService accessService = mock(ResourceAccessService.class);
        KbDocument existing = new KbDocument();
        existing.setId(10L);
        existing.setSpaceId(1L);
        when(accessService.requireDocumentManage(10L)).thenReturn(existing);
        DocumentController controller = new DocumentController(documentMapper, mock(KbDocumentChunkMapper.class),
                mock(KbFaqMapper.class), mock(KbSpaceMapper.class), mock(DocumentParseService.class),
                mock(ChunkService.class), mock(LLMClient.class), accessService, "uploads");
        KbDocument request = new KbDocument();
        request.setSpaceId(999L);
        request.setTitle("moved");
        request.setSummary("summary");
        request.setKeywords("k1,k2");
        request.setFileUrl("/tmp/evil");
        request.setContentText("rewritten content");
        request.setParseStatus("FAILED");
        request.setVectorStatus("FAILED");
        request.setStatus("DELETED");
        request.setUploaderId(999L);

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
}
