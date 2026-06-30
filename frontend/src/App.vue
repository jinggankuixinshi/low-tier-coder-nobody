<template>
  <div id="app">
    <template v-if="isLoginPage">
      <router-view />
    </template>

    <template v-else>
      <div v-if="notification.visible" class="notification-bar" :class="notification.type">
        <span>{{ notification.message }}</span>
        <el-button link class="close-btn" @click="notification.visible = false">&times;</el-button>
      </div>

      <el-container class="layout-container">
        <el-aside width="220px" class="layout-aside">
          <div class="logo">
            <h2>智能评价系统</h2>
          </div>
          <el-menu
            :default-active="activeMenu"
            router
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409EFF"
          >
            <el-menu-item index="/dashboard">
              <el-icon><DataBoard /></el-icon>
              <span>工作台</span>
            </el-menu-item>
            <el-menu-item index="/tasks">
              <el-icon><Notebook /></el-icon>
              <span>实训任务管理</span>
            </el-menu-item>
            <el-menu-item v-if="authStore.isStudent" index="/upload">
              <el-icon><UploadFilled /></el-icon>
              <span>成果上传</span>
            </el-menu-item>
            <el-menu-item v-if="authStore.isStudent" index="/my-submissions">
              <el-icon><Document /></el-icon>
              <span>我的提交</span>
            </el-menu-item>
            <el-menu-item v-if="authStore.isTeacher" index="/submissions">
              <el-icon><Checked /></el-icon>
              <span>提交结果</span>
            </el-menu-item>
            <el-menu-item v-if="authStore.isTeacher" index="/evaluation">
              <el-icon><StarFilled /></el-icon>
              <span>多维评价</span>
            </el-menu-item>
            <el-menu-item v-if="authStore.isTeacher" index="/reports">
              <el-icon><Document /></el-icon>
              <span>报表导出</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-container>
          <el-header class="layout-header">
            <span class="header-title">实训成果智能评价系统</span>
            <div class="header-right">
              <el-tag :type="authStore.isTeacher ? 'warning' : 'success'" size="small" class="role-tag">
                {{ authStore.roleName }}
              </el-tag>
              <el-dropdown trigger="click" @command="handleCommand">
                <span class="user-info">
                  <el-icon><User /></el-icon>
                  {{ authStore.realName || authStore.username }}
                  <el-icon><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                    <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </el-header>
          <el-main class="layout-main">
            <router-view />
          </el-main>
        </el-container>
      </el-container>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import websocket from './utils/websocket'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isLoginPage = computed(() => route.path === '/login')
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/tasks')) return '/tasks'
  if (path.startsWith('/submissions')) return '/submissions'
  if (path.startsWith('/my-submissions')) return '/my-submissions'
  return path
})

const notification = reactive({
  visible: false,
  type: 'info' as string,
  message: '',
  timer: null as ReturnType<typeof setTimeout> | null,
})

const wsUnsubscribers: (() => void)[] = []

function showNotification(type: string, message: string) {
  if (notification.timer) clearTimeout(notification.timer)
  notification.type = type
  notification.message = message
  notification.visible = true
  notification.timer = setTimeout(() => { notification.visible = false }, 5000)
}

onMounted(() => {
  if (!authStore.isLoggedIn) return
  websocket.connect(authStore.token)

  wsUnsubscribers.push(
    websocket.on('PARSE_COMPLETE', (data: any) => showNotification('success', data.message || '文件解析完成')),
    websocket.on('PARSE_FAILED', (data: any) => {
      const msg = data.error ? `${data.message}: ${data.error}` : (data.message || '文件解析失败')
      showNotification('error', msg)
    }),
    websocket.on('VERIFICATION_COMPLETE', (data: any) => showNotification('success', data.message || '核查完成')),
    websocket.on('EVALUATION_COMPLETE', (data: any) => showNotification('success', data.message || '评分完成')),
    websocket.on('REPORT_GENERATED', (data: any) => showNotification('success', data.message || '报表生成完成')),
    websocket.on('KICKED', (data: any) => {
      showNotification('error', data.message || '账号已被迫下线')
      setTimeout(() => authStore.clearAuth(), 1500)
    }),
    websocket.on('NEW_TASK', (data: any) => showNotification('info', data.message || '有新任务发布')),
  )
})

onUnmounted(() => {
  wsUnsubscribers.forEach((unsub) => unsub())
})

function handleCommand(command: string | number | object) {
  if (command === 'logout') {
    authStore.logout()
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }

body { font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif; }

.layout-container { height: 100vh; }
.layout-aside { background-color: #304156; overflow-y: auto; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; background-color: #263445; }
.logo h2 { color: #fff; font-size: 18px; white-space: nowrap; }
.layout-header { background-color: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; }
.header-title { font-size: 16px; color: #303133; }
.header-right { display: flex; align-items: center; gap: 16px; }
.role-tag { font-size: 12px; }
.user-info { display: flex; align-items: center; gap: 6px; cursor: pointer; color: #606266; font-size: 14px; }
.user-info:hover { color: #409EFF; }
.layout-main { background-color: var(--ios-bg); padding: 24px; overflow-y: auto; }

.notification-bar {
  position: fixed; top: 0; left: 0; right: 0; z-index: 3000;
  padding: 10px 24px; display: flex; align-items: center; justify-content: space-between;
  color: #fff; font-size: 14px; animation: slideDown 0.3s ease;
}
.notification-bar.info { background-color: #409EFF; }
.notification-bar.success { background-color: #67C23A; }
.notification-bar.error { background-color: #F56C6C; }
.notification-bar .close-btn { color: #fff; font-size: 18px; }
@keyframes slideDown { from { transform: translateY(-100%); } to { transform: translateY(0); } }
</style>
