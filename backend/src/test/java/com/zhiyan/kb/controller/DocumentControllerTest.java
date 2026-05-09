package com.zhiyan.kb.controller;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

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
