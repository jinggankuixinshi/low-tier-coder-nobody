<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>实训成果智能评价系统</h1>
        <p>Training Achievement Intelligent Evaluation System</p>
      </div>
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <el-button link type="primary" @click="showRegister = true">没有账号？立即注册</el-button>
      </div>
    </div>

    <!-- 注册对话框 -->
    <el-dialog v-model="showRegister" title="用户注册" width="460px" :close-on-click-modal="false">
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="registerForm.role">
            <el-radio :value="0">学生</el-radio>
            <el-radio :value="1">教师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="registerForm.role === 1" label="邀请码" prop="inviteCode">
          <el-input v-model="registerForm.inviteCode" placeholder="请输入教师邀请码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { useRouter } from 'vue-router';
import {   ElNotification   } from 'element-plus';
import { useAuthStore } from '../stores/auth';
import { encryptPassword } from '../utils/crypto';
import request from '../utils/request';

const router = useRouter();
const authStore = useAuthStore();

const formRef = ref(null);
const registerFormRef = ref(null);
const loading = ref(false);
const registerLoading = ref(false);
const showRegister = ref(false);

const loginForm = reactive({
  username: '',
  password: '',
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: '',
  role: 0 as number,
  inviteCode: '',
});

const validateConfirmPassword = (_rule: any, value: string, callback: (e?: Error) => void) => {
  if (value !== registerForm.password) {
    callback(new Error('两次密码输入不一致'));
  } else {
    callback();
  }
};

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  inviteCode: [{ required: true, message: '请输入教师邀请码', trigger: 'blur' }],
};

// 切换角色时重置邀请码校验状态
watch(() => registerForm.role, () => {
  registerFormRef.value?.clearValidate('inviteCode');
});

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    await authStore.login(loginForm.username, loginForm.password);
    ElNotification({ type: 'success', title: '登录成功', message: '', position: 'top-right', duration: 3000 });
    router.push('/dashboard');
  } catch (e) {
    ElNotification({ type: 'error', title: e?.message || '登录失败，请检查网络连接或账号密码', message: '', position: 'top-right', duration: 3000 });
  } finally {
    loading.value = false;
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false);
  if (!valid) return;
  registerLoading.value = true;
  try {
    const encryptedPwd = await encryptPassword(registerForm.password);
    const body: Record<string, any> = {
      username: registerForm.username,
      password: encryptedPwd,
      confirmPassword: encryptedPwd,
      realName: registerForm.realName,
      phone: registerForm.phone,
      email: registerForm.email,
      role: registerForm.role,
    };
    if (registerForm.role === 1) {
      body.inviteCode = registerForm.inviteCode;
    }
    await request.post('/auth/register', body);
    ElNotification({ type: 'success', title: '注册成功，请登录', message: '', position: 'top-right', duration: 3000 });
    showRegister.value = false;
    loginForm.username = registerForm.username;
    loginForm.password = '';
    // 重置表单
    registerForm.inviteCode = '';
  } catch (e) {
    // handled by interceptor
  } finally {
    registerLoading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #304156 0%, #1f2d3d 100%);
}
.login-card {
  width: 420px;
  padding: 48px 40px;
  background: var(--ios-card-bg);
  border-radius: var(--ios-card-radius);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}
.login-header {
  text-align: center;
  margin-bottom: 36px;
}
.login-header h1 {
  font-size: 22px;
  color: var(--ios-label);
  margin: 0 0 8px;
}
.login-header p {
  font-size: 13px;
  color: var(--ios-label-2);
  margin: 0;
}
.login-btn {
  width: 100%;
}
.login-footer {
  text-align: center;
  margin-top: 16px;
}
</style>
