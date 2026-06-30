<template>
  <div class="dashboard">
    <!-- 教师端：统计卡片 -->
    <template v-if="authStore.isTeacher">
      <el-row :gutter="16" class="status-row">
        <el-col :span="6">
          <el-card shadow="hover" class="status-card published" @click="$router.push('/tasks')">
            <div class="card-body">
              <div class="card-icon"><el-icon :size="32"><EditPen /></el-icon></div>
              <div class="card-info">
                <div class="card-value">{{ statusCounts.published }}</div>
                <div class="card-label">已发布任务</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="status-card completed" @click="$router.push('/tasks?status=expired')">
            <div class="card-body">
              <div class="card-icon"><el-icon :size="32"><CircleCheckFilled /></el-icon></div>
              <div class="card-info">
                <div class="card-value">{{ statusCounts.completed }}</div>
                <div class="card-label">已截止任务</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="status-card pending" @click="$router.push('/submissions?tab=pending')">
            <div class="card-body">
              <div class="card-icon"><el-icon :size="32"><Clock /></el-icon></div>
              <div class="card-info">
                <div class="card-value">{{ statusCounts.pendingReview }}</div>
                <div class="card-label">待批阅</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="status-card reviewed" style="cursor:default">
            <div class="card-body">
              <div class="card-icon"><el-icon :size="32"><Checked /></el-icon></div>
              <div class="card-info">
                <div class="card-value">{{ statusCounts.completedReview }}</div>
                <div class="card-label">已完成批阅</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 学生端：通用统计 -->
    <template v-else>
      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="8" v-for="(item, idx) in stats" :key="item.label">
          <el-card shadow="hover" class="stat-card" @click="goStat(idx)">
            <div class="stat-content">
              <div class="stat-icon" :style="{ backgroundColor: item.color }">
                <el-icon :size="28"><component :is="item.icon" /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ item.value }}</div>
                <div class="stat-label">{{ item.label }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 最近任务 + 最近提交 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="authStore.isTeacher ? 12 : 24">
        <el-card>
          <template #header><span>{{ authStore.isStudent ? '我的项目' : '最近实训任务' }}</span></template>
          <el-table :data="recentTasks" size="small" v-loading="loadingTasks" @row-click="goTaskSubmissions" style="cursor: pointer">
            <el-table-column prop="title" label="任务名称" min-width="120" />
            <el-table-column label="状态" width="85">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'info' : 'danger'" size="small">
                  {{ row.status === 1 ? '已发布' : row.status === 0 ? '草稿' : '已截止' }}
                </el-tag>
              </template>
            </el-table-column>
            <template v-if="authStore.isTeacher">
              <el-table-column label="提交" width="70">
                <template #default="{ row }">{{ row.submissionCount || 0 }}</template>
              </el-table-column>
              <el-table-column label="已批阅" width="80">
                <template #default="{ row }">{{ row.evaluatedCount || 0 }}</template>
              </el-table-column>
            </template>
            <el-table-column label="截止时间" width="150">
              <template #default="{ row }">{{ fmt(row.deadline) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12" v-if="authStore.isTeacher">
        <el-card>
          <template #header><span>最近提交成果</span></template>
          <el-table :data="recentSubmissions" size="small" v-loading="loadingSubs">
            <el-table-column prop="student_name" label="学生" width="90" />
            <el-table-column prop="task_title" label="任务" />
            <el-table-column label="评价" width="80">
              <template #default="{ row }">
                <el-tag :type="row.evaluation_status ? 'success' : 'info'" size="small">
                  {{ row.evaluation_status ? '已评价' : '未评价' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="150">
              <template #default="{ row }">{{ fmt(row.submit_time) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { getDashboardStats, getRecentTasks, getRecentSubmissions } from '../api/dashboard';
import { useFormatTime } from '../composables/useFormatTime';
import request from '../utils/request';

const router = useRouter();
const authStore = useAuthStore();
const { fmt } = useFormatTime();

const statusCounts = reactive({ published: 0, completed: 0, pendingReview: 0, completedReview: 0 });

const stats = ref([]);

function initStats() {
  if (authStore.isStudent) {
    stats.value = [
      { label: '我的项目', value: 0, icon: 'Notebook', color: '#409EFF' },
      { label: '提交成果', value: 0, icon: 'Document', color: '#67C23A' },
      { label: '已受评', value: 0, icon: 'StarFilled', color: '#F56C6C' },
    ];
  }
}
const recentTasks = ref([]);
const recentSubmissions = ref([]);
const loadingTasks = ref(false);
const loadingSubs = ref(false);

function goTaskSubmissions(row) {
  router.push(`/submissions?taskId=${row.id}`);
}

function goStat(idx) {
  if (authStore.isStudent) {
    if (idx === 0) router.push('/tasks?tab=fav');
    else if (idx === 1) router.push('/my-submissions');
    else router.push('/my-submissions?approvalStatus=2');
  }
}

async function loadStatusCounts() {
  try {
    const data = await request.get('/dashboard/status-counts');
    if (data) {
      statusCounts.published = data.published || 0;
      statusCounts.completed = data.completed || 0;
      statusCounts.pendingReview = data.pendingReview || 0;
      statusCounts.completedReview = data.completedReview || 0;
    }
  } catch { /* use defaults */ }
}

onMounted(async () => {
  initStats();
  await loadAllData();
});

async function loadAllData() {
  try {
    const statsData = await getDashboardStats();
    if (statsData) {
      if (authStore.isStudent) {
        stats.value[0].value = statsData.taskCount || 0;
        stats.value[1].value = statsData.submissionCount || 0;
        stats.value[2].value = statsData.evaluatedCount || 0;
      }
    }
  } catch { /* */ }

  if (authStore.isTeacher) await loadStatusCounts();

  loadingTasks.value = true;
  try { recentTasks.value = (await getRecentTasks()) || []; } catch { /* */ }
  finally { loadingTasks.value = false; }

  loadingSubs.value = true;
  try { recentSubmissions.value = (await getRecentSubmissions()) || []; } catch { /* */ }
  finally { loadingSubs.value = false; }
}
</script>

<style scoped>
.status-row { margin-bottom: 16px; }

.status-card { cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
.status-card:hover { transform: translateY(-3px); }

.status-card .card-body { display: flex; align-items: center; gap: 14px; }
.status-card .card-icon { width: 56px; height: 56px; border-radius: 14px; display: flex; align-items: center; justify-content: center; color: #fff; }
.status-card .card-value { font-size: 26px; font-weight: 700; color: var(--ios-label); }
.status-card .card-label { font-size: 13px; color: var(--ios-label-2); margin-top: 2px; }

.status-card.published .card-icon { background: linear-gradient(135deg, #409EFF, #337ECC); }
.status-card.completed .card-icon { background: linear-gradient(135deg, #67C23A, #529B2E); }
.status-card.pending .card-icon { background: linear-gradient(135deg, #E6A23C, #D48806); }
.status-card.reviewed .card-icon { background: linear-gradient(135deg, #909399, #606266); }

.stat-card { cursor: pointer; transition: transform 0.2s; }
.stat-card:hover { transform: translateY(-3px); }
.stat-content { display: flex; align-items: center; }
.stat-icon { width: 56px; height: 56px; border-radius: 14px; display: flex; align-items: center; justify-content: center; color: #fff; margin-right: 16px; }
.stat-value { font-size: 24px; font-weight: 600; color: var(--ios-label); }
.stat-label { font-size: 13px; color: var(--ios-label-2); margin-top: 4px; }
</style>
