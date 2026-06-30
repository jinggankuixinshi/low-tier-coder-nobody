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

import java.util.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "local")
public class LocalAIService implements AIService {

    private static final String SYSTEM_PROMPT =
            "你是一个专业的教育评价助手，请严格按照JSON格式返回结果。";

    private static final double DEFAULT_TEMPERATURE = 0.3;
    private static final int DEFAULT_MAX_TOKENS = 4096;
    private static final double EVAL_TEMPERATURE = 0.1;
    private static final int EVAL_MAX_TOKENS = 1024;

    private final AiConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LocalAIService(AiConfig aiConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.aiConfig = aiConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        log.info("已启用本地 AI 服务模式，baseUrl: {}, model: {}",
                aiConfig.getLocal().getBaseUrl(), aiConfig.getLocal().getModel());
    }

    @Override
    public Map<String, Object> parseImage(String base64Image) {
        log.info("[本地AI] 开始解析图片");
        String prompt = PromptUtil.getImageParsePrompt();
        String response = callVisionApi(prompt, base64Image);
        return parseJsonResponse(response);
    }

    @Override
    public Map<String, Object> verifySubmission(String parsedContent, String checkPoints) {
        log.info("[本地AI] 开始智能核查");
        String prompt = PromptUtil.getVerificationPrompt(parsedContent, checkPoints);
        String response = callChatApi(prompt, EVAL_TEMPERATURE, EVAL_MAX_TOKENS);
        return parseJsonResponse(response);
    }

    @Override
    public Map<String, Object> evaluateSubmission(String submissionContent, String indicatorName, String indicatorDesc) {
        log.info("[本地AI] 开始自动评分，指标: {}", indicatorName);
        String prompt = PromptUtil.getEvaluationPrompt(submissionContent, indicatorName, indicatorDesc);
        String response = callChatApi(prompt, EVAL_TEMPERATURE, EVAL_MAX_TOKENS);
        return parseScoreResponse(response);
    }

    private String callChatApi(String prompt, double temperature, int maxTokens) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiConfig.getLocal().getModel());
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        return doPost("/chat/completions", requestBody);
    }

    private String callVisionApi(String prompt, String base64Image) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiConfig.getLocal().getModel());
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text", prompt),
                        Map.of("type", "image_url", "image_url",
                                Map.of("url", "data:image/png;base64," + base64Image))
                ))
        ));
        requestBody.put("max_tokens", DEFAULT_MAX_TOKENS);
        return doPost("/chat/completions", requestBody);
    }

    private String doPost(String path, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = aiConfig.getLocal().getBaseUrl() + path;

            log.debug("[本地AI] 请求 URL: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getBody() != null) {
                return extractContentFromResponse(response.getBody());
            }
            return "";
        } catch (Exception e) {
            log.error("[本地AI] API 调用失败: {}", e.getMessage());
            throw new RuntimeException("本地 AI 服务调用失败: " + e.getMessage(), e);
        }
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
            log.warn("[本地AI] 解析响应失败，返回原始内容");
            return response;
        }
    }

    private Map<String, Object> parseJsonResponse(String content) {
        try {
            String json = extractJson(content);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[本地AI] 解析 JSON 失败: {}", e.getMessage());
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
            log.warn("[本地AI] 解析评分响应失败: {}", e.getMessage());
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
