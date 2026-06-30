package com.example.evaluation.service.impl;

import com.example.evaluation.common.BusinessException;
import com.example.evaluation.config.FileUploadConfig;
import com.example.evaluation.dto.EvaluationResultVO;
import com.example.evaluation.dto.ReportDataVO;
import com.example.evaluation.dto.ReportOverviewVO;
import com.example.evaluation.entity.*;
import com.example.evaluation.mapper.*;
import com.example.evaluation.security.SecurityUtil;
import com.example.evaluation.service.EvaluationService;
import com.example.evaluation.service.ReportService;
import com.example.evaluation.utils.ReportHtmlGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextFontResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final ReportRecordMapper reportRecordMapper;
    private final EvaluationResultMapper evaluationResultMapper;
    private final EvaluationIndicatorMapper indicatorMapper;
    private final VerificationResultMapper verificationResultMapper;
    private final EvaluationTemplateMapper templateMapper;
    private final EvaluationService evaluationService;
    private final FileUploadConfig fileUploadConfig;
    private final ReportHtmlGenerator reportHtmlGenerator;
    private final UserMapper userMapper;
    private final com.example.evaluation.config.FontConfig fontConfig;

    public ReportServiceImpl(SubmissionMapper submissionMapper,
                             TaskMapper taskMapper,
                             ReportRecordMapper reportRecordMapper,
                             EvaluationResultMapper evaluationResultMapper,
                             EvaluationIndicatorMapper indicatorMapper,
                             VerificationResultMapper verificationResultMapper,
                             EvaluationTemplateMapper templateMapper,
                             EvaluationService evaluationService,
                             FileUploadConfig fileUploadConfig,
                             ReportHtmlGenerator reportHtmlGenerator,
                             UserMapper userMapper,
                             com.example.evaluation.config.FontConfig fontConfig) {
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.reportRecordMapper = reportRecordMapper;
        this.evaluationResultMapper = evaluationResultMapper;
        this.indicatorMapper = indicatorMapper;
        this.verificationResultMapper = verificationResultMapper;
        this.templateMapper = templateMapper;
        this.evaluationService = evaluationService;
        this.fileUploadConfig = fileUploadConfig;
        this.reportHtmlGenerator = reportHtmlGenerator;
        this.userMapper = userMapper;
        this.fontConfig = fontConfig;
    }

    @Override
    public ReportRecord generateSingleReport(Long submissionId) {
        ReportRecord existing = reportRecordMapper.selectBySubmissionId(submissionId);
        if (existing != null) {
            return existing;
        }

        ReportContext ctx = buildReportContext(submissionId);

        String username = ctx.student != null ? ctx.student.getUsername() : null;
        String html = reportHtmlGenerator.buildSingleReportHtml(ctx.task, ctx.studentName, username, ctx.submission,
                ctx.evalResults, ctx.teacherComment, toHtmlSummary(ctx.summary), ctx.template);
        byte[] pdfData = renderPdf(html);

        String studentNo = ctx.student != null && ctx.student.getStudentNo() != null ? ctx.student.getStudentNo() : "unknown";
        String reportName = sanitizeName(ctx.studentName + "_" + studentNo + "_" + ctx.task.getTitle() + "_评价报告");
        String reportDir = fileUploadConfig.getRootPath() + "/reports/";
        try {
            Path dir = Paths.get(reportDir);
            Files.createDirectories(dir);

            String pdfPath = reportDir + reportName + ".pdf";
            Files.write(Paths.get(pdfPath), pdfData);
            ReportRecord record = new ReportRecord();
            record.setSubmissionId(submissionId);
            record.setTaskId(ctx.submission.getTaskId());
            record.setReportType(0);
            record.setTitle(ctx.studentName + " - " + ctx.task.getTitle() + " 评价报告");
            record.setPdfPath(pdfPath);
            record.setGenerationStatus(2);
            record.setPrecheckPassCount(ctx.precheckPassCount);
            record.setPrecheckPassed(ctx.allPassed ? 1 : 0);
            record.setCompletionScore(ctx.summary.completionScore);
            record.setTechQualityScore(ctx.summary.techScore);
            record.setInnovationScore(ctx.summary.innovationScore);
            record.setDocumentScore(ctx.summary.documentScore);
            record.setWeightedTotal(ctx.summary.totalScore);
            record.setGrade(ctx.summary.grade);
            record.setReviewerId(SecurityUtil.getCurrentUserId());
            record.setReviewDate(LocalDate.now());
            reportRecordMapper.insert(record);

            log.info("生成单次评价报告: {}", record.getTitle());
            return record;
        } catch (IOException e) {
            log.error("生成报表失败", e);
            throw new BusinessException("报表生成失败: " + e.getMessage());
        }
    }

    @Override
    public ReportDataVO getOrGenerateReportData(Long submissionId) {
        Submission sub = submissionMapper.selectById(submissionId);
        if (sub == null) {
            throw new BusinessException(404, "提交不存在");
        }
        if (SecurityUtil.isStudent() && !sub.getStudentId().equals(SecurityUtil.getCurrentUserId())) {
            throw new BusinessException(403, "无权查看");
        }
        ReportRecord existing = reportRecordMapper.selectBySubmissionId(submissionId);
        if (existing == null) {
            generateSingleReport(submissionId);
        }
        return buildReportDataVO(submissionId);
    }

    @Override
    public byte[] exportPdf(Long submissionId) {
        ReportRecord existing = reportRecordMapper.selectBySubmissionId(submissionId);
        if (existing == null) {
            existing = generateSingleReport(submissionId);
        }
        if (existing.getPdfPath() == null) {
            throw new BusinessException("报表PDF文件不存在");
        }
        try {
            return Files.readAllBytes(Paths.get(existing.getPdfPath()));
        } catch (IOException e) {
            throw new BusinessException("读取报表文件失败");
        }
    }

    @Override
    public byte[] batchExportPdfZip(List<Long> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            throw new BusinessException("请选择至少一个提交");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            int index = 0;
            for (Long subId : submissionIds) {
                index++;
                ReportRecord record = reportRecordMapper.selectBySubmissionId(subId);
                if (record == null) {
                    record = generateSingleReport(subId);
                }
                if (record.getPdfPath() != null) {
                    Path pdfPath = Paths.get(record.getPdfPath());
                    if (Files.exists(pdfPath)) {
                        String entryName = String.format("%02d_%s", index,
                                new java.io.File(record.getPdfPath()).getName());
                        ZipEntry entry = new ZipEntry(entryName);
                        zos.putNextEntry(entry);
                        Files.copy(pdfPath, zos);
                        zos.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            throw new BusinessException("ZIP打包失败: " + e.getMessage());
        }
        return baos.toByteArray();
    }

    @Override
    public ReportRecord generateTaskOverview(Long taskId) {
        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        List<Submission> submissions = submissionMapper.selectByTaskId(taskId);
        long totalApproved = submissions.stream()
                .filter(s -> s.getApprovalStatus() != null && s.getApprovalStatus() == 2).count();
        if (totalApproved < submissions.size() || submissions.isEmpty()) {
            throw new BusinessException("该任务尚有未完成审批的提交，无法生成总览报表（已审批" + totalApproved + "/" + submissions.size() + "）");
        }

        ReportRecord existing = reportRecordMapper.selectByTaskIdAndType(taskId, 1);
        if (existing != null && existing.getSubmissionCount() != null
                && existing.getSubmissionCount() == (int) totalApproved) {
            return existing;
        }
        if (existing != null) {
            deleteReport(existing.getId());
        }

        List<Map<String, Object>> studentStats = new ArrayList<>();
        List<Double> allScores = new ArrayList<>();
        Map<String, List<Double>> dimScores = new LinkedHashMap<>();
        dimScores.put("completion", new ArrayList<>());
        dimScores.put("tech", new ArrayList<>());
        dimScores.put("innovation", new ArrayList<>());
        dimScores.put("document", new ArrayList<>());

        for (Submission sub : submissions) {
            User student = userMapper.selectById(sub.getStudentId());
            String studentName = student != null ? student.getRealName() : "未知学生";

            List<EvaluationResult> evalResults = evaluationResultMapper.selectBySubmissionId(sub.getId(), null);
            Long subTemplateId = evaluationResultMapper.selectTemplateIdBySubmissionId(sub.getId());
            EvaluationTemplate subTemplate = subTemplateId != null ? templateMapper.selectById(subTemplateId) : null;
            ScoreSummary summary = calculateScoreSummary(evalResults.stream().map(r -> toVO(r, getIndicatorName(r.getIndicatorId()))).toList(), task, sub.getTotalScore(), subTemplate);

            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("studentName", studentName);
            stat.put("totalScore", summary.totalScore);
            stat.put("grade", summary.grade);
            stat.put("submitTime", sub.getSubmitTime() != null ? sub.getSubmitTime().format(DT_FMT) : "-");
            studentStats.add(stat);
            allScores.add(summary.totalScore != null ? summary.totalScore.doubleValue() : 0);

            if (summary.completionScore != null) dimScores.get("completion").add(summary.completionScore.doubleValue());
            if (summary.techScore != null) dimScores.get("tech").add(summary.techScore.doubleValue());
            if (summary.innovationScore != null) dimScores.get("innovation").add(summary.innovationScore.doubleValue());
            if (summary.documentScore != null) dimScores.get("document").add(summary.documentScore.doubleValue());
        }

        String html = reportHtmlGenerator.buildOverviewHtml(task, studentStats, allScores, dimScores);
        byte[] pdfData = renderPdf(html);

        String reportName = sanitizeName(task.getTitle() + "_任务总览报表");
        String reportDir = fileUploadConfig.getRootPath() + "/reports/";
        try {
            Path dir = Paths.get(reportDir);
            Files.createDirectories(dir);

            String pdfPath = reportDir + reportName + ".pdf";
            Files.write(Paths.get(pdfPath), pdfData);

            ReportRecord record = new ReportRecord();
            record.setTaskId(taskId);
            record.setReportType(1);
            record.setTitle(task.getTitle() + " - 任务总览报表");
            record.setPdfPath(pdfPath);
            record.setGenerationStatus(2);
            record.setSubmissionCount((int) totalApproved);
            record.setReviewDate(LocalDate.now());
            reportRecordMapper.insert(record);

            log.info("生成任务总览报表: {}", record.getTitle());
            return record;
        } catch (IOException e) {
            log.error("生成任务总览报表失败", e);
            throw new BusinessException("报表生成失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getOverviewStatus(Long taskId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Submission> allSubs = submissionMapper.selectByTaskId(taskId);
        int totalSubs = allSubs.size();
        int approvedCount = (int) allSubs.stream()
                .filter(s -> s.getApprovalStatus() != null && s.getApprovalStatus() == 2).count();

        result.put("totalSubmissions", totalSubs);
        result.put("approvedCount", approvedCount);

        if (totalSubs == 0) {
            result.put("status", "blocked");
            result.put("message", "该任务暂无提交记录");
            result.put("overviewExists", false);
            return result;
        }

        if (approvedCount < totalSubs) {
            result.put("status", "blocked");
            result.put("message", "还有 " + (totalSubs - approvedCount) + " 个提交未审批，无法查看总览报表");
            result.put("overviewExists", false);
            return result;
        }

        ReportRecord overview = reportRecordMapper.selectByTaskIdAndType(taskId, 1);
        result.put("overviewExists", overview != null);

        if (overview == null) {
            result.put("status", "ready");
            result.put("message", null);
            return result;
        }

        result.put("reportId", overview.getId());
        int storedCount = overview.getSubmissionCount() != null ? overview.getSubmissionCount() : 0;
        if (storedCount == approvedCount) {
            result.put("status", "unchanged");
            result.put("message", null);
        } else {
            result.put("status", "changed");
            result.put("message", "数据已更新（新增 " + (approvedCount - storedCount) + " 个已审批提交），点击查看将重新生成");
        }
        return result;
    }

    @Override
    public ReportOverviewVO getOrGenerateOverviewData(Long taskId) {
        Map<String, Object> status = getOverviewStatus(taskId);
        String st = (String) status.get("status");
        if ("blocked".equals(st)) {
            throw new BusinessException((String) status.get("message"));
        }
        if ("changed".equals(st) || "ready".equals(st)) {
            generateTaskOverview(taskId);
        }
        return buildOverviewVO(taskId);
    }

    @Override
    public byte[] exportOverviewPdf(Long taskId) {
        // 始终基于当前数据重新生成总览PDF，与网页预览(每次实时重算)保持完全一致，
        // 避免提交数不变时复用旧缓存PDF导致等级等数据与网页不符。
        Map<String, Object> status = getOverviewStatus(taskId);
        if ("blocked".equals(status.get("status"))) {
            throw new BusinessException((String) status.get("message"));
        }
        ReportRecord existing = reportRecordMapper.selectByTaskIdAndType(taskId, 1);
        if (existing != null) {
            deleteReport(existing.getId());
        }
        ReportRecord overview = generateTaskOverview(taskId);
        if (overview == null || overview.getPdfPath() == null) {
            throw new BusinessException("总览报表不存在");
        }
        try {
            return Files.readAllBytes(Paths.get(overview.getPdfPath()));
        } catch (IOException e) {
            throw new BusinessException("读取总览报表文件失败");
        }
    }

    @Override
    public List<ReportRecord> getReportList(Long taskId) {
        return reportRecordMapper.selectByTaskId(taskId);
    }

    @Override
    public byte[] downloadPdf(Long reportId) {
        ReportRecord record = reportRecordMapper.selectById(reportId);
        if (record == null) throw new BusinessException(404, "报表记录不存在");
        if (record.getPdfPath() == null) throw new BusinessException("报表PDF文件不存在");
        try {
            return Files.readAllBytes(Paths.get(record.getPdfPath()));
        } catch (IOException e) {
            throw new BusinessException("读取报表文件失败");
        }
    }

    @Override
    public List<Submission> getApprovedSubmissions(Long taskId) {
        return submissionMapper.selectByTaskIdApproved(taskId);
    }

    @Override
    public List<com.example.evaluation.dto.SubmissionVO> getApprovedSubmissionsWithNames(Long taskId) {
        List<Submission> subs = submissionMapper.selectByTaskIdApproved(taskId);
        List<com.example.evaluation.dto.SubmissionVO> vos = new ArrayList<>();
        for (Submission s : subs) {
            com.example.evaluation.dto.SubmissionVO vo = new com.example.evaluation.dto.SubmissionVO();
            org.springframework.beans.BeanUtils.copyProperties(s, vo);
            TrainingTask t = taskMapper.selectById(s.getTaskId());
            if (t != null) vo.setTaskName(t.getTitle());
            if (s.getStudentId() != null) {
                User u = userMapper.selectById(s.getStudentId());
                if (u != null) {
                    vo.setStudentName(u.getRealName());
                    vo.setUsername(u.getUsername());
                }
            }
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public boolean isAllApproved(Long taskId) {
        List<Submission> all = submissionMapper.selectByTaskId(taskId);
        if (all.isEmpty()) return false;
        return all.stream().allMatch(s -> s.getApprovalStatus() != null && s.getApprovalStatus() == 2);
    }

    // ==================== private helpers ====================

    private ReportContext buildReportContext(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new BusinessException(404, "提交记录不存在");
        if (submission.getApprovalStatus() == null || submission.getApprovalStatus() != 2) {
            throw new BusinessException("该提交尚未完成审批，无法生成报表");
        }

        TrainingTask task = taskMapper.selectById(submission.getTaskId());
        if (task == null) throw new BusinessException(404, "实训任务不存在");

        User student = userMapper.selectById(submission.getStudentId());
        String studentName = student != null ? student.getRealName() : "未知学生";

        Long actualTemplateId = evaluationResultMapper.selectTemplateIdBySubmissionId(submissionId);
        EvaluationTemplate template = templateMapper.selectById(actualTemplateId);
        List<EvaluationResultVO> evalResults = evaluationService.getEvaluationResult(submissionId, actualTemplateId);
        List<VerificationResult> verifications = verificationResultMapper.selectBySubmissionId(submissionId);

        ScoreSummary summary = calculateScoreSummary(evalResults, task, submission.getTotalScore(), template);
        int precheckPassCount = countPassedVerifications(verifications);
        boolean allPassed = allVerificationsPassed(verifications);

        String teacherComment = buildTeacherComment(evalResults);
        if (submission.getTeacherComment() != null && !submission.getTeacherComment().isBlank()) {
            teacherComment = teacherComment + "\n综合评价：" + submission.getTeacherComment();
        }

        return new ReportContext(submission, task, student, studentName, template, evalResults,
                verifications, summary, teacherComment, precheckPassCount, allPassed);
    }

    private ReportDataVO buildReportDataVO(Long submissionId) {
        ReportContext ctx = buildReportContext(submissionId);

        ReportDataVO vo = new ReportDataVO();
        vo.setSubmissionId(submissionId);
        vo.setDate(LocalDate.now().format(DATE_FMT));
        vo.setPrecheckPassCount(ctx.precheckPassCount);
        vo.setAllPassed(ctx.allPassed);
        vo.setTeacherComment(ctx.teacherComment);
        vo.setSubjectiveScore(ctx.submission.getSubjectiveScore() != null
                ? ctx.submission.getSubjectiveScore().doubleValue() : null);
        vo.setSubjectiveReason(ctx.submission.getSubjectiveReason());

        ReportDataVO.TaskInfo taskInfo = new ReportDataVO.TaskInfo();
        taskInfo.setId(ctx.task.getId());
        taskInfo.setTitle(ctx.task.getTitle());
        taskInfo.setCourseName(ctx.task.getCourseName());
        taskInfo.setModuleName(ctx.task.getModuleName());
        taskInfo.setSubject(ctx.task.getSubject());
        taskInfo.setWeightCompletion(ctx.task.getWeightCompletion());
        taskInfo.setWeightTech(ctx.task.getWeightTech());
        taskInfo.setWeightInnovation(ctx.task.getWeightInnovation());
        taskInfo.setWeightDocument(ctx.task.getWeightDocument());
        vo.setTask(taskInfo);

        ReportDataVO.StudentInfo studentInfo = new ReportDataVO.StudentInfo();
        studentInfo.setName(ctx.studentName);
        studentInfo.setStudentNo(ctx.student != null ? ctx.student.getStudentNo() : null);
        studentInfo.setUsername(ctx.student != null ? ctx.student.getUsername() : null);
        vo.setStudent(studentInfo);

        ReportDataVO.TemplateInfo templateInfo = new ReportDataVO.TemplateInfo();
        if (ctx.template != null) {
            templateInfo.setId(ctx.template.getId());
            templateInfo.setName(ctx.template.getName());
            templateInfo.setEvalMethod(ctx.template.getEvalMethod());
            templateInfo.setWeightCompletion(ctx.template.getWeightCompletion());
            templateInfo.setWeightTech(ctx.template.getWeightTech());
            templateInfo.setWeightInnovation(ctx.template.getWeightInnovation());
            templateInfo.setWeightDocument(ctx.template.getWeightDocument());
        }
        vo.setTemplate(templateInfo);

        ReportDataVO.ScoreSummary scoreSummary = new ReportDataVO.ScoreSummary();
        scoreSummary.setCompletionScore(ctx.summary.completionScore != null ? ctx.summary.completionScore.doubleValue() : null);
        scoreSummary.setTechScore(ctx.summary.techScore != null ? ctx.summary.techScore.doubleValue() : null);
        scoreSummary.setInnovationScore(ctx.summary.innovationScore != null ? ctx.summary.innovationScore.doubleValue() : null);
        scoreSummary.setDocumentScore(ctx.summary.documentScore != null ? ctx.summary.documentScore.doubleValue() : null);
        scoreSummary.setTotalScore(ctx.summary.totalScore != null ? ctx.summary.totalScore.doubleValue() : null);
        scoreSummary.setGrade(ctx.summary.grade);
        vo.setSummary(scoreSummary);

        List<ReportDataVO.ResultItem> resultItems = new ArrayList<>();
        for (EvaluationResultVO r : ctx.evalResults) {
            ReportDataVO.ResultItem item = new ReportDataVO.ResultItem();
            item.setIndicatorName(r.getIndicatorName());
            item.setDimension(r.getDimension());
            item.setWeight(r.getWeight() != null ? r.getWeight().doubleValue() : null);
            item.setEvalType(r.getEvalType());
            item.setAutoScore(r.getAutoScore() != null ? r.getAutoScore().doubleValue() : null);
            item.setAutoComment(r.getAutoComment());
            item.setManualScore(r.getManualScore() != null ? r.getManualScore().doubleValue() : null);
            item.setManualComment(r.getManualComment());
            item.setAdjustScore(r.getAdjustScore() != null ? r.getAdjustScore().doubleValue() : null);
            item.setAdjustReason(r.getAdjustReason());
            item.setFinalScore(r.getFinalScore() != null ? r.getFinalScore().doubleValue() : null);
            resultItems.add(item);
        }
        vo.setResults(resultItems);

        List<ReportDataVO.VerifItem> verifItems = new ArrayList<>();
        for (VerificationResult v : ctx.verifications) {
            ReportDataVO.VerifItem item = new ReportDataVO.VerifItem();
            item.setCheckItem(v.getCheckItem());
            item.setCheckType(v.getCheckType());
            item.setStatus(v.getStatus());
            item.setDetail(v.getDetail());
            verifItems.add(item);
        }
        vo.setVerifications(verifItems);

        return vo;
    }

    private void deleteReport(Long reportId) {
        ReportRecord record = reportRecordMapper.selectById(reportId);
        if (record == null) return;
        if (record.getPdfPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(record.getPdfPath()));
            } catch (IOException e) {
                log.warn("删除PDF文件失败: {}", record.getPdfPath());
            }
        }
        reportRecordMapper.deletePhysically(reportId);
    }

    private ScoreSummary calculateScoreSummary(List<EvaluationResultVO> results, TrainingTask task,
                                                BigDecimal storedTotal, EvaluationTemplate template) {
        BigDecimal wCompletion = template != null && template.getWeightCompletion() != null
                ? template.getWeightCompletion() : new BigDecimal("0.25");
        BigDecimal wTech = template != null && template.getWeightTech() != null
                ? template.getWeightTech() : new BigDecimal("0.25");
        BigDecimal wInnovation = template != null && template.getWeightInnovation() != null
                ? template.getWeightInnovation() : new BigDecimal("0.25");
        BigDecimal wDocument = template != null && template.getWeightDocument() != null
                ? template.getWeightDocument() : new BigDecimal("0.25");

        BigDecimal completionScore = BigDecimal.ZERO;
        BigDecimal techScore = BigDecimal.ZERO;
        BigDecimal innovationScore = BigDecimal.ZERO;
        BigDecimal documentScore = BigDecimal.ZERO;

        Map<String, BigDecimal> dimTotalWeight = new HashMap<>();
        Map<String, BigDecimal> dimTotalScore = new HashMap<>();

        for (EvaluationResultVO r : results) {
            String dim = r.getDimension();
            if (dim == null || "precheck".equals(dim)) continue;

            BigDecimal weight = r.getWeight() != null ? r.getWeight() : BigDecimal.ZERO;
            BigDecimal score = r.getFinalScore() != null ? r.getFinalScore() : BigDecimal.ZERO;
            BigDecimal weighted = score.multiply(weight);

            dimTotalWeight.merge(dim, weight, BigDecimal::add);
            dimTotalScore.merge(dim, weighted, BigDecimal::add);
        }

        for (Map.Entry<String, BigDecimal> e : dimTotalScore.entrySet()) {
            String dim = e.getKey();
            BigDecimal totalW = dimTotalWeight.getOrDefault(dim, BigDecimal.ONE);
            BigDecimal dimAvg = totalW.compareTo(BigDecimal.ZERO) > 0
                    ? e.getValue().divide(totalW, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            switch (dim) {
                case "completion": completionScore = dimAvg; break;
                case "tech": techScore = dimAvg; break;
                case "innovation": innovationScore = dimAvg; break;
                case "document": documentScore = dimAvg; break;
            }
        }

        // 总分以教师提交时存储的 submission.totalScore 为准（直接编辑、不参与权重换算）；
        // 仅当未存储时回退到按维度加权求和，保证旧数据兼容。
        BigDecimal total;
        if (storedTotal != null) {
            total = storedTotal.setScale(2, RoundingMode.HALF_UP);
        } else {
            total = completionScore.multiply(wCompletion)
                    .add(techScore.multiply(wTech))
                    .add(innovationScore.multiply(wInnovation))
                    .add(documentScore.multiply(wDocument))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        String grade;
        double t = total.doubleValue();
        if (t >= 90) grade = "优秀";
        else if (t >= 75) grade = "良好";
        else if (t >= 60) grade = "中等";
        else grade = "不及格";

        return new ScoreSummary(completionScore, techScore, innovationScore, documentScore, total, grade);
    }

    private int countPassedVerifications(List<VerificationResult> verifications) {
        return (int) verifications.stream().filter(v -> v.getStatus() != null && v.getStatus() == 1).count();
    }

    private boolean allVerificationsPassed(List<VerificationResult> verifications) {
        if (verifications.isEmpty()) return false;
        return verifications.stream().allMatch(v -> v.getStatus() != null && v.getStatus() == 1);
    }

    private String buildTeacherComment(List<EvaluationResultVO> results) {
        StringBuilder sb = new StringBuilder();
        for (EvaluationResultVO r : results) {
            if (r.getManualScore() != null || (r.getManualComment() != null && !r.getManualComment().isBlank())) {
                sb.append("【").append(r.getIndicatorName()).append("】");
                if (r.getManualScore() != null) sb.append(" 教师评分: ").append(r.getManualScore()).append("分");
                if (r.getManualComment() != null && !r.getManualComment().isBlank())
                    sb.append(" 评语: ").append(r.getManualComment());
                sb.append("；\n");
            }
        }
        return sb.toString();
    }

    private EvaluationResultVO toVO(EvaluationResult result, String indicatorName) {
        EvaluationResultVO vo = new EvaluationResultVO();
        vo.setId(result.getId());
        vo.setSubmissionId(result.getSubmissionId());
        vo.setTemplateId(result.getTemplateId());
        vo.setIndicatorId(result.getIndicatorId());
        vo.setIndicatorName(indicatorName);
        vo.setDimension(result.getDimension());
        vo.setAutoScore(result.getAutoScore());
        vo.setAutoComment(result.getAutoComment());
        vo.setManualScore(result.getManualScore());
        vo.setManualComment(result.getManualComment());
        vo.setAdjustScore(result.getAdjustScore());
        vo.setAdjustReason(result.getAdjustReason());
        vo.setFinalScore(result.getFinalScore());
        EvaluationIndicator ind = indicatorMapper.selectById(result.getIndicatorId());
        if (ind != null) {
            vo.setWeight(ind.getWeight() != null ? ind.getWeight() : BigDecimal.ZERO);
            vo.setEvalType(ind.getEvalType());
        } else {
            vo.setWeight(BigDecimal.ZERO);
        }
        vo.setCreateTime(result.getCreateTime() != null ? result.getCreateTime().format(DT_FMT) : null);
        vo.setUpdateTime(result.getUpdateTime() != null ? result.getUpdateTime().format(DT_FMT) : null);
        return vo;
    }

    private String getIndicatorName(Long indicatorId) {
        EvaluationIndicator ind = indicatorMapper.selectById(indicatorId);
        return ind != null ? ind.getName() : "(已删除)";
    }

    private String sanitizeName(String name) {
        if (name == null) return "report";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
    }

    private ReportOverviewVO buildOverviewVO(Long taskId) {
        TrainingTask task = taskMapper.selectById(taskId);
        List<Submission> submissions = submissionMapper.selectByTaskId(taskId);
        List<Submission> approvedSubs = submissions.stream()
                .filter(s -> s.getApprovalStatus() != null && s.getApprovalStatus() == 2).toList();

        ReportOverviewVO vo = new ReportOverviewVO();
        vo.setDate(LocalDate.now().format(DATE_FMT));
        vo.setTotalSubmissions(submissions.size());
        vo.setApprovedCount(approvedSubs.size());

        ReportDataVO.TaskInfo taskInfo = new ReportDataVO.TaskInfo();
        taskInfo.setId(task.getId());
        taskInfo.setTitle(task.getTitle());
        taskInfo.setCourseName(task.getCourseName());
        taskInfo.setModuleName(task.getModuleName());
        taskInfo.setSubject(task.getSubject());
        taskInfo.setWeightCompletion(task.getWeightCompletion());
        taskInfo.setWeightTech(task.getWeightTech());
        taskInfo.setWeightInnovation(task.getWeightInnovation());
        taskInfo.setWeightDocument(task.getWeightDocument());
        vo.setTask(taskInfo);

        List<ReportOverviewVO.StudentStat> studentStats = new ArrayList<>();
        List<Double> allScores = new ArrayList<>();
        Map<String, List<Double>> dimScores = new LinkedHashMap<>();
        dimScores.put("completion", new ArrayList<>());
        dimScores.put("tech", new ArrayList<>());
        dimScores.put("innovation", new ArrayList<>());
        dimScores.put("document", new ArrayList<>());

        for (Submission sub : approvedSubs) {
            User student = userMapper.selectById(sub.getStudentId());
            String studentName = student != null ? student.getRealName() : "未知学生";

            List<EvaluationResult> evalResults = evaluationResultMapper.selectBySubmissionId(sub.getId(), null);
            Long subTemplateId = evaluationResultMapper.selectTemplateIdBySubmissionId(sub.getId());
            EvaluationTemplate subTemplate = subTemplateId != null ? templateMapper.selectById(subTemplateId) : null;
            ScoreSummary summary = calculateScoreSummary(evalResults.stream()
                    .map(r -> toVO(r, getIndicatorName(r.getIndicatorId()))).toList(), task, sub.getTotalScore(), subTemplate);

            ReportOverviewVO.StudentStat stat = new ReportOverviewVO.StudentStat();
            stat.setStudentName(studentName);
            stat.setTotalScore(summary.totalScore != null ? summary.totalScore.doubleValue() : null);
            stat.setGrade(summary.grade);
            stat.setSubmitTime(sub.getSubmitTime() != null ? sub.getSubmitTime().format(DT_FMT) : "-");
            studentStats.add(stat);

            if (summary.totalScore != null) allScores.add(summary.totalScore.doubleValue());
            if (summary.completionScore != null) dimScores.get("completion").add(summary.completionScore.doubleValue());
            if (summary.techScore != null) dimScores.get("tech").add(summary.techScore.doubleValue());
            if (summary.innovationScore != null) dimScores.get("innovation").add(summary.innovationScore.doubleValue());
            if (summary.documentScore != null) dimScores.get("document").add(summary.documentScore.doubleValue());
        }
        vo.setStudentStats(studentStats);
        vo.setDimScores(dimScores);
        vo.setAllScores(allScores);

        if (!allScores.isEmpty()) {
            double avg = allScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double max = allScores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double min = allScores.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            vo.setAvgScore(Math.round(avg * 100.0) / 100.0);
            vo.setMaxScore(max);
            vo.setMinScore(min);
        }

        ReportOverviewVO.GradeCounts gradeCounts = new ReportOverviewVO.GradeCounts();
        for (ReportOverviewVO.StudentStat s : studentStats) {
            if ("优秀".equals(s.getGrade())) gradeCounts.setExcellent(gradeCounts.getExcellent() + 1);
            else if ("良好".equals(s.getGrade())) gradeCounts.setGood(gradeCounts.getGood() + 1);
            else if ("中等".equals(s.getGrade())) gradeCounts.setMedium(gradeCounts.getMedium() + 1);
            else gradeCounts.setFail(gradeCounts.getFail() + 1);
        }
        vo.setGradeCounts(gradeCounts);

        return vo;
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            // Auto-detect and register system CJK fonts (TTC support)
            try {
                ITextFontResolver fontResolver = (ITextFontResolver) renderer.getFontResolver();
                boolean found = false;
                // 优先注册内置 Noto Sans CJK SC（与网页/SVG 图表字体统一来源）
                if (fontConfig != null && fontConfig.getFontFilePath() != null) {
                    try {
                        fontResolver.addFont(fontConfig.getFontFilePath(), "Identity-H", true);
                        log.info("已注册内置CJK字体: {}", fontConfig.getFontFamily());
                        found = true;
                    } catch (Exception e) {
                        log.warn("内置CJK字体注册失败，回退系统字体: {}", e.getMessage());
                    }
                }
                // Try common TTC font paths on 麒麟 / LoongArch first
                String[][] cjkPaths = {
                    {"/usr/share/fonts/wqy-microhei/wqy-microhei.ttc", "WenQuanYi Micro Hei"},
                    {"/usr/share/fonts/truetype/wqy/wqy-microhei.ttc", "WenQuanYi Micro Hei"},
                    {"/usr/share/fonts/google-noto-cjk/NotoSansCJK-Regular.ttc", "Noto Sans CJK SC"},
                    {"/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc", "Noto Sans CJK SC"},
                    {"/usr/share/fonts/wqy-zenhei/wqy-zenhei.ttc", "WenQuanYi Zen Hei"},
                    {"/usr/share/fonts/noto-cjk/NotoSansCJKsc-Regular.otf", "Noto Sans CJK SC"},
                };
                boolean systemFound = found;
                if (!systemFound) for (String[] f : cjkPaths) {
                    try {
                        fontResolver.addFont(f[0] + ",0", f[1], true);
                        fontResolver.addFont(f[0] + ",0", f[1], false);
                        log.info("已注册CJK字体: {}", f[0]);
                        found = true;
                        break;
                    } catch (Exception ignored) {}
                }
                if (!found) {
                    // Fallback: scan directories for non-TTC fonts
                    String[] fontDirs = {"/usr/share/fonts", "/usr/local/share/fonts"};
                    for (String dir : fontDirs) {
                        try { fontResolver.addFontDirectory(dir, true); found = true; break; }
                        catch (Exception ignored) {}
                    }
                }
                if (!found) {
                    log.warn("未找到CJK字体，PDF中文可能显示异常");
                }
            } catch (Exception e) {
                log.warn("字体自动探测失败: {}", e.getMessage());
            }

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            byte[] pdf = baos.toByteArray();
            if (pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P' || pdf[2] != 'D' || pdf[3] != 'F' || pdf[4] != '-') {
                throw new BusinessException("PDF生成失败，输出不是有效PDF文件");
            }
            log.info("PDF 生成成功，大小 {} KB", pdf.length / 1024);
            return pdf;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF渲染失败", e);
            String msg = e.getMessage();
            throw new BusinessException("PDF渲染异常: " + (msg != null ? msg : e.getClass().getSimpleName()));
        }
    }

    private ReportHtmlGenerator.ScoreSummary toHtmlSummary(ScoreSummary s) {
        return new ReportHtmlGenerator.ScoreSummary(
                s.completionScore != null ? s.completionScore.doubleValue() : null,
                s.techScore != null ? s.techScore.doubleValue() : null,
                s.innovationScore != null ? s.innovationScore.doubleValue() : null,
                s.documentScore != null ? s.documentScore.doubleValue() : null,
                s.totalScore != null ? s.totalScore.doubleValue() : null,
                s.grade);
    }

    private static class ReportContext {
        final Submission submission;
        final TrainingTask task;
        final User student;
        final String studentName;
        final EvaluationTemplate template;
        final List<EvaluationResultVO> evalResults;
        final List<VerificationResult> verifications;
        final ScoreSummary summary;
        final String teacherComment;
        final int precheckPassCount;
        final boolean allPassed;

        ReportContext(Submission submission, TrainingTask task, User student, String studentName,
                      EvaluationTemplate template, List<EvaluationResultVO> evalResults,
                      List<VerificationResult> verifications, ScoreSummary summary,
                      String teacherComment, int precheckPassCount, boolean allPassed) {
            this.submission = submission;
            this.task = task;
            this.student = student;
            this.studentName = studentName;
            this.template = template;
            this.evalResults = evalResults;
            this.verifications = verifications;
            this.summary = summary;
            this.teacherComment = teacherComment;
            this.precheckPassCount = precheckPassCount;
            this.allPassed = allPassed;
        }
    }

    private static class ScoreSummary {
        final BigDecimal completionScore, techScore, innovationScore, documentScore, totalScore;
        final String grade;
        ScoreSummary(BigDecimal c, BigDecimal t, BigDecimal i, BigDecimal d, BigDecimal total, String grade) {
            this.completionScore = c; this.techScore = t; this.innovationScore = i; this.documentScore = d;
            this.totalScore = total; this.grade = grade;
        }
    }
}
