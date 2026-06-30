import request from '../utils/request'
import type { Submission } from '@/types/submission'

export function uploadFile(data: FormData) {
  return request.post('/files/upload', data)
}

export function getSubmissionList(taskId: number) {
  return request.get('/files/submissions', { params: { taskId } }) as Promise<Submission[]>
}

export function getSubmissionDetail(id: number) {
  return request.get(`/files/submission/${id}`) as Promise<Submission>
}

export function downloadSubmissionFile(submissionId: number) {
  return request.get(`/files/download/${submissionId}`, {
    responseType: 'blob',
  }) as Promise<Blob>
}

export function downloadSubmissionZip(submissionId: number) {
  return request.get(`/files/download-zip/${submissionId}`, {
    responseType: 'blob',
  }) as Promise<Blob>
}
