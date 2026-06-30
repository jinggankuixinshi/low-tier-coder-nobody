export interface ApiResponse<T = any> {
  code: number
  data: T
  message?: string
}

export interface PageData<T> {
  records: T[]
  total: number
  page: number
  size: number
}
