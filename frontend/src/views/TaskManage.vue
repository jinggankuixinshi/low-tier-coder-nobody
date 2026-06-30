<template>
  <div class="task-manage">
    <!-- ========== 教师视图 ========== -->
    <template v-if="authStore.isTeacher">
      <div class="page-header">
        <h3>实训任务管理</h3>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon> 新建任务
        </el-button>
      </div>
      <el-card style="margin-bottom:16px">
        <el-form :inline="true">
          <el-form-item label="搜索">
            <el-input v-model="searchKeyword" placeholder="任务标题 / 技术栈" clearable style="width:280px" @clear="loadTasks" @input="onSearchInput">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterStatus" placeholder="全部" clearable style="width:140px" @change="loadTasks">
              <el-option label="草稿" :value="0"/>
              <el-option label="已发布" :value="1"/>
              <el-option label="已截止" :value="-1"/>
            </el-select>
          </el-form-item>
          <el-form-item label="排列">
            <el-radio-group v-model="sortOrder" size="small" @change="loadTasks">
              <el-radio-button value="desc">倒序 ↓</el-radio-button>
              <el-radio-button value="asc">正序 ↑</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </el-card>
      <el-table :data="filteredList" border stripe v-loading="loading" style="cursor:pointer">
        <el-table-column prop="id" label="ID" width="65" />
        <el-table-column prop="title" label="任务标题" min-width="160" />
        <el-table-column prop="subject" label="技术栈" width="130" />
        <el-table-column label="状态" width="85">
          <template #default="{ row }"><el-tag :type="statusTag(row).type" size="small">{{ statusTag(row).text }}</el-tag></template>
        </el-table-column>
        <el-table-column label="截止时间" width="160">
          <template #default="{ row }">{{ fmt(row.deadline) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="goDetail(row)">详情</el-button>
            <el-button size="small" type="success" @click.stop="goSubmissions(row)">查看结果</el-button>
            <el-button size="small" @click.stop="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="success" @click.stop="publishTask(row)" v-if="row.status === 0">发布</el-button>
            <el-button size="small" type="danger" @click.stop="deleteTask(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- ========== 学生视图 ========== -->
    <template v-else>
      <div class="page-header">
        <h3>实训任务浏览</h3>
        <el-radio-group v-model="taskTab" @change="loadTasks" size="small">
          <el-radio-button value="all">全部实训</el-radio-button>
          <el-radio-button value="fav">我的项目</el-radio-button>
        </el-radio-group>
      </div>
      <el-card style="margin-bottom:16px">
        <el-form :inline="true">
          <el-form-item label="搜索"><el-input v-model="searchKeyword" placeholder="搜索任务标题或科目" clearable style="width:320px" @clear="loadTasks" @input="onSearchInput"><template #prefix><el-icon><Search /></el-icon></template></el-input></el-form-item>
          <el-form-item label="状态"><el-select v-model="filterStatus" placeholder="全部" clearable style="width:140px" @change="loadTasks"><el-option label="已发布" :value="1"/><el-option label="已截止" :value="-1"/></el-select></el-form-item>
          <el-form-item label="排列">
            <el-radio-group v-model="sortOrder" size="small" @change="loadTasks">
              <el-radio-button value="desc">倒序 ↓</el-radio-button>
              <el-radio-button value="asc">正序 ↑</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </el-card>
      <div v-loading="loading">
        <el-empty v-if="!filteredList.length && !loading" description="暂无实训任务"/>
        <el-row :gutter="16">
          <el-col :span="8" v-for="task in filteredList" :key="task.id" style="margin-bottom:16px">
            <el-card shadow="hover" class="task-card" @click="goDetail(task)">
              <div class="card-top">
                <el-tag :type="statusTag(task.status).type" size="small">{{ statusTag(task.status).text }}</el-tag>
                <span class="card-subject">{{ task.subject || '通用' }}</span>
                <template v-if="submittedTaskIds.includes(task.id)">
                  <el-tag size="small" type="success" effect="dark" style="margin-left:auto;margin-right:8px">已提交</el-tag>
                  <el-button v-if="favoriteIds.includes(task.id)" size="small" type="danger" plain class="fav-btn" @click.stop="toggleFav(task)">移除</el-button>
                  <el-button v-else size="small" class="fav-btn" @click.stop="toggleFav(task)" type="primary">添加</el-button>
                </template>
                <template v-else-if="favoriteIds.includes(task.id)">
                  <el-tag size="small" type="info" effect="plain" style="margin-left:auto;margin-right:8px">未提交</el-tag>
                  <el-button size="small" type="danger" plain class="fav-btn" @click.stop="toggleFav(task)">移除</el-button>
                </template>
                <el-button v-else size="small" class="fav-btn" @click.stop="toggleFav(task)" type="primary">添加到我的项目</el-button>
              </div>
              <h4 class="card-title">{{ task.title }}</h4>
              <p class="card-desc">{{ truncate(task.brief || task.description, 80) }}</p>
              <div class="card-footer">
                <span class="card-deadline"><el-icon><Clock /></el-icon> {{ fmt(task.deadline) || '无截止' }}</span>
                <div>
                  <el-button size="small" type="primary" @click.stop="goDetail(task)">查看详情</el-button>
                  <el-button size="small" type="success" @click.stop="goUpload(task)" :disabled="task.status !== 1">提交项目</el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </template>

    <!-- ========== 教师新建/编辑任务对话框 ========== -->
    <el-dialog :title="editingId ? '编辑任务' : '新建实训任务'" v-model="dialogVisible" width="850px" top="3vh" @closed="resetForm">
      <el-form :model="form" label-width="120px" class="task-form">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="任务标题" required>
          <el-input v-model="form.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="任务简介">
          <el-input v-model="form.brief" type="textarea" :rows="2" placeholder="简要描述任务目标与内容" />
        </el-form-item>
        <el-form-item label="技术栈/科目">
          <el-input v-model="form.subject" placeholder="如: Java Web, Python 数据分析, Spring Boot" />
        </el-form-item>

        <el-divider content-position="left">业务场景与条件</el-divider>
        <el-form-item label="业务场景">
          <el-input v-model="form.businessScenario" type="textarea" :rows="3" placeholder="描述业务背景和应用场景，如：某高校学生管理业务，需支持学籍信息维护..." />
        </el-form-item>
        <el-form-item label="实现条件">
          <el-input v-model="form.implConditions" type="textarea" :rows="4" placeholder="描述实现条件，如环境要求、技术平台、硬件要求等" />
        </el-form-item>

        <el-divider content-position="left">实训内容</el-divider>
        <el-form-item label="预期输出">
          <el-input v-model="form.expectedOutput" type="textarea" :rows="2" placeholder="描述预期成果、交付物和验收标准" />
        </el-form-item>

        <el-divider content-position="left">需求文档</el-divider>
        <el-form-item label="上传文档">
          <div>
            <el-upload
              :action="uploadDocUrl"
              :headers="uploadHeaders"
              :on-success="onDocUploaded"
              :on-error="onDocError"
              :before-upload="beforeDocUpload"
              :show-file-list="false"
              accept=".doc,.docx,.pdf"
            >
              <el-button size="small" type="primary"><el-icon><Upload /></el-icon> 上传Word/PDF文档</el-button>
            </el-upload>
          </div>
          <div v-if="uploadedDocs.length" style="margin-top:8px">
            <el-tag v-for="(doc, i) in uploadedDocs" :key="i" size="small" closable style="margin:2px" @close="uploadedDocs.splice(i,1)">{{ doc.name }}</el-tag>
          </div>
        </el-form-item>

        <el-divider content-position="left">其他设置</el-divider>
        <el-form-item label="提交截止">
          <el-date-picker v-model="form.deadline" type="datetime" placeholder="选择截止时间" style="width:100%"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTask" :loading="saving">
          {{ saving ? '保存中...' : '确定' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import {   ElNotification , ElMessageBox  } from 'element-plus';
import { useAuthStore } from '../stores/auth';
import { createTask, updateTask, deleteTask as delTask, getFavoriteIds, toggleFavorite, getFavoriteTasks } from '../api/task';
import request from '../utils/request';
import { useFormatTime } from '../composables/useFormatTime';

const { fmt } = useFormatTime();
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const taskList = ref([]);
const searchKeyword = ref('');
const filterStatus = ref(null);
const sortOrder = ref('desc');
const editingId = ref(null);
const uploadedDocs = ref([]);
const uploadDocUrl = '/api/tasks/upload-doc';
const taskTab = ref('all');
const favoriteIds = ref([]);
const submittedTaskIds = ref([]);

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
});

