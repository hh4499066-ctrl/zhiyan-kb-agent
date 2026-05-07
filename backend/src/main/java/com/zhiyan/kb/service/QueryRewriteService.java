package com.zhiyan.kb.service;

import com.zhiyan.kb.dto.ChatMemoryMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryRewriteService {
    public String rewrite(String question, List<ChatMemoryMessage> context) {
        if (context == null || context.isEmpty() || question.length() > 12) {
            return question;
        }
        String lastUserQuestion = context.stream()
                .filter(m -> "USER".equals(m.getRole()))
                .map(ChatMemoryMessage::getContent)
                .reduce((a, b) -> b)
                .orElse("");
        if (lastUserQuestion.isBlank()) {
            return question;
        }
        return lastUserQuestion + "。进一步追问：" + question;
    }
}
