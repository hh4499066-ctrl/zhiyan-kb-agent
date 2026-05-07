package com.zhiyan.kb.service;

import com.zhiyan.kb.dto.ChatAskRequest;
import com.zhiyan.kb.vo.ChatAskResponse;

public interface ChatService {
    ChatAskResponse ask(ChatAskRequest request);
}
