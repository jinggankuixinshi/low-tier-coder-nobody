<template>
  <div class="profile-page">
    <div class="page-header">
      <h3>个人信息</h3>
    </div>

    <el-card v-loading="loading">
      <el-descriptions :column="2" border size="large" v-if="user">
        <el-descriptions-item label="用户ID">{{ user.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ user.realName }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="user.role === 1 ? 'warning' : 'success'" size="small">
            {{ user.role === 1 ? '教师' : '学生' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ user.email || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ user.phone || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatTime(user.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="最后更新">{{ formatTime(user.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="user.deleted === 0 ? 'success' : 'danger'" size="small">
            {{ user.deleted === 0 ? '正常' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="拥有权限">
          <el-tag v-for="p in permissions" :key="p" size="small" style="margin-right: 4px">{{ p }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="加载失败" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useAuthStore } from '../stores/auth';
import request from '../utils/request';

const authStore = useAuthStore();
const user = ref(null);
const loading = ref(false);
const permissions = ref(authStore.permissions || []);

function formatTime(t) {
  if (!t) return '-';
  return t.replace('T', ' ').substring(0, 19);
}

onMounted(async () => {
  loading.value = true;
  try {
    user.value = await request.get('/users/me');
  } catch {
    user.value = null;
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
</style>
