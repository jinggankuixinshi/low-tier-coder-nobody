package com.example.evaluation.utils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PromptUtil {

    private static final Map<String, String> PROMPTS = new ConcurrentHashMap<>();
    private static final Map<String, String> VERSIONS = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:prompts/*.txt");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                String name = filename.replace(".txt", "");
                try (InputStream is = resource.getInputStream()) {
                    String content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
                    PROMPTS.put(name, content);
                    String version = extractVersion(content);
                    if (version != null) {
                        VERSIONS.put(name, version);
                    }
                    log.info("加载提示词模板: {} (version: {})", filename, version);
                }
            }
            log.info("共加载 {} 个提示词模板", PROMPTS.size());
        } catch (Exception e) {
            log.error("加载提示词模板失败: {}", e.getMessage(), e);
        }
    }

    private static String extractVersion(String content) {
        if (content == null) return null;
        for (String line : content.split("\n")) {
            if (line.startsWith("#version ")) {
                return line.substring("#version ".length()).trim();
            }
        }
        return null;
    }

    private static String getPrompt(String name) {
        String content = PROMPTS.get(name);
        if (content == null) {
            log.warn("提示词模板未找到: {}", name);
            return "";
        }
        if (content.startsWith("#version ")) {
            int idx = content.indexOf('\n');
            return idx >= 0 ? content.substring(idx + 1).trim() : content;
        }
        return content;
    }

    public static String getImageParsePrompt() {
        return getPrompt("image-parse-prompt");
    }

    public static String getVerificationPrompt(String parsedContent, String checkPoints) {
        return getPrompt("verification-prompt")
                .replace("{parsedContent}", truncateContent(parsedContent))
                .replace("{checkPoints}", checkPoints);
    }

    public static String getEvaluationPrompt(String submissionContent, String indicatorName, String indicatorDesc) {
        return getPrompt("evaluation-prompt")
                .replace("{submissionContent}", truncateContent(submissionContent))
                .replace("{indicatorName}", indicatorName)
                .replace("{indicatorDesc}", indicatorDesc);
    }

    public static String getSystemPrompt() {
        return getPrompt("system-prompt");
    }

    public static String getPromptByKey(String key) {
        return getPrompt(key);
    }

    public static String getVersion(String name) {
        return VERSIONS.getOrDefault(name, null);
    }

    public static String truncateContent(String content) {
        if (content == null || content.isEmpty()) {
            return "（无内容）";
        }
        int maxLength = 24000;
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "\n...（内容过长已截断）";
    }
}
