package com.example.evaluation.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Base64;
import java.util.Locale;

/**
 * 纯 Java 图表生成器：手绘 SVG -> Batik 光栅化为 PNG -> base64 data-uri。
 * 不依赖 Node.js，JVM 内完成，兼容 LoongArch/麒麟。
 * 中文标签统一使用内置 Noto Sans CJK SC（已由 FontConfig 注册到 AWT）。
 */
@Slf4j
@Component
public class ChartSvgGenerator {

    private static final String FONT = "Noto Sans CJK SC, sans-serif";
    private static final double RAD = Math.PI / 180.0;

    /** 个人报表：评分仪表盘 */
    public String gaugeDataUri(Double score, String grade) {
        try {
            double v = clamp(score);
            int w = 280, h = 220;
            double cx = w / 2.0, cy = 150, r = 92, band = 16;
            StringBuilder sb = svgStart(w, h);

            // 彩色刻度弧（210° -> -30°，开口朝下），分段着色
            double start = 210, end = -30, span = start - end; // 240
            int steps = 120;
            for (int i = 0; i < steps; i++) {
                double t0 = (double) i / steps, t1 = (double) (i + 1) / steps;
                double a0 = start - span * t0, a1 = start - span * t1;
                String color = zoneColor((t0 + t1) / 2);
                sb.append(line(cx + r * Math.cos(a0 * RAD), cy - r * Math.sin(a0 * RAD),
                        cx + r * Math.cos(a1 * RAD), cy - r * Math.sin(a1 * RAD), color, band, "round"));
            }
            // 指针
            double va = start - span * (v / 100.0);
            double px = cx + (r - band) * Math.cos(va * RAD);
            double py = cy - (r - band) * Math.sin(va * RAD);
            sb.append(line(cx, cy, px, py, "#555", 4, "round"));
            sb.append(String.format(Locale.ROOT,
                    "<circle cx='%.1f' cy='%.1f' r='6' fill='#555'/>", cx, cy));
            // 中心分值 + 等级
            sb.append(text(cx, cy + 46, fmt(v), 34, gradeColor(grade), "middle", true));
            sb.append(text(cx, cy + 70, grade == null ? "" : grade, 15, "#666", "middle", false));
            sb.append("</svg>");
            return rasterize(sb.toString(), w, h);
        } catch (Exception e) {
            log.warn("生成仪表盘图失败: {}", e.getMessage());
            return null;
        }
    }

