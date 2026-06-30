package com.example.evaluation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生提交成果表
 */
@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联任务ID */
    private Long taskId;

    /** 提交学生ID */
    private Long studentId;

    /** 上传文件路径（JSON数组） */
    private String filePaths;

    /** 原始文件名（JSON数组） */
    private String fileNames;

    /** 文件大小（JSON数组，单位字节） */
    private String fileSizes;

    /** 教师综合评价 */
    private String teacherComment;

    /** 评价状态: 0=未评价, 1=已有评价数据 */
    private Integer evaluationStatus;

    /** 是否已提交: 0=未提交, 1=已提交(不可改) */
    private Integer submitted;

    /** 审批状态: null/0=未审批, 1=草稿(可改), 2=已审批(不可改) */
    private Integer approvalStatus;

    /** 当前生效的草稿模板ID（approvalStatus=1时有效） */
    private Long draftTemplateId;

    /** AI评分状态: 0=未启动, 1=评分中, 2=已完成, 3=失败 */
    private Integer aiScoreStatus;

    /** 加权总分（= 加权计算分 + 主观评分，0~100） */
    private BigDecimal totalScore;

    /** 教师主观评分（在加权分基础上增减，范围 ±20） */
    private BigDecimal subjectiveScore;

    /** 主观评分原因 */
    private String subjectiveReason;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
