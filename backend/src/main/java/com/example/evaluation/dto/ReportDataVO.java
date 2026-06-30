package com.example.evaluation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReportDataVO {

    private Long submissionId;
    private TaskInfo task;
    private StudentInfo student;
    private TemplateInfo template;
    private String date;
    private ScoreSummary summary;
    private List<ResultItem> results;
    private List<VerifItem> verifications;
    private int precheckPassCount;
    private boolean allPassed;
    private String teacherComment;
    private Double subjectiveScore;
    private String subjectiveReason;

    @Data
    public static class TaskInfo {
        private Long id;
        private String title;
        private String courseName;
        private String moduleName;
        private String subject;
        private BigDecimal weightCompletion;
        private BigDecimal weightTech;
        private BigDecimal weightInnovation;
        private BigDecimal weightDocument;
    }

    @Data
    public static class StudentInfo {
        private String name;
        private String studentNo;
        private String username;
    }

    @Data
    public static class TemplateInfo {
        private Long id;
        private String name;
        private Integer evalMethod;
        private BigDecimal weightCompletion;
        private BigDecimal weightTech;
        private BigDecimal weightInnovation;
        private BigDecimal weightDocument;
    }

    @Data
    public static class ScoreSummary {
        private Double completionScore;
        private Double techScore;
        private Double innovationScore;
        private Double documentScore;
        private Double totalScore;
        private String grade;
    }

    @Data
    public static class ResultItem {
        private String indicatorName;
        private String dimension;
        private Double weight;
        private Integer evalType;
        private Double autoScore;
        private String autoComment;
        private Double manualScore;
        private String manualComment;
        private Double adjustScore;
        private String adjustReason;
        private Double finalScore;
    }

    @Data
    public static class VerifItem {
        private String checkItem;
        private Integer checkType;
        private Integer status;
        private String detail;
    }
}
