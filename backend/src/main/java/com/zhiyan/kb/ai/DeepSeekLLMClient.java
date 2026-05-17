package com.zhiyan.kb.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.dto.ChatMemoryMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class DeepSeekLLMClient implements LLMClient {
    private static final List<String> CHAT_MODEL_ALLOWLIST = List.of("deepseek-v4-flash", "deepseek-v4-pro");
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
        return complete(prompt, List.of());
    }

    @Override
    public String complete(String prompt, List<ChatMemoryMessage> context) {
        return complete(prompt, context, null);
    }

    @Override
    public String complete(String prompt, List<ChatMemoryMessage> context, String model) {
        String apiKey = properties.cleanApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("Real AI mode is enabled, but DEEPSEEK_API_KEY is not configured");
        }
        String chatModel = resolveChatModel(model);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are an enterprise R&D knowledge-base AI assistant. Prioritize the provided knowledge context. If no relevant knowledge-base context is provided, answer using your general model capability and clearly state that there is no knowledge-base source. Never fabricate citations. Long-term memories in the user prompt describe the current logged-in user, not you. Never adopt the user's name, identity, preferences, or project background as your own."));
        appendContextMessages(messages, context);
        messages.add(Map.of("role", "user", "content", prompt));
        Map<String, Object> request = Map.of(
                "model", chatModel,
                "temperature", properties.getTemperature(),
                "max_tokens", properties.getMaxTokens(),
                "stream", false,
                "messages", messages
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
                log.info("DeepSeek request succeeded model={} elapsedMs={}", chatModel, System.currentTimeMillis() - started);
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

    private String resolveChatModel(String model) {
        String requested = model == null ? "" : model.trim();
        if (requested.isBlank()) {
            return properties.cleanChatModel();
        }
        if (!CHAT_MODEL_ALLOWLIST.contains(requested)) {
            throw new BusinessException(400, "Unsupported chat model: " + requested);
        }
        return requested;
    }

    private void appendContextMessages(List<Map<String, Object>> messages, List<ChatMemoryMessage> context) {
        if (context == null || context.isEmpty()) {
            return;
        }
        context.stream()
                .filter(message -> message.getContent() != null && !message.getContent().isBlank())
                .forEach(message -> {
                    String role = normalizeRole(message.getRole());
                    if (role != null) {
                        messages.add(Map.of("role", role, "content", message.getContent()));
                    }
                });
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "USER" -> "user";
            case "ASSISTANT" -> "assistant";
            case "SYSTEM" -> "system";
            default -> null;
        };
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://api.deepseek.com";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
