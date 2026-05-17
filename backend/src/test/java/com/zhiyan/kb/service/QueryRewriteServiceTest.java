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
    void rewriteShortIndependentQuestionShouldRemainUnchanged() {
        QueryRewriteService service = new QueryRewriteService();
        List<ChatMemoryMessage> context = List.of(
                new ChatMemoryMessage("USER", "你是谁，我是谁", LocalDateTime.now()),
                new ChatMemoryMessage("ASSISTANT", "我是企业知识库助手。", LocalDateTime.now())
        );

        assertThat(service.rewrite("我爱打瓦", context)).isEqualTo("我爱打瓦");
        assertThat(service.shouldUseContext("我爱打瓦", context)).isFalse();
    }

    @Test
    void rewritePronounFollowUpShouldUseLastUserQuestion() {
        QueryRewriteService service = new QueryRewriteService();
        List<ChatMemoryMessage> context = List.of(
                new ChatMemoryMessage("USER", "周杰伦的歌好听吗？", LocalDateTime.now()),
                new ChatMemoryMessage("ASSISTANT", "好听。", LocalDateTime.now())
        );

        String rewritten = service.rewrite("林俊杰和他比谁的歌好听？", context);

        assertThat(rewritten)
                .contains("周杰伦的歌好听吗")
                .contains("林俊杰和他比谁的歌好听");
    }

    @Test
    void rewriteGenericTipsFollowUpShouldUsePreviousTopic() {
        QueryRewriteService service = new QueryRewriteService();
        List<ChatMemoryMessage> context = List.of(
                new ChatMemoryMessage("USER", "我爱打瓦", LocalDateTime.now()),
                new ChatMemoryMessage("ASSISTANT", "打瓦通常指《无畏契约》。", LocalDateTime.now())
        );

        String rewritten = service.rewrite("给我来点游戏技巧", context);

        assertThat(rewritten)
                .contains("我爱打瓦")
                .contains("给我来点游戏技巧");
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
