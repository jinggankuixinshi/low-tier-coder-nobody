package com.example.evaluation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskExpirationScheduler {

    private final JdbcTemplate jdbcTemplate;

    public TaskExpirationScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedRate = 60000)
    public void autoExpireTasks() {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE training_task SET status = 2 WHERE status = 1 AND deadline IS NOT NULL AND deadline < NOW()");
            if (updated > 0) {
                log.info("自动截止 {} 个已过期的实训任务", updated);
            }
        } catch (Exception e) {
            log.warn("自动截止任务检查失败: {}", e.getMessage());
        }
    }
}
