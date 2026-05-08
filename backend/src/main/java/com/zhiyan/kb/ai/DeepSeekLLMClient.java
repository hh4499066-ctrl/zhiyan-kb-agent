package com.zhiyan.kb.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zhiyan.kb.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeepSeekLLMClient implements LLMClient {
    private final AiProperties properties;
    private final RestClient restClient;

    public DeepSeekLLMClient(AiProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());
        this.restClient = builder
                .baseUrl(trimTrailingSlash(properties.cleanBaseUrl()))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String complete(String prompt) {
        String apiKey = properties.cleanApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("Real AI mode is enabled, but DEEPSEEK_API_KEY is not configured");
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
        int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long started = System.currentTimeMillis();
            try {
                String response = restClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + apiKey)
                        .body(request)
                        .retrieve()
                        .body(String.class);
                log.info("DeepSeek request succeeded model={} elapsedMs={}", properties.cleanChatModel(), System.currentTimeMillis() - started);
                return parseContent(response);
            } catch (RestClientResponseException ex) {
                handleHttpError(ex, attempt, maxAttempts);
            } catch (ResourceAccessException ex) {
                handleRetryableError("DeepSeek network timeout", ex, attempt, maxAttempts);
            } catch (Exception ex) {
                log.error("DeepSeek request failed", ex);
                throw new BusinessException("DeepSeek request failed: " + ex.getMessage());
            }
        }
        throw new BusinessException("DeepSeek request failed");
    }

    private void handleHttpError(RestClientResponseException ex, int attempt, int maxAttempts) {
        int status = ex.getStatusCode().value();
        if (status == 401 || status == 403) {
            throw new BusinessException(502, "DeepSeek authentication failed");
        }
        if (status == 429 || status >= 500) {
            handleRetryableError("DeepSeek retryable HTTP " + status, ex, attempt, maxAttempts);
            return;
        }
        throw new BusinessException(502, "DeepSeek HTTP error " + status + ": " + safeResponse(ex));
    }

    private void handleRetryableError(String message, Exception ex, int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            log.error("{} after {} attempts", message, attempt, ex);
            throw new BusinessException(502, message + ": " + ex.getMessage());
        }
        long delay = Math.min(1000L * (1L << (attempt - 1)), 4000L);
        log.warn("{} attempt={} nextDelayMs={}", message, attempt, delay);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new BusinessException(502, "DeepSeek retry interrupted");
        }
    }

    private String safeResponse(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return ex.getMessage();
        }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }

    private String parseContent(String response) {
        JSONObject root = JSONUtil.parseObj(response);
        JSONArray choices = root.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException("DeepSeek response contains no choices");
        }
        JSONObject first = choices.getJSONObject(0);
        JSONObject message = first.getJSONObject("message");
        String content = message == null ? null : message.getStr("content");
        if (content == null || content.isBlank()) {
            throw new BusinessException("DeepSeek response contains no message.content");
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
