package com.example.evaluation.service;

import com.example.evaluation.dto.ReportDataVO;
import com.example.evaluation.dto.ReportOverviewVO;
import com.example.evaluation.dto.SubmissionVO;
import com.example.evaluation.entity.ReportRecord;
import com.example.evaluation.entity.Submission;

import java.util.List;
import java.util.Map;

public interface ReportService {

    ReportRecord generateSingleReport(Long submissionId);

    ReportDataVO getOrGenerateReportData(Long submissionId);

    byte[] exportPdf(Long submissionId);

    byte[] batchExportPdfZip(List<Long> submissionIds);

    ReportRecord generateTaskOverview(Long taskId);

    Map<String, Object> getOverviewStatus(Long taskId);

    ReportOverviewVO getOrGenerateOverviewData(Long taskId);

    byte[] exportOverviewPdf(Long taskId);

    List<ReportRecord> getReportList(Long taskId);

    byte[] downloadPdf(Long reportId);

    List<Submission> getApprovedSubmissions(Long taskId);

    List<SubmissionVO> getApprovedSubmissionsWithNames(Long taskId);

    boolean isAllApproved(Long taskId);
}
