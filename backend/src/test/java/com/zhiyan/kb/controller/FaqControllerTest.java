package com.zhiyan.kb.controller;

import com.zhiyan.kb.dto.UpdateFaqRequest;
import com.zhiyan.kb.entity.KbFaq;
import com.zhiyan.kb.mapper.KbFaqMapper;
import com.zhiyan.kb.service.ResourceAccessService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FaqControllerTest {
    @Test
    void updateOnlyPersistsEditableFaqFields() {
        KbFaqMapper faqMapper = mock(KbFaqMapper.class);
        ResourceAccessService accessService = mock(ResourceAccessService.class);
        KbFaq existing = new KbFaq();
        existing.setId(10L);
        existing.setSpaceId(1L);
        existing.setStatus("NORMAL");
        when(faqMapper.selectById(10L)).thenReturn(existing);
        FaqController controller = new FaqController(faqMapper, accessService);
        UpdateFaqRequest request = new UpdateFaqRequest();
        request.setQuestion("question");
        request.setAnswer("answer");
        request.setTags("tag");

        controller.update(10L, request);

        verify(accessService).requireSpaceManage(1L);
        ArgumentCaptor<KbFaq> captor = ArgumentCaptor.forClass(KbFaq.class);
        verify(faqMapper).updateById(captor.capture());
        KbFaq update = captor.getValue();
        assertThat(update.getId()).isEqualTo(10L);
        assertThat(update.getQuestion()).isEqualTo("question");
        assertThat(update.getAnswer()).isEqualTo("answer");
        assertThat(update.getTags()).isEqualTo("tag");
        assertThat(update.getSpaceId()).isNull();
        assertThat(update.getDocumentId()).isNull();
        assertThat(update.getCreateType()).isNull();
        assertThat(update.getStatus()).isNull();
    }
}
