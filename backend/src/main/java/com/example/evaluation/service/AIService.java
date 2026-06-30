package com.example.evaluation.service;

import java.util.Map;

/**
 * AI 服务接口
 * 提供图片OCR、智能核查、自动评分能力
 */
public interface AIService {

    /**
     * 调用 AI 解析图片内容（OCR + 描述）
     * @param base64Image 图片 base64 编码
     * @return 解析结果：{ "description": "...", "text": "...", "isScreenshot": true/false, "codeContent": "...", "outputContent": "..." }
     */
    Map<String, Object> parseImage(String base64Image);

    /**
     * 调用 AI 核查实训成果
     * @param submissionContent 提交文件提取的完整内容
     * @param checkPoints       检查点（JSON）
     * @return 核查结果列表
     */
    Map<String, Object> verifySubmission(String submissionContent, String checkPoints);

    /**
     * 调用 AI 按指标自动评分
     * @param submissionContent 提交文件提取的完整内容
     * @param indicatorName     指标名称
     * @param indicatorDesc     指标描述
     * @return 评分结果：{ "score": 85.5, "comment": "..." }
     */
    Map<String, Object> evaluateSubmission(String submissionContent, String indicatorName, String indicatorDesc);
}
