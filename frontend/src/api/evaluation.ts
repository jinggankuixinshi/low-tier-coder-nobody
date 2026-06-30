import request from '../utils/request'
import type { EvaluationTemplate, EvaluationIndicator, EvaluationResult, ManualScoreRequest, AutoScoreResponse } from '@/types/evaluation'

export function getTemplates(taskId?: number) {
  return request.get('/evaluation/templates', { params: { taskId } }) as Promise<EvaluationTemplate[]>
}

export function createTemplate(data: Partial<EvaluationTemplate>) {
  return request.post('/evaluation/templates', data) as Promise<EvaluationTemplate>
}

export function updateTemplate(id: number, data: Partial<EvaluationTemplate>) {
  return request.put(`/evaluation/templates/${id}`, data) as Promise<EvaluationTemplate>
}

export function deleteTemplate(id: number) {
  return request.delete(`/evaluation/templates/${id}`)
}

export function getIndicators(templateId: number) {
  return request.get(`/evaluation/templates/${templateId}/indicators`) as Promise<EvaluationIndicator[]>
}

export function createIndicator(data: Partial<EvaluationIndicator>) {
  return request.post('/evaluation/indicators', data) as Promise<EvaluationIndicator>
}

export function updateIndicator(id: number, data: Partial<EvaluationIndicator>) {
  return request.put(`/evaluation/indicators/${id}`, data) as Promise<EvaluationIndicator>
}

export function batchSaveIndicators(templateId: number, indicators: Partial<EvaluationIndicator>[]) {
  return request.put(`/evaluation/templates/${templateId}/indicators/batch`, indicators)
}

export function deleteIndicator(id: number) {
  return request.delete(`/evaluation/indicators/${id}`)
}

export function triggerAutoScore(submissionId: number, templateId: number) {
  return request.post(`/evaluation/auto-score/${submissionId}`, null, {
    params: { templateId },
  }) as Promise<AutoScoreResponse>
}

export function submitManualScore(data: ManualScoreRequest) {
  return request.post('/evaluation/manual-score', data)
}

export function submitEvaluationResult(submissionId: number, teacherComment?: string, subjectiveScore?: number | null, subjectiveReason?: string) {
  return request.post(`/evaluation/submit/${submissionId}`, {
    teacherComment: teacherComment || '',
    subjectiveScore,
    subjectiveReason: subjectiveReason || '',
  })
}

export function getEvaluationHistory() {
  return request.get('/evaluation/history')
}

export function getEvaluationResult(submissionId: number, templateId?: number) {
  return request.get(`/evaluation/result/${submissionId}`, { params: { templateId } }) as Promise<EvaluationResult[]>
}

export function initResults(submissionId: number, templateId: number) {
  return request.post(`/evaluation/init-results/${submissionId}`, null, {
    params: { templateId },
  }) as Promise<EvaluationResult[]>
}

export function replaceDraft(submissionId: number, oldTemplateId: number, newTemplateId: number) {
  return request.post(`/evaluation/replace-draft/${submissionId}`, null, {
    params: { oldTemplateId, newTemplateId },
  })
}
