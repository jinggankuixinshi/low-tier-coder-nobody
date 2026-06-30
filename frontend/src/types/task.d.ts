export type TaskStatus = 0 | 1 | 2 // 0=草稿, 1=已发布, 2=已截止

export interface Task {
  id: number
  title: string
  subject: string
  description?: string
  expectedOutput?: string
  courseName?: string
  moduleName?: string
  brief?: string
  businessScenario?: string
  implConditions?: string
  requirementDocs?: string
  teacherId?: number
  status: TaskStatus
  deadline?: string
  createTime?: string
  updateTime?: string
  weightCompletion?: number
  weightTech?: number
  weightInnovation?: number
  weightDocument?: number
}

export interface TaskFormData {
  title: string
  brief: string
  subject: string
  businessScenario: string
  implConditions: string
  expectedOutput: string
  deadline: string
  requirementDocs?: string
}

export interface TaskSearchParams {
  keyword?: string
  status?: number | null
}
