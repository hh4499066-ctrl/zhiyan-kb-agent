package com.zhiyan.kb.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordSearchServiceTest {
    @Test
    void scoreShouldBeHigherWhenContentContainsQuestionTerms() {
        KeywordSearchService service = new KeywordSearchService();

        double matched = service.score("项目启动失败端口冲突怎么办", "项目启动失败时优先检查端口占用，端口冲突可修改 server.port。");
        double missed = service.score("项目启动失败端口冲突怎么办", "代码提交信息应包含类型和说明。");

        assertThat(matched).isGreaterThan(missed);
    }
}
