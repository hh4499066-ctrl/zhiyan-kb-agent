package com.zhiyan.kb.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zhiyan.ai")
public class AiProperties {
    private String mode = "mock";
    private String provider = "deepseek";
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String chatModel = "deepseek-v4-flash";
    private java.util.List<String> chatModelAllowlist = java.util.List.of("deepseek-v4-flash", "deepseek-v4-pro");
    private Double temperature = 0.3;
    private Integer maxTokens = 1600;
    private Integer connectTimeoutMillis = 5000;
    private Integer readTimeoutMillis = 30000;
    private Integer maxRetries = 2;

    public boolean realMode() {
        return "real".equalsIgnoreCase(clean(mode));
    }

    public String cleanBaseUrl() {
        String value = clean(baseUrl);
        return value == null || value.isBlank() ? "https://api.deepseek.com" : value;
    }

    public String cleanApiKey() {
        return clean(apiKey);
    }

    public String cleanChatModel() {
        String value = clean(chatModel);
        return value == null || value.isBlank() ? "deepseek-v4-flash" : value;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
