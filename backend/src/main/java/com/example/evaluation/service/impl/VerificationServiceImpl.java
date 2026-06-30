package com.example.evaluation.service.impl;

import com.example.evaluation.common.BusinessException;
import com.example.evaluation.entity.*;
import com.example.evaluation.mapper.*;
import com.example.evaluation.service.AIService;
import com.example.evaluation.service.VerificationService;
import com.example.evaluation.utils.FileContentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VerificationServiceImpl implements VerificationService {

    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final VerificationResultMapper verificationResultMapper;
    private final AIService aiService;
    private final FileContentExtractor fileContentExtractor;

    public VerificationServiceImpl(SubmissionMapper submissionMapper,
                                   TaskMapper taskMapper,
                                   VerificationResultMapper verificationResultMapper,
                                   AIService aiService,
                                   FileContentExtractor fileContentExtractor) {
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.verificationResultMapper = verificationResultMapper;
        this.aiService = aiService;
        this.fileContentExtractor = fileContentExtractor;
    }

    @Override
    public List<VerificationResult> verify(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new BusinessException(404, "提交记录不存在");

        TrainingTask task = taskMapper.selectById(submission.getTaskId());
        if (task == null) throw new BusinessException(404, "关联任务不存在");

        // 当场提取文件内容
        String submissionContent = fileContentExtractor.extractContent(
                submission.getFilePaths(), submission.getTaskId(), submission.getId());

        if (submissionContent == null || submissionContent.isBlank()) {
            throw new BusinessException("提交内容为空，无法进行核查");
        }

        verificationResultMapper.deleteBySubmissionId(submissionId);

        try {
            String checkPoints = task.getExpectedOutput() != null ? task.getExpectedOutput() : "";
            Map<String, Object> aiResult = aiService.verifySubmission(submissionContent, checkPoints);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> checks = (List<Map<String, Object>>) aiResult.get("results");

            if (checks == null || checks.isEmpty()) {
                log.warn("AI核查返回空结果: submissionId={}", submissionId);
                return List.of();
            }

            for (Map<String, Object> check : checks) {
                VerificationResult result = new VerificationResult();
                result.setSubmissionId(submissionId);
                result.setTaskId(submission.getTaskId());
                result.setCheckItem(String.valueOf(check.getOrDefault("checkItem", "")));
                result.setCheckType(toInt(check.get("checkType"), 0));
                result.setExpectedContent(String.valueOf(check.getOrDefault("expectedContent", "")));
                result.setActualContent(String.valueOf(check.getOrDefault("actualContent", "")));
                result.setStatus(toInt(check.get("status"), 0));
                result.setDetail(String.valueOf(check.getOrDefault("detail", "")));
                verificationResultMapper.insert(result);
                log.info("核查项: {} status={}", result.getCheckItem(), result.getStatus());
            }
        } catch (Exception e) {
            log.error("AI核查失败: {}", e.getMessage(), e);
            throw new BusinessException("AI核查失败: " + e.getMessage());
        }

        return getResults(submissionId);
    }

    @Override
    public List<VerificationResult> getResults(Long submissionId) {
        return verificationResultMapper.selectBySubmissionId(submissionId);
    }

    private int toInt(Object obj, int defaultVal) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt((String) obj); }
            catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }
}
