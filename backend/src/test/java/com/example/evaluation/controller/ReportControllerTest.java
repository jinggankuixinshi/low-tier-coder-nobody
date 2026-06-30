package com.example.evaluation.controller;

import com.example.evaluation.dto.ReportDataVO;
import com.example.evaluation.entity.ReportRecord;
import com.example.evaluation.mapper.TaskMapper;
import com.example.evaluation.security.JwtProperties;
import com.example.evaluation.security.JwtUtil;
import com.example.evaluation.security.TokenManager;
import com.example.evaluation.security.UserDetailsServiceImpl;
import com.example.evaluation.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private TokenManager tokenManager;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private ReportService reportService;

    @MockBean
    private TaskMapper taskMapper;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @BeforeEach
    void setUp() {
        when(jwtProperties.getHeader()).thenReturn("Authorization");
        when(jwtProperties.getPrefix()).thenReturn("Bearer ");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getApprovedSubmissions_asTeacher_shouldReturn200() throws Exception {
        when(reportService.getApprovedSubmissionsWithNames(anyLong())).thenReturn(Collections.emptyList());
        when(reportService.isAllApproved(anyLong())).thenReturn(false);

        mockMvc.perform(get("/api/report/approved-submissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getReportData_shouldReturn200() throws Exception {
        ReportDataVO data = new ReportDataVO();
        data.setSubmissionId(1L);
        when(reportService.getOrGenerateReportData(anyLong())).thenReturn(data);

        mockMvc.perform(get("/api/report/data/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void exportPdf_shouldReturn200() throws Exception {
        ReportDataVO data = new ReportDataVO();
        data.setSubmissionId(1L);
        when(reportService.exportPdf(anyLong())).thenReturn(new byte[]{1, 2, 3});
        when(reportService.getOrGenerateReportData(anyLong())).thenReturn(data);

        mockMvc.perform(get("/api/report/export/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void batchExport_asTeacher_shouldReturn200() throws Exception {
        when(reportService.batchExportPdfZip(anyList())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(post("/api/report/batch-export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void generateTaskOverview_asTeacher_shouldReturn200() throws Exception {
        ReportRecord record = new ReportRecord();
        record.setId(1L);
        record.setTitle("Overview");
        when(reportService.generateTaskOverview(anyLong())).thenReturn(record);

        mockMvc.perform(post("/api/report/generate-task-overview/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getOverviewStatus_asTeacher_shouldReturn200() throws Exception {
        when(reportService.getOverviewStatus(anyLong())).thenReturn(Map.of("status", "done"));

        mockMvc.perform(get("/api/report/overview-status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getOverviewData_asTeacher_shouldReturn200() throws Exception {
        when(reportService.getOrGenerateOverviewData(anyLong())).thenReturn(null);

        mockMvc.perform(get("/api/report/overview-data/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
