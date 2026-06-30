import request from '../utils/request'
import type { ReportRecord, ReportDataVO, ReportOverviewVO, OverviewStatus } from '@/types/report'

export function getApprovedSubmissions(taskId: number) {
  return request.get(`/report/approved-submissions/${taskId}`)
}

export function getReportData(submissionId: number) {
  return request.get(`/report/data/${submissionId}`) as Promise<ReportDataVO>
}

export function exportReport(submissionId: number) {
  return request.get(`/report/export/${submissionId}`, { responseType: 'blob' }) as Promise<Blob>
}

export function batchExport(submissionIds: number[]) {
  return request.post('/report/batch-export', submissionIds, { responseType: 'blob' }) as Promise<Blob>
}

export function getOverviewStatus(taskId: number) {
  return request.get(`/report/overview-status/${taskId}`) as Promise<OverviewStatus>
}

export function getOverviewData(taskId: number) {
  return request.get(`/report/overview-data/${taskId}`) as Promise<ReportOverviewVO>
}

export function exportOverview(taskId: number) {
  return request.get(`/report/overview-export/${taskId}`, { responseType: 'blob' }) as Promise<Blob>
}

export function generateTaskOverview(taskId: number) {
  return request.post(`/report/generate-task-overview/${taskId}`) as Promise<ReportRecord>
}

export function getReportList(taskId?: number) {
  return request.get('/report/list', { params: taskId ? { taskId } : {} }) as Promise<ReportRecord[]>
}

export function downloadPdf(reportId: number) {
  return request.get(`/report/download-pdf/${reportId}`, { responseType: 'blob' }) as Promise<Blob>
}
