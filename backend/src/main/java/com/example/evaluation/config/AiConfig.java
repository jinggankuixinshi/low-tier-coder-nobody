package com.example.evaluation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /**
     * AI 服务提供者: cloud / local
     */
    private String provider = "cloud";

    /**
     * 云端 API 配置
     */
    private CloudConfig cloud = new CloudConfig();

    /**
     * 本地模型配置
     */
    private LocalConfig local = new LocalConfig();

    /**
     * AI 调用超时时间（秒）
     */
    private int timeout = 120;

    @Data
    public static class CloudConfig {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "gpt-3.5-turbo";
        private String visionModel = "gpt-4-vision-preview";
    }

    @Data
    public static class LocalConfig {
        private String baseUrl = "http://localhost:11434/v1";
        private String model = "llama3";
    }
}
