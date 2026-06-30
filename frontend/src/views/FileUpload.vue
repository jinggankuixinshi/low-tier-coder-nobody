<template>
  <div class="file-upload">
    <div class="page-header">
      <h3>实训成果上传</h3>
    </div>

    <el-card>
      <el-form :inline="true">
        <el-form-item label="选择任务">
          <el-select v-model="selectedTaskId" placeholder="请选择我的项目" style="width: 360px" filterable>
            <el-option
              v-for="t in tasks"
              :key="t.id"
              :label="t.title"
              :value="t.id"
            >
              <span>{{ t.title }}</span>
              <span style="float:right;color:#909399;font-size:12px">{{ t.subject }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="当前用户">
          <el-tag type="success">{{ authStore.realName }}（学生）</el-tag>
        </el-form-item>
      </el-form>

      <el-alert v-if="deadlinePassed" title="该任务已过截止时间，无法提交" type="error" :closable="false" show-icon style="margin-bottom:16px" />

      <el-divider />

      <el-upload
        class="upload-area"
        drag
        multiple
        :auto-upload="false"
        :show-file-list="false"
        :on-change="onFileChange"
        :before-upload="beforeUpload"
        :accept="acceptTypes"
      >
        <el-icon class="el-icon--upload" :size="48"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或 <em>点击选择文件</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 Word/PDF/PPT、压缩包(.zip/.rar/.7z)、视频、图片、代码文件等，单个文件最大 500MB
          </div>
        </template>
      </el-upload>

      <div v-if="pendingFiles.length" class="file-preview">
        <h4>待提交文件（{{ pendingFiles.length }} 个，共 {{ totalSizeStr }}）</h4>
        <div v-for="(f, i) in pendingFiles" :key="i" class="file-item">
          <el-icon :size="20"><component :is="fileIcon(f.name)" /></el-icon>
          <span class="file-name">{{ f.name }}</span>
          <span class="file-size">{{ formatSize(f.size) }}</span>
          <el-button link type="danger" @click="removeFile(i)"><el-icon><Delete /></el-icon></el-button>
        </div>
      </div>

      <div style="margin-top:20px;text-align:center" v-if="pendingFiles.length">
        <el-button type="primary" size="large" @click="submitFiles" :loading="submitting" :disabled="deadlinePassed">
          <el-icon><Upload /></el-icon> 提交
        </el-button>
        <el-button size="large" @click="pendingFiles=[]" :disabled="submitting">清空</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import {   ElNotification   } from 'element-plus';
import { useAuthStore } from '../stores/auth';
import { getFavoriteTasks } from '../api/task';
import request from '../utils/request';
import { useFormatSize } from '../composables/useFormatSize'

const route = useRoute();
const authStore = useAuthStore();
const tasks = ref([]);
const selectedTaskId = ref(null);
const pendingFiles = ref([]);
const submitting = ref(false);
const selectedTask = ref(null);
const { formatSize } = useFormatSize()

const acceptTypes = '.doc,.docx,.pdf,.ppt,.pptx,.xls,.xlsx,.zip,.rar,.7z,.tar,.gz,.tgz,.mp4,.avi,.mov,.wmv,.webm,.png,.jpg,.jpeg,.gif,.bmp,.txt,.java,.py,.js,.ts,.cpp,.c,.h,.html,.css,.xml,.json,.md,.yml,.yaml,.sql,.sh,.bat,.vue,.go,.rs';

const totalSizeStr = computed(() => {
  const total = pendingFiles.value.reduce((s, f) => s + f.size, 0);
  return formatSize(total);
});

const deadlinePassed = computed(() => {
  if (!selectedTask.value?.deadline) return false;
  return new Date() > new Date(selectedTask.value.deadline);
});

function fileIcon(name) {
  const ext = name.split('.').pop()?.toLowerCase();
  if (['zip','rar','7z','tar','gz','tgz'].includes(ext)) return 'FolderOpened';
  if (['mp4','avi','mov','wmv','webm'].includes(ext)) return 'VideoCamera';
  if (['png','jpg','jpeg','gif','bmp'].includes(ext)) return 'Picture';
  if (['ppt','pptx'].includes(ext)) return 'TrendCharts';
  if (['xls','xlsx'].includes(ext)) return 'Grid';
  if (['doc','docx','pdf'].includes(ext)) return 'Document';
  return 'Files';
}

onMounted(async () => {
  try { tasks.value = await getFavoriteTasks() || []; } catch { tasks.value = []; }
  const tid = route.query.taskId;
  if (tid) selectedTaskId.value = Number(tid);
});

watch(selectedTaskId, async (tid) => {
  if (!tid) { selectedTask.value = null; return; }
  try { selectedTask.value = await request.get(`/tasks/${tid}`); } catch { selectedTask.value = null; }
});



function onFileChange(file) {
  pendingFiles.value.push(file.raw);
}

function removeFile(index) {
  pendingFiles.value.splice(index, 1);
}

async function beforeUpload(file) {
  const ext = '.' + file.name.split('.').pop()?.toLowerCase();
  const validTypes = acceptTypes.split(',');
  if (!validTypes.includes(ext)) {
    ElNotification({ type: 'error', title: `不支持的文件类型: ${ext}`, message: '', position: 'top-right', duration: 3000 });
    return false;
  }
  if (file.size > 500 * 1024 * 1024) {
    ElNotification({ type: 'error', title: `"${file.name}" 超过 500MB 限制`, message: '', position: 'top-right', duration: 3000 });
    return false;
  }
  return true;
}

async function submitFiles() {
  if (!selectedTaskId.value) {
    ElNotification({ type: 'warning', title: '请先选择任务', message: '', position: 'top-right', duration: 3000 });
    return;
  }
  if (!pendingFiles.value.length) {
    ElNotification({ type: 'warning', title: '请先添加要上传的文件', message: '', position: 'top-right', duration: 3000 });
    return;
  }
  submitting.value = true;
  try {
    const fd = new FormData();
    pendingFiles.value.forEach(f => fd.append('files', f));
    fd.append('taskId', selectedTaskId.value);

    await request.post('/files/upload', fd);
    ElNotification({ type: 'success', title: '提交成功', message: '', position: 'top-right', duration: 3000 });
    pendingFiles.value = [];
  } catch { /* handled by interceptor */ }
  finally { submitting.value = false; }
}
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.upload-area { width: 100%; }

.file-preview { margin-top: 16px; }
.file-preview h4 { margin-bottom: 10px; color: var(--ios-label); font-size: 15px; }
.file-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; background: var(--ios-fill); border-radius: 10px; margin-bottom: 8px;
  transition: background 0.2s;
}
.file-item:hover { background: #e8f0fe; }
.file-name { flex:1; font-size: 14px; color: var(--ios-label); }
.file-size { font-size: 12px; color: var(--ios-label-2); white-space: nowrap; }
</style>
