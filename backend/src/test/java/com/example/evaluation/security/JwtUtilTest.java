package com.example.evaluation.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String TEST_SECRET =
            "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUE=";

    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpiration(7200000L);
        jwtUtil = new JwtUtil(jwtProperties);
    }

    @Test
    @DisplayName("generateToken produces non-null non-empty token")
    void testGenerateToken_ProducesValidToken() {
        String token = jwtUtil.generateToken(1L, "testuser", 1);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("parseToken returns valid claims from generated token")
    void testParseToken_ReturnsValidClaims() {
        String token = jwtUtil.generateToken(2L, "testuser2", 0);
        Claims claims = jwtUtil.parseToken(token);

        assertNotNull(claims);
        assertEquals("testuser2", claims.getSubject());
        assertEquals(2L, claims.get("userId", Long.class));
        assertEquals(0, claims.get("role", Integer.class));
    }

    @Test
    @DisplayName("validateToken returns true for valid token")
    void testValidateToken_ValidToken_ReturnsTrue() {
        String token = jwtUtil.generateToken(3L, "validuser", 1);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for expired token")
    void testValidateToken_ExpiredToken_ReturnsFalse() throws InterruptedException {
        JwtProperties shortLiveProps = new JwtProperties();
        shortLiveProps.setSecret(TEST_SECRET);
        shortLiveProps.setExpiration(1L);
        JwtUtil shortLiveJwt = new JwtUtil(shortLiveProps);

        String token = shortLiveJwt.generateToken(4L, "expireduser", 0);
        Thread.sleep(10);

        assertFalse(shortLiveJwt.validateToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for tampered token")
    void testValidateToken_TamperedToken_ReturnsFalse() {
        String token = jwtUtil.generateToken(5L, "tamperuser", 1);
        String tampered = token + "x";

        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    @DisplayName("getUserIdFromToken returns correct userId")
    void testGetUserIdFromToken_ReturnsCorrectUserId() {
        String token = jwtUtil.generateToken(100L, "user100", 1);
        assertEquals(100L, jwtUtil.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("getUsernameFromToken returns correct username")
    void testGetUsernameFromToken_ReturnsCorrectUsername() {
        String token = jwtUtil.generateToken(6L, "alice", 0);
        assertEquals("alice", jwtUtil.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("getRoleFromToken returns correct role")
    void testGetRoleFromToken_ReturnsCorrectRole() {
        String token = jwtUtil.generateToken(7L, "teacher1", 1);
        assertEquals(1, jwtUtil.getRoleFromToken(token));

        String studentToken = jwtUtil.generateToken(8L, "student1", 0);
        assertEquals(0, jwtUtil.getRoleFromToken(studentToken));
    }
}
