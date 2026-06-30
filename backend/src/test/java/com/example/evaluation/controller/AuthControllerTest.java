package com.example.evaluation.controller;

import com.example.evaluation.common.GlobalExceptionHandler;
import com.example.evaluation.entity.User;
import com.example.evaluation.mapper.UserMapper;
import com.example.evaluation.security.JwtUtil;
import com.example.evaluation.security.RsaKeyManager;
import com.example.evaluation.security.TokenManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RsaKeyManager rsaKeyManager;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private TokenManager tokenManager;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private com.example.evaluation.security.JwtProperties jwtProperties;

    @MockBean
    private com.example.evaluation.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPassword("$2a$10$encrypted_password_hash");
        mockUser.setRealName("TestUser");
        mockUser.setRole(0);
        mockUser.setEmail("test@example.com");
        mockUser.setPhone("13800138000");
        mockUser.setDeleted(0);
    }

    @Test
    @DisplayName("GET /api/auth/public-key should return 200 with publicKey and algorithm")
    void getPublicKey_returnsOk() throws Exception {
        when(rsaKeyManager.getPublicKeyPem()).thenReturn("-----BEGIN PUBLIC KEY-----\nmockkey\n-----END PUBLIC KEY-----");

        mockMvc.perform(get("/api/auth/public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.publicKey").value("-----BEGIN PUBLIC KEY-----\nmockkey\n-----END PUBLIC KEY-----"))
                .andExpect(jsonPath("$.data.algorithm").value("RSA"));
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials should return 200")
    void login_validCredentials_returnsOk() throws Exception {
        String encryptedPassword = "encrypted_password_base64";
        when(rsaKeyManager.decrypt(encryptedPassword)).thenReturn("plainPassword");
        when(userMapper.selectByUsername("testuser")).thenReturn(mockUser);
        when(passwordEncoder.matches("plainPassword", mockUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser", 0)).thenReturn("mock.jwt.token");
        when(jwtUtil.getExpiration()).thenReturn(7200000L);

        com.example.evaluation.dto.LoginRequest loginRequest = new com.example.evaluation.dto.LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword(encryptedPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.role").value(0))
                .andExpect(jsonPath("$.data.roleName").value("学生"))
                .andExpect(jsonPath("$.data.permissions").isArray());

        verify(tokenManager).registerToken(eq(1L), eq("mock.jwt.token"), eq(7200000L));
    }

    @Test
    @DisplayName("POST /api/auth/login with bad credentials should return business error")
    void login_badCredentials_returnsBusinessError() throws Exception {
        String encryptedPassword = "bad_encrypted_password";
        when(rsaKeyManager.decrypt(encryptedPassword)).thenThrow(new RuntimeException("decryption failed"));

        com.example.evaluation.dto.LoginRequest loginRequest = new com.example.evaluation.dto.LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword(encryptedPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("密码解密失败，请确保使用正确的公钥加密"));
    }

    @Test
    @DisplayName("POST /api/auth/register with valid student request should return 200")
    void register_validStudentRequest_returnsOk() throws Exception {
        when(rsaKeyManager.decrypt("encrypted_pwd")).thenReturn("plainPwd");
        when(userMapper.countByUsername("newstudent")).thenReturn(0L);
        when(passwordEncoder.encode("plainPwd")).thenReturn("$2a$10$hashed");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        com.example.evaluation.dto.RegisterRequest registerRequest = new com.example.evaluation.dto.RegisterRequest();
        registerRequest.setUsername("newstudent");
        registerRequest.setPassword("encrypted_pwd");
        registerRequest.setConfirmPassword("encrypted_pwd");
        registerRequest.setRealName("NewStudent");
        registerRequest.setPhone("13900139000");
        registerRequest.setEmail("new@example.com");
        registerRequest.setRole(0);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("注册成功"));
    }

    @Test
    @DisplayName("POST /api/auth/register with wrong invite code for teacher should return business error")
    void register_wrongInviteCode_returnsBusinessError() throws Exception {
        com.example.evaluation.dto.RegisterRequest registerRequest = new com.example.evaluation.dto.RegisterRequest();
        registerRequest.setUsername("newteacher");
        registerRequest.setPassword("encrypted_pwd");
        registerRequest.setConfirmPassword("encrypted_pwd");
        registerRequest.setRealName("NewTeacher");
        registerRequest.setPhone("13900139001");
        registerRequest.setEmail("teacher@example.com");
        registerRequest.setRole(1);
        registerRequest.setInviteCode("wrong_code");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("教师邀请码无效"));
    }

    @Test
    @DisplayName("POST /api/auth/register with correct invite code for teacher should return 200")
    void register_correctInviteCode_returnsOk() throws Exception {
        when(rsaKeyManager.decrypt("encrypted_pwd")).thenReturn("plainPwd");
        when(userMapper.countByUsername("newteacher")).thenReturn(0L);
        when(passwordEncoder.encode("plainPwd")).thenReturn("$2a$10$hashed");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        com.example.evaluation.dto.RegisterRequest registerRequest = new com.example.evaluation.dto.RegisterRequest();
        registerRequest.setUsername("newteacher");
        registerRequest.setPassword("encrypted_pwd");
        registerRequest.setConfirmPassword("encrypted_pwd");
        registerRequest.setRealName("NewTeacher");
        registerRequest.setPhone("13900139001");
        registerRequest.setEmail("teacher@example.com");
        registerRequest.setRole(1);
        registerRequest.setInviteCode("jiaoshi");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("注册成功"));
    }

    @Test
    @DisplayName("POST /api/auth/logout when authenticated should return 200")
    @WithMockUser
    void logout_authenticated_returnsOk() throws Exception {
        when(tokenManager.getActiveToken(anyLong())).thenReturn(null);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("已退出登录"));
    }

    @Test
    @DisplayName("POST /api/auth/logout when not authenticated should return 200")
    @WithAnonymousUser
    void logout_notAuthenticated_returnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("已退出登录"));
    }
}
