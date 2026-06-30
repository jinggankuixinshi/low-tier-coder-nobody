import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    role?: number
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/tasks',
    name: 'TaskManage',
    component: () => import('../views/TaskManage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/tasks/:taskId',
    name: 'TaskDetail',
    component: () => import('../views/TaskDetail.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/upload',
    name: 'FileUpload',
    component: () => import('../views/FileUpload.vue'),
    meta: { requiresAuth: true, role: 0 },
  },
  {
    path: '/my-submissions',
    name: 'MySubmissions',
    component: () => import('../views/MySubmissions.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/submissions',
    name: 'SubmissionResult',
    component: () => import('../views/VerificationResult.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/evaluation',
    name: 'EvaluationManage',
    component: () => import('../views/EvaluationManage.vue'),
    meta: { requiresAuth: true, role: 1 },
  },
  {
    path: '/reports',
    name: 'ReportExport',
    component: () => import('../views/ReportExport.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/report-view/:submissionId',
    name: 'ReportView',
    component: () => import('../views/ReportView.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const token = authStore.token
  const role = authStore.role

  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
    return
  }

  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }

  if (to.meta.role !== undefined && role !== to.meta.role) {
    next('/dashboard')
    return
  }

  next()
})

export default router
