package com.zhiyan.kb.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class DocumentParseService {
    private static final int MAX_TEXT_CHARS = 500_000;
    private static final int MAX_PDF_PAGES = 200;
    private static final int MAX_DOCX_PARAGRAPHS = 10_000;
    private static final int MAX_DOCX_TABLES = 200;
    private static final int MAX_DOCX_TABLE_CELLS = 20_000;

    public String parse(File file, String fileType) throws IOException {
        String type = fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
        return switch (type) {
            case "txt", "md" -> readLimitedText(file);
            case "pdf" -> parsePdfIfAvailable(file);
            case "docx" -> parseDocxIfAvailable(file);
            default -> throw new IOException("Unsupported document type: " + fileType);
        };
    }

    private String readLimitedText(File file) throws IOException {
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[8192];
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            int read;
            while ((read = reader.read(buffer)) != -1) {
                appendLimited(text, new String(buffer, 0, read));
            }
        }
        return text.toString();
    }

    private String parsePdfIfAvailable(File file) throws IOException {
        try {
            Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
            Class<?> documentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> stripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
            Object document = loaderClass.getMethod("loadPDF", File.class).invoke(null, file);
            try {
                int pages = (Integer) documentClass.getMethod("getNumberOfPages").invoke(document);
                if (pages > MAX_PDF_PAGES) {
                    throw new IOException("PDF page count exceeds " + MAX_PDF_PAGES);
                }
                Object stripper = stripperClass.getConstructor().newInstance();
                Method getText = stripperClass.getMethod("getText", documentClass);
                return requireWithinLimit(String.valueOf(getText.invoke(stripper, document)));
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
                if (paragraphs.size() > MAX_DOCX_PARAGRAPHS) {
                    throw new IOException("DOCX paragraph count exceeds " + MAX_DOCX_PARAGRAPHS);
                }
                StringBuilder text = new StringBuilder();
                for (Object paragraph : paragraphs) {
                    appendBlock(text, invokeGetText(paragraph));
                }

                Method getTables = documentClass.getMethod("getTables");
                @SuppressWarnings("unchecked")
                java.util.List<Object> tables = (java.util.List<Object>) getTables.invoke(document);
                if (tables.size() > MAX_DOCX_TABLES) {
                    throw new IOException("DOCX table count exceeds " + MAX_DOCX_TABLES);
                }
                int cellCount = 0;
                for (Object table : tables) {
                    TableText tableText = tableText(table);
                    cellCount += tableText.cellCount();
                    if (cellCount > MAX_DOCX_TABLE_CELLS) {
                        throw new IOException("DOCX table cell count exceeds " + MAX_DOCX_TABLE_CELLS);
                    }
                    appendBlock(text, tableText.text());
                }
                return text.toString().trim();
            } finally {
                documentClass.getMethod("close").invoke(document);
            }
        } catch (ClassNotFoundException ex) {
            throw new IOException("DOCX parsing requires the poi-ooxml dependency. Run Maven reload or use txt/md for this build.", ex);
        } catch (ReflectiveOperationException ex) {
            throw new IOException("DOCX parsing failed", ex);
        }
    }

    private TableText tableText(Object table) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Object> rows = (java.util.List<Object>) table.getClass().getMethod("getRows").invoke(table);
            StringBuilder text = new StringBuilder();
            int cellCount = 0;
            for (Object row : rows) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> cells = (java.util.List<Object>) row.getClass().getMethod("getTableCells").invoke(row);
                    cellCount += cells.size();
                    StringBuilder rowText = new StringBuilder();
                    for (Object cell : cells) {
                        if (!rowText.isEmpty()) {
                            rowText.append(" | ");
                        }
                        rowText.append(invokeGetText(cell));
                    }
                    if (!text.isEmpty()) {
                        text.append('\n');
                    }
                    text.append(rowText);
                } catch (ReflectiveOperationException ex) {
                    return new TableText("", cellCount);
                }
            }
            return new TableText(text.toString(), cellCount);
        } catch (ReflectiveOperationException ex) {
            return new TableText("", 0);
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

    private void appendBlock(StringBuilder target, String text) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!target.isEmpty()) {
            appendLimited(target, "\n\n");
        }
        appendLimited(target, text.trim());
    }

    private String requireWithinLimit(String text) throws IOException {
        if (text != null && text.length() > MAX_TEXT_CHARS) {
            throw new IOException("Parsed text exceeds " + MAX_TEXT_CHARS + " characters");
        }
        return text;
    }

    private void appendLimited(StringBuilder target, String text) throws IOException {
        if (target.length() + text.length() > MAX_TEXT_CHARS) {
            throw new IOException("Parsed text exceeds " + MAX_TEXT_CHARS + " characters");
        }
        target.append(text);
    }

    private record TableText(String text, int cellCount) {
    }
}
