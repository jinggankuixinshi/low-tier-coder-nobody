export type EvalMethod = 0 | 1 | 2 | 3 // 0=纯AI, 1=纯人工, 2=混合, 3=逐项自定义
export type EvalType = 0 | 1 | 2 // 0=AI, 1=人工, 2=混合

export interface EvaluationTemplate {
  id?: number
  name: string
  description: string
  taskId?: number
  evalMethod: EvalMethod
  aiWeight?: number
  manualWeight?: number
  aiModel?: string
  weightCompletion?: number
  weightTech?: number
  weightInnovation?: number
  weightDocument?: number
  createTime?: string
  updateTime?: string
}

export interface EvaluationIndicator {
  id?: number
  templateId?: number
  name: string
  description: string
  weight: number
  maxScore?: number
  evalType: EvalType
  aiWeight?: number
  manualWeight?: number
  dim?: string
  sortOrder?: number
  createTime?: string
}

export interface EvaluationResult {
  id?: number
  submissionId: number
  templateId: number
  indicatorId: number
  indicatorName?: string
  weight?: number
  maxScore?: number
  evalType: EvalType
  aiWeight?: number
  manualWeight?: number
  autoScore?: number
  autoComment?: string
  manualScore?: number
  manualComment?: string
  adjustScore?: number
  adjustReason?: string
  finalScore?: number
  dimension?: string
  _manualScore?: number
  _manualComment?: string
  _saving?: boolean
}

export interface ManualScoreRequest {
  resultId: number
  manualScore?: number
  manualComment?: string
  adjustScore?: number
  adjustReason?: string
}

export interface EvaluationSummary {
  contentEmpty: boolean
  totalIndicators: number
  totalAiIndicators: number
  aiFailed: number
  aiScoredZero: number
  aiScoredPositive: number
}

export interface AutoScoreResponse {
  results: EvaluationResult[]
  statusSummary: EvaluationSummary
}
