package com.example.evaluation.controller;

import com.example.evaluation.common.BusinessException;
import com.example.evaluation.common.Result;
import com.example.evaluation.dto.*;
import com.example.evaluation.entity.User;
import com.example.evaluation.mapper.UserMapper;
import com.example.evaluation.security.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenManager tokenManager;
    private final RsaKeyManager rsaKeyManager;

    @Value("${app.teacher-invite-code:jiaoshi}")
    private String teacherInviteCode;

    public AuthController(UserMapper userMapper,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, TokenManager tokenManager,
                          RsaKeyManager rsaKeyManager) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenManager = tokenManager;
        this.rsaKeyManager = rsaKeyManager;
    }

    @GetMapping("/public-key")
    public Result<PublicKeyResponse> getPublicKey() {
        return Result.success(new PublicKeyResponse(rsaKeyManager.getPublicKeyPem(), "RSA"));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String decryptedPassword;
        try {
            decryptedPassword = rsaKeyManager.decrypt(request.getPassword());
        } catch (Exception e) {
            throw new BusinessException("密码解密失败，请确保使用正确的公钥加密");
        }

        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getDeleted() == 1) {
            throw new BusinessException("该账号已被禁用");
        }

        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        tokenManager.registerToken(user.getId(), token, jwtUtil.getExpiration());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        response.setRoleName(user.getRole() == 1 ? "教师" : "学生");
        response.setPermissions(
                user.getRole() == 1
                        ? List.of("task:manage", "evaluation:manage", "report:manage", "verification:view")
                        : List.of("task:view", "submission:upload", "result:view"));

        return Result.success("登录成功", response);
    }

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }

        if (req.getRole() == 1) {
            if (req.getInviteCode() == null || !teacherInviteCode.equals(req.getInviteCode())) {
                throw new BusinessException("教师邀请码无效");
            }
        }

        String decryptedPassword;
        try {
            decryptedPassword = rsaKeyManager.decrypt(req.getPassword());
        } catch (Exception e) {
            throw new BusinessException("密码解密失败，请确保使用正确的公钥加密");
        }

        long count = userMapper.countByUsername(req.getUsername());
        if (count > 0) {
            throw new BusinessException("该用户名已被注册");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(decryptedPassword));
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setRole(req.getRole());
        userMapper.insert(user);

        return Result.success("注册成功");
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        LoginUser loginUser = SecurityUtil.getCurrentUser();
        if (loginUser != null) {
            String activeToken = tokenManager.getActiveToken(loginUser.getUserId());
            if (activeToken != null) {
                tokenManager.removeToken(loginUser.getUserId(), activeToken);
            }
        }
        return Result.success("已退出登录");
    }
}
