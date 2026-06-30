package com.example.evaluation.controller;

import com.example.evaluation.common.Result;
import com.example.evaluation.entity.User;
import com.example.evaluation.mapper.UserMapper;
import com.example.evaluation.security.SecurityUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Result<?> list(@RequestParam(required = false) Integer role) {
        List<User> users = userMapper.selectByRole(role);
        return Result.success(users);
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(userMapper.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> create(@RequestBody User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.insert(user);
        return Result.success("创建成功", user);
    }

    @GetMapping("/me")
    public Result<?> currentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
}
