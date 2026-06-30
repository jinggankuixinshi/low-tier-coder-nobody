package com.example.evaluation.service.impl;

import com.example.evaluation.common.BusinessException;
import com.example.evaluation.dto.AutoScoreResponse;
import com.example.evaluation.dto.EvaluationResultVO;
import com.example.evaluation.dto.ManualScoreRequest;
import com.example.evaluation.dto.SubmissionVO;
import com.example.evaluation.entity.*;
import com.example.evaluation.mapper.*;
import com.example.evaluation.security.SecurityUtil;
import com.example.evaluation.service.AIService;
import com.example.evaluation.service.EvaluationService;
import com.example.evaluation.utils.FileContentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final BigDecimal DEFAULT_AI_W = new BigDecimal("0.60");
    private static final BigDecimal DEFAULT_MAN_W = new BigDecimal("0.40");
    private static final BigDecimal MAX_INDICATOR_ADJUST = new BigDecimal("20");
    private static final BigDecimal MAX_SUBJECTIVE_ADJUST = new BigDecimal("20");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EvaluationTemplateMapper templateMapper;
    private final EvaluationIndicatorMapper indicatorMapper;
    private final EvaluationResultMapper evaluationResultMapper;
    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final AIService aiService;
    private final FileContentExtractor fileContentExtractor;

    public EvaluationServiceImpl(EvaluationTemplateMapper templateMapper,
                                 EvaluationIndicatorMapper indicatorMapper,
                                 EvaluationResultMapper evaluationResultMapper,
                                 SubmissionMapper submissionMapper,
                                 TaskMapper taskMapper,
                                 AIService aiService,
                                 FileContentExtractor fileContentExtractor) {
        this.templateMapper = templateMapper;
        this.indicatorMapper = indicatorMapper;
        this.evaluationResultMapper = evaluationResultMapper;
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.aiService = aiService;
        this.fileContentExtractor = fileContentExtractor;
    }

    // ==================== 统一权重解析 ====================

    private BigDecimal resolveAiWeight(EvaluationIndicator ind, EvaluationTemplate tpl) {
        if (ind != null && ind.getAiWeight() != null) return ind.getAiWeight();
        if (tpl != null && tpl.getAiWeight() != null) return tpl.getAiWeight();
        return DEFAULT_AI_W;
    }

    private BigDecimal resolveManualWeight(EvaluationIndicator ind, EvaluationTemplate tpl) {
        if (ind != null && ind.getManualWeight() != null) return ind.getManualWeight();
        if (tpl != null && tpl.getManualWeight() != null) return tpl.getManualWeight();
        return DEFAULT_MAN_W;
    }

    // ==================== 维度内指标权重自动均分 ====================

    /**
     * 按维度对指标权重进行均值分配：同一维度下指标均分 100%，
     * floor(100/n)，末位补齐余数。precheck 维度权重为 0。
     */
    private void redistributeIndicatorWeights(Long templateId) {
        List<EvaluationIndicator> all = indicatorMapper.selectByTemplateId(templateId);
        Map<String, List<EvaluationIndicator>> dimGroups = new LinkedHashMap<>();
        for (EvaluationIndicator ind : all) {
            String dim = ind.getDimension();
            if (dim == null || "precheck".equals(dim)) {
                ind.setWeight(BigDecimal.ZERO);
                indicatorMapper.updateById(ind);
                continue;
            }
            dimGroups.computeIfAbsent(dim, k -> new ArrayList<>()).add(ind);
        }
        for (Map.Entry<String, List<EvaluationIndicator>> entry : dimGroups.entrySet()) {
            List<EvaluationIndicator> group = entry.getValue();
            int n = group.size();
            if (n == 0) continue;
            int base = 100 / n;
            int remainder = 100 - base * n;
            for (int i = 0; i < n; i++) {
                int w = base;
                if (i == n - 1) w += remainder;
                group.get(i).setWeight(BigDecimal.valueOf(w));
                indicatorMapper.updateById(group.get(i));
            }
        }
    }

    private void recomputeFinalScore(EvaluationResult result,
                                      EvaluationIndicator indicator, EvaluationTemplate template) {
        int evalType = indicator != null ? indicator.getEvalType() : result.getEvalType();
        BigDecimal auto = result.getAutoScore() != null ? result.getAutoScore() : BigDecimal.ZERO;
        BigDecimal manual = result.getManualScore() != null ? result.getManualScore() : BigDecimal.ZERO;
        BigDecimal adjust = result.getAdjustScore() != null ? result.getAdjustScore() : BigDecimal.ZERO;
        boolean hasAuto = result.getAutoScore() != null && result.getAutoScore().compareTo(BigDecimal.ZERO) > 0;
        boolean hasManual = result.getManualScore() != null;

        if (evalType == 0) {
            result.setFinalScore(clampScore(auto.add(adjust)));
        } else if (evalType == 1) {
            result.setFinalScore(clampScore(manual.add(adjust)));
        } else {
            BigDecimal aiW = resolveAiWeight(indicator, template);
            BigDecimal manW = resolveManualWeight(indicator, template);
            if (hasAuto && hasManual) {
                result.setFinalScore(clampScore(
                        auto.multiply(aiW).add(manual.multiply(manW)).add(adjust)));
            } else if (hasAuto) {
                result.setFinalScore(clampScore(auto.multiply(aiW).add(adjust)));
            } else {
                result.setFinalScore(clampScore(manual.add(adjust)));
            }
        }
    }

    @Override
    public List<EvaluationTemplate> getTemplates(Long taskId) {
        return templateMapper.selectByTaskId(taskId);
    }

    @Override
    public EvaluationTemplate createTemplate(EvaluationTemplate template) {
        if (template.getEvalMethod() == null) template.setEvalMethod(2);
        if (template.getAiWeight() == null) template.setAiWeight(new BigDecimal("0.60"));
        if (template.getManualWeight() == null) template.setManualWeight(new BigDecimal("0.40"));
        templateMapper.insert(template);
        log.info("创建评价模板: {}", template.getName());
        return template;
    }

    @Override
    public EvaluationTemplate updateTemplate(EvaluationTemplate template) {
        if (templateMapper.selectById(template.getId()) == null) {
            throw new BusinessException(404, "模板不存在");
        }
        templateMapper.updateById(template);
        log.info("更新评价模板: {}", template.getName());
        return template;
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        EvaluationTemplate template = templateMapper.selectById(id);
        if (template == null) throw new BusinessException(404, "模板不存在");

        List<Long> indicatorIds = indicatorMapper.selectIdsByTemplateId(id);
        if (!indicatorIds.isEmpty()) {
            evaluationResultMapper.deleteByTemplateId(id);
        }

        indicatorMapper.deleteByTemplateId(id);
        templateMapper.deleteById(id);
        log.info("删除评价模板: {}", id);
    }

    @Override
    public List<EvaluationIndicator> getIndicators(Long templateId) {
        return indicatorMapper.selectByTemplateId(templateId);
    }

    @Override
    public EvaluationIndicator createIndicator(EvaluationIndicator indicator) {
        if (indicator.getEvalType() == null) indicator.setEvalType(2);
        if (indicator.getAiWeight() == null) indicator.setAiWeight(new BigDecimal("0.60"));
        if (indicator.getManualWeight() == null) indicator.setManualWeight(new BigDecimal("0.40"));
        indicatorMapper.insert(indicator);
        log.info("创建评价指标: {}", indicator.getName());
        return indicator;
    }

    @Override
    public EvaluationIndicator updateIndicator(EvaluationIndicator indicator) {
        indicatorMapper.updateById(indicator);
        log.info("更新评价指标: {}", indicator.getName());
        return indicator;
    }

    @Override
    public void deleteIndicator(Long id) {
        indicatorMapper.deleteById(id);
        log.info("删除评价指标: {}", id);
    }

    @Override
    @Transactional
    public void batchSaveIndicators(Long templateId, List<EvaluationIndicator> indicators) {
        List<EvaluationIndicator> existing = indicatorMapper.selectByTemplateId(templateId);
        java.util.Set<Long> keepIds = new java.util.HashSet<>();
        for (EvaluationIndicator ind : indicators) {
            if (ind.getId() != null) keepIds.add(ind.getId());
        }
        for (EvaluationIndicator old : existing) {
            if (!keepIds.contains(old.getId())) {
                indicatorMapper.deleteById(old.getId());
            }
        }
        for (int i = 0; i < indicators.size(); i++) {
            EvaluationIndicator ind = indicators.get(i);
            ind.setTemplateId(templateId);
            ind.setSortOrder(i + 1);
            if (ind.getId() != null) {
                indicatorMapper.updateById(ind);
            } else {
                indicatorMapper.insert(ind);
            }
        }
        log.info("批量保存评价指标: templateId={}, count={}", templateId, indicators.size());
        // 保存后自动按维度均分指标权重
        redistributeIndicatorWeights(templateId);
    }

    @Override
    @Transactional
    public List<EvaluationResultVO> initResults(Long submissionId, Long templateId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new BusinessException(404, "提交记录不存在");
        if (submission.getApprovalStatus() != null && submission.getApprovalStatus() == 2) {
            throw new BusinessException("评价已审批，不可重新初始化");
        }

        EvaluationTemplate template = templateMapper.selectById(templateId);
        if (template == null) throw new BusinessException(404, "评价模板不存在");

        List<EvaluationIndicator> indicators = getIndicators(templateId);
        if (indicators.isEmpty()) throw new BusinessException("模板下没有评价指标");

        List<EvaluationResult> results = new ArrayList<>();
        for (EvaluationIndicator indicator : indicators) {
            EvaluationResult existing = evaluationResultMapper.selectByThreeKeys(
                    submissionId, templateId, indicator.getId());
            if (existing != null) {
                results.add(existing);
                continue;
            }
            EvaluationResult result = new EvaluationResult();
            result.setSubmissionId(submissionId);
            result.setTemplateId(templateId);
            result.setIndicatorId(indicator.getId());
            result.setEvalType(indicator.getEvalType());
            result.setDimension(indicator.getDimension());
            evaluationResultMapper.insert(result);
            results.add(result);
        }

        log.info("评价记录初始化: submissionId={} templateId={} count={}", submissionId, templateId, results.size());
        return results.stream().map(r -> toVO(r, getIndicator(r.getIndicatorId()))).toList();
    }

    @Override
    @Transactional
    public AutoScoreResponse autoScore(Long submissionId, Long templateId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new BusinessException(404, "提交记录不存在");
        if (submission.getApprovalStatus() != null && submission.getApprovalStatus() == 2) {
            throw new BusinessException("评价已审批，不可重新评分");
        }

        // ===== AI防滥用状态机 =====
        Integer aiStatus = submission.getAiScoreStatus();
        if (aiStatus != null && aiStatus == 1) {
            long runningSeconds = 0;
            if (submission.getUpdateTime() != null) {
                runningSeconds = java.time.Duration.between(
                        submission.getUpdateTime(), java.time.LocalDateTime.now()).getSeconds();
            }
            if (runningSeconds > 300) {
                log.warn("AI评分状态异常（运行中超过5分钟），自动重置: submissionId={}", submissionId);
                submission.setAiScoreStatus(0);
                submissionMapper.updateById(submission);
            } else {
                throw new BusinessException("AI评分正在进行中，请稍后重试");
            }
        }
        if (aiStatus != null && aiStatus == 2) {
            log.info("AI评分已完成，本次为重试，清除之前的AI评分结果: submissionId={}", submissionId);
            List<EvaluationResult> resultsToClear = evaluationResultMapper.selectBySubmissionId(submissionId, templateId);
            for (EvaluationResult r : resultsToClear) {
                r.setAutoScore(null);
                r.setAutoComment(null);
                evaluationResultMapper.updateById(r);
            }
        }
        submission.setAiScoreStatus(1);
        submissionMapper.updateById(submission);

        // ===== 确保记录存在（向后兼容） =====
        List<EvaluationResult> existingRecords = evaluationResultMapper.selectBySubmissionId(submissionId, templateId);
        if (existingRecords.isEmpty()) {
            initResults(submissionId, templateId);
        }

        EvaluationTemplate template = templateMapper.selectById(templateId);
        if (template == null) throw new BusinessException(404, "评价模板不存在");
        List<EvaluationIndicator> indicators = getIndicators(templateId);
        if (indicators.isEmpty()) throw new BusinessException("模板下没有评价指标");

        // ===== 当场提取文件内容（教师点击AI评价时） =====
        String submissionContent;
        try {
            submissionContent = fileContentExtractor.extractContent(
                    submission.getFilePaths(), submission.getTaskId(), submission.getId());
        } catch (Exception e) {
            log.error("文件内容提取失败: submissionId={}", submissionId, e);
            submission.setAiScoreStatus(3);
            submissionMapper.updateById(submission);
            throw new BusinessException("文件内容提取失败: " + e.getMessage());
        }

        boolean contentEmpty = submissionContent == null || submissionContent.isBlank();

        // ===== 统计追踪 =====
        int totalAiIndicators = 0;
        int aiFailed = 0;
        int aiScoredPositive = 0;

        try {
            for (EvaluationIndicator indicator : indicators) {
                EvaluationResult result = evaluationResultMapper.selectByThreeKeys(
                        submissionId, templateId, indicator.getId());

                if (indicator.getEvalType() == 0 || indicator.getEvalType() == 2) {
                    totalAiIndicators++;
                    if (contentEmpty) {
                        result.setAutoScore(BigDecimal.ZERO);
                        result.setAutoComment("提交内容为空，无法评估");
                    } else {
                        try {
                            Map<String, Object> aiResult = aiService.evaluateSubmission(
                                    submissionContent,
                                    indicator.getName(),
                                    indicator.getDescription());
                            BigDecimal score = clampScore(parseScore(aiResult.get("score")));
                            result.setAutoScore(score);
                            result.setAutoComment((String) aiResult.getOrDefault("comment", ""));
                            if (score.compareTo(BigDecimal.ZERO) > 0) {
                                aiScoredPositive++;
                            }
                        } catch (Exception e) {
                            log.warn("AI评价指标[{}]失败: {}", indicator.getName(), e.getMessage());
                            aiFailed++;
                            result.setAutoScore(BigDecimal.ZERO);
                            result.setAutoComment("AI评价失败: " + e.getMessage());
                        }
                    }
                }

                recomputeFinalScore(result, indicator, template);
                evaluationResultMapper.updateById(result);
                log.info("指标[{}] evalType={} autoScore={}", indicator.getName(), indicator.getEvalType(), result.getAutoScore());
            }

            submission.setAiScoreStatus(aiFailed > 0 ? 3 : 2);
            submissionMapper.updateById(submission);

        } catch (Exception e) {
            submission.setAiScoreStatus(3);
            submissionMapper.updateById(submission);
            throw e;
        }

        List<EvaluationResultVO> voList = evaluationResultMapper.selectBySubmissionId(submissionId, templateId)
                .stream().map(r -> toVO(r, getIndicator(r.getIndicatorId()))).toList();

        Map<String, Object> statusSummary = new HashMap<>();
        statusSummary.put("contentEmpty", contentEmpty);
        statusSummary.put("totalIndicators", indicators.size());
        statusSummary.put("totalAiIndicators", totalAiIndicators);
        statusSummary.put("aiFailed", aiFailed);
        statusSummary.put("aiScoredZero", totalAiIndicators - aiFailed - aiScoredPositive);
        statusSummary.put("aiScoredPositive", aiScoredPositive);

        AutoScoreResponse response = new AutoScoreResponse();
        response.setResults(voList);
        response.setStatusSummary(statusSummary);
        return response;
    }

    @Override
    @Transactional
    public void manualScore(ManualScoreRequest request) {
        EvaluationResult result = evaluationResultMapper.selectById(request.getResultId());
        if (result == null) throw new BusinessException(404, "评价结果不存在");

        if (request.getManualScore() != null) result.setManualScore(request.getManualScore());
        if (request.getManualComment() != null) result.setManualComment(request.getManualComment());
        if (request.getAdjustScore() != null) {
            if (request.getAdjustScore().abs().compareTo(MAX_INDICATOR_ADJUST) > 0) {
                throw new BusinessException("单项加减分超出范围（±20）");
            }
            result.setAdjustScore(request.getAdjustScore());
        }
        if (request.getAdjustReason() != null) result.setAdjustReason(request.getAdjustReason());

        EvaluationIndicator indicator = indicatorMapper.selectById(result.getIndicatorId());
        EvaluationTemplate template = templateMapper.selectById(result.getTemplateId());

        recomputeFinalScore(result, indicator, template);
        evaluationResultMapper.updateById(result);

        Submission submission = submissionMapper.selectById(result.getSubmissionId());
        if (submission != null && (submission.getApprovalStatus() == null || submission.getApprovalStatus() == 0)) {
            submission.setApprovalStatus(1);
            submission.setDraftTemplateId(result.getTemplateId());
            submission.setEvaluationStatus(1);
            submissionMapper.updateById(submission);
        }
        log.info("手动评分: resultId={} final={}", request.getResultId(), result.getFinalScore());
    }

    @Override
    public void submitResult(Long submissionId) {
        submitResult(submissionId, null, null, null);
    }

    @Override
    public void submitResult(Long submissionId, String teacherComment) {
        submitResult(submissionId, teacherComment, null, null);
    }

    @Override
    public void submitResult(Long submissionId, String teacherComment, BigDecimal subjectiveScore) {
        submitResult(submissionId, teacherComment, subjectiveScore, null);
    }

    @Override
    @Transactional
    public void submitResult(Long submissionId, String teacherComment,
                             BigDecimal subjectiveScore, String subjectiveReason) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new BusinessException(404, "提交记录不存在");

        List<EvaluationResult> results = evaluationResultMapper.selectBySubmissionId(submissionId, null);
        if (results.isEmpty()) throw new BusinessException("尚未进行评价，无法提交审批");

        Long templateId = results.get(0).getTemplateId();
        EvaluationTemplate template = templateMapper.selectById(templateId);

        // ===== 层级1：计算各维度综合得分 =====
        Map<String, BigDecimal> dimWeightedScore = new LinkedHashMap<>();
        Map<String, BigDecimal> dimTotalWeight = new LinkedHashMap<>();

        for (EvaluationResult r : results) {
            EvaluationIndicator ind = getIndicator(r.getIndicatorId());
            if (ind == null) continue;
            String dim = ind.getDimension();
            if (dim == null || "precheck".equals(dim)) continue;

            BigDecimal fs = r.getFinalScore() != null ? r.getFinalScore() : BigDecimal.ZERO;
            BigDecimal w = ind.getWeight() != null ? ind.getWeight() : BigDecimal.ZERO;
            dimWeightedScore.merge(dim, fs.multiply(w), BigDecimal::add);
            dimTotalWeight.merge(dim, w, BigDecimal::add);
        }

        BigDecimal compScore = safeDivide(dimWeightedScore.get("completion"), dimTotalWeight.get("completion"));
        BigDecimal techScore = safeDivide(dimWeightedScore.get("tech"), dimTotalWeight.get("tech"));
        BigDecimal innoScore = safeDivide(dimWeightedScore.get("innovation"), dimTotalWeight.get("innovation"));
        BigDecimal docScore  = safeDivide(dimWeightedScore.get("document"), dimTotalWeight.get("document"));

        // ===== 层级2：全局维度加权总分 =====
        BigDecimal wComp = template != null && template.getWeightCompletion() != null
                ? template.getWeightCompletion() : new BigDecimal("0.25");
        BigDecimal wTech = template != null && template.getWeightTech() != null
                ? template.getWeightTech() : new BigDecimal("0.25");
        BigDecimal wInno = template != null && template.getWeightInnovation() != null
                ? template.getWeightInnovation() : new BigDecimal("0.25");
        BigDecimal wDoc  = template != null && template.getWeightDocument() != null
                ? template.getWeightDocument() : new BigDecimal("0.25");

        BigDecimal total = compScore.multiply(wComp)
                .add(techScore.multiply(wTech))
                .add(innoScore.multiply(wInno))
                .add(docScore.multiply(wDoc));

        // 教师主观评分（在加权分基础上增减，范围 ±20）
        BigDecimal subjective = subjectiveScore != null ? subjectiveScore : BigDecimal.ZERO;
        if (subjective.abs().compareTo(MAX_SUBJECTIVE_ADJUST) > 0) {
            throw new BusinessException("主观评分超出范围（±20），请修改后再提交");
        }

        total = clampScore(total.add(subjective));

        if (teacherComment != null && !teacherComment.isBlank()) {
            submission.setTeacherComment(teacherComment);
        }
        submission.setSubmitted(1);
        submission.setApprovalStatus(2);
        submission.setEvaluationStatus(1);
        submission.setTotalScore(total);
        submission.setSubjectiveScore(subjective);
        submission.setSubjectiveReason(subjectiveReason);
        submissionMapper.updateById(submission);
        log.info("评价已提交: submissionId={} comp={} tech={} inno={} doc={} subjective={} total={}",
                submissionId, compScore, techScore, innoScore, docScore, subjective, total);
    }

    @Override
    public List<SubmissionVO> getHistory() {
        Long userId = SecurityUtil.getCurrentUserId();
        Integer role = SecurityUtil.getCurrentRole();

        if (role == 1) {
            List<TrainingTask> tasks = taskMapper.selectByTeacherId(userId);
            List<Long> taskIds = tasks.stream().map(TrainingTask::getId).toList();
            if (taskIds.isEmpty()) return List.of();

            List<Submission> submissions = submissionMapper.selectByTaskIdsApproved(taskIds);
            return submissions.stream().map(this::toHistoryVO).toList();
        } else {
            List<Submission> submissions = submissionMapper.selectByStudentIdApproved(userId);
            return submissions.stream().map(this::toHistoryVO).toList();
        }
    }

    private SubmissionVO toHistoryVO(Submission s) {
        SubmissionVO vo = new SubmissionVO();
        BeanUtils.copyProperties(s, vo);
        TrainingTask task = taskMapper.selectById(s.getTaskId());
        if (task != null) vo.setTaskName(task.getTitle());
        if (s.getApprovalStatus() != null && s.getApprovalStatus() == 1) {
            vo.setTemplateId(s.getDraftTemplateId());
        } else if (s.getApprovalStatus() != null && s.getApprovalStatus() == 2) {
            Long templateId = evaluationResultMapper.selectTemplateIdBySubmissionId(s.getId());
            vo.setTemplateId(templateId);
        }
        return vo;
    }

    @Override
    public List<EvaluationResultVO> getEvaluationResult(Long submissionId, Long templateId) {
        List<EvaluationResult> results = evaluationResultMapper.selectBySubmissionId(submissionId, templateId);
        return results.stream().map(r -> toVO(r, getIndicator(r.getIndicatorId()))).toList();
    }

    @Override
    @Transactional
    public void replaceDraft(Long submissionId, Long oldTemplateId, Long newTemplateId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new BusinessException(404, "提交记录不存在");
        if (submission.getApprovalStatus() != null && submission.getApprovalStatus() == 2) {
            throw new BusinessException("评价已审批，不可修改");
        }
        evaluationResultMapper.deleteBySubmissionIdAndTemplateId(submissionId, oldTemplateId);
        submission.setDraftTemplateId(newTemplateId);
        submission.setApprovalStatus(1);
        submission.setAiScoreStatus(0);  // 换模板重置AI状态
        if (submission.getEvaluationStatus() == null || submission.getEvaluationStatus() == 0) {
            submission.setEvaluationStatus(1);
        }
        submissionMapper.updateById(submission);
        log.info("草稿模板替换: submissionId={} old={} new={}", submissionId, oldTemplateId, newTemplateId);
    }

    private BigDecimal parseScore(Object scoreObj) {
        if (scoreObj instanceof Number) return BigDecimal.valueOf(((Number) scoreObj).doubleValue()).setScale(2, RoundingMode.HALF_UP);
        if (scoreObj instanceof String) {
            try { return new BigDecimal((String) scoreObj).setScale(2, RoundingMode.HALF_UP); }
            catch (NumberFormatException e) { return BigDecimal.ZERO; }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal safeDivide(BigDecimal scoreSum, BigDecimal weightSum) {
        if (scoreSum == null || weightSum == null || weightSum.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return scoreSum.divide(weightSum, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal clampScore(BigDecimal score) {
        if (score.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (score.compareTo(new BigDecimal("100")) > 0) return new BigDecimal("100");
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private EvaluationIndicator getIndicator(Long indicatorId) {
        return indicatorMapper.selectById(indicatorId);
    }

    private EvaluationResultVO toVO(EvaluationResult result, EvaluationIndicator indicator) {
        EvaluationResultVO vo = new EvaluationResultVO();
        BeanUtils.copyProperties(result, vo);
        if (indicator != null) {
            vo.setIndicatorName(indicator.getName());
            vo.setWeight(indicator.getWeight() != null ? indicator.getWeight() : BigDecimal.ZERO);
            vo.setMaxScore(indicator.getMaxScore() != null ? indicator.getMaxScore() : new BigDecimal("100"));
            vo.setEvalType(indicator.getEvalType() != null ? indicator.getEvalType() : result.getEvalType());
            vo.setAiWeight(indicator.getAiWeight());
            vo.setManualWeight(indicator.getManualWeight());
        } else {
            vo.setIndicatorName("(指标已删除)");
            vo.setWeight(BigDecimal.ZERO);
            vo.setMaxScore(new BigDecimal("100"));
            vo.setEvalType(result.getEvalType());
        }
        if (result.getCreateTime() != null) vo.setCreateTime(result.getCreateTime().format(FMT_DATETIME));
        if (result.getUpdateTime() != null) vo.setUpdateTime(result.getUpdateTime().format(FMT_DATETIME));
        return vo;
    }
}
