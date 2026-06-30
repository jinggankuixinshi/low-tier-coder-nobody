package com.example.evaluation.ai;

import com.example.evaluation.config.AiConfig;
import com.example.evaluation.service.AIService;
import com.example.evaluation.utils.PromptUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * 云端 AI 服务实现（OpenAI 兼容接口）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "cloud", matchIfMissing = true)
public class CloudAIService implements AIService {

    private static final String SYSTEM_PROMPT =
            "你是严格公正的教育评价系统，必须基于实际提交内容进行客观评分。" +
            "不得编造、臆断、猜测内容。如内容为空或与指标无关，必须返回score=0并说明原因。" +
            "只输出JSON格式结果，不输出任何额外解释或标记。";

    private static final int DEFAULT_MAX_TOKENS = 4096;
    private static final double EVAL_TEMPERATURE = 0.1;
    private static final int EVAL_MAX_TOKENS = 1024;
    private static final int MAX_RETRY = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    private final AiConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CloudAIService(AiConfig aiConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.aiConfig = aiConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> parseImage(String base64Image) {
        log.info("开始解析图片");
        String prompt = PromptUtil.getImageParsePrompt();
        String response = callVisionApi(prompt, base64Image);
        return parseJsonResponse(response);
    }

    @Override
    public Map<String, Object> verifySubmission(String parsedContent, String checkPoints) {
        log.info("开始智能核查");
        String prompt = PromptUtil.getVerificationPrompt(parsedContent, checkPoints);
        String response = callChatApi(prompt, aiConfig.getCloud().getModel(), EVAL_TEMPERATURE, EVAL_MAX_TOKENS);
        return parseJsonResponse(response);
    }

    @Override
    public Map<String, Object> evaluateSubmission(String submissionContent, String indicatorName, String indicatorDesc) {
        log.info("开始自动评分，指标: {}", indicatorName);
        String prompt = PromptUtil.getEvaluationPrompt(submissionContent, indicatorName, indicatorDesc);
        String response = callChatApi(prompt, aiConfig.getCloud().getModel(), EVAL_TEMPERATURE, EVAL_MAX_TOKENS);
        return parseScoreResponse(response);
    }

    private String callChatApi(String prompt, String model, double temperature, int maxTokens) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        return doPost("/chat/completions", requestBody);
    }

    private String callVisionApi(String prompt, String base64Image) {
        String visionModel = aiConfig.getCloud().getVisionModel();
        if (visionModel == null || visionModel.isBlank()) {
            visionModel = aiConfig.getCloud().getModel();
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", visionModel);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text",
                            "你是实训成果分析助手，只输出JSON格式结果，不输出任何额外解释。" + prompt),
                        Map.of("type", "image_url", "image_url",
                                Map.of("url", "data:image/png;base64," + base64Image))
                ))
        ));
        requestBody.put("max_tokens", DEFAULT_MAX_TOKENS);

        return doPost("/chat/completions", requestBody);
    }

    private String doPost(String path, Map<String, Object> body) {
        Exception lastException = null;
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + aiConfig.getCloud().getApiKey());

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                String baseUrl = aiConfig.getCloud().getBaseUrl().replaceAll("/+$", "");
                String url = baseUrl + path;

                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    if (response.getBody() != null) {
                        return extractContentFromResponse(response.getBody());
                    }
                    return "";
                }

                int statusCode = response.getStatusCode().value();
                if (statusCode == 401 || statusCode == 403) {
                    log.error("AI API 鉴权失败 (HTTP {}): 请检查 AI_API_KEY 是否正确", statusCode);
                    throw new RuntimeException("AI 鉴权失败，请检查 API Key 配置");
                }
                if (statusCode >= 400 && statusCode < 500) {
                    log.error("AI API 客户端错误 (HTTP {}): {}", statusCode, response.getBody());
                    throw new RuntimeException("AI API 请求错误: HTTP " + statusCode);
                }
                log.warn("AI API 服务端错误 (HTTP {}), 第{}次尝试", statusCode, attempt + 1);
                lastException = new RuntimeException("AI 服务返回错误: " + statusCode);

            } catch (RuntimeException e) {
                if (e.getMessage() != null && (e.getMessage().contains("鉴权失败") || e.getMessage().contains("API Key"))) {
                    throw e;
                }
                lastException = e;
                log.warn("AI API 调用失败 (第{}/{}次): {}", attempt + 1, MAX_RETRY, e.getMessage());
            }

            if (attempt < MAX_RETRY - 1) {
                long delay = INITIAL_RETRY_DELAY_MS * (1L << attempt);
                log.info("等待 {}ms 后重试...", delay);
                try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        log.error("AI API 调用失败，已重试{}次", MAX_RETRY);
        throw new RuntimeException("AI 服务调用失败（已重试" + MAX_RETRY + "次）: " +
                (lastException != null ? lastException.getMessage() : "未知错误"), lastException);
    }

    @SuppressWarnings("unchecked")
    private String extractContentFromResponse(String response) {
        try {
            Map<String, Object> map = objectMapper.readValue(response, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) map.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
            return response;
        } catch (Exception e) {
            log.warn("解析 AI 响应失败，返回原始内容");
            return response;
        }
    }

    private Map<String, Object> parseJsonResponse(String content) {
        try {
            String json = extractJson(content);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析 AI 返回的 JSON 失败: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("score", 0);
            fallback.put("comment", "AI返回格式异常，无法解析评分");
            fallback.put("raw", content);
            return fallback;
        }
    }

    private Map<String, Object> parseScoreResponse(String content) {
        try {
            String json = extractJson(content);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

            Object scoreObj = map.get("score");
            if (!(scoreObj instanceof Number)) {
                map.put("score", 0);
            } else {
                double s = ((Number) scoreObj).doubleValue();
                if (s < 0 || s > 100) {
                    map.put("score", 0);
                }
            }

            Object commentObj = map.get("comment");
            if (!(commentObj instanceof String) || ((String) commentObj).isBlank()) {
                map.put("comment", "");
            }

            return map;
        } catch (Exception e) {
            log.warn("解析 AI 评分响应失败: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("score", 0);
            fallback.put("comment", "AI返回格式异常，无法解析评分");
            fallback.put("raw", content);
            return fallback;
        }
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
