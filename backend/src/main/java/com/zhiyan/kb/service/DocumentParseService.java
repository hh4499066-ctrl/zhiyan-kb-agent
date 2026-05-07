package com.zhiyan.kb.service;

import cn.hutool.core.io.FileUtil;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentParseService {
    public String parse(File file, String fileType) {
        if ("txt".equalsIgnoreCase(fileType) || "md".equalsIgnoreCase(fileType)) {
            return FileUtil.readString(file, StandardCharsets.UTF_8);
        }
        return "这是一个 " + fileType + " 文件的 Mock 解析内容。真实环境可在 DocumentParseService 中接入 PDF/DOCX 解析器。";
    }
}