const form = reactive({
  title: '', brief: '', subject: '', businessScenario: '', implConditions: '',
  expectedOutput: '', deadline: '',
});

function isExpired(task) {
  return task.deadline && new Date(task.deadline) < new Date();
}

const statusTag = (arg) => {
  let s, d;
  if (typeof arg === 'number') { s = arg; d = null; }
  else { s = arg.status; d = arg.deadline; }
  if (d && new Date(d) < new Date()) return { type: 'danger', text: '已截止' };
  return ({0:{type:'info',text:'草稿'},1:{type:'success',text:'已发布'},2:{type:'danger',text:'已截止'}}[s]||{type:'',text:'未知'});
};
function truncate(s, n) { if (!s) return ''; return s.length > n ? s.substring(0,n)+'...' : s; }
function goDetail(row) { router.push(`/tasks/${row.id}`); }
function goSubmissions(row) { router.push(`/submissions?taskId=${row.id}`); }
function goUpload(task) { router.push(`/upload?taskId=${task.id}`); }

let searchTimer: ReturnType<typeof setTimeout> | null = null;

function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => loadTasks(), 300);
}

const filteredList = computed(() => {
  let list = taskList.value;
  if (authStore.isTeacher) {
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase();
      list = list.filter(t => (t.title||'').toLowerCase().includes(kw) || (t.subject||'').toLowerCase().includes(kw));
    }
    if (filterStatus.value === -1) {
      list = list.filter(t => isExpired(t));
    } else if (filterStatus.value != null) {
      list = list.filter(t => t.status === filterStatus.value);
    }
  }
  list = [...list];
  if (sortOrder.value === 'asc') list.reverse();
  return list;
});

