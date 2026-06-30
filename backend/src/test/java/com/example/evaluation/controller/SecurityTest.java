package com.example.evaluation.controller;

import com.example.evaluation.dto.LoginRequest;
import com.example.evaluation.entity.User;
import com.example.evaluation.mapper.UserMapper;
import com.example.evaluation.security.JwtUtil;
import com.example.evaluation.security.TokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        if (userMapper.countByUsername("security_test_user") == 0) {
            User user = new User();
            user.setUsername("security_test_user");
            user.setPassword(passwordEncoder.encode("correct_password_123"));
            user.setRealName("Security Test User");
            user.setPhone("13900001111");
            user.setEmail("security_test@example.com");
            user.setRole(1);
            userMapper.insert(user);
            testUserId = user.getId();
        } else {
            testUserId = userMapper.selectByUsername("security_test_user").getId();
        }
    }

    @Test
    @DisplayName("GET /api/auth/public-key returns 200 without auth (public endpoint)")
    void testPublicKeyEndpoint_Returns200_WithoutAuth() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/api/auth/public-key", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("data"));
    }

    @Test
    @DisplayName("GET /api/evaluation/templates requires authentication (returns 403)")
    void testEvaluationTemplates_WithoutAuth() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/api/evaluation/templates", Map.class);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/tasks requires authentication (returns 403)")
    void testTasksEndpoint_WithoutAuth() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/api/tasks", Map.class);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /api/evaluation/templates returns 403 when not authenticated")
    void testCreateTemplate_Returns403_WhenNotAuthenticated() {
        Map<String, Object> templateBody = Map.of(
                "name", "Test Template",
                "taskId", 1,
                "evalMethod", 2);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(templateBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/evaluation/templates", request, Map.class);

        assertTrue(
                response.getStatusCode() == HttpStatus.FORBIDDEN
                        || response.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /api/tasks with valid JWT token returns 200")
    void testTasksEndpoint_WithValidJwt_Returns200() {
        assertNotNull(testUserId);
        String token = jwtUtil.generateToken(testUserId, "security_test_user", 1);
        tokenManager.registerToken(testUserId, token, jwtUtil.getExpiration());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/tasks", HttpMethod.GET, request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("POST /api/auth/login with wrong password returns error")
    void testLogin_WrongPassword_ReturnsError() throws Exception {
        ResponseEntity<Map> pubKeyResp = restTemplate.getForEntity(
                baseUrl + "/api/auth/public-key", Map.class);
        assertNotNull(pubKeyResp.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) pubKeyResp.getBody().get("data");
        String pem = (String) data.get("publicKey");

        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal("wrongPassword_xyz".getBytes(StandardCharsets.UTF_8));
        String encryptedPassword = Base64.getEncoder().encodeToString(encrypted);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("security_test_user");
        loginRequest.setPassword(encryptedPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login", request, Map.class);

        assertNotNull(response.getBody());
        Object code = response.getBody().get("code");
        assertNotNull(code);
        assertNotEquals(200, ((Number) code).intValue());
    }
}
