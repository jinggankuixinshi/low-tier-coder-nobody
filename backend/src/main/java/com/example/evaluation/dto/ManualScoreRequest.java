package com.example.evaluation.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 教师手动评分请求 DTO
 */
@Data
public class ManualScoreRequest {

    /** 评价结果ID */
    private Long resultId;

    /** 教师评分 */
    private BigDecimal manualScore;

    /** 教师评语 */
    private String manualComment;

    /** 调整分（加减分，可为负数） */
    private BigDecimal adjustScore;

    /** 调整原因 */
    private String adjustReason;
}
