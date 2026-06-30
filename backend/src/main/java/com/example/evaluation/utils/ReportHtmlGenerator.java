package com.example.evaluation.utils;

import com.example.evaluation.dto.EvaluationResultVO;
import com.example.evaluation.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class ReportHtmlGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String CSS = ""
        + "@page{size:A4;margin:1.4cm 1.2cm}\n"
        + "*{margin:0;padding:0}\n"
        + "body{font-family:'Noto Sans CJK SC',sans-serif;color:#333;background:#fff;font-size:13px;line-height:1.6}\n"
        + ".chart-box{text-align:center;margin:6px 0 10px}\n"
        + ".chart-box img{display:inline-block}\n"
        + ".page{width:100%}\n"
        + ".header{text-align:center;padding-bottom:18px;border-bottom:3px solid #1a6fb5;margin-bottom:24px}\n"
        + ".header h1{font-size:22px;color:#1a6fb5;margin-bottom:12px;font-weight:bold;letter-spacing:2px}\n"
        + ".header .meta{text-align:center;font-size:13px;color:#555}\n"
        + ".header .meta span{margin-left:12px;margin-right:12px}\n"
        + ".header .meta b{color:#333;font-weight:bold}\n"
        + ".section{margin-bottom:22px;page-break-inside:avoid}\n"
        + ".section-flow{margin-bottom:22px}\n"
        + ".section-title{font-size:15px;color:#1a6fb5;border-left:4px solid #1a6fb5;padding-left:10px;margin-bottom:12px;font-weight:bold}\n"
        + ".overview-table{width:100%;border-collapse:collapse;background-color:#f5f9fc;border:1px solid #e3edf6}\n"
        + ".overview-td{vertical-align:middle;padding:14px}\n"
        + ".overview-gauge-td{width:220px;text-align:center;vertical-align:middle;padding:14px}\n"
        + ".overview-total-td{width:150px;text-align:center;vertical-align:middle;padding:14px}\n"
        + ".total-score{font-size:34px;font-weight:700;color:#1a6fb5;line-height:1.2}\n"
        + ".grade-tag-lg{display:inline-block;font-weight:bold;font-size:15px;color:#fff;padding:4px 18px;margin-top:8px}\n"
        + ".sc-pass{color:#67c23a;font-weight:bold}.sc-fail{color:#f56c6c;font-weight:bold}.sc-mid{color:#409eff;font-weight:bold}\n"
        + ".data-table{width:100%;border-collapse:collapse;-fs-table-paginate:paginate}\n"
        + ".data-table th,.data-table td{padding:7px 9px;text-align:center;font-size:12.5px;border:1px solid #d0dbe8}\n"
        + ".data-table thead{display:table-header-group}\n"
        + ".data-table th{background-color:#e8f0f8;color:#1a6fb5;font-weight:bold}\n"
        + ".data-table tr{page-break-inside:avoid}\n"
        + ".row-even{background-color:#f6fafd}\n"
        + ".dim-total{font-size:16px;font-weight:bold;color:#1a6fb5}\n"
        + ".dim-header{font-size:13px;font-weight:bold;color:#1a6fb5;margin-bottom:8px;padding:6px 12px;background-color:#f0f5fb;border-left:3px solid #1a6fb5}\n"
        + ".grade-badge{font-weight:bold;font-size:12px;color:#fff;padding:2px 12px}\n"
        + ".grade-yx{background-color:#67c23a}.grade-lh{background-color:#409eff}\n"
        + ".grade-zd{background-color:#e6a23c}.grade-bj{background-color:#f56c6c}\n"
        + ".comment-box{padding:18px 22px;background-color:#fdf8f0;border-left:4px solid #e6a23c;font-size:13px;line-height:1.8;color:#555}\n"
        + ".subjective-box{padding:16px 22px;background-color:#eef5fc;border-left:4px solid #1a6fb5;font-size:13px;line-height:1.8;color:#555}\n"
        + ".subjective-label{font-weight:bold;color:#333}\n"
        + ".subjective-val{font-size:18px;font-weight:bold}\n"
        + ".subjective-reason{margin-top:6px;color:#666}\n"
        + ".issue-group{margin-bottom:14px}\n"
        + ".issue-group h4{font-size:13px;color:#1a6fb5;margin-bottom:6px;font-weight:bold}\n"
        + ".issue-item{padding:4px 0 4px 16px;font-size:12.5px;color:#666;border-left:2px solid #e0e8f0;margin-bottom:4px}\n"
        + ".footer{text-align:center;color:#999;font-size:12px;padding-top:18px;border-top:1px solid #e0e8f0;margin-top:28px}\n"
        + ".stat-table{width:100%;border-collapse:collapse;margin-bottom:8px}\n"
        + ".stat-td{text-align:center;padding:14px 8px;background-color:#f5f9fc;border:1px solid #e0e8f0}\n"
        + ".stat-val{font-size:26px;font-weight:bold;color:#1a6fb5}\n"
        + ".stat-label{font-size:12px;color:#909399;margin-top:4px}\n"
        + ".score-cell{font-weight:bold}\n";

    private final ChartSvgGenerator chartSvgGenerator;

    public ReportHtmlGenerator(ChartSvgGenerator chartSvgGenerator) {
        this.chartSvgGenerator = chartSvgGenerator;
    }

    private void writeChart(StringBuilder sb, String dataUri, String alt, int width) {
        if (dataUri == null) return;
        sb.append("<div class=\"chart-box\"><img alt=\"").append(alt)
          .append("\" style=\"width:").append(width).append("px;max-width:").append(width).append("px\" src=\"")
          .append(dataUri).append("\"/></div>\n");
    }

    // ==================== public API ====================

    public String buildSingleReportHtml(TrainingTask task, String studentName, String username, Submission submission,
                                         List<EvaluationResultVO> results,
                                         String teacherComment,
                                         ScoreSummary summary,
                                         com.example.evaluation.entity.EvaluationTemplate template) {
        StringBuilder sb = new StringBuilder(8192);
        writeDocStart(sb);
        sb.append("<div class=\"page\">\n");

        writeSingleHeader(sb, task, studentName, username, template);
        writeScoreOverview(sb, summary, template);
        writeDimensionTables(sb, results, template);
        writeSubjectiveScore(sb, submission);
        writeTeacherComment(sb, teacherComment);
        writeIssues(sb, results);

        sb.append("<div class=\"footer\">由实训成果智能评价系统自动生成</div>\n");
        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    public String buildOverviewHtml(TrainingTask task,
                                     List<Map<String, Object>> studentStats,
                                     List<Double> allScores,
                                     Map<String, List<Double>> dimScores) {
        StringBuilder sb = new StringBuilder(8192);
        writeDocStart(sb);
        sb.append("<div class=\"page\">\n");

        // Header
        sb.append("<div class=\"header\">\n");
        sb.append("<h1>").append(esc(task.getTitle())).append(" 任务总览报表</h1>\n");
        sb.append("<div class=\"meta\">\n");
        sb.append("<span><b>课程：</b>").append(esc(task.getCourseName())).append("</span>\n");
        sb.append("<span><b>日期：</b>").append(LocalDate.now().format(DATE_FMT)).append("</span>\n");
        sb.append("<span><b>提交数：</b>").append(studentStats.size()).append("</span>\n");
        sb.append("</div></div>\n");

        if (studentStats.isEmpty()) {
            sb.append("<p style=\"text-align:center;padding:40px;color:#999\">暂无数据</p>\n");
            sb.append("</div>\n</body>\n</html>");
            return sb.toString();
        }

        // Stats
        double total = 0, max = 0, min = 100;
        for (double s : allScores) { total += s; if (s > max) max = s; if (s < min) min = s; }
        double avg = allScores.isEmpty() ? 0 : total / allScores.size();

        sb.append("<div class=\"section\"><div class=\"section-title\">一、基本统计</div>\n");
        sb.append("<table class=\"stat-table\"><tr>\n");
        writeStatCell(sb, "提交团队数", String.valueOf(studentStats.size()));
        writeStatCell(sb, "平均分", String.format("%.1f", avg));
        writeStatCell(sb, "最高分", String.format("%.1f", max));
        writeStatCell(sb, "最低分", String.format("%.1f", min));
        sb.append("</tr></table></div>\n");

        // Section 2: Grade distribution
        writeGradeDistribution(sb, studentStats);

        // Section 3: Dimension averages
        writeDimAvgBars(sb, dimScores);

        // 四、学生成绩明细（与预览页一致：学生/总分/等级/提交时间，按传入顺序展示）
        // 该表可能较长：用 section-flow 允许跨页，并由 thead 重复表头 + 行内不割裂
        sb.append("<div class=\"section-flow\"><div class=\"section-title\">四、学生成绩明细</div>\n");
        sb.append("<table class=\"data-table\">");
        sb.append("<colgroup><col/><col style=\"width:90px\"/><col style=\"width:80px\"/><col style=\"width:160px\"/></colgroup>");
        sb.append("<thead><tr><th>学生</th><th>总分</th><th>等级</th><th>提交时间</th></tr></thead><tbody>\n");
        int sri = 0;
        for (Map<String, Object> s : studentStats) {
            sb.append(sri++ % 2 == 1 ? "<tr class=\"row-even\">" : "<tr>");
            sb.append("<td style=\"text-align:left;padding-left:12px\">").append(esc(String.valueOf(s.getOrDefault("studentName", "")))).append("</td>");
            sb.append("<td class=\"score-cell\">").append(fmt(s.get("totalScore"))).append("</td>");
            String grade = String.valueOf(s.getOrDefault("grade", ""));
            sb.append("<td style=\"font-weight:bold;color:").append(gradeTextColor(grade)).append("\">").append(esc(grade)).append("</td>");
            sb.append("<td>").append(fmt(s.get("submitTime"))).append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</tbody></table></div>\n");

        sb.append("<div class=\"footer\">由实训成果智能评价系统自动生成</div>\n");
        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    // ==================== private helpers ====================

    private void writeDocStart(StringBuilder sb) {
        sb.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"/>");
        sb.append("<title>软件实训成果评价报告</title>");
        sb.append("<style>\n").append(CSS).append("</style>");
        sb.append("</head><body>\n");
    }

    private void writeSingleHeader(StringBuilder sb, TrainingTask task, String studentName, String username,
                                    com.example.evaluation.entity.EvaluationTemplate template) {
        sb.append("<div class=\"header\">\n");
        sb.append("<h1>软件实训成果评价报告</h1>\n");
        sb.append("<div class=\"meta\">\n");
        sb.append("<span><b>姓名：</b>").append(esc(studentName)).append("</span>\n");
        if (username != null && !username.isEmpty())
            sb.append("<span><b>账号：</b>").append(esc(username)).append("</span>\n");
        sb.append("<span><b>任务：</b>").append(esc(task.getTitle())).append("</span>\n");
        if (task.getCourseName() != null && !task.getCourseName().isEmpty())
            sb.append("<span><b>课程：</b>").append(esc(task.getCourseName())).append("</span>\n");
        sb.append("<span><b>日期：</b>").append(LocalDate.now().format(DATE_FMT)).append("</span>\n");
        sb.append("<span><b>方式：</b>").append(evalMethodLabel(template)).append("</span>\n");
        sb.append("</div></div>\n");
    }

    private void writeScoreOverview(StringBuilder sb, ScoreSummary summary,
                                     com.example.evaluation.entity.EvaluationTemplate template) {
        sb.append("<div class=\"section\"><div class=\"section-title\">一、评分总览</div>\n");
        sb.append("<table class=\"overview-table\"><tr>\n");

        // 得分表
        sb.append("<td class=\"overview-td\">\n");
        sb.append("<table class=\"data-table\">");
        sb.append("<colgroup><col/><col style=\"width:90px\"/><col style=\"width:70px\"/><col style=\"width:90px\"/></colgroup>");
        sb.append("<thead><tr>");
        sb.append("<th>评价维度</th><th>维度得分</th><th>权重</th><th>加权得分</th></tr></thead><tbody>\n");
        BigDecimal wComp = template != null && template.getWeightCompletion() != null ? template.getWeightCompletion() : BigDecimal.ZERO;
        BigDecimal wTech = template != null && template.getWeightTech() != null ? template.getWeightTech() : BigDecimal.ZERO;
        BigDecimal wInno = template != null && template.getWeightInnovation() != null ? template.getWeightInnovation() : BigDecimal.ZERO;
        BigDecimal wDoc  = template != null && template.getWeightDocument() != null ? template.getWeightDocument() : BigDecimal.ZERO;
        writeDimRow(sb, "完成度", summary.completionScore, wComp, 0);
        writeDimRow(sb, "技术质量", summary.techScore, wTech, 1);
        writeDimRow(sb, "创新", summary.innovationScore, wInno, 2);
        writeDimRow(sb, "文档", summary.documentScore, wDoc, 3);
        sb.append("</tbody></table>\n");
        sb.append("</td>\n");

        // 总分框
        sb.append("<td class=\"overview-total-td\">\n");
        sb.append("<div class=\"total-score\">").append(fmt(summary.totalScore)).append("</div>\n");
        sb.append("<span class=\"grade-tag-lg ").append(gradeClass(summary.grade)).append("\">")
          .append(esc(summary.grade)).append("</span>\n");
        sb.append("</td>\n");

        sb.append("</tr></table></div>\n");
    }

    private void writeDimRow(StringBuilder sb, String name, Double score, BigDecimal weight, int idx) {
        double w = weight != null ? weight.doubleValue() : 0;
        double s = score != null ? score : 0;
        sb.append(idx % 2 == 1 ? "<tr class=\"row-even\">" : "<tr>");
        sb.append("<td style=\"text-align:left;padding-left:12px;font-weight:600\">").append(name).append("</td>");
        sb.append("<td>").append(fmt(score)).append("</td>");
        sb.append("<td>").append(String.format("%.2f", w)).append("</td>");
        sb.append("<td>").append(String.format("%.2f", s * w)).append("</td></tr>\n");
    }

    private void writeDimensionTables(StringBuilder sb, List<EvaluationResultVO> results,
                                       com.example.evaluation.entity.EvaluationTemplate template) {
        String[] dims = {"completion", "tech", "innovation", "document"};
        String[] dimNames = {"二、完成度评价", "三、技术质量评价", "四、创新评价", "五、文档评价"};
        Map<String, BigDecimal> dimWeights = Map.of(
            "completion", template != null && template.getWeightCompletion() != null ? template.getWeightCompletion() : BigDecimal.ZERO,
            "tech", template != null && template.getWeightTech() != null ? template.getWeightTech() : BigDecimal.ZERO,
            "innovation", template != null && template.getWeightInnovation() != null ? template.getWeightInnovation() : BigDecimal.ZERO,
            "document", template != null && template.getWeightDocument() != null ? template.getWeightDocument() : BigDecimal.ZERO
        );

        for (int i = 0; i < dims.length; i++) {
            final String dim = dims[i];
            List<EvaluationResultVO> items = results.stream()
                    .filter(r -> dim.equals(r.getDimension()))
                    .toList();
            if (items.isEmpty()) continue;

            sb.append("<div class=\"section\"><div class=\"section-title\">").append(dimNames[i]).append("</div>\n");
            sb.append("<div class=\"dim-header\">权重合计 ")
              .append(String.format("%.0f", dimWeights.get(dim).doubleValue() * 100)).append("%</div>\n");
            sb.append("<table class=\"data-table\">");
            sb.append("<colgroup><col/><col style=\"width:64px\"/><col style=\"width:74px\"/><col style=\"width:86px\"/><col style=\"width:86px\"/></colgroup>");
            sb.append("<thead><tr>");
            sb.append("<th>指标</th><th>权重</th><th>AI评分</th><th>教师评分</th><th>最终得分</th></tr></thead><tbody>\n");

            int ri = 0;
            for (EvaluationResultVO x : items) {
                sb.append(ri++ % 2 == 1 ? "<tr class=\"row-even\">" : "<tr>");
                sb.append("<td style=\"text-align:left;padding-left:12px\">").append(esc(x.getIndicatorName())).append("</td>");
                sb.append("<td>").append(indicatorWeightPercent(x.getWeight())).append("%</td>");
                sb.append(scoreCell(x.getAutoScore(), "sc-pass", false));
                sb.append(scoreCell(x.getManualScore(), "sc-mid", false));
                sb.append(scoreCell(x.getFinalScore(), "sc-mid", true));
                sb.append("</tr>\n");
            }
            sb.append("</tbody></table></div>\n");
        }
    }

    // 六、教师主观评分（在加权分基础上增减 ±20）
    private void writeSubjectiveScore(StringBuilder sb, Submission submission) {
        if (submission == null) return;
        BigDecimal sc = submission.getSubjectiveScore();
        String reason = submission.getSubjectiveReason();
        boolean hasScore = sc != null && sc.compareTo(BigDecimal.ZERO) != 0;
        boolean hasReason = reason != null && !reason.isBlank();
        if (!hasScore && !hasReason) return;

        String valText = sc == null ? "0" : (sc.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + fmt(sc);
        String valColor = sc != null && sc.compareTo(BigDecimal.ZERO) < 0 ? "#f56c6c" : "#1a6fb5";
        sb.append("<div class=\"section\"><div class=\"section-title\">六、教师主观评分</div>\n");
        sb.append("<div class=\"subjective-box\">");
        sb.append("<span class=\"subjective-label\">主观评分：</span>");
        sb.append("<span class=\"subjective-val\" style=\"color:").append(valColor).append("\">")
          .append(valText).append(" 分</span>");
        if (hasReason) {
            sb.append("<div class=\"subjective-reason\"><b>原因：</b>")
              .append(esc(reason).replace("\n", "<br/>")).append("</div>");
        }
        sb.append("</div></div>\n");
    }

    private void writeTeacherComment(StringBuilder sb, String comment) {
        if (comment == null || comment.isBlank()) return;
        sb.append("<div class=\"section\"><div class=\"section-title\">七、教师综合评价</div>\n");
        sb.append("<div class=\"comment-box\">")
          .append(esc(comment).replace("\n", "<br/>"))
          .append("</div></div>\n");
    }

    private void writeIssues(StringBuilder sb, List<EvaluationResultVO> results) {
        Map<String, List<String>> issues = new LinkedHashMap<>();
        for (EvaluationResultVO r : results) {
            String dim = r.getDimension();
            if (dim == null || "precheck".equals(dim)) continue;
            List<String> parts = new ArrayList<>();
            if (r.getAutoComment() != null && !r.getAutoComment().isBlank()) parts.add(r.getAutoComment());
            if (r.getManualComment() != null && !r.getManualComment().isBlank()) parts.add(r.getManualComment());
            if (r.getAdjustReason() != null && !r.getAdjustReason().isBlank()) parts.add(r.getAdjustReason());
            if (!parts.isEmpty()) issues.computeIfAbsent(dim, k -> new ArrayList<>()).addAll(parts);
        }
        if (issues.isEmpty()) return;

        Map<String, String> dimLabel = Map.of(
            "completion", "完成度", "tech", "技术质量",
            "innovation", "创新", "document", "文档"
        );

        sb.append("<div class=\"section\"><div class=\"section-title\">八、存在问题与改进建议</div>\n");
        for (Map.Entry<String, List<String>> e : issues.entrySet()) {
            sb.append("<div class=\"issue-group\"><h4>").append(dimLabel.getOrDefault(e.getKey(), e.getKey())).append("</h4>\n");
            for (String part : e.getValue()) {
                sb.append("<div class=\"issue-item\">").append(esc(part)).append("</div>\n");
            }
            sb.append("</div>\n");
        }
        sb.append("</div>\n");
    }

    private void writeStatCell(StringBuilder sb, String label, String value) {
        sb.append("<td class=\"stat-td\"><div class=\"stat-val\">").append(value)
          .append("</div><div class=\"stat-label\">").append(label).append("</div></td>\n");
    }

    private void writeGradeDistribution(StringBuilder sb, List<Map<String, Object>> studentStats) {
        Map<String, Integer> gc = new LinkedHashMap<>();
        gc.put("优秀", 0); gc.put("良好", 0); gc.put("中等", 0); gc.put("不及格", 0);
        for (Map<String, Object> s : studentStats) {
            String g = String.valueOf(s.getOrDefault("grade", ""));
            if (gc.containsKey(g)) gc.put(g, gc.get(g) + 1);
        }
        sb.append("<div class=\"section\"><div class=\"section-title\">二、成绩分布</div>\n");
        writeChart(sb, chartSvgGenerator.pieDataUri(gc.get("优秀"), gc.get("良好"), gc.get("中等"), gc.get("不及格")),
                "成绩分布饼图", 360);
        sb.append("</div>\n");
    }

    private void writeDimAvgBars(StringBuilder sb, Map<String, List<Double>> dimScores) {
        if (dimScores == null || dimScores.isEmpty()) return;
        sb.append("<div class=\"section\"><div class=\"section-title\">三、各维度平均得分</div>\n");
        String[] dims = {"completion", "tech", "innovation", "document"};
        String[] labels = {"完成度", "技术质量", "创新", "文档"};
        double[] avgs = new double[4];
        for (int i = 0; i < dims.length; i++) {
            List<Double> vals = dimScores.getOrDefault(dims[i], Collections.emptyList());
            avgs[i] = vals.isEmpty() ? 0 : vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }
        writeChart(sb, chartSvgGenerator.barDataUri(avgs[0], avgs[1], avgs[2], avgs[3]), "各维度平均得分柱状图", 460);
        sb.append("</div>\n");
    }

    // ==================== formatting ====================

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String fmt(Object v) {
        if (v == null) return "-";
        if (v instanceof Number n) {
            if (n.doubleValue() == n.longValue()) return String.valueOf(n.longValue());
            return String.format("%.2f", n.doubleValue());
        }
        return v.toString();
    }

    private static String weightToPercent(BigDecimal w) {
        if (w == null) return "0";
        return String.format("%.0f", w.doubleValue() * 100);
    }

    // 指标占比：已是 0-100 刻度，直接取整显示（与预览页 row.weight.toFixed(0) 一致）
    private static String indicatorWeightPercent(BigDecimal w) {
        if (w == null) return "0";
        return String.format("%.0f", w.doubleValue());
    }

    // 颜色标记的分数单元格：≥60 用 passClass，<60 用红色 sc-fail；bold 时加粗
    private static String scoreCell(Object score, String passClass, boolean bold) {
        if (score == null || (score instanceof Number n && Double.isNaN(n.doubleValue()))) {
            return "<td>-</td>";
        }
        double v = (score instanceof Number n) ? n.doubleValue() : 0;
        String cls = v >= 60 ? passClass : "sc-fail";
        String text = fmt(score);
        if (bold) text = "<b>" + text + "</b>";
        return "<td class=\"" + cls + "\">" + text + "</td>";
    }

    private static String gradeClass(String g) {
        if (g == null) return "grade-bj";
        return switch (g) {
            case "优秀" -> "grade-yx";
            case "良好" -> "grade-lh";
            case "中等" -> "grade-zd";
            default -> "grade-bj";
        };
    }

    // 等级纯文字颜色（用于总览表格，避免彩色背景徽标在窄列中溢出）
    private static String gradeTextColor(String g) {
        if (g == null) return "#f56c6c";
        return switch (g) {
            case "优秀" -> "#67c23a";
            case "良好" -> "#409eff";
            case "中等" -> "#e6a23c";
            default -> "#f56c6c";
        };
    }

    private static String evalMethodLabel(com.example.evaluation.entity.EvaluationTemplate tpl) {
        if (tpl == null) return "混合评价";
        return switch (tpl.getEvalMethod()) {
            case 0 -> "纯AI评价";
            case 1 -> "纯人工评价";
            case 2 -> "混合AI+人工评价";
            case 3 -> "逐项自定义评价";
            default -> "混合评价";
        };
    }

    private static double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return 0;
    }

    // ==================== ScoreSummary DTO ====================

    public static class ScoreSummary {
        private final Double completionScore;
        private final Double techScore;
        private final Double innovationScore;
        private final Double documentScore;
        private final Double totalScore;
        private final String grade;

        public ScoreSummary(Double completionScore, Double techScore, Double innovationScore,
                           Double documentScore, Double totalScore, String grade) {
            this.completionScore = completionScore;
            this.techScore = techScore;
            this.innovationScore = innovationScore;
            this.documentScore = documentScore;
            this.totalScore = totalScore;
            this.grade = grade;
        }

        public Double getCompletionScore() { return completionScore; }
        public Double getTechScore() { return techScore; }
        public Double getInnovationScore() { return innovationScore; }
        public Double getDocumentScore() { return documentScore; }
        public Double getTotalScore() { return totalScore; }
        public String getGrade() { return grade; }
    }
}
