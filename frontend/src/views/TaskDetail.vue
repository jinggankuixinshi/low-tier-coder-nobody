<template>
  <div class="task-detail">
    <div class="page-header">
      <el-button @click="$router.push('/tasks')"><el-icon><ArrowLeft /></el-icon> 返回任务管理</el-button>
      <h3 style="margin-left:16px">{{ task?.title || '加载中...' }}</h3>
      <div v-if="authStore.isStudent" style="margin-left:auto;display:flex;align-items:center;gap:10px">
        <el-button
          :type="isFav ? 'success' : 'primary'"
          :plain="isFav"
          @click="toggleFav"
        >{{ isFav ? '已添加到我的项目' : '添加到我的项目' }}</el-button>
        <el-button v-if="task?.status === 1" type="success" size="large" @click="$router.push(`/upload?taskId=${task.id}`)"><el-icon><UploadFilled /></el-icon> 提交项目</el-button>
      </div>
    </div>

    <el-card v-if="task" class="info-card">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="技术栈/科目">{{ task.subject || '通用' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(task.status).type" size="small">{{ statusTag(task.status).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ fmt(task.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ fmt(task.deadline) || '无限制' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="task.brief" class="section">
        <h4>任务简介</h4>
        <p class="text-block">{{ task.brief }}</p>
      </div>

      <div v-if="task.businessScenario" class="section">
        <h4>业务场景</h4>
        <p class="text-block">{{ task.businessScenario }}</p>
      </div>

      <div v-if="task.implConditions" class="section">
        <h4>实现条件</h4>
        <p class="text-block">{{ task.implConditions }}</p>
      </div>

      <div v-if="task.description" class="section">
        <h4>任务描述</h4>
        <p class="text-block">{{ task.description }}</p>
      </div>

      <div v-if="task.expectedOutput" class="section">
        <h4>预期输出/成果</h4>
        <p class="text-block">{{ task.expectedOutput }}</p>
      </div>

      <div v-if="parsedDocs.length" class="section">
        <h4>需求文档</h4>
        <div>
          <el-tag v-for="(doc, i) in parsedDocs" :key="i" size="small" type="primary" style="margin:3px">
            <el-icon style="margin-right:4px"><Document /></el-icon> {{ doc.name }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <template v-if="authStore.isTeacher">
      <el-card style="margin-top:20px">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>提交成果列表（{{ submissionCountLabel }}）</span>
            <el-button size="small" type="primary" @click="goSubmissions">
              <el-icon><List /></el-icon> 查看全部提交结果
            </el-button>
          </div>
        </template>
        <el-table :data="submissions" border stripe v-loading="subLoading" empty-text="暂无提交">
          <el-table-column prop="studentName" label="学生" width="110"/>
          <el-table-column label="文件" min-width="180">
            <template #default="{ row }">
              <template v-if="row.fileNames"><el-tag v-for="(f,i) in (Array.isArray(row.fileNames)?row.fileNames:parseJson(row.fileNames))" :key="i" size="small" style="margin:2px">{{ f }}</el-tag></template>
              <span v-else style="color:#909399">无</span>
            </template>
          </el-table-column>
          <el-table-column label="得分" width="80">
            <template #default="{ row }">
              <span v-if="row.evaluationStatus && row.totalScore != null" class="score-text">{{ row.totalScore }}</span>
              <span v-else style="color:#909399">-</span>
            </template>
          </el-table-column>
          <el-table-column label="审批" width="80">
            <template #default="{ row }"><el-tag :type="row.evaluationStatus?'success':'info'" size="small">{{ row.evaluationStatus?'已审批':'未审批' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="提交时间" width="160"><template #default="{ row }">{{ fmt(row.submitTime) }}</template></el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="viewSubmissionDetail(row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-dialog v-model="detailVisible" title="提交详情" width="700px">
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="学生">{{ detail.studentName }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">
          <el-tag :type="detail.evaluationStatus?'success':'info'" size="small">{{ detail.evaluationStatus?'已审批':'未审批' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总分">
          <span v-if="detail.evaluationStatus && detail.totalScore != null" class="detail-score">{{ detail.totalScore }}</span>
          <span v-else style="color:#909399">未审批</span>
        </el-descriptions-item>
        <el-descriptions-item label="评价模板">
          <span v-if="detail.templateName">{{ detail.templateName }}</span>
          <span v-else style="color:#909399">未评价</span>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ fmt(detail.submitTime) }}</el-descriptions-item>
        <el-descriptions-item label="文件">
          <template v-if="detail.fileNames"><el-tag v-for="(f,i) in (Array.isArray(detail.fileNames)?detail.fileNames:parseJson(detail.fileNames))" :key="i" size="small" style="margin:2px">{{ f }}</el-tag></template>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {   ElNotification   } from 'element-plus';
import { useAuthStore } from '../stores/auth';
import { toggleFavorite, getFavoriteIds } from '../api/task';
import request from '../utils/request';
import { useFormatTime } from '../composables/useFormatTime';

const { fmt } = useFormatTime();
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const taskId = ref(Number(route.params.taskId));
const task = ref(null);
const submissions = ref([]);
const submissionTotal = ref(0);
const subLoading = ref(false);
const detailVisible = ref(false);
const detail = ref(null);
const isFav = ref(false);

const parsedDocs = computed(() => {
  if (!task.value?.requirementDocs) return [];
  try { return JSON.parse(task.value.requirementDocs); } catch { return []; }
});

const submissionCountLabel = computed(() => {
  const n = submissionTotal.value;
  if (n <= 10) return `${n} 项`;
  if (n < 100) return '10+';
  if (n < 1000) return '100+';
  return '1000+';
});

function truncate(s,n) { if (!s) return ''; return s.length>n?s.substring(0,n)+'\n...':s; }
function parseJson(v) { try { return JSON.parse(v); } catch { return []; } }
const statusTag = (s)=>({0:{type:'info',text:'草稿'},1:{type:'success',text:'已发布'},2:{type:'danger',text:'已截止'}}[s]||{type:'',text:'未知'});

async function loadTask() { task.value = await request.get(`/tasks/${taskId.value}`); }

async function loadSubmissions() {
  subLoading.value = true;
  try {
    const res = await request.get('/files/all-submissions', {
      params: { taskId: taskId.value, size: 10, sortOrder: 'desc' }
    });
    const list = res?.records || res || [];
    submissionTotal.value = res?.total || list.length;
    for (const s of list) {
      if (s.evaluationStatus) {
        try {
          const results = await request.get(`/evaluation/result/${s.id}`);
          if (results && results.length) {
            let weightedSum = 0;
            let totalWeight = 0;
            results.forEach(r => {
              const score = Number(r.finalScore || r.autoScore || 0);
              const weight = Number(r.weight || 0);
              weightedSum += score * weight;
              totalWeight += weight;
            });
            s.totalScore = totalWeight > 0 ? Math.round(weightedSum / totalWeight * 100) / 100 : 0;
          }
        } catch { s.totalScore = null; }
      }
    }
    submissions.value = list;
  } catch (e) { console.error('加载提交列表失败:', e); submissions.value = []; submissionTotal.value = 0; }
  finally { subLoading.value = false; }
}

async function viewSubmissionDetail(row) {
  try {
    const sub = await request.get(`/files/submission/${row.id}`);
    if (sub) {
      detail.value = { ...sub, evaluationStatus: row.evaluationStatus, totalScore: row.totalScore };
      if (row.evaluationStatus) {
        try {
          const results = await request.get(`/evaluation/result/${row.id}`);
          if (results && results.length) {
            const templateId = results[0].templateId;
            if (templateId) {
              const templates = await request.get('/evaluation/templates');
              const tlist = templates?.records || templates || [];
              const tpl = tlist.find(t => t.id === templateId);
              detail.value.templateName = tpl ? tpl.name : null;
            }
          }
        } catch {}
      }
    }
    detailVisible.value = true;
  } catch { ElNotification({ type: 'error', title: '加载失败', message: '', position: 'top-right', duration: 3000 }); }
}

async function toggleFav() {
  try {
    const res = await toggleFavorite(taskId.value);
    if (res?.favorited !== undefined) {
      isFav.value = res.favorited;
    }
  } catch { /* */ }
}

async function checkFavStatus() {
  try {
    const ids = await getFavoriteIds() || [];
    isFav.value = ids.includes(taskId.value);
  } catch { isFav.value = false; }
}

onMounted(() => { loadTask(); if (authStore.isTeacher) loadSubmissions(); if (authStore.isStudent) checkFavStatus(); });

function goSubmissions() {
  router.push(`/submissions?taskId=${taskId.value}`);
}
</script>

<style scoped>
.page-header { display:flex; align-items:center; margin-bottom:20px; }
.info-card { margin-bottom:0; }
.section { margin-top:20px; }
.section h4 { margin-bottom:10px; color:#303133; font-size:15px; }
.text-block { white-space:pre-wrap; color:#606266; line-height:1.7; font-size:14px; }
.html-content { line-height:1.7; font-size:14px; }
.html-content :deep(h3) { margin:0 0 8px; }
.html-content :deep(h4) { margin:0 0 6px; }
.html-content :deep(p) { margin:0 0 6px; }
.html-content :deep(ol), .html-content :deep(ul) { padding-left:20px; margin-bottom:8px; }
.html-content :deep(li) { margin-bottom:4px; }
.html-content :deep(table) { border-collapse:collapse; width:100%; }
.html-content :deep(td), .html-content :deep(th) { border:1px solid #ddd; padding:6px 10px; }
.content-pre { white-space:pre-wrap; font-size:13px; max-height:400px; overflow-y:auto; background:var(--ios-fill); padding:12px; border-radius:10px; }
.score-text { font-weight:600; color:#409EFF; font-size:15px; }
.detail-score { font-weight:700; color:#67C23A; font-size:18px; }
</style>
