package com.zhiyan.kb.service;

import com.zhiyan.kb.dto.ChatMemoryMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class QueryRewriteService {
    private static final Pattern REFERENCE_WORDS = Pattern.compile("(?i).*(\\bit\\b|\\bthis\\b|\\bthat\\b|\\babove\\b|\\bprevious\\b|\\bformer\\b|\\blatter\\b|\\bthey\\b|\\bthem\\b|\\bthese\\b|\\bthose\\b|\\bwhat about\\b|\\bhow about\\b|\\bcompared?\\b|\\bthe config\\b|\\bthe issue\\b|他|她|它|这个|这[个些样种首篇段]?|那个|那些|上面|上述|前面|刚才|前者|后者|同上|该问题|该配置|相比|比较|和他比|跟他比|比他|比她|比它).*");
    private static final Pattern ELLIPTICAL_FOLLOW_UP = Pattern.compile("(?i).*(\\bwhat about\\b|\\bhow about\\b|\\bprod\\b|\\btest\\b|\\btips?\\b|\\bguide\\b|\\badvice\\b|呢|那|还有|继续|展开|详细|怎么办|怎么做|如何处理|生产|测试|预发|技巧|攻略|建议|推荐|玩法|来点|讲讲|说说).*");

    public String rewrite(String question, List<ChatMemoryMessage> context) {
        if (question == null || question.isBlank()) {
            return question;
        }
        String trimmed = question.trim();
        if (!shouldUseContext(trimmed, context)) {
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

    public boolean shouldUseContext(String question, List<ChatMemoryMessage> context) {
        if (question == null || question.isBlank() || context == null || context.isEmpty()) {
            return false;
        }
        String trimmed = question.trim();
        if (REFERENCE_WORDS.matcher(trimmed).matches()) {
            return true;
        }
        return trimmed.length() <= 24 && ELLIPTICAL_FOLLOW_UP.matcher(trimmed).matches();
    }
}
