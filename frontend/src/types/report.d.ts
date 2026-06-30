export interface ReportRecord {
  id: number
  title: string
  reportType: 0 | 1
  taskId?: number
  submissionId?: number
  pdfPath?: string
  weightedTotal?: number
  grade?: string
  completionScore?: number
  techQualityScore?: number
  innovationScore?: number
  documentScore?: number
  createTime: string
}

export interface ReportDataVO {
  submissionId: number
  task: {
    id: number; title: string; courseName: string; moduleName: string; subject: string
    weightCompletion: number; weightTech: number; weightInnovation: number; weightDocument: number
  }
  student: { name: string; studentNo: string; username: string }
  template: { id: number; name: string; evalMethod: number
    weightCompletion?: number; weightTech?: number; weightInnovation?: number; weightDocument?: number
  }
  date: string
  summary: {
    completionScore: number; techScore: number; innovationScore: number
    documentScore: number; totalScore: number; grade: string
  }
  results: Array<{
    indicatorName: string; dimension: string; weight: number; evalType: number
    autoScore: number | null; autoComment: string | null
    manualScore: number | null; manualComment: string | null
    adjustScore: number | null; adjustReason: string | null; finalScore: number
  }>
  verifications: Array<{ checkItem: string; checkType: number; status: number; detail: string }>
  precheckPassCount: number
  allPassed: boolean
  teacherComment: string
  subjectiveScore: number | null
  subjectiveReason: string | null
}

export interface ReportOverviewVO {
  task: {
    id: number; title: string; courseName: string; moduleName: string; subject: string
    weightCompletion: number; weightTech: number; weightInnovation: number; weightDocument: number
  }
  date: string
  totalSubmissions: number
  approvedCount: number
  studentStats: Array<{ studentName: string; totalScore: number | null; grade: string; submitTime: string }>
  dimScores: { completion: number[]; tech: number[]; innovation: number[]; document: number[] }
  allScores: number[]
  gradeCounts: { excellent: number; good: number; medium: number; fail: number }
  avgScore: number | null
  maxScore: number | null
  minScore: number | null
}

export interface OverviewStatus {
  status: 'ready' | 'unchanged' | 'changed' | 'blocked'
  message: string | null
  totalSubmissions: number
  approvedCount: number
  overviewExists: boolean
  reportId?: number
}
