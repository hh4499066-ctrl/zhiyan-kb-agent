package com.zhiyan.kb.ai;

import org.springframework.stereotype.Component;

@Component
public class AIResponseParser {
    public String compact(String response) {
        return response == null ? "" : response.trim();
    }
}
