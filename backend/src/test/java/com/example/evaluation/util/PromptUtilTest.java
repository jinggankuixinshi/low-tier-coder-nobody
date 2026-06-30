package com.example.evaluation.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class PromptUtilTest {

    @BeforeEach
    void setUp() throws Exception {
        Field promptsField = PromptUtil.class.getDeclaredField("PROMPTS");
        promptsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> prompts = (Map<String, String>) promptsField.get(null);

        prompts.put("evaluation-prompt",
                "请评估以下提交内容。\n指标名称：{indicatorName}\n指标描述：{indicatorDesc}\n提交内容：\n{submissionContent}\n\n请给出评分和评语。");
        prompts.put("verification-prompt",
                "请核查以下内容是否符合要求。\n解析内容：\n{parsedContent}\n核查要点：\n{checkPoints}\n\n请逐项核查。");
        prompts.put("image-parse-prompt",
                "请分析图片内容并给出详细描述，包括：步骤截图、界面布局、交互流程。");
        prompts.put("system-prompt",
                "你是一个实训成果评价助手，请严格按照评价标准进行评判。");
    }

    @AfterEach
    void tearDown() throws Exception {
        Field promptsField = PromptUtil.class.getDeclaredField("PROMPTS");
        promptsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> prompts = (Map<String, String>) promptsField.get(null);
        prompts.clear();

        Field versionsField = PromptUtil.class.getDeclaredField("VERSIONS");
        versionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> versions = (Map<String, String>) versionsField.get(null);
        versions.clear();
    }

    @Test
    @DisplayName("buildEvaluationPrompt returns non-empty string with task info and content")
    void testBuildEvaluationPrompt_ReturnsNonEmptyString() {
        String prompt = PromptUtil.getEvaluationPrompt(
                "学生提交的代码和文档内容", "技术质量", "评估代码结构是否清晰、注释是否完整");
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("技术质量"));
        assertTrue(prompt.contains("代码结构是否清晰"));
        assertTrue(prompt.contains("学生提交的代码和文档内容"));
    }

    @Test
    @DisplayName("buildEvaluationPrompt includes indicator name and description")
    void testBuildEvaluationPrompt_IncludesIndicatorInfo() {
        String prompt = PromptUtil.getEvaluationPrompt(
                "实训报告内容...", "文档规范性", "检查文档格式、排版是否规范");
        assertTrue(prompt.contains("文档规范性"));
        assertTrue(prompt.contains("文档格式"));
        assertFalse(prompt.contains("{indicatorName}"));
        assertFalse(prompt.contains("{indicatorDesc}"));
    }

    @Test
    @DisplayName("buildVerificationPrompt contains expected output")
    void testBuildVerificationPrompt_ContainsExpectedOutput() {
        String prompt = PromptUtil.getVerificationPrompt(
                "解析后的内容文本", "检查项1: 功能完整性, 检查项2: 界面美观度");
        assertNotNull(prompt);
        assertTrue(prompt.contains("解析后的内容文本"));
        assertTrue(prompt.contains("检查项1"));
        assertTrue(prompt.contains("检查项2"));
        assertFalse(prompt.contains("{parsedContent}"));
        assertFalse(prompt.contains("{checkPoints}"));
    }

    @Test
    @DisplayName("prompt handles null/missing fields gracefully")
    void testPrompt_HandlesNullFieldsGracefully() {
        String evaluationPrompt = PromptUtil.getEvaluationPrompt("", "", "");
        assertNotNull(evaluationPrompt);
        assertFalse(evaluationPrompt.isEmpty());

        String verificationPrompt = PromptUtil.getVerificationPrompt("", "");
        assertNotNull(verificationPrompt);
        assertFalse(verificationPrompt.isEmpty());

        String truncated = PromptUtil.truncateContent(null);
        assertEquals("（无内容）", truncated);

        String emptyTruncated = PromptUtil.truncateContent("");
        assertEquals("（无内容）", emptyTruncated);
    }

    @Test
    @DisplayName("truncateContent truncates very long content")
    void testTruncateContent_VeryLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longContent.append("ABCD");
        }
        String content = longContent.toString();

        String truncated = PromptUtil.truncateContent(content);
        assertNotNull(truncated);
        assertTrue(truncated.length() <= 24030);
        assertTrue(truncated.contains("内容过长已截断"));
        assertTrue(truncated.length() < content.length());
    }

    @Test
    @DisplayName("truncateContent returns placeholder for null content")
    void testTruncateContent_NullContent() {
        String result = PromptUtil.truncateContent(null);
        assertEquals("（无内容）", result);
    }

    @Test
    @DisplayName("truncateContent returns placeholder for empty content")
    void testTruncateContent_EmptyContent() {
        String result = PromptUtil.truncateContent("");
        assertEquals("（无内容）", result);
    }

    @Test
    @DisplayName("truncateContent does not truncate short content")
    void testTruncateContent_ShortContent() {
        String shortContent = "This is short content.";
        String result = PromptUtil.truncateContent(shortContent);
        assertEquals(shortContent, result);
    }
}
