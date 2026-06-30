package com.example.evaluation.controller;

import com.example.evaluation.entity.User;
import com.example.evaluation.mapper.DashboardMapper;
import com.example.evaluation.security.JwtProperties;
import com.example.evaluation.security.JwtUtil;
import com.example.evaluation.security.LoginUser;
import com.example.evaluation.security.TokenManager;
import com.example.evaluation.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

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
    private DashboardMapper dashboardMapper;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getHeader()).thenReturn("Authorization");
        when(jwtProperties.getPrefix()).thenReturn("Bearer ");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class TeacherDashboard {

        @BeforeEach
        void setUp() {
            User teacher = new User();
            teacher.setId(1L);
            teacher.setUsername("teacher01");
            teacher.setRole(1);
            teacher.setDeleted(0);

            LoginUser loginUser = new LoginUser(teacher);
            Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    loginUser, null, loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @Test
        void stats_asTeacher_shouldReturn200_withFourFields() throws Exception {
            when(dashboardMapper.countAllTasks()).thenReturn(10);
            when(dashboardMapper.countAllSubmissions()).thenReturn(50);
            when(dashboardMapper.countVerifiedSubmissions()).thenReturn(30);
            when(dashboardMapper.countEvaluatedSubmissions()).thenReturn(20);

            mockMvc.perform(get("/api/dashboard/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.taskCount").value(10))
                    .andExpect(jsonPath("$.data.submissionCount").value(50))
                    .andExpect(jsonPath("$.data.verifiedCount").value(30))
                    .andExpect(jsonPath("$.data.evaluatedCount").value(20));
        }

        @Test
        void statusCounts_shouldReturn200() throws Exception {
            when(dashboardMapper.countPublishedTasks()).thenReturn(5);
            when(dashboardMapper.countExpiredTasks()).thenReturn(3);
            when(dashboardMapper.countPendingReviewSubmissions()).thenReturn(10);
            when(dashboardMapper.countEvaluatedSubmissions()).thenReturn(15);

            mockMvc.perform(get("/api/dashboard/status-counts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.published").value(5))
                    .andExpect(jsonPath("$.data.completed").value(3))
                    .andExpect(jsonPath("$.data.pendingReview").value(10))
                    .andExpect(jsonPath("$.data.completedReview").value(15));
        }

        @Test
        void recentTasks_asTeacher_shouldReturn200() throws Exception {
            when(dashboardMapper.selectRecentTasksForTeacher()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/dashboard/recent-tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        void recentSubmissions_asTeacher_shouldReturn200() throws Exception {
            when(dashboardMapper.selectRecentSubmissionsForTeacher()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/dashboard/recent-submissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    class StudentDashboard {

        @BeforeEach
        void setUp() {
            User student = new User();
            student.setId(2L);
            student.setUsername("student01");
            student.setRole(0);
            student.setDeleted(0);

            LoginUser loginUser = new LoginUser(student);
            Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    loginUser, null, loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @Test
        void stats_asStudent_shouldReturn200_withThreeFields() throws Exception {
            when(dashboardMapper.countStudentFavoriteTasks(anyLong())).thenReturn(5);
            when(dashboardMapper.countStudentSubmissions(anyLong())).thenReturn(10);
            when(dashboardMapper.countStudentEvaluated(anyLong())).thenReturn(3);

            mockMvc.perform(get("/api/dashboard/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.taskCount").value(5))
                    .andExpect(jsonPath("$.data.submissionCount").value(10))
                    .andExpect(jsonPath("$.data.evaluatedCount").value(3));
        }

        @Test
        void recentTasks_asStudent_shouldReturn200() throws Exception {
            when(dashboardMapper.selectRecentTasksForStudent(anyLong())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/dashboard/recent-tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        void recentSubmissions_asStudent_shouldReturn200() throws Exception {
            when(dashboardMapper.selectRecentSubmissionsForStudent(anyLong())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/dashboard/recent-submissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }
}
