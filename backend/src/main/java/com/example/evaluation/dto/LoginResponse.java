package com.example.evaluation.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String realName;
    private Integer role;
    private String roleName;
    private List<String> permissions;
}
