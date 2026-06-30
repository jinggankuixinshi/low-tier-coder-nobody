<template>
  <div class="report-view" v-loading="loading">
    <div class="page-header">
      <el-button @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h3>评价报告详情</h3>
    </div>

    <el-empty v-if="!loading && !data && !overviewData" description="报表数据加载失败" />

    <!-- Overview Report -->
    <div v-if="isOverview && overviewData" class="report-body">
      <div class="report-section header-section">
        <h1>{{ overviewData.task.title }} 任务总览报表</h1>
        <div class="meta-row">
          <span><b>日期：</b>{{ overviewData.date }}</span>
          <span><b>提交数：</b>{{ overviewData.studentStats.length }}</span>
        </div>
      </div>
      <div class="report-section">
        <h3 class="section-title">一、基本统计</h3>
        <div style="display:flex;gap:32px;padding:12px 0;font-size:14px;flex-wrap:wrap">
          <span>提交团队数：<b>{{ overviewData.studentStats.length }}</b></span>
          <span v-if="overviewData.avgScore != null">平均分：<b>{{ overviewData.avgScore.toFixed(1) }}</b></span>
          <span v-if="overviewData.maxScore != null">最高分：<b>{{ overviewData.maxScore.toFixed(1) }}</b></span>
          <span v-if="overviewData.minScore != null">最低分：<b>{{ overviewData.minScore.toFixed(1) }}</b></span>
        </div>
      </div>
      <div class="report-section">
        <h3 class="section-title">二、成绩分布</h3>
        <div ref="overviewPieRef" style="width:100%;height:300px"></div>
      </div>
      <div class="report-section">
        <h3 class="section-title">三、各维度平均得分</h3>
        <div ref="overviewBarRef" style="width:100%;height:260px"></div>
      </div>
      <div class="report-section">
        <h3 class="section-title">四、学生成绩明细</h3>
        <el-table :data="overviewData.studentStats" border size="small">
          <el-table-column prop="studentName" label="学生" min-width="120" />
          <el-table-column label="总分" width="100" align="center">
            <template #default="{ row }">{{ row.totalScore != null ? row.totalScore : '-' }}</template>
          </el-table-column>
          <el-table-column label="等级" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="gradeType(row.grade)" size="small">{{ row.grade }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">{{ fmt(row.submitTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <p class="report-footer">由实训成果智能评价系统自动生成</p>
    </div>

    <div v-if="data" class="report-body">
      <!-- Header -->
      <div class="report-section header-section">
        <h1>软件实训成果评价报告</h1>
        <div class="meta-row">
          <span><b>姓名：</b>{{ data.student.name }}</span>
          <span v-if="data.student.username"><b>账号：</b>{{ data.student.username }}</span>
          <span><b>任务：</b>{{ data.task.title }}</span>
          <span v-if="data.task.courseName"><b>课程：</b>{{ data.task.courseName }}</span>
          <span><b>日期：</b>{{ data.date }}</span>
          <span><b>方式：</b>{{ evalMethodText(data.template.evalMethod) }}</span>
        </div>
      </div>

      <!-- 一、评分总览 -->
      <div class="report-section">
        <h3 class="section-title">一、评分总览</h3>
        <div class="overview-row">
          <div class="gauge-box" ref="gaugeRef" style="width:200px;height:200px"></div>
          <el-table :data="scoreTableData" border size="small" style="flex:1">
            <el-table-column prop="dim" label="评价维度" />
            <el-table-column prop="score" label="维度得分" width="100" align="center" />
            <el-table-column prop="weight" label="权重" width="80" align="center" />
            <el-table-column prop="weighted" label="加权得分" width="100" align="center" />
          </el-table>
          <div class="total-box">
            <div class="total-score">{{ data.summary.totalScore }}</div>
            <el-tag :type="gradeType(data.summary.grade)" size="large" effect="dark">
              {{ data.summary.grade }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 二、维度雷达图 -->
      <div class="report-section">
        <h3 class="section-title">二、维度雷达图</h3>
        <div ref="radarRef" style="width:100%;height:350px"></div>
      </div>

      <!-- 三~六、各维度指标评分 -->
      <div v-for="dim in dims" :key="dim.key" class="report-section">
        <h3 class="section-title">{{ dim.title }}</h3>
        <div class="dim-weight">权重合计 {{ dimPct(dim.key) }}%</div>
        <el-table :data="dimResults(dim.key)" border size="small">
          <el-table-column prop="indicatorName" label="指标" min-width="160" />
          <el-table-column label="权重" width="75" align="center">
            <template #default="{ row }">{{ (row.weight || 0).toFixed(0) }}%</template>
          </el-table-column>
          <el-table-column label="AI评分" width="90" align="center">
            <template #default="{ row }">
              <span v-if="row.autoScore != null" :style="{color:row.autoScore>=60?'#67c23a':'#f56c6c'}">
                {{ row.autoScore }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="教师评分" width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.manualScore != null" :style="{color:row.manualScore>=60?'#409eff':'#f56c6c'}">
                {{ row.manualScore }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="最终得分" width="100" align="center">
            <template #default="{ row }">
              <b :style="{color:row.finalScore>=60?'#409eff':'#f56c6c'}">
                {{ row.finalScore != null ? row.finalScore : '-' }}
              </b>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 七、教师主观评分 -->
      <div v-if="(data.subjectiveScore != null && data.subjectiveScore !== 0) || data.subjectiveReason" class="report-section">
        <h3 class="section-title">七、教师主观评分</h3>
        <div class="subjective-box">
          <span class="subjective-label">主观评分：</span>
          <span class="subjective-val" :style="{ color: (data.subjectiveScore || 0) < 0 ? '#f56c6c' : '#1a6fb5' }">
            {{ (data.subjectiveScore || 0) > 0 ? '+' : '' }}{{ data.subjectiveScore || 0 }} 分
          </span>
          <div v-if="data.subjectiveReason" class="subjective-reason"><b>原因：</b>{{ data.subjectiveReason }}</div>
        </div>
      </div>

      <!-- 八、教师综合评价 -->
      <div v-if="data.teacherComment" class="report-section">
        <h3 class="section-title">八、教师综合评价</h3>
        <div class="comment-box">{{ data.teacherComment }}</div>
      </div>

      <!-- 九、问题与改进建议 -->
      <div v-if="issueGroups.length" class="report-section">
        <h3 class="section-title">九、存在问题与改进建议</h3>
        <div v-for="g in issueGroups" :key="g.dim" class="issue-group">
          <h4>{{ dimLabel(g.dim) }}</h4>
          <div v-for="(txt,i) in g.items" :key="i" class="issue-item">{{ txt }}</div>
        </div>
      </div>

      <p class="report-footer">由实训成果智能评价系统自动生成</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElNotification } from 'element-plus'
import * as echarts from 'echarts'
import { getReportData, getOverviewData } from '../api/report'
import { useFormatTime } from '../composables/useFormatTime'
import type { ReportDataVO, ReportOverviewVO } from '@/types/report'
import '../styles/report-font.css'

const { fmt } = useFormatTime()

const CHART_FONT = 'Noto Sans CJK SC, sans-serif'

const route = useRoute()
const isOverview = computed(() => route.query.type === 'overview')
const data = ref<ReportDataVO | null>(null)
const overviewData = ref<ReportOverviewVO | null>(null)
const loading = ref(false)
const gaugeRef = ref<HTMLElement | null>(null)
const radarRef = ref<HTMLElement | null>(null)
const overviewPieRef = ref<HTMLElement | null>(null)
const overviewBarRef = ref<HTMLElement | null>(null)
let gaugeChart: echarts.ECharts | null = null
let radarChart: echarts.ECharts | null = null
let overviewPieChart: echarts.ECharts | null = null
let overviewBarChart: echarts.ECharts | null = null

const dims = [
  { key: 'completion', title: '三、完成度评价' },
  { key: 'tech', title: '四、技术质量评价' },
  { key: 'innovation', title: '五、创新评价' },
  { key: 'document', title: '六、文档评价' },
]
const dimLabelMap: Record<string, string> = { completion: '完成度', tech: '技术质量', innovation: '创新', document: '文档' }

const scoreTableData = ref<Array<{ dim:string; score:string; weight:string; weighted:string }>>([])
const issueGroups = ref<Array<{ dim:string; items:string[] }>>([])

function dimPct(dim: string) {
  if (!data.value) return 0
  const tpl = data.value.template
  const w = (tpl as any)['weight' + dim.charAt(0).toUpperCase() + dim.slice(1)] as number
  return w != null ? (w * 100).toFixed(0) : '25'
}

function dimResults(dim: string) {
  return (data.value?.results || []).filter(r => r.dimension === dim)
}

function dimLabel(dim: string) { return dimLabelMap[dim] || dim }
function evalMethodText(m: number) {
  return ['纯AI评价', '纯人工评价', '混合AI+人工评价', '逐项自定义评价'][m] || '混合评价'
}
function gradeType(g: string) {
  if (g === '优秀') return 'success'
  if (g === '良好') return ''
  if (g === '中等') return 'warning'
  return 'danger'
}

function buildIssues() {
  if (!data.value) return
  const map: Record<string, string[]> = {}
  data.value.results.forEach(r => {
    if (!r.dimension || r.dimension === 'precheck') return
    const parts: string[] = []
    if (r.autoComment) parts.push(r.autoComment)
    if (r.manualComment) parts.push(r.manualComment)
    if (r.adjustReason) parts.push(r.adjustReason)
    if (parts.length) {
      if (!map[r.dimension]) map[r.dimension] = []
      map[r.dimension].push(...parts)
    }
  })
  issueGroups.value = Object.entries(map).map(([dim, items]) => ({ dim, items }))
}

function buildScoreTable() {
  if (!data.value) return
  const s = data.value.summary
  const t = data.value.template
  const wComp = (t.weightCompletion != null ? t.weightCompletion : 0.25) as number
  const wTech = (t.weightTech != null ? t.weightTech : 0.25) as number
  const wInno = (t.weightInnovation != null ? t.weightInnovation : 0.25) as number
  const wDoc  = (t.weightDocument != null ? t.weightDocument : 0.25) as number
  scoreTableData.value = [
    { dim: '完成度', score: s.completionScore != null ? String(s.completionScore) : '-', weight: wComp.toFixed(2), weighted: s.completionScore != null ? (s.completionScore * wComp).toFixed(2) : '-' },
    { dim: '技术质量', score: s.techScore != null ? String(s.techScore) : '-', weight: wTech.toFixed(2), weighted: s.techScore != null ? (s.techScore * wTech).toFixed(2) : '-' },
    { dim: '创新', score: s.innovationScore != null ? String(s.innovationScore) : '-', weight: wInno.toFixed(2), weighted: s.innovationScore != null ? (s.innovationScore * wInno).toFixed(2) : '-' },
    { dim: '文档', score: s.documentScore != null ? String(s.documentScore) : '-', weight: wDoc.toFixed(2), weighted: s.documentScore != null ? (s.documentScore * wDoc).toFixed(2) : '-' },
  ]
}

function renderOverviewCharts() {
  const od = overviewData.value
  if (!od) return

  if (overviewPieRef.value) {
    if (overviewPieChart) overviewPieChart.dispose()
    overviewPieChart = echarts.init(overviewPieRef.value)
    const gc = od.gradeCounts
    const pieData: any[] = []
    if (gc.excellent) pieData.push({ value: gc.excellent, name: '优秀', itemStyle: { color: '#67c23a' } })
    if (gc.good) pieData.push({ value: gc.good, name: '良好', itemStyle: { color: '#409eff' } })
    if (gc.medium) pieData.push({ value: gc.medium, name: '中等', itemStyle: { color: '#e6a23c' } })
    if (gc.fail) pieData.push({ value: gc.fail, name: '不及格', itemStyle: { color: '#f56c6c' } })
    overviewPieChart.setOption({
      textStyle: { fontFamily: CHART_FONT },
      tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
      legend: { bottom: 8, data: pieData.map(d => d.name) },
      series: [{
        type: 'pie', radius: ['45%', '70%'], center: ['50%', '48%'],
        label: { formatter: '{b}\n{d}%', fontSize: 12 },
        labelLine: { length: 16, length2: 20 },
        data: pieData
      }]
    })
  }

  if (overviewBarRef.value) {
    if (overviewBarChart) overviewBarChart.dispose()
    overviewBarChart = echarts.init(overviewBarRef.value)
    const ds = od.dimScores
    const dims: [string, string, string][] = [
      ['completion', '完成度', '#1a6fb5'],
      ['tech', '技术质量', '#409eff'],
      ['innovation', '创新', '#67c23a'],
      ['document', '文档', '#e6a23c']
    ]
    const barData = dims.map(([k, label, color]) => {
      const vals = ds[k] || []
      const avg = vals.length ? vals.reduce((a, b) => a + b, 0) / vals.length : 0
      return { value: +avg.toFixed(1), name: label, itemStyle: { color } }
    }).sort((a, b) => b.value - a.value)
    overviewBarChart.setOption({
      textStyle: { fontFamily: CHART_FONT },
      tooltip: { trigger: 'axis', formatter: '{b}: {c}' },
      grid: { left: 100, right: 50, top: 10, bottom: 20 },
      xAxis: { max: 100, axisLabel: { formatter: '{value}' } },
      yAxis: { type: 'category', data: barData.map(d => d.name), inverse: true,
        axisLabel: { fontSize: 13 } },
      series: [{
        type: 'bar', barWidth: 22, label: { show: true, position: 'right', fontSize: 13, fontWeight: 'bold' },
        data: barData
      }]
    })
  }
}

async function loadData() {
  loading.value = true
  data.value = null
  overviewData.value = null
  try {
    if (isOverview.value) {
      const taskId = Number(route.query.taskId)
      if (!taskId) throw new Error('Missing taskId')
      overviewData.value = await getOverviewData(taskId)
      await nextTick()
      renderOverviewCharts()
    } else {
      const subId = Number(route.params.submissionId)
      if (!subId) return
      data.value = await getReportData(subId)
      buildScoreTable()
      buildIssues()
      await nextTick()
      renderCharts()
    }
  } catch {
    ElNotification({ type: 'error', title: '加载报表数据失败', message: '', position: 'top-right' })
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  const s = data.value?.summary
  if (!s) return

  if (gaugeRef.value) {
    gaugeChart = echarts.init(gaugeRef.value)
    const gaugeColors: any[] = [[0.3, '#f56c6c'], [0.6, '#e6a23c'], [0.75, '#409eff'], [1, '#67c23a']]
    const gaugeOption: any = {
      textStyle: { fontFamily: CHART_FONT },
      series: [{
        type: 'gauge', startAngle: 210, endAngle: -30, center: ['50%', '55%'], radius: '90%',
        min: 0, max: 100, splitNumber: 10,
        axisLine: { lineStyle: { width: 12, color: gaugeColors } },
        pointer: { length: '60%', width: 6 },
        axisTick: { distance: -12, length: 8, lineStyle: { width: 1, color: '#999' } },
        splitLine: { distance: -18, length: 16, lineStyle: { width: 2, color: '#999' } },
        axisLabel: { distance: 20, fontSize: 12 },
        detail: { valueAnimation: false, formatter: '{value}', fontSize: 28, offsetCenter: [0, '70%'] },
        data: [{ value: s.totalScore || 0, name: s.grade || '' }]
      }]
    }
    gaugeChart.setOption(gaugeOption)
  }

  if (radarRef.value) {
    radarChart = echarts.init(radarRef.value)
    const radarOption: any = {
      textStyle: { fontFamily: CHART_FONT },
      tooltip: {},
      radar: {
        center: ['50%', '55%'], radius: '65%',
        indicator: [
          { name: '完成度', max: 100 }, { name: '技术质量', max: 100 },
          { name: '创新', max: 100 }, { name: '文档', max: 100 }
        ],
        axisName: { fontSize: 13 }
      },
      series: [{
        type: 'radar',
        data: [{
          value: [s.completionScore || 0, s.techScore || 0, s.innovationScore || 0, s.documentScore || 0],
          name: '得分', areaStyle: { opacity: 0.2 }
        }],
        symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2, color: '#1a6fb5' },
        itemStyle: { color: '#1a6fb5' }
      }]
    }
    radarChart.setOption(radarOption)
  }
}

onMounted(loadData)

onUnmounted(() => {
  [gaugeChart, radarChart, overviewPieChart, overviewBarChart].forEach(c => c?.dispose())
})
</script>

<style scoped>
.report-view { font-family: 'Noto Sans CJK SC', 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif; }
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:20px; }
.page-header h3 { margin:0; }

.report-body { max-width:960px; margin:0 auto; }
.report-section { margin-bottom:28px; }

.header-section { text-align:center; padding-bottom:20px; border-bottom:3px solid #1a6fb5; }
.header-section h1 { font-size:22px; color:#1a6fb5; letter-spacing:2px; margin-bottom:12px; }
.meta-row { display:flex; flex-wrap:wrap; justify-content:center; gap:6px 24px; color:#555; font-size:13px; }
.meta-row b { color:#333; }

.section-title { font-size:15px; color:#1a6fb5; border-left:4px solid #1a6fb5; padding-left:10px; margin-bottom:12px; }

.overview-row { display:flex; align-items:center; gap:24px; padding:16px; background:#f5f9fc; border-radius:8px; }
.total-box { text-align:center; flex-shrink:0; }
.total-score { font-size:36px; font-weight:700; color:#1a6fb5; }
.gauge-box { flex-shrink:0; }

.precheck-row { display:flex; flex-wrap:wrap; gap:10px; padding:10px 0; }
.precheck-summary { margin-top:8px; font-size:13px; color:#666; }

.dim-weight { font-size:13px; color:#1a6fb5; padding:4px 10px; background:#f0f5fb; border-radius:4px; margin-bottom:8px; }

.comment-box { padding:18px; background:#fdf8f0; border-left:4px solid #e6a23c; border-radius:4px; line-height:1.8; color:#555; white-space:pre-wrap; }

.subjective-box { padding:16px 18px; background:#eef5fc; border-left:4px solid #1a6fb5; border-radius:4px; line-height:1.8; color:#555; }
.subjective-label { font-weight:bold; color:#333; }
.subjective-val { font-size:18px; font-weight:bold; }
.subjective-reason { margin-top:6px; color:#666; }

.issue-group { margin-bottom:14px; }
.issue-group h4 { font-size:14px; color:#1a6fb5; margin-bottom:6px; }
.issue-item { padding:3px 0 3px 14px; font-size:13px; color:#666; border-left:2px solid #e0e8f0; margin-bottom:3px; }

.report-footer { text-align:center; color:#999; font-size:12px; padding-top:20px; border-top:1px solid #e0e8f0; margin-top:24px; }
</style>
