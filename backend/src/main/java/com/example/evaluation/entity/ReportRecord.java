package com.example.evaluation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("report_record")
public class ReportRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联提交ID（单次评价报表） */
    private Long submissionId;

    /** 关联任务ID（批量统计报表） */
    private Long taskId;

    /** 报表类型: 0=单次评价报告, 1=任务总览报表 */
    private Integer reportType;

    /** 报表标题 */
    private String title;

    /** PDF文件路径 */
    private String pdfPath;

    /** 批次组ID（批量生成时关联） */
    private String batchGroupId;

    /** 生成状态: 0=待生成, 1=生成中, 2=已完成, 3=失败 */
    private Integer generationStatus;

    /** 生成失败原因 */
    private String generationError;

    /** 前置检查通过数 */
    private Integer precheckPassCount;

    /** 前置检查全部通过 */
    private Integer precheckPassed;

    /** 完成度得分 */
    private BigDecimal completionScore;

    /** 技术质量得分 */
    private BigDecimal techQualityScore;

    /** 创新得分 */
    private BigDecimal innovationScore;

    /** 文档得分 */
    private BigDecimal documentScore;

    /** 加权总分 */
    private BigDecimal weightedTotal;

    /** 等级: 优秀/良好/中等/不及格 */
    private String grade;

    /** 教师评语 */
    private String teacherComment;

    /** 教学反思 */
    private String teachingReflection;

    /** 评价教师ID */
    private Long reviewerId;

    /** 评价日期 */
    private LocalDate reviewDate;

    /** 总览报表关联的提交数（仅 reportType=1 时有效） */
    private Integer submissionCount;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;
}
