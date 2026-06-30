package com.example.evaluation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("evaluation_indicator")
public class EvaluationIndicator {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String name;

    private String description;

    private BigDecimal weight;

    private BigDecimal maxScore;

    private Integer sortOrder;

    /** 该指标的评价方式：0=AI, 1=人工, 2=混合(template权重) */
    private Integer evalType;

    /** AI评分占比 */
    private BigDecimal aiWeight;

    /** 人工评分占比 */
    private BigDecimal manualWeight;

    /** 所属维度: completion/tech/innovation/document/precheck */
    private String dimension;

    private LocalDateTime createTime;
}
