package com.zhiyan.kb.service;

import com.zhiyan.kb.dto.ChatMemoryMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryRewriteServiceTest {
    @Test
    void rewriteShortQuestionShouldUseLastUserQuestion() {
        QueryRewriteService service = new QueryRewriteService();
        List<ChatMemoryMessage> context = List.of(
                new ChatMemoryMessage("USER", "How do I configure the Java service port?", LocalDateTime.now()),
                new ChatMemoryMessage("ASSISTANT", "Check server.port in application.yml.", LocalDateTime.now())
        );

        String rewritten = service.rewrite("What about prod?", context);

        assertThat(rewritten)
                .contains("How do I configure the Java service port?")
                .contains("What about prod?");
    }

    @Test
    void rewriteLongQuestionWithReferenceWordShouldUseContext() {
        QueryRewriteService service = new QueryRewriteService();
        List<ChatMemoryMessage> context = List.of(
                new ChatMemoryMessage("USER", "How is the upload directory configured?", LocalDateTime.now())
        );

        String rewritten = service.rewrite("Does this need to change in the test environment?", context);

        assertThat(rewritten).contains("upload directory").contains("test environment");
    }

    @Test
    void rewriteLongIndependentQuestionShouldRemainUnchanged() {
        QueryRewriteService service = new QueryRewriteService();
        List<ChatMemoryMessage> context = List.of(
                new ChatMemoryMessage("USER", "How is Redis used?", LocalDateTime.now())
        );
        String question = "Explain how document chunks are embedded and inserted into the vector store";

        assertThat(service.rewrite(question, context)).isEqualTo(question);
    }
}
