package com.example.evaluation.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AutoScoreResponse {

    private List<EvaluationResultVO> results;
    private Map<String, Object> statusSummary;
}
