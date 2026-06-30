package com.example.evaluation.controller;

import com.example.evaluation.common.Result;
import com.example.evaluation.mapper.DashboardMapper;
import com.example.evaluation.security.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardMapper dashboardMapper;

    public DashboardController(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    @GetMapping("/stats")
    public Result<?> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            boolean isStudent = SecurityUtil.isStudent();
            Long userId = SecurityUtil.getCurrentUserId();

            if (isStudent) {
                data.put("taskCount", dashboardMapper.countStudentFavoriteTasks(userId));
                data.put("submissionCount", dashboardMapper.countStudentSubmissions(userId));
                data.put("evaluatedCount", dashboardMapper.countStudentEvaluated(userId));
            } else {
                data.put("taskCount", dashboardMapper.countAllTasks());
                data.put("submissionCount", dashboardMapper.countAllSubmissions());
                data.put("verifiedCount", dashboardMapper.countVerifiedSubmissions());
                data.put("evaluatedCount", dashboardMapper.countEvaluatedSubmissions());
            }
        } catch (Exception e) {
            data.put("taskCount", 0);
            data.put("submissionCount", 0);
            data.put("evaluatedCount", 0);
        }
        return Result.success(data);
    }

    @GetMapping("/status-counts")
    public Result<?> statusCounts() {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            data.put("published", dashboardMapper.countPublishedTasks());
            data.put("completed", dashboardMapper.countExpiredTasks());
            data.put("pendingReview", dashboardMapper.countPendingReviewSubmissions());
            data.put("completedReview", dashboardMapper.countEvaluatedSubmissions());
        } catch (Exception e) {
            data.put("published", 0);
            data.put("completed", 0);
            data.put("pendingReview", 0);
            data.put("completedReview", 0);
        }
        return Result.success(data);
    }

    @GetMapping("/recent-tasks")
    public Result<?> recentTasks() {
        List<Map<String, Object>> list;
        try {
            if (SecurityUtil.isStudent()) {
                list = dashboardMapper.selectRecentTasksForStudent(SecurityUtil.getCurrentUserId());
            } else {
                list = dashboardMapper.selectRecentTasksForTeacher();
            }
        } catch (Exception e) {
            list = List.of();
        }
        return Result.success(list);
    }

    @GetMapping("/recent-submissions")
    public Result<?> recentSubmissions() {
        List<Map<String, Object>> list;
        try {
            if (SecurityUtil.isStudent()) {
                list = dashboardMapper.selectRecentSubmissionsForStudent(SecurityUtil.getCurrentUserId());
            } else {
                list = dashboardMapper.selectRecentSubmissionsForTeacher();
            }
        } catch (Exception e) {
            list = List.of();
        }
        return Result.success(list);
    }
}
