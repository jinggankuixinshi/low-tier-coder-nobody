export type UserRole = 0 | 1 // 0=学生, 1=教师

export interface UserInfo {
  id: number
  username: string
  realName: string
  role: UserRole
  roleName: string
  email?: string
  phone?: string
  studentNo?: string
  createTime?: string
  updateTime?: string
  deleted?: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  realName: string
  role: UserRole
  roleName: string
  permissions: string[]
}

export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  studentNo: string
  realName: string
  role: UserRole
  email?: string
  phone?: string
  teamName?: string
  schoolName?: string
  advisor?: string
  memberNames: string[]
}

export interface AuthState {
  token: string
  userId: number | null
  username: string
  realName: string
  role: UserRole | null
  roleName: string
  permissions: string[]
}
