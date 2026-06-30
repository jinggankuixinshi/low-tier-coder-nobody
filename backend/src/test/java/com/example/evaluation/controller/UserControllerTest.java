package com.example.evaluation.controller;

import com.example.evaluation.entity.User;
import com.example.evaluation.mapper.UserMapper;
import com.example.evaluation.security.JwtProperties;
import com.example.evaluation.security.JwtUtil;
import com.example.evaluation.security.LoginUser;
import com.example.evaluation.security.TokenManager;
import com.example.evaluation.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

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
    private UserMapper userMapper;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @BeforeEach
    void setUp() {
        when(jwtProperties.getHeader()).thenReturn("Authorization");
        when(jwtProperties.getPrefix()).thenReturn("Bearer ");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void list_shouldReturn200() throws Exception {
        when(userMapper.selectByRole(isNull())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void listByRole_shouldReturn200() throws Exception {
        when(userMapper.selectByRole(eq(1))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users").param("role", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void detail_shouldReturn200() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        when(userMapper.selectById(eq(1L))).thenReturn(user);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void create_asTeacher_shouldReturn200() throws Exception {
        User userToCreate = new User();
        userToCreate.setUsername("newuser");
        userToCreate.setPassword("password123");

        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userToCreate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void currentUser_shouldReturn200() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("teacher01");
        mockUser.setRealName("Teacher");
        mockUser.setRole(1);
        mockUser.setDeleted(0);

        LoginUser loginUser = new LoginUser(mockUser);
        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userMapper.selectById(eq(1L))).thenReturn(mockUser);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        SecurityContextHolder.clearContext();
    }
}
