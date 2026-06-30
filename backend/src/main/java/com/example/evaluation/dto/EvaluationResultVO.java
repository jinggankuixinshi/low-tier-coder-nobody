package com.example.evaluation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EvaluationResultVO {

    private Long id;
    private Long submissionId;
    private Long templateId;
    private Long indicatorId;
    private String indicatorName;
    private BigDecimal weight;
    private BigDecimal maxScore;
    private Integer evalType;
    private BigDecimal aiWeight;
    private BigDecimal manualWeight;
    private BigDecimal autoScore;
    private String autoComment;
    private BigDecimal manualScore;
    private String manualComment;
    private BigDecimal adjustScore;
    private String adjustReason;
    private String dimension;
    private String improvementSuggestion;
    private String issueSeverity;
    private BigDecimal finalScore;
    private String createTime;
    private String updateTime;
}
