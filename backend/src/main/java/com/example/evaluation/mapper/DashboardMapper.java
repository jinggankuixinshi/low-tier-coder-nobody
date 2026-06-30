package com.example.evaluation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Dashboard 统计查询 Mapper
 */
@Mapper
public interface DashboardMapper {

    int countStudentFavoriteTasks(@Param("userId") Long userId);

    int countStudentSubmissions(@Param("userId") Long userId);

    int countStudentEvaluated(@Param("userId") Long userId);

    int countAllTasks();

    int countAllSubmissions();

    int countVerifiedSubmissions();

    int countEvaluatedSubmissions();

    int countPublishedTasks();

    int countExpiredTasks();

    int countPendingReviewSubmissions();

    List<Map<String, Object>> selectRecentTasksForStudent(@Param("userId") Long userId);

    List<Map<String, Object>> selectRecentTasksForTeacher();

    List<Map<String, Object>> selectRecentSubmissionsForStudent(@Param("userId") Long userId);

    List<Map<String, Object>> selectRecentSubmissionsForTeacher();
}
