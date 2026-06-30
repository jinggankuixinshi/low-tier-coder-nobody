package com.example.evaluation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("evaluation_template")
public class EvaluationTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Long taskId;

    private Long teacherId;

    /** 评价方式：0=纯AI, 1=纯人工, 2=混合AI+人工 */
    private Integer evalMethod;

    /** AI评分占比（混合模式） */
    private BigDecimal aiWeight;

    /** 人工评分占比（混合模式） */
    private BigDecimal manualWeight;

    /** 默认AI模型 */
    private String aiModel;

    /** 完成度维度全局权重（0~1） */
    private BigDecimal weightCompletion;

    /** 技术质量维度全局权重（0~1） */
    private BigDecimal weightTech;

    /** 创新维度全局权重（0~1） */
    private BigDecimal weightInnovation;

    /** 文档维度全局权重（0~1） */
    private BigDecimal weightDocument;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