    /** 个人报表：维度雷达图 */
    public String radarDataUri(Double completion, Double tech, Double innovation, Double document) {
        try {
            int w = 360, h = 320;
            double cx = w / 2.0, cy = 160, r = 110;
            // 轴顺序：上(完成度) 右(技术质量) 下(创新) 左(文档)
            double[] angles = {90, 0, -90, 180};
            String[] labels = {"完成度", "技术质量", "创新", "文档"};
            double[] vals = {clamp(completion), clamp(tech), clamp(innovation), clamp(document)};
            StringBuilder sb = svgStart(w, h);

            // 网格环
            for (int ring = 1; ring <= 4; ring++) {
                double rr = r * ring / 4.0;
                StringBuilder pts = new StringBuilder();
                for (double a : angles) {
                    pts.append(String.format(Locale.ROOT, "%.1f,%.1f ",
                            cx + rr * Math.cos(a * RAD), cy - rr * Math.sin(a * RAD)));
                }
                sb.append("<polygon points='").append(pts.toString().trim())
                        .append("' fill='none' stroke='#dcdfe6' stroke-width='1'/>");
            }
            // 轴线 + 标签
            for (int i = 0; i < 4; i++) {
                double a = angles[i];
                double ex = cx + r * Math.cos(a * RAD), ey = cy - r * Math.sin(a * RAD);
                sb.append(line(cx, cy, ex, ey, "#dcdfe6", 1, "butt"));
                double lx = cx + (r + 22) * Math.cos(a * RAD), ly = cy - (r + 22) * Math.sin(a * RAD);
                String anchor = a == 0 ? "start" : a == 180 ? "end" : "middle";
                sb.append(text(lx, ly + 4, labels[i], 13, "#333", anchor, false));
            }
            // 数据多边形
            StringBuilder dpts = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                double rr = r * vals[i] / 100.0;
                dpts.append(String.format(Locale.ROOT, "%.1f,%.1f ",
                        cx + rr * Math.cos(angles[i] * RAD), cy - rr * Math.sin(angles[i] * RAD)));
            }
            sb.append("<polygon points='").append(dpts.toString().trim())
                    .append("' fill='#1a6fb5' fill-opacity='0.25' stroke='#1a6fb5' stroke-width='2'/>");
            for (int i = 0; i < 4; i++) {
                double rr = r * vals[i] / 100.0;
                sb.append(String.format(Locale.ROOT, "<circle cx='%.1f' cy='%.1f' r='3.5' fill='#1a6fb5'/>",
                        cx + rr * Math.cos(angles[i] * RAD), cy - rr * Math.sin(angles[i] * RAD)));
            }
            sb.append("</svg>");
            return rasterize(sb.toString(), w, h);
        } catch (Exception e) {
            log.warn("生成雷达图失败: {}", e.getMessage());
            return null;
        }
    }

    /** 总览报表：成绩分布饼图（环形） */
    public String pieDataUri(int excellent, int good, int medium, int fail) {
        try {
            int total = excellent + good + medium + fail;
            int w = 360, h = 240;
            double cx = 120, cy = 120, r = 90, inner = 45;
            StringBuilder sb = svgStart(w, h);
            if (total <= 0) {
                sb.append(text(w / 2.0, h / 2.0, "暂无数据", 14, "#999", "middle", false));
                sb.append("</svg>");
                return rasterize(sb.toString(), w, h);
            }
            int[] vals = {excellent, good, medium, fail};
            String[] colors = {"#67c23a", "#409eff", "#e6a23c", "#f56c6c"};
            String[] names = {"优秀", "良好", "中等", "不及格"};

            // 统计非零等级数：仅一个等级占满 100% 时，360° 扇形退化为零长弧(起止点重合)，
            // SVG 不渲染导致饼图空白；此时改用整圆 <circle> 绘制。
            int nonZero = 0, onlyIdx = -1;
            for (int i = 0; i < 4; i++) {
                if (vals[i] > 0) { nonZero++; onlyIdx = i; }
            }
            if (nonZero == 1) {
                sb.append(String.format(Locale.ROOT,
                        "<circle cx='%.1f' cy='%.1f' r='%.1f' fill='%s'/>", cx, cy, r, colors[onlyIdx]));
            } else {
                double a0 = -90; // 从正上方顺时针
                for (int i = 0; i < 4; i++) {
                    if (vals[i] <= 0) continue;
                    double sweep = 360.0 * vals[i] / total;
                    double a1 = a0 + sweep;
                    sb.append(wedge(cx, cy, r, a0, a1, colors[i]));
                    a0 = a1;
                }
            }
            // 环形中心挖空
            sb.append(String.format(Locale.ROOT,
                    "<circle cx='%.1f' cy='%.1f' r='%.1f' fill='#ffffff'/>", cx, cy, inner));
            sb.append(text(cx, cy + 5, total + "人", 16, "#333", "middle", true));

            // 图例
            double ly = 50;
            for (int i = 0; i < 4; i++) {
                double y = ly + i * 28;
                sb.append(String.format(Locale.ROOT,
                        "<rect x='240' y='%.1f' width='14' height='14' rx='2' fill='%s'/>", y - 11, colors[i]));
                sb.append(text(262, y, names[i] + "  " + vals[i] + "人", 13, "#555", "start", false));
            }
            sb.append("</svg>");
            return rasterize(sb.toString(), w, h);
        } catch (Exception e) {
            log.warn("生成饼图失败: {}", e.getMessage());
            return null;
        }
    }

    /** 总览报表：各维度平均得分柱状图（横向） */
    public String barDataUri(double completion, double tech, double innovation, double document) {
        try {
            int w = 460, h = 220;
            double left = 80, barMaxW = 300, top = 24, barH = 24, gap = 22;
            String[] labels = {"完成度", "技术质量", "创新", "文档"};
            double[] vals = {clamp(completion), clamp(tech), clamp(innovation), clamp(document)};
            String[] colors = {"#1a6fb5", "#409eff", "#67c23a", "#e6a23c"};
            StringBuilder sb = svgStart(w, h);
            for (int i = 0; i < 4; i++) {
                double y = top + i * (barH + gap);
                sb.append(text(left - 10, y + barH / 2 + 4, labels[i], 13, "#333", "end", false));
                sb.append(String.format(Locale.ROOT,
                        "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' rx='3' fill='#eef2f7'/>",
                        left, y, barMaxW, barH));
                double fw = barMaxW * vals[i] / 100.0;
                sb.append(String.format(Locale.ROOT,
                        "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' rx='3' fill='%s'/>",
                        left, y, fw, barH, colors[i]));
                sb.append(text(left + fw + 8, y + barH / 2 + 4, fmt(vals[i]), 13, "#333", "start", true));
            }
            sb.append("</svg>");
            return rasterize(sb.toString(), w, h);
        } catch (Exception e) {
            log.warn("生成柱状图失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== helpers ====================

    private StringBuilder svgStart(int w, int h) {
        return new StringBuilder(2048).append(String.format(Locale.ROOT,
                "<svg xmlns='http://www.w3.org/2000/svg' width='%d' height='%d' viewBox='0 0 %d %d'>"
                        + "<rect width='%d' height='%d' fill='#ffffff'/>", w, h, w, h, w, h));
    }

    private String line(double x1, double y1, double x2, double y2, String color, double width, String cap) {
        return String.format(Locale.ROOT,
                "<line x1='%.2f' y1='%.2f' x2='%.2f' y2='%.2f' stroke='%s' stroke-width='%.1f' stroke-linecap='%s'/>",
                x1, y1, x2, y2, color, width, cap);
    }

    private String text(double x, double y, String content, int size, String color, String anchor, boolean bold) {
        return String.format(Locale.ROOT,
                "<text x='%.1f' y='%.1f' font-family='%s' font-size='%d' fill='%s' text-anchor='%s'%s>%s</text>",
                x, y, FONT, size, color, anchor, bold ? " font-weight='bold'" : "", esc(content));
    }

    private String wedge(double cx, double cy, double r, double a0, double a1, String color) {
        double x0 = cx + r * Math.cos(a0 * RAD), y0 = cy + r * Math.sin(a0 * RAD);
        double x1 = cx + r * Math.cos(a1 * RAD), y1 = cy + r * Math.sin(a1 * RAD);
        int largeArc = (a1 - a0) > 180 ? 1 : 0;
        return String.format(Locale.ROOT,
                "<path d='M %.2f %.2f L %.2f %.2f A %.2f %.2f 0 %d 1 %.2f %.2f Z' fill='%s'/>",
                cx, cy, x0, y0, r, r, largeArc, x1, y1, color);
    }

    private String zoneColor(double t) {
        if (t < 0.3) return "#f56c6c";
        if (t < 0.6) return "#e6a23c";
        if (t < 0.75) return "#409eff";
        return "#67c23a";
    }

    private String gradeColor(String grade) {
        if (grade == null) return "#909399";
        return switch (grade) {
            case "优秀" -> "#67c23a";
            case "良好" -> "#409eff";
            case "中等" -> "#e6a23c";
            default -> "#f56c6c";
        };
    }

    private double clamp(Double v) {
        if (v == null) return 0;
        return Math.max(0, Math.min(100, v));
    }

    private String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** SVG -> PNG(2x 高清) -> base64 data-uri */
    private String rasterize(String svg, int w, int h) throws Exception {
        PNGTranscoder transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) (w * 2));
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) (h * 2));
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            transcoder.transcode(new TranscoderInput(new StringReader(svg)), new TranscoderOutput(baos));
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }
}
