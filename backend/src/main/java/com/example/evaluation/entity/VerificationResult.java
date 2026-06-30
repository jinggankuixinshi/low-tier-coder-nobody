package com.example.evaluation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核查结果表
 */
@Data
@TableName("verification_result")
public class VerificationResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联提交ID */
    private Long submissionId;

    /** 关联任务ID */
    private Long taskId;

    /** 检查项名称 */
    private String checkItem;

    /** 检查类型: 0=步骤完整性, 1=效果校验, 2=逻辑漏洞 */
    private Integer checkType;

    /** 预期内容 */
    private String expectedContent;

    /** 实际内容 */
    private String actualContent;

    /** 核查状态: 0=待核查, 1=通过, 2=不通过 */
    private Integer status;

    /** 详细说明 */
    private String detail;

    /** 创建时间 */
    private LocalDateTime createTime;
}
