package com.example.evaluation.service;

import com.example.evaluation.dto.AutoScoreResponse;
import com.example.evaluation.dto.EvaluationResultVO;
import com.example.evaluation.dto.ManualScoreRequest;
import com.example.evaluation.dto.SubmissionVO;
import com.example.evaluation.entity.EvaluationIndicator;
import com.example.evaluation.entity.EvaluationResult;
import com.example.evaluation.entity.EvaluationTemplate;
import com.example.evaluation.entity.Submission;
import com.example.evaluation.entity.TrainingTask;
import com.example.evaluation.mapper.EvaluationIndicatorMapper;
import com.example.evaluation.mapper.EvaluationResultMapper;
import com.example.evaluation.mapper.EvaluationTemplateMapper;
import com.example.evaluation.mapper.SubmissionMapper;
import com.example.evaluation.mapper.TaskMapper;
import com.example.evaluation.utils.FileContentExtractor;
import com.example.evaluation.service.impl.EvaluationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private EvaluationTemplateMapper templateMapper;
    @Mock
    private EvaluationIndicatorMapper indicatorMapper;
    @Mock
    private EvaluationResultMapper evaluationResultMapper;
    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private AIService aiService;

    @Mock
    private FileContentExtractor fileContentExtractor;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    // ==================== Test Helpers ====================

    private EvaluationTemplate createTemplate(Long id, String name, Long taskId) {
        EvaluationTemplate t = new EvaluationTemplate();
        t.setId(id);
        t.setName(name);
        t.setTaskId(taskId);
        t.setEvalMethod(2);
        t.setAiWeight(new BigDecimal("0.60"));
        t.setManualWeight(new BigDecimal("0.40"));
        return t;
    }

    private EvaluationIndicator createIndicator(Long id, Long templateId, String name, int evalType) {
        EvaluationIndicator ind = new EvaluationIndicator();
        ind.setId(id);
        ind.setTemplateId(templateId);
        ind.setName(name);
        ind.setDescription(name + " description");
        ind.setEvalType(evalType);
        ind.setWeight(new BigDecimal("25.00"));
        ind.setMaxScore(new BigDecimal("100"));
        ind.setDimension("tech");
        return ind;
    }

    private Submission createSubmission(Long id, Long taskId) {
        Submission s = new Submission();
        s.setId(id);
        s.setTaskId(taskId);
        s.setStudentId(1L);
        s.setFilePaths("[\"/tmp/test.txt\"]");
        s.setApprovalStatus(null);
        s.setAiScoreStatus(null);
        return s;
    }

    private EvaluationResult createResult(Long id, Long submissionId, Long templateId, Long indicatorId) {
        EvaluationResult r = new EvaluationResult();
        r.setId(id);
        r.setSubmissionId(submissionId);
        r.setTemplateId(templateId);
        r.setIndicatorId(indicatorId);
        r.setEvalType(2);
        r.setAutoScore(new BigDecimal("85.00"));
        r.setFinalScore(new BigDecimal("85.00"));
        return r;
    }

    // ==================== Test Cases ====================

    @Test
    @DisplayName("getTemplates returns list of templates for a task")
    void testGetTemplates_ReturnsList() {
        Long taskId = 1L;
        List<EvaluationTemplate> expected = List.of(
                createTemplate(1L, "Template A", taskId),
                createTemplate(2L, "Template B", taskId));
        when(templateMapper.selectByTaskId(taskId)).thenReturn(expected);

        List<EvaluationTemplate> result = evaluationService.getTemplates(taskId);

        assertEquals(2, result.size());
        assertEquals("Template A", result.get(0).getName());
        assertEquals("Template B", result.get(1).getName());
        verify(templateMapper).selectByTaskId(taskId);
    }

    @Test
    @DisplayName("getTemplates returns specific template by task")
    void testGetTemplates_ReturnsSpecificTemplate() {
        Long taskId = 5L;
        EvaluationTemplate template = createTemplate(10L, "Template X", taskId);
        when(templateMapper.selectByTaskId(taskId)).thenReturn(List.of(template));

        List<EvaluationTemplate> result = evaluationService.getTemplates(taskId);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("Template X", result.get(0).getName());
    }

    @Test
    @DisplayName("createTemplate succeeds and inserts")
    void testCreateTemplate_Succeeds() {
        EvaluationTemplate template = new EvaluationTemplate();
        template.setName("New Template");
        template.setTaskId(1L);
        doAnswer(invocation -> {
            template.setId(100L);
            return null;
        }).when(templateMapper).insert(template);

        EvaluationTemplate result = evaluationService.createTemplate(template);

        assertNotNull(result);
        assertEquals("New Template", result.getName());
        assertEquals(Integer.valueOf(2), result.getEvalMethod());
        assertEquals(new BigDecimal("0.60"), result.getAiWeight());
        assertEquals(new BigDecimal("0.40"), result.getManualWeight());
        verify(templateMapper).insert(template);
    }

    @Test
    @DisplayName("updateTemplate succeeds for existing template")
    void testUpdateTemplate_Succeeds() {
        EvaluationTemplate template = new EvaluationTemplate();
        template.setId(1L);
        template.setName("Updated Template");
        when(templateMapper.selectById(1L)).thenReturn(createTemplate(1L, "Old Name", 1L));

        EvaluationResultMapper localResultMapper = evaluationResultMapper;
        evaluationService.updateTemplate(template);

        verify(templateMapper).updateById(template);
    }

    @Test
    @DisplayName("deleteTemplate succeeds and cleans up indicators and results")
    void testDeleteTemplate_Succeeds() {
        Long templateId = 1L;
        when(templateMapper.selectById(templateId)).thenReturn(createTemplate(templateId, "To Delete", 1L));
        when(indicatorMapper.selectIdsByTemplateId(templateId)).thenReturn(List.of(1L, 2L));
        when(evaluationResultMapper.deleteByTemplateId(templateId)).thenReturn(2);
        when(indicatorMapper.deleteByTemplateId(templateId)).thenReturn(2);
        when(templateMapper.deleteById(templateId)).thenReturn(1);

        evaluationService.deleteTemplate(templateId);

        verify(evaluationResultMapper).deleteByTemplateId(templateId);
        verify(indicatorMapper).deleteByTemplateId(templateId);
        verify(templateMapper).deleteById(templateId);
    }

    @Test
    @DisplayName("getIndicators returns indicators for a template")
    void testGetIndicators_ReturnsIndicators() {
        Long templateId = 1L;
        List<EvaluationIndicator> indicators = List.of(
                createIndicator(1L, templateId, "Completeness", 0),
                createIndicator(2L, templateId, "Code Quality", 2));
        when(indicatorMapper.selectByTemplateId(templateId)).thenReturn(indicators);

        List<EvaluationIndicator> result = evaluationService.getIndicators(templateId);

        assertEquals(2, result.size());
        assertEquals("Completeness", result.get(0).getName());
        assertEquals("Code Quality", result.get(1).getName());
        verify(indicatorMapper).selectByTemplateId(templateId);
    }

    @Test
    @DisplayName("batchSaveIndicators saves multiple indicators")
    void testBatchSaveIndicators_SavesMultiple() {
        Long templateId = 1L;
        when(indicatorMapper.selectByTemplateId(templateId)).thenReturn(List.of());

        EvaluationIndicator ind1 = new EvaluationIndicator();
        ind1.setName("Indicator 1");
        EvaluationIndicator ind2 = new EvaluationIndicator();
        ind2.setName("Indicator 2");
        List<EvaluationIndicator> indicators = List.of(ind1, ind2);

        doAnswer(invocation -> {
            EvaluationIndicator ind = invocation.getArgument(0);
            if (ind.getId() == null) ind.setId(10L);
            return null;
        }).when(indicatorMapper).insert(any());

        evaluationService.batchSaveIndicators(templateId, indicators);

        verify(indicatorMapper, times(2)).insert(any());
    }

    @Test
    @DisplayName("initResults creates result records and updates submission status")
    void testInitResults_CreatesResultRecords() {
        Long submissionId = 1L;
        Long templateId = 1L;

        Submission submission = createSubmission(submissionId, 1L);
        EvaluationTemplate template = createTemplate(templateId, "Template", 1L);
        EvaluationIndicator indicator1 = createIndicator(1L, templateId, "Indicator 1", 2);
        EvaluationIndicator indicator2 = createIndicator(2L, templateId, "Indicator 2", 0);

        when(submissionMapper.selectById(submissionId)).thenReturn(submission);
        when(templateMapper.selectById(templateId)).thenReturn(template);
        when(indicatorMapper.selectByTemplateId(templateId)).thenReturn(List.of(indicator1, indicator2));
        when(evaluationResultMapper.selectByThreeKeys(eq(submissionId), eq(templateId), anyLong()))
                .thenReturn(null);
        doAnswer(invocation -> {
            EvaluationResult r = invocation.getArgument(0);
            r.setId(100L);
            return null;
        }).when(evaluationResultMapper).insert(any());
        when(indicatorMapper.selectById(1L)).thenReturn(indicator1);
        when(indicatorMapper.selectById(2L)).thenReturn(indicator2);

        List<EvaluationResultVO> results = evaluationService.initResults(submissionId, templateId);

        assertEquals(2, results.size());
        verify(evaluationResultMapper, times(2)).insert(any());
    }

    @Test
    @DisplayName("autoScore triggers AI scoring and sets status")
    void testAutoScore_TriggersAI() {
        Long submissionId = 1L;
        Long templateId = 1L;

        Submission submission = createSubmission(submissionId, 1L);

        EvaluationTemplate template = createTemplate(templateId, "Template", 1L);
        EvaluationIndicator indicator = createIndicator(1L, templateId, "Code Quality", 0);
        indicator.setEvalType(0);

        EvaluationResult existingResult = new EvaluationResult();
        existingResult.setId(100L);
        existingResult.setSubmissionId(submissionId);
        existingResult.setTemplateId(templateId);
        existingResult.setIndicatorId(1L);
        existingResult.setEvalType(0);

        when(submissionMapper.selectById(submissionId)).thenReturn(submission);
        when(submissionMapper.updateById(any())).thenReturn(1);
        when(fileContentExtractor.extractContent(any(), any(), any())).thenReturn("Some code content here");
        when(evaluationResultMapper.selectBySubmissionId(submissionId, templateId))
                .thenReturn(List.of(existingResult));
        when(templateMapper.selectById(templateId)).thenReturn(template);
        when(indicatorMapper.selectByTemplateId(templateId)).thenReturn(List.of(indicator));
        when(evaluationResultMapper.selectByThreeKeys(submissionId, templateId, 1L))
                .thenReturn(existingResult);
        when(aiService.evaluateSubmission(anyString(), eq("Code Quality"), anyString()))
                .thenReturn(Map.of("score", 90, "comment", "Well structured"));
        when(evaluationResultMapper.updateById(any())).thenReturn(1);
        when(indicatorMapper.selectById(1L)).thenReturn(indicator);

        AutoScoreResponse response = evaluationService.autoScore(submissionId, templateId);

        assertNotNull(response);
        assertNotNull(response.getResults());
        assertEquals(1, response.getResults().size());
        assertNotNull(response.getStatusSummary());
        verify(aiService).evaluateSubmission(anyString(), eq("Code Quality"), anyString());
        verify(submissionMapper, atLeast(2)).updateById(any());
    }

    @Test
    @DisplayName("manualScore updates result with teacher score")
    void testManualScore_UpdatesResult() {
        ManualScoreRequest request = new ManualScoreRequest();
        request.setResultId(100L);
        request.setManualScore(new BigDecimal("88.00"));
        request.setManualComment("Good work, needs improvement on documentation");
        request.setAdjustScore(new BigDecimal("5.00"));
        request.setAdjustReason("Bonus for creativity");

        EvaluationResult result = createResult(100L, 1L, 1L, 10L);
        result.setEvalType(2);
        result.setAutoScore(new BigDecimal("85.00"));
        result.setFinalScore(new BigDecimal("85.00"));

        EvaluationIndicator indicator = createIndicator(10L, 1L, "Indicator", 2);
        EvaluationTemplate template = createTemplate(1L, "Template", 1L);

        Submission submission = createSubmission(1L, 1L);
        submission.setApprovalStatus(0);

        when(evaluationResultMapper.selectById(100L)).thenReturn(result);
        when(indicatorMapper.selectById(10L)).thenReturn(indicator);
        when(templateMapper.selectById(1L)).thenReturn(template);
        when(evaluationResultMapper.updateById(any())).thenReturn(1);
        when(submissionMapper.selectById(1L)).thenReturn(submission);
        when(submissionMapper.updateById(any())).thenReturn(1);

        evaluationService.manualScore(request);

        assertEquals(new BigDecimal("88.00"), result.getManualScore());
        assertEquals("Good work, needs improvement on documentation", result.getManualComment());
        verify(evaluationResultMapper).updateById(result);
    }

    @Test
    @DisplayName("submitResult computes weighted final score")
    void testSubmitResult_ComputesWeightedScore() {
        Long submissionId = 1L;

        Submission submission = createSubmission(submissionId, 1L);
        submission.setApprovalStatus(1);

        EvaluationResult result1 = new EvaluationResult();
        result1.setId(1L);
        result1.setSubmissionId(submissionId);
        result1.setIndicatorId(1L);
        result1.setFinalScore(new BigDecimal("90.00"));

        EvaluationResult result2 = new EvaluationResult();
        result2.setId(2L);
        result2.setSubmissionId(submissionId);
        result2.setIndicatorId(2L);
        result2.setFinalScore(new BigDecimal("80.00"));

        EvaluationIndicator ind1 = new EvaluationIndicator();
        ind1.setId(1L);
        ind1.setWeight(new BigDecimal("50.00"));

        EvaluationIndicator ind2 = new EvaluationIndicator();
        ind2.setId(2L);
        ind2.setWeight(new BigDecimal("50.00"));

        when(submissionMapper.selectById(submissionId)).thenReturn(submission);
        when(evaluationResultMapper.selectBySubmissionId(submissionId, null))
                .thenReturn(List.of(result1, result2));
        when(indicatorMapper.selectById(1L)).thenReturn(ind1);
        when(indicatorMapper.selectById(2L)).thenReturn(ind2);
        when(submissionMapper.updateById(any())).thenReturn(1);

        evaluationService.submitResult(submissionId);

        verify(submissionMapper).updateById(any());
        assertEquals(1, submission.getSubmitted());
        assertEquals(2, submission.getApprovalStatus());
        assertNotNull(submission.getTotalScore());
    }

    @Test
    @DisplayName("getHistory returns evaluation history for student")
    void testGetHistory_ReturnsHistory() {
        try (var securityUtilMock = mockStatic(com.example.evaluation.security.SecurityUtil.class)) {
            securityUtilMock.when(com.example.evaluation.security.SecurityUtil::getCurrentUserId).thenReturn(1L);
            securityUtilMock.when(com.example.evaluation.security.SecurityUtil::getCurrentRole).thenReturn(0);

            Submission submission = createSubmission(1L, 1L);
            submission.setApprovalStatus(2);
            submission.setTotalScore(new BigDecimal("85.00"));

            TrainingTask task = new TrainingTask();
            task.setId(1L);
            task.setTitle("Test Task");

            when(submissionMapper.selectByStudentIdApproved(1L)).thenReturn(List.of(submission));
            when(taskMapper.selectById(1L)).thenReturn(task);
            when(evaluationResultMapper.selectTemplateIdBySubmissionId(1L)).thenReturn(1L);

            List<SubmissionVO> history = evaluationService.getHistory();

            assertEquals(1, history.size());
            assertEquals("Test Task", history.get(0).getTaskName());
        }
    }
}
