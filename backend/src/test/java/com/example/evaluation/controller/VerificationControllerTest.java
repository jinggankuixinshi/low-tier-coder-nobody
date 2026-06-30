package com.example.evaluation.controller;

import com.example.evaluation.security.JwtProperties;
import com.example.evaluation.security.JwtUtil;
import com.example.evaluation.security.TokenManager;
import com.example.evaluation.security.UserDetailsServiceImpl;
import com.example.evaluation.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VerificationController.class)
class VerificationControllerTest {

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
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getHeader()).thenReturn("Authorization");
        when(jwtProperties.getPrefix()).thenReturn("Bearer ");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void check_asTeacher_shouldReturn200() throws Exception {
        when(verificationService.verify(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/verification/check/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getResult_shouldReturn200() throws Exception {
        when(verificationService.getResults(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/verification/result/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void check_asStudent_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/verification/check/1"))
                .andExpect(status().isForbidden());
    }
}
