package com.example.evaluation.controller;

import com.example.evaluation.dto.AutoScoreResponse;
import com.example.evaluation.dto.ManualScoreRequest;
import com.example.evaluation.entity.EvaluationIndicator;
import com.example.evaluation.entity.EvaluationTemplate;
import com.example.evaluation.mapper.EvaluationIndicatorMapper;
import com.example.evaluation.mapper.EvaluationResultMapper;
import com.example.evaluation.mapper.EvaluationTemplateMapper;
import com.example.evaluation.security.JwtProperties;
import com.example.evaluation.security.JwtUtil;
import com.example.evaluation.security.TokenManager;
import com.example.evaluation.security.UserDetailsServiceImpl;
import com.example.evaluation.service.EvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EvaluationController.class)
class EvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private TokenManager tokenManager;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private EvaluationService evaluationService;

    @MockBean
    private EvaluationTemplateMapper evaluationTemplateMapper;

    @MockBean
    private EvaluationResultMapper evaluationResultMapper;

    @MockBean
    private EvaluationIndicatorMapper evaluationIndicatorMapper;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getHeader()).thenReturn("Authorization");
        when(jwtProperties.getPrefix()).thenReturn("Bearer ");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getTemplates_shouldReturn200() throws Exception {
        when(evaluationService.getTemplates(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/evaluation/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createTemplate_asTeacher_shouldReturn200() throws Exception {
        EvaluationTemplate template = new EvaluationTemplate();
        template.setName("test-template");

        when(evaluationService.createTemplate(any())).thenReturn(template);

        mockMvc.perform(post("/api/evaluation/templates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(template)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createTemplate_asStudent_shouldReturn403() throws Exception {
        EvaluationTemplate template = new EvaluationTemplate();
        template.setName("test-template");

        mockMvc.perform(post("/api/evaluation/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(template)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateTemplate_asTeacher_shouldReturn200() throws Exception {
        EvaluationTemplate template = new EvaluationTemplate();
        template.setName("updated-template");

        when(evaluationService.updateTemplate(any())).thenReturn(template);

        mockMvc.perform(put("/api/evaluation/templates/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(template)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void deleteTemplate_asTeacher_shouldReturn200() throws Exception {
        doNothing().when(evaluationService).deleteTemplate(anyLong());

        mockMvc.perform(delete("/api/evaluation/templates/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getIndicators_shouldReturn200() throws Exception {
        when(evaluationService.getIndicators(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/evaluation/templates/1/indicators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createIndicator_asTeacher_shouldReturn200() throws Exception {
        EvaluationIndicator indicator = new EvaluationIndicator();
        indicator.setName("test-indicator");

        when(evaluationService.createIndicator(any())).thenReturn(indicator);

        mockMvc.perform(post("/api/evaluation/indicators")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(indicator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateIndicator_asTeacher_shouldReturn200() throws Exception {
        EvaluationIndicator indicator = new EvaluationIndicator();
        indicator.setName("updated-indicator");

        when(evaluationService.updateIndicator(any())).thenReturn(indicator);

        mockMvc.perform(put("/api/evaluation/indicators/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(indicator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void batchSaveIndicators_asTeacher_shouldReturn200() throws Exception {
        doNothing().when(evaluationService).batchSaveIndicators(anyLong(), anyList());

        mockMvc.perform(put("/api/evaluation/templates/1/indicators/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.emptyList())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void deleteIndicator_asTeacher_shouldReturn200() throws Exception {
        doNothing().when(evaluationService).deleteIndicator(anyLong());

        mockMvc.perform(delete("/api/evaluation/indicators/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void initResults_asTeacher_shouldReturn200() throws Exception {
        when(evaluationService.initResults(anyLong(), anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/evaluation/init-results/1")
                        .with(csrf())
                        .param("templateId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void autoScore_asTeacher_shouldReturn200() throws Exception {
        AutoScoreResponse response = new AutoScoreResponse();
        response.setResults(java.util.Collections.emptyList());
        when(evaluationService.autoScore(anyLong(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/evaluation/auto-score/1")
                        .with(csrf())
                        .param("templateId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void manualScore_asTeacher_shouldReturn200() throws Exception {
        ManualScoreRequest request = new ManualScoreRequest();
        request.setResultId(1L);
        request.setManualScore(new BigDecimal("85.0"));
        doNothing().when(evaluationService).manualScore(any());

        mockMvc.perform(post("/api/evaluation/manual-score")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void submitResult_asTeacher_shouldReturn200() throws Exception {
        doNothing().when(evaluationService).submitResult(anyLong(), any(), any());

        mockMvc.perform(post("/api/evaluation/submit/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
