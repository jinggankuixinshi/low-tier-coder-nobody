<template>
  <div class="my-submissions">
    <div class="page-header">
      <h3>我的提交</h3>
    </div>

    <el-card style="margin-bottom:16px">
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input v-model="searchKeyword" placeholder="搜索任务名称" clearable style="width:280px" @clear="searchKeyword = ''">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="排列">
          <el-radio-group v-model="sortOrder" size="small">
            <el-radio-button value="desc">倒序 ↓</el-radio-button>
            <el-radio-button value="asc">正序 ↑</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="approvalStatus" placeholder="全部" clearable style="width:140px">
            <el-option label="已审批" :value="2"/>
            <el-option label="未审批" :value="-1"/>
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="filteredSubmissions" border stripe v-loading="loading" empty-text="暂无提交记录">
        <el-table-column prop="id" label="ID" width="65" />
        <el-table-column prop="taskName" label="所属任务" min-width="160" />
        <el-table-column label="提交文件" min-width="240">
          <template #default="{ row }">
            <FileTagsDisplay :file-names="row.fileNames" :file-sizes="row.fileSizes" :max-tags="3" />
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="110">
          <template #default="{ row }">{{ formatTotalSize(row.fileSizes) }}</template>
        </el-table-column>
        <el-table-column label="审批状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.approvalStatus === 2" type="success" size="small">已审批</el-tag>
            <el-tag v-else type="info" size="small">未审批</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmt(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="ops-cell">
              <el-button size="small" type="primary" @click="goDetail(row)">查看详情</el-button>
              <el-button size="small" type="warning" @click="viewEvaluation(row)">查看评价</el-button>
              <el-button size="small" type="success" @click="handleExport(row)">
                <el-icon><Download /></el-icon>导出报表
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElNotification } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import request from '../utils/request';
import { exportReport } from '../api/report';
import { useFormatTime } from '../composables/useFormatTime';
import { useFormatSize } from '../composables/useFormatSize'
import { useDownload } from '../composables/useDownload'
import { useAuthStore } from '../stores/auth'
import FileTagsDisplay from '../components/common/FileTagsDisplay.vue'

const { fmt } = useFormatTime();
const { formatTotalSize } = useFormatSize()
const { downloadBlob, sanitizeFilename } = useDownload()
const authStore = useAuthStore()
const router = useRouter();
const route = useRoute();
const submissions = ref([]);
const loading = ref(false);
const searchKeyword = ref('');
const sortOrder = ref('desc');
const approvalStatus = ref<number | null>(null);

const filteredSubmissions = computed(() => {
  let list = [...submissions.value];
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase();
    list = list.filter(s => (s.taskName || '').toLowerCase().includes(kw));
  }
  if (approvalStatus.value != null) {
    if (approvalStatus.value === -1) {
      list = list.filter(s => s.approvalStatus !== 2);
    } else {
      list = list.filter(s => s.approvalStatus === approvalStatus.value);
    }
  }
  if (sortOrder.value === 'asc') list.reverse();
  return list;
});

onMounted(() => {
  const q = route.query;
  if (q.keyword !== undefined) searchKeyword.value = String(q.keyword);
  if (q.sortOrder === 'asc' || q.sortOrder === 'desc') sortOrder.value = q.sortOrder;
  if (q.approvalStatus !== undefined && q.approvalStatus !== '') approvalStatus.value = Number(q.approvalStatus);
  loadSubmissions();
});

watch([searchKeyword, sortOrder, approvalStatus], () => {
  const query: Record<string, string> = {};
  if (searchKeyword.value) query.keyword = searchKeyword.value;
  if (sortOrder.value && sortOrder.value !== 'desc') query.sortOrder = sortOrder.value;
  if (approvalStatus.value != null) query.approvalStatus = String(approvalStatus.value);
  router.replace({ query });
});

async function loadSubmissions() {
  loading.value = true;
  try {
    const res = await request.get('/files/all-submissions', {
      params: { size: 100 }
    });
    submissions.value = res?.records || res || [];
  } catch (e) {
    console.error('加载我的提交失败:', e);
    submissions.value = [];
  } finally { loading.value = false; }
}

function viewEvaluation(row) {
  router.push(`/report-view/${row.id}`);
}

function goDetail(row) {
  router.push(`/tasks/${row.taskId}`);
}

async function handleExport(row) {
  try {
    const blob = await exportReport(row.id);
    const studentName = authStore.realName || authStore.username || '未知';
    const fileName = sanitizeFilename(`${studentName}_${row.taskName || '报告'}_评价报告`) + '.pdf';
    downloadBlob(blob, fileName);
    ElNotification({ type: 'success', title: '导出成功', message: '', position: 'top-right', duration: 3000 });
  } catch {
    ElNotification({ type: 'error', title: '导出失败', message: '', position: 'top-right', duration: 3000 });
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.ops-cell { display: flex; gap: 6px; flex-wrap: nowrap; }
</style>
