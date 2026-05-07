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
                new ChatMemoryMessage("USER", "Java 项目启动失败怎么办？", LocalDateTime.now()),
                new ChatMemoryMessage("ASSISTANT", "检查端口、配置和依赖。", LocalDateTime.now())
        );

        String rewritten = service.rewrite("端口呢？", context);

        assertThat(rewritten).contains("Java 项目启动失败怎么办？").contains("端口呢？");
    }
}
