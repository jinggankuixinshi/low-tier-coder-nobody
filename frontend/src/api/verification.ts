import request from '../utils/request'

export function triggerVerification(submissionId: number) {
  return request.post(`/verification/check/${submissionId}`)
}

export function getVerificationResult(submissionId: number) {
  return request.get(`/verification/result/${submissionId}`)
}
