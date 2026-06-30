<template>
  <div class="submission-result">
    <div class="page-header">
      <h3>提交结果</h3>
    </div>

    <el-card style="margin-bottom:16px">
      <el-form :inline="true">
        <el-form-item label="分类">
          <el-radio-group v-model="activeTab" @change="onTabChange">
            <el-radio-button value="all">全部提交</el-radio-button>
            <el-radio-button value="pending">待批阅</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="筛选任务">
          <el-select v-model="filterTaskId" placeholder="全部任务" clearable style="width: 300px" filterable @change="onFilterChange">
            <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排列">
          <el-radio-group v-model="sortOrder" @change="onSortChange" size="small">
            <el-radio-button value="asc">正序 ↑</el-radio-button>
            <el-radio-button value="desc">倒序 ↓</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="submissions" border stripe v-loading="loading" empty-text="暂无提交记录">
        <el-table-column prop="id" label="ID" width="65" />
        <el-table-column prop="studentName" label="学生" width="110" />
        <el-table-column prop="taskName" label="所属任务" min-width="160" />
        <el-table-column label="提交文件" min-width="260">
          <template #default="{ row }">
            <FileTagsDisplay :file-names="row.fileNames" :file-sizes="row.fileSizes" :max-tags="3" />
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="110">
          <template #default="{ row }">{{ formatTotalSize(row.fileSizes) }}</template>
        </el-table-column>
        <el-table-column label="审批状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.approvalStatus === 2 || row.submitted === 1" type="success" size="small">已审批</el-tag>
            <el-tag v-else-if="row.approvalStatus === 1" type="warning" size="small">已保存</el-tag>
            <el-tag v-else type="info" size="small">未审批</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmt(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="goDetail(row)">查看详情</el-button>
            <el-button size="small" type="success" @click="downloadFiles(row)" :disabled="!row.filePaths">
              <el-icon><Download /></el-icon> 下载
            </el-button>
            <el-button v-if="row.approvalStatus === 2 || row.submitted === 1" size="small" type="info" @click="viewEvaluation(row)">
              <el-icon><View /></el-icon> 查看评价
            </el-button>
            <el-button v-else size="small" type="warning" @click="goEvaluation(row)">
              <el-icon><StarFilled /></el-icon> 去评价
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <CustomPagination :current-page="currentPage" :total-pages="totalPages" :total="total" @page-change="(p: number) => { goPage(p); loadSubmissions(); }" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {   ElNotification   } from 'element-plus';

import { downloadSubmissionZip } from '../api/file';
import request from '../utils/request';
import { useFormatTime } from '../composables/useFormatTime';
import { useFormatSize } from '../composables/useFormatSize'
import { useDownload } from '../composables/useDownload'
import { usePagination } from '../composables/usePagination'
import CustomPagination from '../components/common/CustomPagination.vue'
import FileTagsDisplay from '../components/common/FileTagsDisplay.vue'

const { fmt } = useFormatTime();
const { formatTotalSize } = useFormatSize()
const { downloadBlob } = useDownload()
const route = useRoute();
const router = useRouter();
const tasks = ref([]);
const submissions = ref([]);
const filterTaskId = ref(null);
const loading = ref(false);
const activeTab = ref('all');
const sortOrder = ref('desc');
const { currentPage, pageSize, total, totalPages, visiblePages, goPage } = usePagination(14)

onMounted(async () => {
  const tasksRes = await request.get('/tasks/search') || [];
  tasks.value = (Array.isArray(tasksRes) ? tasksRes : []).filter(t => t.status !== 0);
  parseQueryParams();
  loadSubmissions();
});

watch(() => route.query, () => {
  parseQueryParams();
  loadSubmissions();
});

function parseQueryParams() {
  const tid = route.query.taskId;
  if (tid) filterTaskId.value = Number(tid);
  if (route.query.tab === 'pending') activeTab.value = 'pending';
}

function onTabChange() {
  filterTaskId.value = null;
  currentPage.value = 1;
  loadSubmissions();
}

function onFilterChange() {
  currentPage.value = 1;
  loadSubmissions();
}

function onSortChange() {
  currentPage.value = 1;
  loadSubmissions();
}

async function loadSubmissions() {
  loading.value = true;
  try {
    const params = { page: currentPage.value, size: pageSize.value, sortOrder: sortOrder.value };
    if (activeTab.value === 'pending') params.pendingOnly = true;
    if (filterTaskId.value) params.taskId = filterTaskId.value;
    const res = await request.get('/files/all-submissions', { params });
    const records = res?.records || res || [];
    total.value = res?.total || records.length;
    submissions.value = records;
    submissions.value.forEach(s => {
      const t = tasks.value.find(t => t.id === s.taskId);
      s.taskName = t ? t.title : '未知任务';
    });
  } catch (e) {
    console.error('加载提交列表失败:', e);
    submissions.value = [];
  } finally { loading.value = false; }
}

function goDetail(row) {
  router.push(`/tasks/${row.taskId}`);
}

function goEvaluation(row) {
  router.push(`/evaluation?evalSub=${row.id}&evalTask=${row.taskId}&evalName=${encodeURIComponent(row.studentName || '')}&evalTaskName=${encodeURIComponent(row.taskName || '')}`);
}

function viewEvaluation(row) {
  router.push(`/evaluation?evalView=${row.id}&evalTask=${row.taskId}&evalName=${encodeURIComponent(row.studentName || '')}&evalTaskName=${encodeURIComponent(row.taskName || '')}`);
}

async function downloadFiles(row) {
  try {
    const blob = await downloadSubmissionZip(row.id);
    const studentName = row.studentName || '学生';
    const fileName = `${studentName}_提交文件.zip`;
    downloadBlob(blob, fileName);
    ElNotification({ type: 'success', title: '文件下载中...', message: '', position: 'top-right', duration: 3000 });
  } catch {
    ElNotification({ type: 'error', title: '文件下载失败', message: '', position: 'top-right', duration: 3000 });
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.file-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 2px; }
.file-count { font-size: 12px; color: #909399; margin-left: 6px; }
.pagination-wrap {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  margin-top: 20px; flex-wrap: wrap;
}
.pagination-wrap .el-button + .el-button { margin-left: 0; }
.page-info { font-size: 13px; color: #909399; margin-left: 16px; }
</style>
