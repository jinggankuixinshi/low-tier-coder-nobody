package com.example.evaluation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SubmissionVO {

    private Long id;
    private Long taskId;
    private Long studentId;
    private String studentName;
    private String username;
    private String fileNames;
    private String filePaths;
    private String fileSizes;
    private Integer evaluationStatus;
    private Integer submitted;
    private Integer approvalStatus;
    private Long draftTemplateId;
    private Long templateId;
    private String taskName;
    private BigDecimal totalScore;
    private BigDecimal subjectiveScore;
    private String subjectiveReason;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
}
