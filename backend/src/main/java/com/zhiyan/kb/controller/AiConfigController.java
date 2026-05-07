package com.zhiyan.kb.controller;

import com.zhiyan.kb.ai.AiProperties;
import com.zhiyan.kb.common.RequireRole;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-config")
@RequireRole(RoleNames.ADMIN)
public class AiConfigController {
    private final AiProperties properties;

    public AiConfigController(AiProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public Result<Map<String, Object>> config() {
        return Result.ok(Map.of(
                "mode", properties.getMode(),
                "provider", properties.getProvider(),
                "baseUrl", properties.cleanBaseUrl(),
                "chatModel", properties.cleanChatModel(),
                "apiKeyConfigured", properties.cleanApiKey() != null && !properties.cleanApiKey().isBlank()
        ));
    }
}
