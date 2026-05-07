package com.zhiyan.kb.service;

import java.util.List;

public interface SemanticChunkService {
    default List<String> semanticSplit(String text) {
        return List.of(text);
    }
}
