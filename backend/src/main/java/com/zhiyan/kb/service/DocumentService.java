package com.zhiyan.kb.service;

import com.zhiyan.kb.mapper.KbDocumentMapper;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {
    private final KbDocumentMapper mapper;

    public DocumentService(KbDocumentMapper mapper) {
        this.mapper = mapper;
    }

    public long count() {
        return mapper.selectCount(null);
    }
}
