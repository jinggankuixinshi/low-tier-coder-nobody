import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'
import { encryptPassword } from '../utils/crypto'
import websocket from '../utils/websocket'
import router from '../router'
import type { AuthState, LoginResponse, UserRole } from '@/types/user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userId = ref<number | null>(Number(localStorage.getItem('userId')) || null)
  const username = ref<string>(localStorage.getItem('username') || '')
  const realName = ref<string>(localStorage.getItem('realName') || '')
  const role = ref<UserRole | null>(Number(localStorage.getItem('role')) as UserRole || null)
  const roleName = ref<string>(localStorage.getItem('roleName') || '')
  const permissions = ref<string[]>(JSON.parse(localStorage.getItem('permissions') || '[]'))

  const isLoggedIn = computed(() => !!token.value)
  const isTeacher = computed(() => role.value === 1)
  const isStudent = computed(() => role.value === 0)

  async function login(username_: string, password: string): Promise<LoginResponse> {
    const encryptedPwd = await encryptPassword(password)
    const res: LoginResponse = await request.post('/auth/login', {
      username: username_,
      password: encryptedPwd,
    })
    setAuthData(res)
    websocket.connect(res.token)
    return res
  }

  function setAuthData(data: LoginResponse): void {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName
    role.value = data.role
    roleName.value = data.roleName
    permissions.value = data.permissions || []

    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('username', data.username)
    localStorage.setItem('realName', data.realName)
    localStorage.setItem('role', String(data.role))
    localStorage.setItem('roleName', data.roleName)
    localStorage.setItem('permissions', JSON.stringify(data.permissions || []))
  }

  async function logout(): Promise<void> {
    try {
      await request.post('/auth/logout')
    } catch {
      // ignore logout API errors
    }
    clearAuth()
  }

  function clearAuth(): void {
    token.value = ''
    userId.value = null
    username.value = ''
    realName.value = ''
    role.value = null
    roleName.value = ''
    permissions.value = []
    websocket.disconnect()

    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('realName')
    localStorage.removeItem('role')
    localStorage.removeItem('roleName')
    localStorage.removeItem('permissions')

    router.push('/login')
  }

  function hasPermission(perm: string): boolean {
    return permissions.value.includes(perm)
  }

  return {
    token, userId, username, realName, role, roleName, permissions,
    isLoggedIn, isTeacher, isStudent,
    login, logout, clearAuth, setAuthData, hasPermission,
  }
})
