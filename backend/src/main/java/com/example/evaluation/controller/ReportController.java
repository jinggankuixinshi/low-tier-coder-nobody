package com.example.evaluation.controller;

import com.example.evaluation.common.Result;
import com.example.evaluation.dto.ReportDataVO;
import com.example.evaluation.entity.ReportRecord;
import com.example.evaluation.entity.TrainingTask;
import com.example.evaluation.mapper.TaskMapper;
import com.example.evaluation.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;
    private final TaskMapper taskMapper;

    public ReportController(ReportService reportService, TaskMapper taskMapper) {
        this.reportService = reportService;
        this.taskMapper = taskMapper;
    }

    @GetMapping("/approved-submissions/{taskId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> getApprovedSubmissions(@PathVariable Long taskId) {
        var list = reportService.getApprovedSubmissionsWithNames(taskId);
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("allApproved", reportService.isAllApproved(taskId));
        return Result.success(result);
    }

    @GetMapping("/data/{submissionId}")
    @PreAuthorize("isAuthenticated()")
    public Result<?> getReportData(@PathVariable Long submissionId) {
        ReportDataVO data = reportService.getOrGenerateReportData(submissionId);
        return Result.success(data);
    }

    @GetMapping("/export/{submissionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long submissionId) {
        byte[] data = reportService.exportPdf(submissionId);
        ReportDataVO reportData = reportService.getOrGenerateReportData(submissionId);
        String filename = buildFilename(reportData);
        String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @PostMapping("/batch-export")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<byte[]> batchExport(@RequestBody List<Long> submissionIds) {
        byte[] data = reportService.batchExportPdfZip(submissionIds);
        String encodedName = URLEncoder.encode("评价报告批量导出.zip", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(data);
    }

    @PostMapping("/generate-task-overview/{taskId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> generateTaskOverview(@PathVariable Long taskId) {
        ReportRecord record = reportService.generateTaskOverview(taskId);
        return Result.success("任务总览报表生成成功", record);
    }

    @GetMapping("/overview-status/{taskId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> getOverviewStatus(@PathVariable Long taskId) {
        return Result.success(reportService.getOverviewStatus(taskId));
    }

    @GetMapping("/overview-data/{taskId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> getOverviewData(@PathVariable Long taskId) {
        return Result.success(reportService.getOrGenerateOverviewData(taskId));
    }

    @GetMapping("/overview-export/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportOverview(@PathVariable Long taskId) {
        byte[] data = reportService.exportOverviewPdf(taskId);
        TrainingTask task = taskMapper.selectById(taskId);
        String filename = (task != null ? task.getTitle() : "任务") + "_任务总览报表.pdf";
        String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Long taskId) {
        return Result.success(reportService.getReportList(taskId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/download-pdf/{reportId}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long reportId) {
        byte[] data = reportService.downloadPdf(reportId);
        String filename = "report_" + reportId + ".pdf";
        String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    private String buildFilename(ReportDataVO data) {
        String studentName = data.getStudent() != null && data.getStudent().getName() != null
                ? data.getStudent().getName() : "未知";
        String taskTitle = data.getTask() != null && data.getTask().getTitle() != null
                ? data.getTask().getTitle() : "未知任务";
        return studentName + "_" + taskTitle + "_评价报告.pdf";
    }
}
