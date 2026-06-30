package com.example.evaluation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evaluation.dto.AutoScoreResponse;
import com.example.evaluation.dto.EvaluationResultVO;
import com.example.evaluation.dto.ManualScoreRequest;
import com.example.evaluation.entity.EvaluationIndicator;
import com.example.evaluation.entity.EvaluationTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * 评价管理服务接口
 */
public interface EvaluationService {

    // ========== 评价模板 CRUD ==========

    List<EvaluationTemplate> getTemplates(Long taskId);

    EvaluationTemplate createTemplate(EvaluationTemplate template);

    EvaluationTemplate updateTemplate(EvaluationTemplate template);

    void deleteTemplate(Long id);

    // ========== 评价指标 CRUD ==========

    List<EvaluationIndicator> getIndicators(Long templateId);

    EvaluationIndicator createIndicator(EvaluationIndicator indicator);

    EvaluationIndicator updateIndicator(EvaluationIndicator indicator);

    void batchSaveIndicators(Long templateId, List<EvaluationIndicator> indicators);

    void deleteIndicator(Long id);

    // ========== 评价记录初始化 ==========

    /**
     * 根据模板初始化评价结果记录（不调AI，仅创建空记录，次幂等）
     */
    List<EvaluationResultVO> initResults(Long submissionId, Long templateId);

    // ========== 自动评分 ==========

    AutoScoreResponse autoScore(Long submissionId, Long templateId);

    // ========== 教师手动评分 ==========

    void manualScore(ManualScoreRequest request);

    void submitResult(Long submissionId);

    void submitResult(Long submissionId, String teacherComment);

    void submitResult(Long submissionId, String teacherComment, BigDecimal subjectiveScore);

    void submitResult(Long submissionId, String teacherComment, BigDecimal subjectiveScore, String subjectiveReason);

    List<com.example.evaluation.dto.SubmissionVO> getHistory();

    List<EvaluationResultVO> getEvaluationResult(Long submissionId, Long templateId);

    void replaceDraft(Long submissionId, Long oldTemplateId, Long newTemplateId);

}
