package com.example.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublicKeyResponse {
    private String publicKey;
    private String algorithm;
}
