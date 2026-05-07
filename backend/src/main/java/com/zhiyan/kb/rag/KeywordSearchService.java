package com.zhiyan.kb.rag;

import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class KeywordSearchService {
    public double score(String question, String content) {
        if (question == null || content == null) {
            return 0;
        }
        String normalized = question.replaceAll("[，。！？；、,.!?;]", " ");
        long hits = Arrays.stream(normalized.split("\\s+"))
                .filter(w -> !w.isBlank() && w.length() > 1)
                .filter(content::contains)
                .count();
        if (hits == 0) {
            for (int i = 0; i < question.length(); i += 2) {
                int end = Math.min(question.length(), i + 2);
                if (end > i && content.contains(question.substring(i, end))) {
                    hits++;
                }
            }
        }
        return Math.min(1.0, hits / 6.0);
    }
}
