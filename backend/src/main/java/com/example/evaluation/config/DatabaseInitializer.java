package com.example.evaluation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库结构初始化器。
 * 仅负责建表(schema)和补列(schema migration)，不插入任何业务数据。
 * 业务数据通过 sql/init.sql + sql/data.sql 手动初始化。
 */
@Slf4j
@Component
public class DatabaseInitializer implements InitializingBean {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            ensureFavoriteTaskTable();
            addMissingColumns();
        } catch (Exception e) {
            log.warn("数据库结构初始化失败: {}", e.getMessage());
        }
    }

    // ==================== 建表与补列 ====================

    private void ensureFavoriteTaskTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS favorite_task (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
                user_id BIGINT NOT NULL COMMENT '用户ID',
                task_id BIGINT NOT NULL COMMENT '任务ID',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                UNIQUE KEY uk_user_task (user_id, task_id),
                KEY idx_user_id (user_id),
                KEY idx_task_id (task_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏任务表'
        """);
        log.info("favorite_task 表检查/创建完成");
    }

    private void addMissingColumns() {
        // --- submission 表补列 ---
        tryAddColumn("submission", "file_sizes", "TEXT DEFAULT NULL COMMENT '文件大小（JSON数组，字节）'", "file_names");
        tryAddColumn("submission", "student_id", "BIGINT DEFAULT NULL COMMENT '提交学生ID'", "task_id");
        tryAddColumn("submission", "evaluation_status", "TINYINT NOT NULL DEFAULT 0 COMMENT '评价状态'", "file_sizes");
        tryAddColumn("submission", "submitted", "TINYINT NOT NULL DEFAULT 0 COMMENT '是否已提交'", "evaluation_status");
        tryAddColumn("submission", "approval_status", "TINYINT DEFAULT NULL COMMENT '审批状态: 0=未审批, 1=草稿, 2=已审批'", "submitted");
        tryAddColumn("submission", "draft_template_id", "BIGINT DEFAULT NULL COMMENT '当前草稿模板ID'", "approval_status");
        tryAddColumn("submission", "ai_score_status", "TINYINT DEFAULT 0 COMMENT 'AI评分状态:0=未启动,1=评分中,2=已完成,3=失败'", "draft_template_id");
        tryAddColumn("submission", "total_score", "DECIMAL(5,2) DEFAULT NULL COMMENT '加权总分'", "ai_score_status");
        tryAddColumn("submission", "subjective_score", "DECIMAL(5,2) DEFAULT NULL COMMENT '教师主观评分(±20)'", "total_score");
        tryAddColumn("submission", "subjective_reason", "VARCHAR(500) DEFAULT NULL COMMENT '主观评分原因'", "subjective_score");

        // --- evaluation_template 表补列 ---
        tryAddColumn("evaluation_template", "eval_method", "TINYINT NOT NULL DEFAULT 2 COMMENT '评价方式:0=纯AI,1=纯人工,2=混合'", "teacher_id");
        tryAddColumn("evaluation_template", "ai_weight", "DECIMAL(5,2) DEFAULT 0.60 COMMENT 'AI权重占比'", "eval_method");
        tryAddColumn("evaluation_template", "manual_weight", "DECIMAL(5,2) DEFAULT 0.40 COMMENT '人工权重占比'", "ai_weight");
        tryAddColumn("evaluation_template", "ai_model", "VARCHAR(100) DEFAULT NULL COMMENT '默认AI模型'", "manual_weight");
        tryAddColumn("evaluation_template", "weight_completion", "DECIMAL(5,4) DEFAULT 0.2500 COMMENT '完成度维度全局权重'", "ai_model");
        tryAddColumn("evaluation_template", "weight_tech",       "DECIMAL(5,4) DEFAULT 0.2500 COMMENT '技术质量维度全局权重'", "weight_completion");
        tryAddColumn("evaluation_template", "weight_innovation", "DECIMAL(5,4) DEFAULT 0.2500 COMMENT '创新维度全局权重'", "weight_tech");
        tryAddColumn("evaluation_template", "weight_document",   "DECIMAL(5,4) DEFAULT 0.2500 COMMENT '文档维度全局权重'", "weight_innovation");

        // --- evaluation_indicator 表补列 ---
        tryAddColumn("evaluation_indicator", "eval_type", "TINYINT NOT NULL DEFAULT 2 COMMENT '评价方式:0=AI,1=人工,2=混合'", "max_score");
        tryAddColumn("evaluation_indicator", "ai_weight", "DECIMAL(5,2) DEFAULT 0.60 COMMENT 'AI评分占比'", "eval_type");
        tryAddColumn("evaluation_indicator", "manual_weight", "DECIMAL(5,2) DEFAULT 0.40 COMMENT '人工评分占比'", "ai_weight");

        // --- evaluation_result 表补列 ---
        tryAddColumn("evaluation_result", "eval_type", "TINYINT NOT NULL DEFAULT 2 COMMENT '评价方式:0=AI,1=人工,2=混合'", "indicator_id");
        tryAddColumn("evaluation_result", "adjust_score", "DECIMAL(5,2) DEFAULT NULL COMMENT '调整分（±）'", "manual_comment");
        tryAddColumn("evaluation_result", "adjust_reason", "VARCHAR(500) DEFAULT NULL COMMENT '调整原因'", "adjust_score");

        // --- report_record 表补 deleted 列 ---
        tryAddColumn("report_record", "deleted", "INT DEFAULT 0 COMMENT '逻辑删除:0=正常,1=已删除'", "submission_count");
    }

    private void tryAddColumn(String table, String column, String definition, String afterColumn) {
        try {
            jdbcTemplate.execute(String.format(
                    "ALTER TABLE %s ADD COLUMN %s %s AFTER %s", table, column, definition, afterColumn));
            log.info("{}.{} 列已添加", table, column);
        } catch (Exception ignored) {
            // 列已存在则忽略
        }
    }
}
