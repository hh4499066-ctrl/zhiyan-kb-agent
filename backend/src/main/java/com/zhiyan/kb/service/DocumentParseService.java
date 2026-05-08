package com.zhiyan.kb.service;

import cn.hutool.core.io.FileUtil;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class DocumentParseService {
    public String parse(File file, String fileType) throws IOException {
        String type = fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
        return switch (type) {
            case "txt", "md" -> FileUtil.readString(file, StandardCharsets.UTF_8);
            case "pdf" -> parsePdfIfAvailable(file);
            case "docx" -> parseDocxIfAvailable(file);
            default -> throw new IOException("Unsupported document type: " + fileType);
        };
    }

    private String parsePdfIfAvailable(File file) throws IOException {
        try {
            Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
            Class<?> documentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> stripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
            Object document = loaderClass.getMethod("loadPDF", File.class).invoke(null, file);
            try {
                Object stripper = stripperClass.getConstructor().newInstance();
                Method getText = stripperClass.getMethod("getText", documentClass);
                return String.valueOf(getText.invoke(stripper, document));
            } finally {
                documentClass.getMethod("close").invoke(document);
            }
        } catch (ClassNotFoundException ex) {
            throw new IOException("PDF parsing requires the pdfbox dependency. Run Maven reload or use txt/md for this build.", ex);
        } catch (ReflectiveOperationException ex) {
            throw new IOException("PDF parsing failed", ex);
        }
    }

    private String parseDocxIfAvailable(File file) throws IOException {
        try {
            Class<?> documentClass = Class.forName("org.apache.poi.xwpf.usermodel.XWPFDocument");
            Object document = documentClass.getConstructor(java.io.InputStream.class).newInstance(new FileInputStream(file));
            try {
                Method getParagraphs = documentClass.getMethod("getParagraphs");
                @SuppressWarnings("unchecked")
                java.util.List<Object> paragraphs = (java.util.List<Object>) getParagraphs.invoke(document);
                String paragraphText = paragraphs.stream()
                        .map(this::invokeGetText)
                        .filter(text -> text != null && !text.isBlank())
                        .collect(Collectors.joining("\n\n"));

                Method getTables = documentClass.getMethod("getTables");
                @SuppressWarnings("unchecked")
                java.util.List<Object> tables = (java.util.List<Object>) getTables.invoke(document);
                String tableText = tables.stream()
                        .map(this::tableText)
                        .filter(text -> !text.isBlank())
                        .collect(Collectors.joining("\n\n"));
                return (paragraphText + "\n\n" + tableText).trim();
            } finally {
                documentClass.getMethod("close").invoke(document);
            }
        } catch (ClassNotFoundException ex) {
            throw new IOException("DOCX parsing requires the poi-ooxml dependency. Run Maven reload or use txt/md for this build.", ex);
        } catch (ReflectiveOperationException ex) {
            throw new IOException("DOCX parsing failed", ex);
        }
    }

    private String tableText(Object table) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Object> rows = (java.util.List<Object>) table.getClass().getMethod("getRows").invoke(table);
            return rows.stream()
                    .map(row -> {
                        try {
                            @SuppressWarnings("unchecked")
                            java.util.List<Object> cells = (java.util.List<Object>) row.getClass().getMethod("getTableCells").invoke(row);
                            return cells.stream().map(this::invokeGetText).collect(Collectors.joining(" | "));
                        } catch (ReflectiveOperationException ex) {
                            return "";
                        }
                    })
                    .collect(Collectors.joining("\n"));
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }

    private String invokeGetText(Object target) {
        try {
            Object text = target.getClass().getMethod("getText").invoke(target);
            return text == null ? "" : String.valueOf(text).trim();
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }
}
