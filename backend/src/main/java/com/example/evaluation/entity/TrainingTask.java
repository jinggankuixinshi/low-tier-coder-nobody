package com.example.evaluation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("training_task")
public class TrainingTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String subject;

    private String description;

    private String expectedOutput;

    /** 课程名称 */
    private String courseName;

    /** 所属章节/模块 */
    private String moduleName;

    /** 任务简介 */
    private String brief;

    /** 业务场景 */
    private String businessScenario;

    /** 实现条件 */
    private String implConditions;

    /** 需求文档文件路径JSON */
    private String requirementDocs;

    private Long teacherId;

    private Integer status;

    private LocalDateTime deadline;

    /** 完成度权重 */
    private BigDecimal weightCompletion;

    /** 技术质量权重 */
    private BigDecimal weightTech;

    /** 创新权重 */
    private BigDecimal weightInnovation;

    /** 文档权重 */
    private BigDecimal weightDocument;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
