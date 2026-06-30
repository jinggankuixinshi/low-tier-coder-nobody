package com.example.evaluation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("evaluation_result")
public class EvaluationResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private Long templateId;

    private Long indicatorId;

    /** 评价方式：0=AI, 1=人工, 2=混合 */
    private Integer evalType;

    private BigDecimal autoScore;

    private String autoComment;

    private BigDecimal manualScore;

    private String manualComment;

    /** 所属维度 */
    private String dimension;

    /** 调整分（教师加减分，可为负数） */
    private BigDecimal adjustScore;

    /** 调整原因 */
    private String adjustReason;

    private BigDecimal finalScore;

    /** 改进建议 */
    private String improvementSuggestion;

    /** 严重程度: 严重/一般/建议 */
    private String issueSeverity;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
