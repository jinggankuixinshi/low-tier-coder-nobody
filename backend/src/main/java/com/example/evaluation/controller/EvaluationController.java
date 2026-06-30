package com.example.evaluation.controller;

import com.example.evaluation.common.BusinessException;
import com.example.evaluation.common.Result;
import com.example.evaluation.dto.ManualScoreRequest;
import com.example.evaluation.entity.EvaluationIndicator;
import com.example.evaluation.entity.EvaluationTemplate;
import com.example.evaluation.service.EvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    // ==================== 评价模板 ====================

    @GetMapping("/templates")
    public Result<?> getTemplates(@RequestParam(required = false) Long taskId) {
        return Result.success(evaluationService.getTemplates(taskId));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> createTemplate(@RequestBody EvaluationTemplate template) {
        validateDimWeights(template);
        return Result.success("创建成功", evaluationService.createTemplate(template));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> updateTemplate(@PathVariable Long id, @RequestBody EvaluationTemplate template) {
        template.setId(id);
        validateDimWeights(template);
        return Result.success("更新成功", evaluationService.updateTemplate(template));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> deleteTemplate(@PathVariable Long id) {
        evaluationService.deleteTemplate(id);
        return Result.success("删除成功");
    }

    // ==================== 评价指标 ====================

    @GetMapping("/templates/{templateId}/indicators")
    public Result<?> getIndicators(@PathVariable Long templateId) {
        return Result.success(evaluationService.getIndicators(templateId));
    }

    @PostMapping("/indicators")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> createIndicator(@RequestBody EvaluationIndicator indicator) {
        return Result.success("创建成功", evaluationService.createIndicator(indicator));
    }

    @PutMapping("/indicators/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> updateIndicator(@PathVariable Long id, @RequestBody EvaluationIndicator indicator) {
        indicator.setId(id);
        return Result.success("更新成功", evaluationService.updateIndicator(indicator));
    }

    @PutMapping("/templates/{templateId}/indicators/batch")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> batchSaveIndicators(@PathVariable Long templateId,
                                          @RequestBody List<EvaluationIndicator> indicators) {
        evaluationService.batchSaveIndicators(templateId, indicators);
        return Result.success("批量保存成功");
    }

    @DeleteMapping("/indicators/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> deleteIndicator(@PathVariable Long id) {
        evaluationService.deleteIndicator(id);
        return Result.success("删除成功");
    }

    // ==================== 评价流程 ====================

    /**
     * 初始化评价结果记录（不调AI，根据模板创建空记录）
     */
    @PostMapping("/init-results/{submissionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> initResults(@PathVariable Long submissionId,
                                  @RequestParam Long templateId) {
        log.info("初始化评价记录: submissionId={}, templateId={}", submissionId, templateId);
        return Result.success("初始化成功", evaluationService.initResults(submissionId, templateId));
    }

    /**
     * AI自动评分：根据模板和指标配置，AI对指定提交进行评分
     * eval_type=0(AI)或=2(混合)的指标由AI评分，=1(人工)的指标创建空记录
     */
    @PostMapping("/auto-score/{submissionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> autoScore(@PathVariable Long submissionId,
                                @RequestParam Long templateId) {
        log.info("AI自动评分: submissionId={}, templateId={}", submissionId, templateId);
        return Result.success("AI评分完成", evaluationService.autoScore(submissionId, templateId));
    }

    @PostMapping("/manual-score")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> manualScore(@RequestBody ManualScoreRequest request) {
        evaluationService.manualScore(request);
        return Result.success("教师评分已保存");
    }

    @PostMapping("/submit/{submissionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> submitResult(@PathVariable Long submissionId,
                                   @RequestBody(required = false) Map<String, Object> body) {
        String teacherComment = body != null ? (String) body.get("teacherComment") : null;
        String subjectiveReason = body != null ? (String) body.get("subjectiveReason") : null;
        BigDecimal subjectiveScore = null;
        if (body != null && body.get("subjectiveScore") != null) {
            Object ss = body.get("subjectiveScore");
            if (ss instanceof Number) {
                subjectiveScore = BigDecimal.valueOf(((Number) ss).doubleValue());
            } else if (ss instanceof String && !((String) ss).isBlank()) {
                subjectiveScore = new BigDecimal((String) ss);
            }
        }
        evaluationService.submitResult(submissionId, teacherComment, subjectiveScore, subjectiveReason);
        return Result.success("评价已提交");
    }

    @GetMapping("/history")
    public Result<?> getHistory() {
        return Result.success(evaluationService.getHistory());
    }

    @GetMapping("/result/{submissionId}")
    public Result<?> getEvaluationResult(@PathVariable Long submissionId,
                                          @RequestParam(required = false) Long templateId) {
        return Result.success(evaluationService.getEvaluationResult(submissionId, templateId));
    }

    @PostMapping("/replace-draft/{submissionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> replaceDraft(@PathVariable Long submissionId,
                                  @RequestParam Long oldTemplateId,
                                  @RequestParam Long newTemplateId) {
        evaluationService.replaceDraft(submissionId, oldTemplateId, newTemplateId);
        return Result.success("草稿模板已替换");
    }

    private void validateDimWeights(EvaluationTemplate template) {
        BigDecimal wComp = template.getWeightCompletion() != null ? template.getWeightCompletion() : BigDecimal.ZERO;
        BigDecimal wTech = template.getWeightTech() != null ? template.getWeightTech() : BigDecimal.ZERO;
        BigDecimal wInno = template.getWeightInnovation() != null ? template.getWeightInnovation() : BigDecimal.ZERO;
        BigDecimal wDoc  = template.getWeightDocument() != null ? template.getWeightDocument() : BigDecimal.ZERO;
        BigDecimal sum = wComp.add(wTech).add(wInno).add(wDoc).setScale(4, RoundingMode.HALF_UP);
        if (sum.compareTo(new BigDecimal("1.0000")) != 0) {
            throw new BusinessException("维度全局权重之和必须为100%（当前为 " 
                    + sum.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "%），请调整后再保存");
        }
    }

}
