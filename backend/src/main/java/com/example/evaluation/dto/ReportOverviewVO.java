package com.example.evaluation.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ReportOverviewVO {

    private ReportDataVO.TaskInfo task;
    private String date;
    private int totalSubmissions;
    private int approvedCount;
    private List<StudentStat> studentStats;
    private Map<String, List<Double>> dimScores;
    private List<Double> allScores;
    private GradeCounts gradeCounts;
    private Double avgScore;
    private Double maxScore;
    private Double minScore;

    @Data
    public static class StudentStat {
        private String studentName;
        private Double totalScore;
        private String grade;
        private String submitTime;
    }

    @Data
    public static class GradeCounts {
        private int excellent;
        private int good;
        private int medium;
        private int fail;
    }
}
