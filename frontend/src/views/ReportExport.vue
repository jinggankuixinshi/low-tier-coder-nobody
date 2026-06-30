<template>
  <div class="reports">
    <div class="page-header">
      <h3>报表生成与导出</h3>
    </div>

    <template v-if="authStore.isTeacher">
      <!-- 任务选择 -->
      <el-card style="margin-bottom: 20px">
        <template #header><span>选择任务</span></template>
        <el-form inline>
          <el-form-item label="实训任务">
            <el-select v-model="selectedTaskId" placeholder="请选择任务" style="width: 400px"
              filterable @change="onTaskChange">
              <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>

      <template v-if="selectedTaskId">
        <el-row :gutter="16">
          <!-- 左侧：已审批提交列表 -->
          <el-col :span="14">
            <el-card v-loading="submissionsLoading">
              <template #header><span>已审批提交列表</span></template>
              <div class="search-bar">
                <el-input v-model="searchKeyword" placeholder="搜索账号名或学生姓名" clearable
                  style="width:260px" @clear="searchKeyword = ''" @input="searchKeyword = $event.target.value">
                  <template #prefix><el-icon><Search /></el-icon></template>
                </el-input>
                <el-button type="primary" :disabled="selectedSubIds.length === 0"
                  @click="batchExportHandler" :loading="exporting">
                  批量导出选中 ({{ selectedSubIds.length }})
                </el-button>
              </div>
              <el-table :data="filteredSubmissions" border stripe @selection-change="onSelectionChange" ref="subTable" style="width:100%">
                <el-table-column type="selection" width="45" align="center" />
                <el-table-column label="账号名" min-width="100">
                  <template #default="{ row }">{{ row.username || '-' }}</template>
                </el-table-column>
                <el-table-column label="学生" min-width="100">
                  <template #default="{ row }">{{ row.studentName || '-' }}</template>
                </el-table-column>
                <el-table-column label="审批状态" width="85" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.approvalStatus === 2 ? 'success' : 'warning'" size="small">
                      {{ row.approvalStatus === 2 ? '已审批' : '草稿' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="提交时间" width="170" align="left">
                  <template #default="{ row }">{{ fmtTime(row.submitTime) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" @click="viewReport(row.id)"><el-icon><View /></el-icon> 查看报表</el-button>
                    <el-button size="small" type="success" @click="exportSingle(row)"><el-icon><Download /></el-icon> 导出</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <!-- 右侧：任务总览 -->
          <el-col :span="10">
            <el-card v-loading="overviewLoading">
              <template #header><span>任务总览报表</span></template>

              <!-- blocked -->
              <div v-if="overviewStatus?.status === 'blocked'" class="overview-blocked">
                <el-alert :title="overviewStatus.message" type="warning" :closable="false" show-icon />
                <div class="overview-progress">
                  <span class="progress-label">
                    已审批 <b>{{ overviewStatus.approvedCount }}</b> / {{ overviewStatus.totalSubmissions }}
                  </span>
                  <el-progress
                    :percentage="Math.round((overviewStatus.approvedCount / overviewStatus.totalSubmissions) * 100)"
                    :stroke-width="8" status="warning" :show-text="false"
                  />
                  <p class="progress-hint">待全部提交审批通过后，可生成任务总览报表</p>
                </div>
              </div>

              <!-- ready -->
              <div v-else-if="overviewStatus?.status === 'ready'" class="overview-ready">
                <div class="status-badge status-badge--success">
                  <el-icon><CircleCheck /></el-icon>
                  <span>全部审批通过</span>
                </div>
                <p class="status-desc">
                  已审批 <b>{{ overviewStatus.approvedCount }}</b>/{{ overviewStatus.totalSubmissions }}，可以生成总览报表
                </p>
                <div class="overview-actions">
                  <el-button type="primary" @click="viewOverview">查看总览报表</el-button>
                  <el-button type="success" @click="handleOverviewExport">导出 PDF</el-button>
                </div>
              </div>

              <!-- unchanged -->
              <div v-else-if="overviewStatus?.status === 'unchanged'" class="overview-unchanged">
                <template v-if="overviewData">
                  <p class="status-summary">
                    已审批 <b>{{ overviewData.approvedCount }}</b>/{{ overviewData.totalSubmissions }}，报表已是最新
                  </p>
                  <div class="stat-mini-row">
                    <div class="stat-mini" v-if="overviewData.avgScore != null">
                      <span class="stat-mini-icon" style="background:#e6f4ff;color:#1677ff">
                        <el-icon><TrendCharts /></el-icon>
                      </span>
                      <div>
                        <span class="stat-mini-label">平均分</span>
                        <span class="stat-mini-value">{{ overviewData.avgScore.toFixed(1) }}</span>
                      </div>
                    </div>
                    <div class="stat-mini" v-if="overviewData.maxScore != null">
                      <span class="stat-mini-icon" style="background:#e6fffb;color:#13c2c2">
                        <el-icon><Top /></el-icon>
                      </span>
                      <div>
                        <span class="stat-mini-label">最高分</span>
                        <span class="stat-mini-value">{{ overviewData.maxScore.toFixed(1) }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="grade-dist">
                    <span class="grade-label">成绩分布</span>
                    <el-tag type="success" size="small" effect="plain">优秀 {{ overviewData.gradeCounts?.excellent || 0 }}</el-tag>
                    <el-tag size="small" effect="plain">良好 {{ overviewData.gradeCounts?.good || 0 }}</el-tag>
                    <el-tag type="warning" size="small" effect="plain">中等 {{ overviewData.gradeCounts?.medium || 0 }}</el-tag>
                    <el-tag type="danger" size="small" effect="plain">不及格 {{ overviewData.gradeCounts?.fail || 0 }}</el-tag>
                  </div>
                </template>
                <div class="overview-actions">
                  <el-button type="primary" @click="viewOverview">查看总览报表</el-button>
                  <el-button type="success" @click="handleOverviewExport">导出 PDF</el-button>
                </div>
              </div>

              <!-- changed -->
              <div v-else-if="overviewStatus?.status === 'changed'" class="overview-changed">
                <div class="status-badge status-badge--warning">
                  <el-icon><Warning /></el-icon>
                  <span>数据已变更，需要重新生成</span>
                </div>
                <el-alert :title="overviewStatus.message" type="info" :closable="false" show-icon />
                <div class="overview-actions">
                  <el-button type="primary" @click="viewOverview">查看总览报表（重新生成）</el-button>
                  <el-button type="success" @click="handleOverviewExport">导出 PDF（重新生成）</el-button>
                </div>
              </div>

              <!-- loading / no data -->
              <div v-else class="overview-empty">
                <p>请先选择任务</p>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </template>

    <el-empty v-if="!authStore.isTeacher" description="仅教师角色可访问报表功能" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElNotification, ElMessageBox } from 'element-plus'
import { Search, CircleCheck, TrendCharts, Top, Warning } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'
import {
  getApprovedSubmissions, exportReport, batchExport,
  getOverviewStatus, getOverviewData, exportOverview
} from '../api/report'
import type { OverviewStatus, ReportOverviewVO } from '@/types/report'
import { useDownload } from '../composables/useDownload'
import '../styles/report-font.css'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { downloadBlob, sanitizeFilename, fmtTime } = useDownload()

const tasks = ref<any[]>([])
const selectedTaskId = ref<number | null>(null)
const approvedSubmissions = ref<any[]>([])
const submissionsLoading = ref(false)
const selectedSubIds = ref<number[]>([])
const exporting = ref(false)
const searchKeyword = ref('')
const overviewLoading = ref(false)
const overviewStatus = ref<OverviewStatus | null>(null)
const overviewData = ref<ReportOverviewVO | null>(null)

const selectedTaskTitle = computed(() =>
  tasks.value.find((t: any) => t.id === selectedTaskId.value)?.title || '任务')

function syncFilterToUrl() {
  const query: Record<string, string> = {}
  if (selectedTaskId.value) query.taskId = String(selectedTaskId.value)
  if (searchKeyword.value) query.keyword = searchKeyword.value
  router.replace({ query })
}

watch([selectedTaskId, searchKeyword], syncFilterToUrl)

const filteredSubmissions = computed(() => {
  if (!searchKeyword.value) return approvedSubmissions.value
  const kw = searchKeyword.value.toLowerCase()
  return approvedSubmissions.value.filter((s: any) =>
    (s.username || '').toLowerCase().includes(kw) ||
    (s.studentName || '').toLowerCase().includes(kw)
  )
})

onMounted(async () => {
  const tasksRes: any = await request.get('/tasks/search') || []
  tasks.value = (Array.isArray(tasksRes) ? tasksRes : []).filter((t: any) => t.status !== 0)

  const qTaskId = route.query.taskId ? Number(route.query.taskId) : null
  const qKeyword = (route.query.keyword as string) || ''
  if (qTaskId && tasks.value.some((t: any) => t.id === qTaskId)) {
    selectedTaskId.value = qTaskId
    await onTaskChange()
    searchKeyword.value = qKeyword
  }
})

async function onTaskChange() {
  selectedSubIds.value = []
  searchKeyword.value = ''
  overviewStatus.value = null
  overviewData.value = null
  if (!selectedTaskId.value) return

  submissionsLoading.value = true
  try {
    const res: any = await getApprovedSubmissions(selectedTaskId.value)
    const data = res?.data || res
    approvedSubmissions.value = data?.records || []
  } catch { approvedSubmissions.value = [] }
  finally { submissionsLoading.value = false }

  await refreshOverview()
}

async function refreshOverview() {
  if (!selectedTaskId.value) return
  overviewLoading.value = true
  try {
    const status: any = await getOverviewStatus(selectedTaskId.value)
    overviewStatus.value = status?.data || status
    if (overviewStatus.value?.status === 'unchanged') {
      const od: any = await getOverviewData(selectedTaskId.value)
      overviewData.value = od?.data || od
    }
  } catch { overviewStatus.value = null }
  finally { overviewLoading.value = false }
}

function onSelectionChange(rows: any[]) {
  selectedSubIds.value = rows.map(r => r.id)
}

function viewReport(submissionId: number) {
  router.push(`/report-view/${submissionId}`)
}

async function exportSingle(row: any) {
  try {
    const blob = await exportReport(row.id)
    const name = sanitizeFilename(`${row.studentName || '未知'}_${selectedTaskTitle.value}_评价报告`) + '.pdf'
    downloadBlob(blob, name)
    ElNotification({ type: 'success', title: '导出成功', message: '', position: 'top-right', duration: 3000 })
  } catch { ElNotification({ type: 'error', title: '导出失败', message: '', position: 'top-right', duration: 3000 }) }
}

async function batchExportHandler() {
  if (selectedSubIds.value.length === 0) {
    ElNotification({ type: 'warning', title: '请选择至少一个提交', message: '', position: 'top-right', duration: 3000 })
    return
  }
  try {
    await ElMessageBox.confirm(
      `将按顺序生成并导出 ${selectedSubIds.value.length} 份报表为 ZIP 文件，耗时可能较长，请勿关闭或刷新页面。`,
      '确认批量导出', { type: 'warning', confirmButtonText: '开始导出', cancelButtonText: '取消' }
    )
  } catch { return }

  exporting.value = true
  try {
    const blob = await batchExport(selectedSubIds.value)
    downloadBlob(blob, '评价报告批量导出.zip')
    ElNotification({ type: 'success', title: '批量导出完成', message: '', position: 'top-right', duration: 3000 })
  } catch {
    ElNotification({ type: 'error', title: '批量导出失败', message: '', position: 'top-right', duration: 3000 })
  } finally { exporting.value = false }
}

async function handleOverviewView() {
  await refreshOverview()
}

function viewOverview() {
  if (!selectedTaskId.value) return
  router.push(`/report-view/0?type=overview&taskId=${selectedTaskId.value}`)
}

async function handleOverviewExport() {
  if (!selectedTaskId.value) return
  try {
    const blob = await exportOverview(selectedTaskId.value)
    downloadBlob(blob, sanitizeFilename(`${selectedTaskTitle.value}_任务总览报表`) + '.pdf')
    await refreshOverview()
    ElNotification({ type: 'success', title: '总览报表导出成功', message: '', position: 'top-right', duration: 3000 })
  } catch (e: any) {
    ElNotification({ type: 'error', title: e?.message || '导出失败', message: '', position: 'top-right', duration: 3000 })
  }
}

</script>

<style scoped>
.reports { font-family: 'Noto Sans CJK SC', 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif; }
.page-header { margin-bottom: 20px; }

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

/* ---- 任务总览卡片 ---- */

.overview-blocked,
.overview-ready,
.overview-unchanged,
.overview-changed {
  min-height: 140px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.overview-progress {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.progress-label { font-size: 13px; color: #606266; }
.progress-label b { color: #303133; }
.progress-hint { margin: 0; font-size: 12px; color: #909399; }

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 8px;
  width: fit-content;
}
.status-badge--success { background: #edfaf2; color: #52c41a; }
.status-badge--warning { background: #fff7e6; color: #fa8c16; }

.status-desc { margin: 0; font-size: 13px; color: #606266; }
.status-desc b { color: #303133; }

.status-summary { margin: 0; font-size: 13px; color: #606266; }
.status-summary b { color: #303133; }

.stat-mini-row {
  display: flex;
  gap: 12px;
}
.stat-mini {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--ios-fill);
  border-radius: 12px;
}
.stat-mini-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  font-size: 16px;
}
.stat-mini-label { display: block; font-size: 11px; color: var(--ios-label-2); }
.stat-mini-value { display: block; font-size: 18px; font-weight: 700; color: var(--ios-label); }

.grade-dist {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 13px;
}
.grade-label { color: #909399; margin-right: 2px; }

.overview-actions { display: flex; gap: 8px; margin-top: 4px; }

.overview-empty {
  text-align: center;
  padding: 24px;
  color: #909399;
  font-size: 13px;
}
.overview-empty p { margin: 0; }
</style>
