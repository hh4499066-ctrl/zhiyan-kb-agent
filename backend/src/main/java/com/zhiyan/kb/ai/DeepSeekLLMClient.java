package com.zhiyan.kb.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zhiyan.kb.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeepSeekLLMClient implements LLMClient {
    private final AiProperties properties;
    private final RestClient restClient;

    public DeepSeekLLMClient(AiProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(trimTrailingSlash(properties.cleanBaseUrl())).build();
    }

    @Override
    public String complete(String prompt) {
        String apiKey = properties.cleanApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("真实 AI 模式已开启，但未配置 DEEPSEEK_API_KEY");
        }
        Map<String, Object> request = Map.of(
                "model", properties.cleanChatModel(),
                "temperature", properties.getTemperature(),
                "max_tokens", properties.getMaxTokens(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an enterprise R&D knowledge-base AI assistant. Answer based on the provided knowledge context. Long-term memories in the user prompt describe the current logged-in user, not you. Never adopt the user's name, identity, preferences, or project background as your own."),
                        Map.of("role", "user", "content", prompt)
                )
        );
        try {
            String response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            return parseContent(response);
        } catch (Exception ex) {
            log.error("DeepSeek 调用失败", ex);
            throw new BusinessException("DeepSeek 调用失败：" + ex.getMessage());
        }
    }

    private String parseContent(String response) {
        JSONObject root = JSONUtil.parseObj(response);
        JSONArray choices = root.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException("DeepSeek 响应为空");
        }
        JSONObject first = choices.getJSONObject(0);
        JSONObject message = first.getJSONObject("message");
        String content = message == null ? null : message.getStr("content");
        if (content == null || content.isBlank()) {
            throw new BusinessException("DeepSeek 响应未包含 message.content");
        }
        return content.trim();
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://api.deepseek.com";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
