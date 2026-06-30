export type ApprovalStatus = 0 | 1 | 2 // 0=未审批, 1=草稿, 2=已审批

export interface Submission {
  id: number
  taskId: number
  teamId?: number
  teamName?: string
  taskName?: string
  projectName?: string
  fileNames?: string | string[]
  filePaths?: string
  fileSizes?: string
  evaluationStatus?: number
  submitted?: number
  approvalStatus: ApprovalStatus
  aiScoreStatus?: number
  draftTemplateId?: number
  submitTime?: string
  createTime?: string
  updateTime?: string
  totalScore?: number
}

export interface SubmissionListParams {
  taskId?: number
  teamId?: number
  pendingOnly?: boolean
  page?: number
  size?: number
  sortOrder?: 'asc' | 'desc'
}