async function loadTasks() {
  loading.value = true;
  try {
    if (authStore.isStudent) {
      if (taskTab.value === 'fav') {
        taskList.value = await getFavoriteTasks() || [];
      } else {
        taskList.value = await request.get('/tasks/search', { params: { keyword: searchKeyword.value, status: filterStatus.value } }) || [];
      }
    } else {
      const res = await request.get('/tasks/search', { params: { keyword: searchKeyword.value, status: filterStatus.value } }) || [];
      taskList.value = res?.records || res || [];
    }
  } catch { taskList.value = []; }
  finally { loading.value = false; }
}

onMounted(async () => {
  if (route.query.status === 'expired') filterStatus.value = -1;
  if (authStore.isStudent) {
    if (route.query.tab === 'fav') taskTab.value = 'fav';
    try { favoriteIds.value = await getFavoriteIds() || []; } catch { favoriteIds.value = []; }
    try {
      const res = await request.get('/files/all-submissions', { params: { studentId: authStore.userId } });
      const subs = res?.records || res || [];
      submittedTaskIds.value = subs.map(s => s.taskId);
    } catch { submittedTaskIds.value = []; }
  }
  loadTasks();
});

async function toggleFav(task) {
  try {
    const res = await toggleFavorite(task.id);
    if (res?.favorited !== undefined && res.favorited) {
      favoriteIds.value.push(task.id);
    } else {
      favoriteIds.value = favoriteIds.value.filter(id => id !== task.id);
    }
    if (taskTab.value === 'fav') loadTasks();
  } catch { /* */ }
}

function resetForm() {
  Object.assign(form, { title: '', brief: '', subject: '', businessScenario: '', implConditions: '', expectedOutput: '', deadline: '' });
  uploadedDocs.value = [];
  editingId.value = null;
}

function openCreateDialog() { resetForm(); dialogVisible.value = true; }

function openEditDialog(row) {
  resetForm();
  editingId.value = row.id;
  Object.assign(form, row);
  if (row.requirementDocs) {
    try { uploadedDocs.value = JSON.parse(row.requirementDocs); } catch {}
  }
  dialogVisible.value = true;
}

async function saveTask() {
  saving.value = true;
  try {
    const data = {
      ...form,
      requirementDocs: JSON.stringify(uploadedDocs.value),
    };
    if (editingId.value) await updateTask(editingId.value, data);
    else await createTask(data);
    ElNotification({ type: 'success', title: editingId.value ? '更新成功' : '创建成功', message: '', position: 'top-right', duration: 3000 });
    dialogVisible.value = false;
    loadTasks();
  } catch { /* handled */ }
  finally { saving.value = false; }
}

async function publishTask(row) {
  await updateTask(row.id, { ...row, status: 1 });
  ElNotification({ type: 'success', title: '已发布', message: '', position: 'top-right', duration: 3000 });
  loadTasks();
}

async function deleteTask(id) {
  await ElMessageBox.confirm('确定删除该任务吗？', '提示', { type: 'warning' });
  await delTask(id);
  ElNotification({ type: 'success', title: '删除成功', message: '', position: 'top-right', duration: 3000 });
  loadTasks();
}

function beforeDocUpload(file) {
  const ext = file.name.split('.').pop().toLowerCase();
  if (!['doc','docx','pdf'].includes(ext)) { ElNotification({ type: 'error', title: '仅支持 Word/PDF', message: '', position: 'top-right', duration: 3000 }); return false; }
  return true;
}
function onDocUploaded(response) {
  if (response?.code === 200 && response.data) {
    uploadedDocs.value.push(response.data);
    ElNotification({ type: 'success', title: '文档上传成功', message: '', position: 'top-right', duration: 3000 });
  }
}
function onDocError() { ElNotification({ type: 'error', title: '文档上传失败', message: '', position: 'top-right', duration: 3000 }); }
</script>

<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.task-card { cursor:pointer; transition:transform 0.2s,box-shadow 0.2s; }
.task-card:hover { transform:translateY(-3px); }
.card-top { display:flex; align-items:center; gap:8px; margin-bottom:8px; }
.card-subject { font-size:12px; color:var(--ios-label-2); flex:1; }
.fav-btn { margin-left:auto; }
.card-title { font-size:16px; margin:0 0 8px; color:var(--ios-label); font-weight:600; }
.card-desc { font-size:13px; color:#636366; margin-bottom:12px; line-height:1.4; min-height:18px; }
.card-footer { display:flex; justify-content:space-between; align-items:center; }
.card-deadline { font-size:12px; color:var(--ios-label-2); display:flex; align-items:center; gap:4px; }
.task-form { max-height:65vh; overflow-y:auto; padding-right:8px; }
</style>
