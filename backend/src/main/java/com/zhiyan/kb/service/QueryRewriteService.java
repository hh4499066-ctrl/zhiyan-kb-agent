package com.zhiyan.kb.service;

import com.zhiyan.kb.dto.ChatMemoryMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class QueryRewriteService {
    private static final Pattern REFERENCE_WORDS = Pattern.compile("(?i).*(it|this|that|above|previous|former|latter|they|them|these|those|\\bthe config\\b|\\bthe issue\\b|它|这个|这|那个|上面|刚才|前者|后者|该问题|该配置|这些|那些).*");

    public String rewrite(String question, List<ChatMemoryMessage> context) {
        if (question == null || question.isBlank() || context == null || context.isEmpty()) {
            return question;
        }
        String trimmed = question.trim();
        if (trimmed.length() > 32 && !REFERENCE_WORDS.matcher(trimmed).matches()) {
            return trimmed;
        }
        String lastUserQuestion = context.stream()
                .filter(m -> "USER".equals(m.getRole()))
                .map(ChatMemoryMessage::getContent)
                .filter(content -> content != null && !content.isBlank())
                .reduce((a, b) -> b)
                .orElse("");
        if (lastUserQuestion.isBlank()) {
            return trimmed;
        }
        return lastUserQuestion + ". Follow-up question: " + trimmed;
    }
}
