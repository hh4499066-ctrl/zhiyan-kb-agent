package com.zhiyan.kb.service;

import com.zhiyan.kb.ai.AIResponseParser;
import com.zhiyan.kb.ai.LLMClient;
import com.zhiyan.kb.ai.PromptBuilder;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.dto.ChatAskRequest;
import com.zhiyan.kb.mapper.ChatRecordMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import com.zhiyan.kb.mapper.UnresolvedQuestionMapper;
import com.zhiyan.kb.rag.HybridRetrievalService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ChatServiceImplValidationTest {
    @Test
    void rejectsUnsupportedModelBeforeCallingRagPipeline() {
        ChatServiceImpl service = service();
        ChatAskRequest request = new ChatAskRequest();
        request.setQuestion("hello");
        request.setModel("unapproved-model");

        assertThatThrownBy(() -> service.ask(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unsupported chat model");
    }

    @Test
    void rejectsOversizedQuestionBeforeCallingRagPipeline() {
        ChatServiceImpl service = service();
        ChatAskRequest request = new ChatAskRequest();
        request.setQuestion("x".repeat(2001));

        assertThatThrownBy(() -> service.ask(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds 2000");
    }

    private ChatServiceImpl service() {
        return new ChatServiceImpl(
                mock(ShortTermMemoryService.class),
                mock(QueryRewriteService.class),
                mock(LongTermMemoryService.class),
                mock(HybridRetrievalService.class),
                mock(PromptBuilder.class),
                mock(LLMClient.class),
                mock(AIResponseParser.class),
                mock(ChatRecordMapper.class),
                mock(UnresolvedQuestionMapper.class),
                mock(KbSpaceMapper.class),
                mock(ResourceAccessService.class),
                mock(TransactionTemplate.class)
        );
    }
}
