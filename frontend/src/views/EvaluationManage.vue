<template>
  <div class="evaluation">
    <div class="page-header">
      <h3>多维度评价管理</h3>
      <template v-if="activeTab === 'templates'">
        <div style="display:flex;align-items:center;gap:12px">
          <el-radio-group v-model="templateSort" size="small" @change="loadTemplates">
            <el-radio-button value="desc">倒序 ↓</el-radio-button>
            <el-radio-button value="asc">正序 ↑</el-radio-button>
          </el-radio-group>
          <el-button type="primary" @click="showTemplateDialog(null)">
            <el-icon><Plus /></el-icon> 新建评价模板
          </el-button>
        </div>
      </template>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 评价模板 -->
      <el-tab-pane label="评价模板" name="templates">
        <el-table :data="sortedTemplates" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="模板名称" min-width="160" />
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column label="评价方式" width="110">
            <template #default="{ row }">
              <el-tag :type="methodTag(row.evalMethod).type" size="small">{{ methodTag(row.evalMethod).text }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="AI/人工权重" width="140">
            <template #default="{ row }">
              <span>{{ row.aiWeight || 0 }} / {{ row.manualWeight || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="showTemplateDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteTemplate(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 评价执行 -->
      <el-tab-pane label="评价执行" name="execute">
        <el-card style="margin-bottom:16px">
          <el-form :inline="true">
            <el-form-item label="选择任务">
              <el-select v-model="execTaskId" filterable @change="loadExecSubmissions" style="width: 300px">
                <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card v-if="execTaskId">
          <el-form :inline="true">
            <el-form-item label="学生">
              <el-input v-model="execSearchName" placeholder="输入姓名搜索" clearable style="width:200px" @input="execCurrentPage = 1" />
            </el-form-item>
            <el-form-item label="审批状态">
              <el-select v-model="execStatusFilter" style="width:130px" @change="execCurrentPage = 1">
                <el-option label="全部" :value="null" />
                <el-option label="已审批" value="submitted" />
                <el-option label="已保存" value="draft" />
                <el-option label="未审批" value="none" />
              </el-select>
            </el-form-item>
            <el-form-item label="排列">
              <el-radio-group v-model="execSortOrder" @change="sortExecSubmissions" size="small">
                <el-radio-button value="asc">正序 ↑</el-radio-button>
                <el-radio-button value="desc">倒序 ↓</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button @click="execSearchName = ''; execStatusFilter = null; execCurrentPage = 1">清除筛选</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="pagedExecSubmissions" border stripe v-loading="execLoading" empty-text="暂无提交记录">
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
            <el-table-column label="审批状态" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.approvalStatus === 2 || row.submitted === 1" type="success" size="small">已审批</el-tag>
                <el-tag v-else-if="row.approvalStatus === 1" type="warning" size="small">已保存</el-tag>
                <el-tag v-else type="info" size="small">未审批</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="160">
              <template #default="{ row }">{{ fmt(row.submitTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="downloadExecFiles(row)" :disabled="!row.filePaths">
                  <el-icon><Download /></el-icon> 下载
                </el-button>
                <el-button v-if="row.approvalStatus === 2 || row.submitted === 1"
                  size="small" type="info" @click="viewApprovedEval(row)">
                  <el-icon><View /></el-icon> 查看评价
                </el-button>
                <el-button v-else size="small" type="warning" @click="openEvalWithTemplate(row)">
                  <el-icon><StarFilled /></el-icon> 去评价
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <CustomPagination :current-page="execCurrentPage" :total-pages="execTotalPages" :total="execTotal" @page-change="execGoPage" />

          <el-dialog v-model="execTemplateDialogVisible" title="选择评价模板" width="450px" :close-on-click-modal="true">
            <el-form label-width="80px">
              <el-form-item label="评价模板">
                <el-select v-model="execSelectedTemplateId" filterable style="width:100%" placeholder="请选择评价模板">
                  <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
                </el-select>
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="execTemplateDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="confirmExecEvaluation" :disabled="!execSelectedTemplateId">确定去评价</el-button>
            </template>
          </el-dialog>
        </el-card>

        <el-dialog v-model="replaceDraftVisible" title="替换草稿模板" width="500px">
          <p>将用新模板替换旧模板的草稿数据，旧模板下的评分数据将被清除。</p>
          <p style="color:#e6a23c">确定继续吗？</p>
          <template #footer>
            <el-button @click="replaceDraftVisible = false">取消</el-button>
            <el-button type="primary" @click="doReplaceDraft">确定替换</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 评价结果 -->
      <el-tab-pane label="评价结果" name="results">
        <template v-if="resultShowRecent">
          <div class="page-header">
            <el-button @click="resultShowRecent = false"><el-icon><ArrowLeft /></el-icon> 返回评价</el-button>
            <h3>历史评价记录</h3>
            <el-button v-if="recentEvaluations.length" size="small" text type="danger" @click="recentEvaluations = []">全部清除</el-button>
          </div>
          <el-card v-loading="historyLoading">
            <div v-if="recentEvaluations.length" class="recent-list">
              <div v-for="item in recentEvaluations" :key="item.id" class="recent-item" @click="openRecent(item)">
                <div class="recent-item-left">
                  <span class="recent-student">{{ item.studentName }}</span>
                  <span class="recent-task">{{ item.taskName }}</span>
                </div>
                <div class="recent-item-right">
                  <el-tag :type="item.approvalStatus === 2 ? 'success' : 'warning'" size="small">
                    {{ item.approvalStatus === 2 ? '已审批' : '已保存' }}
                  </el-tag>
                  <span class="recent-time">{{ fmt(item.updatedAt) }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无历史评价记录" :image-size="60" />
          </el-card>
        </template>

        <template v-else-if="resultEditSub">
          <div class="page-header">
            <el-button @click="clearCurrentEval"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
          </div>
          <el-card v-loading="resultEditLoading">
            <!-- 顶部操作栏：学生信息 + 按钮 -->
            <div class="eval-topbar">
              <div class="eval-topbar-left">
                <span class="eval-student">{{ resultEditSub?.studentName }}</span>
                <span class="eval-sep">|</span>
                <span class="eval-task">{{ resultEditSub?.taskName }}</span>
                <el-tag v-if="resultEditReadOnly" type="success" size="small">已审批</el-tag>
                <el-tag v-else-if="resultEditSub?.approvalStatus === 1" type="warning" size="small">草稿</el-tag>
              </div>
              <div class="eval-topbar-right">
                <el-button v-if="aiButtonState !== 'hidden'" 
                  :type="aiButtonState === 'completed' ? 'success' : 'primary'"
                  @click="runResultAI" :disabled="aiButtonDisabled"
                  :loading="aiButtonState === 'running'" size="small">
                  <el-icon><Cpu /></el-icon> {{ aiButtonText }}
                </el-button>
                <span v-if="aiButtonState === 'ready'" style="font-size:11px;color:#909399;margin-right:8px">
                  {{ getTemplateModel() }}
                </span>
                <el-button size="small" type="primary" @click="saveResultDraft" :disabled="resultAiRunning || resultEditReadOnly">保存草稿</el-button>
                <el-button size="small" type="success" @click="submitResultFinal" :disabled="resultAiRunning || resultEditReadOnly">提交评价</el-button>
              </div>
            </div>
            <el-alert v-if="resultAiFailed" title="AI评价完成但未获得有效评分，请检查提交内容是否已正确解析" type="warning" :closable="false" show-icon style="margin-bottom:12px" />

            <!-- 教师综合评价 -->
            <div class="eval-section-label">教师综合评价</div>
            <el-input v-model="resultTeacherComment" type="textarea" :rows="3" class="eval-teacher-comment"
              placeholder="选填，将展示在报表中" :disabled="resultEditReadOnly" />

            <!-- 主观评分（在加权计算总分基础上增减分值） -->
            <div class="eval-adjust-bar">
              <span class="eval-adjust-label">主观评分</span>
              <el-input-number v-model="resultSubjectiveScore" :min="-20" :max="20" :step="0.5" :precision="1" :controls="false"
                placeholder="0" size="small" :disabled="resultEditReadOnly" @change="onSubjectiveChange" />
              <span class="eval-adjust-hint">±20 分</span>
              <el-input v-model="resultSubjectiveReason" placeholder="主观评分原因" size="small" style="width:200px" :disabled="resultEditReadOnly" />
              <span style="flex:1"></span>
              <span class="eval-total-text">当前总分</span>
              <span class="eval-total-score">{{ resultFinalTotal.toFixed(1) }}分</span>
            </div>

            <!-- 评价指标分隔 -->
            <div class="eval-section-divider">评价指标</div>

            <!-- 指标列表：维度分块卡片，一行最多 2 个 -->
            <div v-if="resultEditResults.length" class="ios-dim-grid">
              <div v-for="grp in groupedEvalResults" :key="grp.dim" class="ios-dim-card">
                <div class="ios-dim-card-head">
                  <span class="ios-dim-title">{{ dimLabelMap[grp.dim] || grp.dim }}</span>
                  <span v-if="grp.totalWeight > 0" class="ios-dim-badge">权重 {{ grp.totalWeight.toFixed(0) }}%</span>
                  <span v-else class="ios-dim-badge ios-dim-badge-muted">不计入总分</span>
                  <span class="ios-dim-scored">已评 {{ grp.scored }}/{{ grp.items.length }}</span>
                </div>
                <div class="ios-ind-list">
                  <div v-for="item in grp.items" :key="item.id" class="ios-ind-item">
                    <div class="ios-ind-head">
                      <span class="ios-ind-name">{{ item.indicatorName }}</span>
                      <el-tag :type="evalTagType(item.evalType)" size="small" round>{{ evalTypeLabel(item.evalType) }}</el-tag>
                      <span class="ios-ind-weight">{{ item.weight || 0 }}%</span>
                      <span v-if="item.evalType === 2" class="ios-ind-mix">AI{{ Number(item.aiWeight || 0.6).toFixed(1) }}·人工{{ Number(item.manualWeight || 0.4).toFixed(1) }}</span>
                    </div>
                    <!-- AI 行 -->
                    <div v-if="showAiForItem(item)" class="ios-score-row ios-score-ai">
                      <span class="ios-score-tag ai">AI</span>
                      <template v-if="item.autoScore != null">
                        <span class="ios-score-val">{{ Number(item.autoScore).toFixed(1) }}</span>
                        <span v-if="item.autoComment" class="ios-score-comment">{{ item.autoComment }}</span>
                      </template>
                      <span v-else class="ios-score-pending">等待AI评分...</span>
                    </div>
                    <!-- 人工评分 + 评语 -->
                    <div v-if="showManualForItem(item)" class="ios-manual-block">
                      <div class="ios-manual-row">
                        <span class="ios-score-tag manual">评分</span>
                        <el-input-number v-model="item._manualScore" :min="0" :max="item.maxScore" :step="0.5"
                          :controls="false" size="small" placeholder="0-100" class="ios-input-score" :disabled="resultEditReadOnly" />
                      </div>
                      <el-input v-model="item._manualComment" type="textarea" :autosize="{ minRows: 1 }"
                        placeholder="教师评语" class="ios-comment-input" :disabled="resultEditReadOnly" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂未加载评价数据" :image-size="60" />
          </el-card>
        </template>

        <template v-else>
          <el-card>
            <el-empty description="请从评价执行中选择任务和提交记录开始评价" :image-size="80" />
            <div style="text-align:center;margin-top:16px">
              <el-button type="primary" @click="resultShowRecent = true; loadHistoryFromApi();">
                <el-icon><Clock /></el-icon> 历史评价记录
              </el-button>
            </div>
          </el-card>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- 模板编辑对话框 -->
    <el-dialog :title="templateForm.id ? '编辑模板' : '新建模板'" v-model="templateDialogVisible" width="1050px">
      <el-form :model="templateForm" label-width="110px">
        <el-form-item label="模板名称" required>
          <el-input v-model="templateForm.name" />
        </el-form-item>
        <el-form-item label="模板描述">
          <el-input v-model="templateForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="默认评价方式">
          <el-radio-group v-model="templateForm.evalMethod" @change="onEvalMethodChange">
            <el-radio :value="0">纯AI评价</el-radio>
            <el-radio :value="1">纯人工评价</el-radio>
            <el-radio :value="2">混合AI+人工</el-radio>
            <el-radio :value="3">逐项自定义</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="templateForm.evalMethod !== 3" label="AI/人工权重">
          <el-input-number v-model="templateForm.aiWeight" :min="templateForm.evalMethod === 2 ? 0.01 : 0" :max="templateForm.evalMethod === 2 ? 0.99 : 1" :step="0.1" :precision="2" :controls="false" :disabled="templateForm.evalMethod === 0 || templateForm.evalMethod === 1" size="small" style="width:150px" @change="onTemplateAiWeightChange" />
          <span style="margin:0 10px">/</span>
          <el-input-number v-model="templateForm.manualWeight" :min="templateForm.evalMethod === 2 ? 0.01 : 0" :max="templateForm.evalMethod === 2 ? 0.99 : 1" :step="0.1" :precision="2" :controls="false" :disabled="templateForm.evalMethod === 0 || templateForm.evalMethod === 1" size="small" style="width:150px" @change="onTemplateManualWeightChange" />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">评价指标</el-divider>
      <div style="margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center;">
        <span style="color: #909399; font-size: 13px;">
          共 {{ totalCount }} 个指标
          <span style="margin-left: 16px" :class="dimWeightsSum !== 100 ? 'dim-total-warn' : ''">维度权重合计: {{ dimWeightsSum }}%</span>
        </span>
        <el-button size="small" type="primary" @click="addTemplateIndicator">
          <el-icon><Plus /></el-icon> 添加指标
        </el-button>
      </div>
      <el-table v-if="sortedIndicators.length" :data="sortedIndicators" :span-method="spanMethod" border stripe size="small">
        <el-table-column label="名称" width="120">
          <template #default="{ row }">
            <el-input v-model="row.name" size="small" placeholder="名称" />
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.description" size="small" placeholder="描述" />
          </template>
        </el-table-column>
          <el-table-column label="评价方式" width="110">
            <template #default="{ row }">
              <el-select v-model="row.evalType" size="small" style="width: 95px"
                :disabled="templateForm.evalMethod !== 3"
                @change="(v) => onIndicatorEvalTypeChange(row, v)">
              <el-option :value="0" label="AI" />
              <el-option :value="1" label="人工" />
              <el-option :value="2" label="混合" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="AI/人工权重" width="175">
          <template #default="{ row }">
            <span style="display:flex;align-items:center;gap:2px;white-space:nowrap">
              <el-input-number v-model="row.aiWeight" :min="0" :max="1" :step="0.1" :precision="2" :controls="false" :disabled="templateForm.evalMethod !== 3" size="small" style="width:68px" @change="(v) => onIndAiChange(row, v)" />
              <span>/</span>
              <el-input-number v-model="row.manualWeight" :min="0" :max="1" :step="0.1" :precision="2" :controls="false" :disabled="templateForm.evalMethod !== 3" size="small" style="width:68px" @change="(v) => onIndManualChange(row, v)" />
            </span>
          </template>
        </el-table-column>
        <el-table-column label="维度" width="95">
          <template #default="{ row }">
            <el-select v-model="row.dim" size="small" style="width:85px" @change="onDimSelectChange">
              <el-option value="completion" label="完成度" />
              <el-option value="tech" label="技术" />
              <el-option value="innovation" label="创新" />
              <el-option value="document" label="文档" />
              <el-option value="precheck" label="预检" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="占比%" width="80">
          <template #default="{ row, $index }">
            <el-input-number v-if="dimWeightField(row.dim || row.dimension || '')"
              :model-value="templateForm[dimWeightField(row.dim || row.dimension || '')]"
              @update:model-value="(v) => onDimWeightEdit(row, v)"
              :min="0" :max="100" :step="1" :controls="false" size="small" style="width:70px"
            />
            <span v-else class="dim-weight-zero">0</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="removeTemplateIndicator(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无指标，请添加" :image-size="60" />

      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">确定</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, onBeforeRouteLeave } from 'vue-router';
import {   ElNotification , ElMessageBox  } from 'element-plus';
import request from '../utils/request';
import { useFormatTime } from '../composables/useFormatTime';
import { useFormatSize } from '../composables/useFormatSize'
import { useDownload } from '../composables/useDownload'
import { usePagination } from '../composables/usePagination'
import CustomPagination from '../components/common/CustomPagination.vue'
import FileTagsDisplay from '../components/common/FileTagsDisplay.vue'
import { getSubmissionList, downloadSubmissionZip } from '../api/file';
import {
  getTemplates, createTemplate, updateTemplate,
  deleteTemplate as delTemplate, getIndicators, batchSaveIndicators,
  deleteIndicator as delIndicator, triggerAutoScore,
  getEvaluationResult, submitManualScore, submitEvaluationResult,
  getEvaluationHistory, replaceDraft, initResults,
} from '../api/evaluation';

onBeforeRouteLeave((to, from, next) => {
  if (resultAiRunning.value) {
    ElMessageBox.confirm('AI评价正在进行中，离开页面将中断评价，确定离开吗？', '提示', { type: 'warning' })
      .then(() => { resultAiRunning.value = false; next(); })
      .catch(() => next(false));
  } else if (hasUnsavedChanges.value) {
    ElMessageBox.confirm('有未保存的修改，离开将丢失，确定离开吗？', '提示', { type: 'warning' })
      .then(() => next())
      .catch(() => next(false));
  } else {
    next();
  }
});

const activeTab = ref('templates');
const templateSort = ref('desc');
const tasks = ref([]);
const templates = ref([]);
const templateDialogVisible = ref(false);
const templateForm = ref({ name: '', description: '', evalMethod: 2, aiWeight: 0.60, manualWeight: 0.40, weightCompletion: 25, weightTech: 25, weightInnovation: 25, weightDocument: 25 });
const templateIndicators = ref([]);
const templateDeletedInds = ref([]);

const execTaskId = ref(null);
const execSubmissions = ref([]);
const execSearchName = ref('');
const execStatusFilter = ref(null);
const execSortOrder = ref('desc');
const execLoading = ref(false);
const execTemplateDialogVisible = ref(false);
const execSelectedTemplateId = ref(null);
const execEvalRow = ref(null);

const { currentPage: execCurrentPage, pageSize: execPageSize, visiblePages: execVisiblePages, goPage: execGoPage } = usePagination(14)

const resultEditSub = ref(null);
const resultEditResults = ref([]);
const resultEditLoading = ref(false);
const resultEditTemplateId = ref(null);
const resultShowRecent = ref(false);

const resultAiRunning = ref(false);
const resultAiEver = ref(false);
const resultAiFailed = ref(false);
const resultTeacherComment = ref('');
const resultSubjectiveScore = ref<number | null>(null);
const resultSubjectiveReason = ref('');
const originalSubjectiveScore = ref<number | null>(null);
const originalSubjectiveReason = ref('');

const { fmt } = useFormatTime();
const { formatTotalSize } = useFormatSize()
const { downloadBlob } = useDownload()

const hasUnsavedChanges = computed(() => {
  if (!resultEditSub.value || resultEditReadOnly.value) return false;
  return resultEditResults.value.some(item => item._manualScore != null || item._manualComment) ||
         Number(resultSubjectiveScore.value ?? 0) !== Number(originalSubjectiveScore.value ?? 0) ||
         (resultSubjectiveReason.value || '') !== (originalSubjectiveReason.value || '');
});

const resultEditReadOnly = computed(() => {
  if (!resultEditSub.value) return false;
  return resultEditSub.value.approvalStatus === 2;
});

const dimLabelMap: Record<string, string> = { completion: '完成度', tech: '技术质量', innovation: '创新', document: '文档', precheck: '前置检查' };

const groupedEvalResults = computed(() => {
  const order = ['precheck', 'completion', 'tech', 'innovation', 'document'];
  const groups: Record<string, any[]> = {};
  resultEditResults.value.forEach(item => {
    const dim = item.dimension || 'other';
    if (!groups[dim]) groups[dim] = [];
    groups[dim].push(item);
  });
  return order.filter(d => groups[d]).map(dim => {
    const items = groups[dim];
    const totalWeight = items.reduce((s, i) => s + Number(i.weight || 0), 0);
    const scored = items.filter(i =>
      i._manualScore != null
      || (i.autoScore != null && Number(i.autoScore) > 0)
      || (i._manualComment && String(i._manualComment).trim())
      || (i.autoComment && String(i.autoComment).trim())
    ).length;
    return { dim, items, totalWeight, scored };
  });
});

const recentEvaluations = ref([]);
const historyLoading = ref(false);

async function loadHistoryFromApi() {
  historyLoading.value = true;
  try {
    const list = await getEvaluationHistory() || [];
    recentEvaluations.value = list.map(item => ({
      ...item,
      state: item.approvalStatus === 2 ? 'submitted' : 'draft',
      approvalStatus: item.approvalStatus != null ? item.approvalStatus : (item.submitted === 1 ? 2 : 1),
      updatedAt: item.updateTime || item.submitTime || new Date().toISOString(),
    }));
    recentEvaluations.value.sort((a, b) => {
      const da = new Date(a.updatedAt);
      const db = new Date(b.updatedAt);
      if (isNaN(da) && isNaN(db)) return 0;
      if (isNaN(da)) return 1;
      if (isNaN(db)) return -1;
      return db - da;
    });
  } catch { ElNotification({ type: 'error', title: '加载历史记录失败', message: '', position: 'top-right', duration: 3000 }); }
  finally { historyLoading.value = false; }
}

function openRecent(item) {
  resultShowRecent.value = false;
  resultEditTemplateId.value = item.templateId || item.draftTemplateId;
  const status = item.approvalStatus != null ? item.approvalStatus : (item.submitted === 1 ? 2 : (item.state === 'submitted' ? 2 : 1));
  item.approvalStatus = status;
  if (item.submitted === undefined && status === 2) item.submitted = 1;
  enterResultEdit(item);
}

const replaceDraftVisible = ref(false);
const replaceDraftPending = ref(null);

async function doReplaceDraft() {
  const p = replaceDraftPending.value;
  if (!p) return;
  try {
    await replaceDraft(p.submissionId, p.oldTemplateId, p.newTemplateId);
    replaceDraftVisible.value = false;
    resultEditTemplateId.value = p.newTemplateId;
    resultEditResults.value = [];
    const newRow = { ...resultEditSub.value, approvalStatus: 1, draftTemplateId: p.newTemplateId };
    resultEditSub.value = newRow;
    enterResultEdit(newRow);
  } catch { ElNotification({ type: 'error', title: '替换草稿失败', message: '', position: 'top-right', duration: 3000 }); }
}

// 默认模式下，把父权重等比例（继承父的 AI/人工比）同步到全部子指标，并据此回推子指标评价方式
function syncChildrenFromParent() {
  const aw = Number(templateForm.value.aiWeight);
  const mw = Number(templateForm.value.manualWeight);
  templateIndicators.value.forEach(row => {
    row.aiWeight = aw;
    row.manualWeight = mw;
    syncIndMethod(row);
  });
}

const dimWeightsSum = computed(() => {
  return (templateForm.value.weightCompletion || 0)
       + (templateForm.value.weightTech || 0)
       + (templateForm.value.weightInnovation || 0)
       + (templateForm.value.weightDocument || 0);
});

const totalCount = computed(() => templateIndicators.value.length);

// 同维度条目连续排布，确保占比列合并正常
const dimOrder = ['completion', 'tech', 'innovation', 'document', 'precheck', ''];

const sortedIndicators = computed(() => {
  return [...templateIndicators.value].sort((a, b) => {
    const da = dimOrder.indexOf(a.dim || a.dimension || '');
    const db = dimOrder.indexOf(b.dim || b.dimension || '');
    return da - db;
  });
});

function spanMethod({ row, columnIndex, rowIndex }: any) {
  // 占比%列（列序号 5）纵向合并同维度相邻单元格
  if (columnIndex !== 5) return;
  const dim = (row.dim || row.dimension || '') as string;
  const list = sortedIndicators.value;
  let count = 1;
  let isFirst = true;
  for (let i = rowIndex - 1; i >= 0; i--) {
    if ((list[i].dim || list[i].dimension || '') === dim) { isFirst = false; break; }
    else break;
  }
  if (!isFirst) return { rowspan: 0, colspan: 0 };
  for (let i = rowIndex + 1; i < list.length; i++) {
    if ((list[i].dim || list[i].dimension || '') === dim) count++;
    else break;
  }
  return { rowspan: count, colspan: 1 };
}

function dimWeightField(dim: string): string {
  const map: Record<string, string> = {
    completion: 'weightCompletion',
    tech: 'weightTech',
    innovation: 'weightInnovation',
    document: 'weightDocument',
  };
  return map[dim] || '';
}

function onDimWeightEdit(row: any, v: number | null) {
  const field = dimWeightField(row.dim || row.dimension || '');
  if (field) (templateForm.value as any)[field] = v ?? 0;
}

function syncWeightsFromMethod(v) {
  if (v === 0) { templateForm.value.aiWeight = 1; templateForm.value.manualWeight = 0; }
  else if (v === 1) { templateForm.value.aiWeight = 0; templateForm.value.manualWeight = 1; }
  else if (v === 2) { templateForm.value.aiWeight = 0.60; templateForm.value.manualWeight = 0.40; }
  if (v !== 3) {
    // 子指标继承父的 AI/人工比
    syncChildrenFromParent();
  }
}
function onEvalMethodChange(v) {
  syncWeightsFromMethod(v);
  if (v === 3 && templateIndicators.value.length === 0) {
    fillDefaultIndicators();
  }
  if (v !== 3) {
    ElNotification({ type: 'info', title: '已将所有指标的评价方式同步为模板默认值', message: '', position: 'top-right', duration: 2000 });
  }
}

function fillDefaultIndicators() {
  templateIndicators.value = [
    { name: '代码质量', description: '评估代码结构清晰度、命名规范、注释完整性、异常处理合理性', evalType: 2, aiWeight: 0.60, manualWeight: 0.40, weight: 30, maxScore: 100, dim: 'tech' },
    { name: '文档规范性', description: '评估实训文档的完整性、格式规范性和内容质量', evalType: 2, aiWeight: 0.60, manualWeight: 0.40, weight: 20, maxScore: 100, dim: 'document' },
    { name: '功能实现度', description: '对照实训任务要求，评估核心功能的实现完整性和正确性', evalType: 2, aiWeight: 0.60, manualWeight: 0.40, weight: 30, maxScore: 100, dim: 'completion' },
    { name: '创新性与优化', description: '评估在基本功能之上是否有创新设计、性能优化等亮点', evalType: 2, aiWeight: 0.60, manualWeight: 0.40, weight: 20, maxScore: 100, dim: 'innovation' },
  ];
}

function syncMethodFromWeights() {
  const aw = templateForm.value.aiWeight;
  const mw = templateForm.value.manualWeight;
  if (aw === 1 && mw === 0) templateForm.value.evalMethod = 0;
  else if (aw === 0 && mw === 1) templateForm.value.evalMethod = 1;
  else if (templateForm.value.evalMethod !== 3) templateForm.value.evalMethod = 2;
}

function onTemplateAiWeightChange(v) {
  let aw = Number(v || 0);
  // 混合模式：AI 占比为开区间 (0,1)
  if (templateForm.value.evalMethod === 2) {
    if (aw <= 0) aw = 0.01;
    if (aw >= 1) aw = 0.99;
  }
  templateForm.value.aiWeight = +aw.toFixed(2);
  templateForm.value.manualWeight = +(1 - aw).toFixed(2);
  syncMethodFromWeights();
  syncChildrenFromParent();
}
function onTemplateManualWeightChange(v) {
  let mw = Number(v || 0);
  if (templateForm.value.evalMethod === 2) {
    if (mw <= 0) mw = 0.01;
    if (mw >= 1) mw = 0.99;
  }
  templateForm.value.manualWeight = +mw.toFixed(2);
  templateForm.value.aiWeight = +(1 - mw).toFixed(2);
  syncMethodFromWeights();
  syncChildrenFromParent();
}

function onIndicatorEvalTypeChange(row, v) {
  if (v === 0) { row.aiWeight = 1; row.manualWeight = 0; }
  else if (v === 1) { row.aiWeight = 0; row.manualWeight = 1; }
  else if (v === 2) { row.aiWeight = 0.60; row.manualWeight = 0.40; }
}

function onDimSelectChange() {
  // 维度变更后 sortedIndicators 自动按维度排序，无需额外处理
}
function syncIndMethod(row) {
  const aw = Number(row.aiWeight);
  const mw = Number(row.manualWeight);
  if (aw === 1 && mw === 0) row.evalType = 0;
  else if (aw === 0 && mw === 1) row.evalType = 1;
  else row.evalType = 2;
}
function onIndAiChange(row, v) {
  row.manualWeight = +(1 - (v || 0)).toFixed(2);
  syncIndMethod(row);
}
function onIndManualChange(row, v) {
  row.aiWeight = +(1 - (v || 0)).toFixed(2);
  syncIndMethod(row);
}

// 兼容历史数据：若指标权重为 0~1 小数刻度（非前置检查权重之和 ≤ 1.5），归一化为 0~100 整数百分比
function normalizeIndicatorWeights(list) {
  const scoredSum = list
    .filter(i => (i.dim || i.dimension) !== 'precheck')
    .reduce((s, i) => s + Number(i.weight || 0), 0);
  if (scoredSum > 0 && scoredSum <= 1.5) {
    list.forEach(i => {
      if ((i.dim || i.dimension) === 'precheck') { i.weight = 0; }
      else { i.weight = Math.round(Number(i.weight || 0) * 100); }
    });
  }
  return list;
}

// 维度内均分：同一维度下指标均分 100%权重，floor(100/n)，末位补差。precheck 权重为 0
function redistributeWeights() {
  // 先清空 precheck 权重
  templateIndicators.value.forEach(i => {
    if ((i.dim || i.dimension) === 'precheck') i.weight = 0;
  });

  const dimGroups: Record<string, any[]> = {};
  templateIndicators.value.forEach(i => {
    const dim = i.dim || i.dimension || 'other';
    if (dim === 'precheck') return;
    if (!dimGroups[dim]) dimGroups[dim] = [];
    dimGroups[dim].push(i);
  });

  for (const group of Object.values(dimGroups)) {
    const n = group.length;
    if (n === 0) continue;
    const each = Math.floor(100 / n);
    const remainder = 100 - each * n;
    group.forEach((ind, idx) => {
      ind.weight = each;
      if (idx === n - 1) ind.weight += remainder;
    });
  }
}

const methodTag = (m) => ({ 0: { type: 'primary', text: '纯AI' }, 1: { type: 'warning', text: '纯人工' }, 2: { type: 'success', text: '混合' }, 3: { type: 'info', text: '逐项自定义' } }[m] || { type: 'info', text: '未知' });
const evalTypeLabel = (t) => ({ 0: 'AI', 1: '人工', 2: '混合' }[t] || '');
const sortedTemplates = computed(() => {
  const list = [...templates.value];
  return templateSort.value === 'asc' ? list.reverse() : list;
});

const route = useRoute();

onMounted(async () => {
  const tasksRes = await request.get('/tasks/search') || [];
  tasks.value = (Array.isArray(tasksRes) ? tasksRes : []).filter(t => t.status !== 0);
  await loadTemplates();

  const subId = route.query.evalSub;
  const taskId = route.query.evalTask;
  const studentName = route.query.evalName;
  const taskName = route.query.evalTaskName;
  if (subId && taskId) {
    activeTab.value = 'execute';
    execTaskId.value = Number(taskId);
    await loadExecSubmissions();
    execEvalRow.value = execSubmissions.value.find(r => r.id === Number(subId))
      || { id: Number(subId), studentName: studentName || '', taskName: taskName || '', taskId: Number(taskId), submitted: 1, approvalStatus: 2 };
    execTemplateDialogVisible.value = true;
  }

  const viewId = route.query.evalView;
  const viewTaskId = route.query.evalTask;
  const viewName = route.query.evalName;
  const viewTaskName = route.query.evalTaskName;
  if (viewId && viewTaskId) {
    activeTab.value = 'results';
    resultShowRecent.value = false;
    const row = { id: Number(viewId), studentName: viewName || '', taskName: viewTaskName || '', taskId: Number(viewTaskId), submitted: 1, approvalStatus: 2 };
    await enterResultEdit(row);
  }
});

async function loadTemplates() { templates.value = (await getTemplates()) || []; }

async function showTemplateDialog(row) {
  if (row) {
    templateForm.value = { ...row };
    // 后端存储为 0~1 小数，前端使用 0~100 整数百分比；大于 1 视为已是百分比
    const toPct = (v: any) => v == null ? 25 : (Number(v) > 1 ? Math.round(Number(v)) : Math.round(Number(v) * 100));
    templateForm.value.weightCompletion = toPct(templateForm.value.weightCompletion);
    templateForm.value.weightTech = toPct(templateForm.value.weightTech);
    templateForm.value.weightInnovation = toPct(templateForm.value.weightInnovation);
    templateForm.value.weightDocument = toPct(templateForm.value.weightDocument);
    const inds = await getIndicators(row.id) || [];
    templateIndicators.value = normalizeIndicatorWeights(inds.map(i => ({ ...i, dim: i.dimension || '' })));
    templateDeletedInds.value = [];
  } else {
    templateForm.value = { name: '', description: '', evalMethod: 2, aiWeight: 0.60, manualWeight: 0.40, weightCompletion: 25, weightTech: 25, weightInnovation: 25, weightDocument: 25 };
    // 新建模板必须同步配套指标，并自动均分权重（整数、末项补差）
    fillDefaultIndicators();
    redistributeWeights();
    syncChildrenFromParent();
    templateDeletedInds.value = [];
  }
  templateDialogVisible.value = true;
}

const templateSaving = ref(false);

async function saveTemplate() {
  if (templateSaving.value) return;
  if (!templateForm.value.name) { ElNotification({ type: 'warning', title: '请输入模板名称', message: '', position: 'top-right', duration: 3000 }); return; }
  if (templateIndicators.value.length === 0) {
    ElNotification({ type: 'warning', title: '请至少添加一个评价指标', message: '', position: 'top-right', duration: 3000 });
    return;
  }
  // 校验维度全局权重之和 = 100%
  if (dimWeightsSum.value !== 100) {
    ElNotification({ type: 'warning', title: '维度全局权重之和必须为100%（当前：' + dimWeightsSum.value + '%）', message: '', position: 'top-right', duration: 3000 });
    return;
  }
  // 校验每个维度至少有一个非precheck指标
  const hasDim: Record<string, boolean> = {};
  templateIndicators.value.forEach(i => {
    const d = i.dim || i.dimension || '';
    if (d && d !== 'precheck') hasDim[d] = true;
  });
  const requiredDims = ['completion', 'tech', 'innovation', 'document'];
  const missingDims = requiredDims.filter(d => (templateForm.value as any)['weight' + d.charAt(0).toUpperCase() + d.slice(1)] > 0 && !hasDim[d]);
  if (missingDims.length > 0) {
    ElNotification({ type: 'warning', title: '维度"' + missingDims.join(',') + '"权重 > 0 但无指标，请添加指标或将该维度权重设为0', message: '', position: 'top-right', duration: 4000 });
    return;
  }

  const isEditing = !!templateForm.value.id;
  let saveMode: 'overwrite' | 'new' = 'overwrite';

  if (isEditing) {
    try {
      await ElMessageBox.confirm(
        '请选择保存方式：「覆盖原有模板」将更新当前模板；「另存为新模板」会保留原模板并创建一个全新的评价模板。',
        '保存模板',
        { distinguishCancelAndClose: true, confirmButtonText: '覆盖原有模板', cancelButtonText: '另存为新模板', type: 'info' }
      );
      saveMode = 'overwrite';
    } catch (e: any) {
      if (e === 'cancel') { saveMode = 'new'; }
      else { return; }
    }
  }

  // 保存前均分权重并转换为后端格式（维度权重 0-100% → 0-1）
  redistributeWeights();
  templateSaving.value = true;
  try {
  const payload: any = { ...templateForm.value };
  payload.weightCompletion = (payload.weightCompletion || 0) / 100;
  payload.weightTech = (payload.weightTech || 0) / 100;
  payload.weightInnovation = (payload.weightInnovation || 0) / 100;
  payload.weightDocument = (payload.weightDocument || 0) / 100;
  let templateId: number | undefined = payload.id;
  if (saveMode === 'overwrite' && templateId) {
    await updateTemplate(templateId, payload);
  } else {
    const created = await createTemplate({ ...payload, id: undefined });
    templateId = created.id;
    templateForm.value.id = templateId;
  }
  for (const id of templateDeletedInds.value) {
    try { await delIndicator(id); } catch {}
  }
  if (templateId) {
    if (templateForm.value.evalMethod !== 3) {
      // 默认模式：子指标继承父的 AI/人工比后再保存
      syncChildrenFromParent();
    }
    const indsToSave = templateIndicators.value.map(i => {
      const saved: any = { ...i, templateId, dimension: i.dim || i.dimension || '' };
      if (saveMode === 'new') saved.id = undefined;
      return saved;
    });
    await batchSaveIndicators(templateId, indsToSave);
  }
  ElNotification({ type: 'success', title: '保存成功', message: '', position: 'top-right', duration: 3000 });
  templateDialogVisible.value = false;
  loadTemplates();
  } finally {
    templateSaving.value = false;
  }
}

function addTemplateIndicator() {
  templateIndicators.value.push({
    name: '', description: '', evalType: 2, aiWeight: 0.60, manualWeight: 0.40,
    weight: 10, maxScore: 100, dim: '',
  });
  redistributeWeights();
}

function removeTemplateIndicator(row) {
  const idx = templateIndicators.value.findIndex(i => (
    i === row || (i.name === row.name && i.dim === row.dim && i.description === row.description)
  ));
  if (idx < 0) return;
  const ind = templateIndicators.value[idx];
  if (ind.id) templateDeletedInds.value.push(ind.id);
  templateIndicators.value.splice(idx, 1);
  redistributeWeights();
}

async function deleteTemplate(id) {
  await ElMessageBox.confirm('确定删除该模板？', '提示', { type: 'warning' });
  await delTemplate(id);
  ElNotification({ type: 'success', title: '删除成功', message: '', position: 'top-right', duration: 3000 });
  loadTemplates();
}

async function loadExecSubmissions() {
  if (!execTaskId.value) { execSubmissions.value = []; return; }
  execLoading.value = true;
  execCurrentPage.value = 1;
  try {
    const subs = await getSubmissionList(execTaskId.value) || [];
    subs.forEach(s => {
      const t = tasks.value.find(t => t.id === s.taskId);
      s.taskName = t ? t.title : '未知任务';
    });
    execSubmissions.value = subs;
  } catch (e) {
    console.error('加载提交列表失败:', e);
    execSubmissions.value = [];
  } finally { execLoading.value = false; }
}

const execFiltered = computed(() => {
  let list = [...execSubmissions.value];
  if (execSearchName.value) {
    const kw = execSearchName.value.toLowerCase();
    list = list.filter(s => (s.studentName || '').toLowerCase().includes(kw));
  }
  if (execStatusFilter.value !== null) {
    if (execStatusFilter.value === 'submitted') {
      list = list.filter(s => s.approvalStatus === 2 || s.submitted === 1);
    } else if (execStatusFilter.value === 'draft') {
      list = list.filter(s => s.approvalStatus === 1 && s.submitted !== 1);
    } else if (execStatusFilter.value === 'none') {
      list = list.filter(s => !s.approvalStatus || s.approvalStatus === 0);
    }
  }
  const maxPage = Math.max(1, Math.ceil(list.length / execPageSize.value));
  if (execCurrentPage.value > maxPage) {
    execCurrentPage.value = maxPage;
  }
  if (execSortOrder.value === 'asc') list = list.reverse();
  return list;
});

const execTotal = computed(() => execFiltered.value.length);
const execTotalPages = computed(() => Math.max(1, Math.ceil(execTotal.value / execPageSize.value)));

const pagedExecSubmissions = computed(() => {
  const start = (execCurrentPage.value - 1) * execPageSize.value;
  return execFiltered.value.slice(start, start + execPageSize.value);
});

function sortExecSubmissions() {
  execCurrentPage.value = 1;
}

async function downloadExecFiles(row) {
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

function openEvalWithTemplate(row) {
  execEvalRow.value = row;
  execSelectedTemplateId.value = null;
  execTemplateDialogVisible.value = true;
}

function viewApprovedEval(row) {
  activeTab.value = 'results';
  resultShowRecent.value = false;
  resultEditTemplateId.value = null;
  enterResultEdit({ ...row, approvalStatus: 2, submitted: 1 });
}

async function confirmExecEvaluation() {
  const row = execEvalRow.value;
  const templateId = execSelectedTemplateId.value;
  if (!row || !templateId) return;
  execTemplateDialogVisible.value = false;
  activeTab.value = 'results';
  resultShowRecent.value = false;
  resultEditTemplateId.value = templateId;
  enterResultEdit(row);
}

function clearCurrentEval() {
  if (hasUnsavedChanges.value) {
    ElMessageBox.confirm('有未保存的修改，是否保存？', '提示', {
      confirmButtonText: '保存并离开',
      cancelButtonText: '不保存',
      distinguishCancelAndClose: true,
    }).then(() => {
      saveResultDraft().then(() => {
        resultEditSub.value = null;
        resultEditResults.value = [];
        resultSubjectiveScore.value = null;
        resultSubjectiveReason.value = '';
        originalSubjectiveScore.value = null;
        originalSubjectiveReason.value = '';
      });
    }).catch((action) => {
      if (action === 'cancel') {
        resultEditSub.value = null;
        resultEditResults.value = [];
        resultSubjectiveScore.value = null;
        resultSubjectiveReason.value = '';
        originalSubjectiveScore.value = null;
        originalSubjectiveReason.value = '';
      }
    });
  } else {
    resultEditSub.value = null;
    resultEditResults.value = [];
    resultSubjectiveScore.value = null;
    resultSubjectiveReason.value = '';
    originalSubjectiveScore.value = null;
    originalSubjectiveReason.value = '';
  }
}

async function enterResultEdit(row) {
  resultEditSub.value = { ...row };
  resultEditLoading.value = true;
  resultEditResults.value = [];
  resultAiRunning.value = false;
  resultAiEver.value = false;
  resultAiFailed.value = false;
  resultSubjectiveScore.value = null;
  resultSubjectiveReason.value = '';
  originalSubjectiveScore.value = null;
  originalSubjectiveReason.value = '';

  const status = row.approvalStatus != null ? row.approvalStatus : (row.submitted === 1 ? 2 : 0);
  row.approvalStatus = status;
  const isReadOnly = status === 2;
  const isDraft = status === 1;
  const draftTplId = row.draftTemplateId;

  try {
    if (isReadOnly) {
      if (draftTplId) resultEditTemplateId.value = draftTplId;
      let results = await getEvaluationResult(row.id, resultEditTemplateId.value) || [];
      if (results.length === 0) results = await getEvaluationResult(row.id, null) || [];
      resultEditResults.value = results.map(item => ({
        ...item, _manualScore: item.manualScore, _manualComment: item.manualComment || '', _saving: false,
      }));
      resultSubjectiveScore.value = resultEditSub.value && resultEditSub.value.subjectiveScore != null
        && Number(resultEditSub.value.subjectiveScore) !== 0
        ? Number(resultEditSub.value.subjectiveScore) : null;
      resultSubjectiveReason.value = resultEditSub.value && resultEditSub.value.subjectiveReason
        ? resultEditSub.value.subjectiveReason : '';
      originalSubjectiveScore.value = resultSubjectiveScore.value;
      originalSubjectiveReason.value = resultSubjectiveReason.value;
      const aiDone = results.filter(r => r.autoScore != null).length;
      if (aiDone > 0) resultAiEver.value = true;
      if (row.aiScoreStatus != null) resultEditSub.value.aiScoreStatus = row.aiScoreStatus;
      return;
    }

    let results = await getEvaluationResult(row.id, resultEditTemplateId.value) || [];

    if (results.length === 0 && isDraft && draftTplId && resultEditTemplateId.value && draftTplId !== resultEditTemplateId.value) {
      replaceDraftPending.value = { submissionId: row.id, oldTemplateId: draftTplId, newTemplateId: resultEditTemplateId.value };
      replaceDraftVisible.value = true;
      return;
    }

    if (results.length === 0 && resultEditTemplateId.value) {
      try {
        results = await initResults(row.id, resultEditTemplateId.value) || [];
      } catch (e) {
        ElNotification({ type: 'error', title: '初始化评价记录失败', message: e?.message || '', position: 'top-right', duration: 3000 });
        resultEditLoading.value = false;
        return;
      }
    }

    resultEditResults.value = results.map(item => ({
      ...item, _manualScore: item.manualScore, _manualComment: item.manualComment || '', _saving: false,
    }));
    resultSubjectiveScore.value = resultEditSub.value && resultEditSub.value.subjectiveScore != null
      && Number(resultEditSub.value.subjectiveScore) !== 0
      ? Number(resultEditSub.value.subjectiveScore) : null;
    resultSubjectiveReason.value = resultEditSub.value && resultEditSub.value.subjectiveReason
      ? resultEditSub.value.subjectiveReason : '';
    originalSubjectiveScore.value = resultSubjectiveScore.value;
    originalSubjectiveReason.value = resultSubjectiveReason.value;
    const aiDone = results.filter(r => r.autoScore != null).length;
    if (aiDone > 0) resultAiEver.value = true;
    if (row.aiScoreStatus != null) resultEditSub.value.aiScoreStatus = row.aiScoreStatus;
  } catch (e) {
    ElNotification({ type: 'error', title: '加载评价数据失败', message: '', position: 'top-right', duration: 3000 });
  } finally {
    resultEditLoading.value = false;
  }
}

const aiButtonState = computed(() => {
  if (resultEditReadOnly.value) return 'hidden';
  const tid = resultEditTemplateId.value;
  const tpl = tid ? templates.value.find(t => t.id === tid) : null;

  if (!tpl) {
    const hasAI = resultEditResults.value.some(item => item.evalType === 0 || item.evalType === 2);
    return hasAI ? 'ready' : 'hidden';
  }
  const em = tpl.evalMethod;
  if (em === 1) return 'hidden';
  if (em === 3) {
    const hasAI = resultEditResults.value.some(item =>
      item.evalType === 0 || (item.evalType === 2 && (Number(item.aiWeight) || 0) > 0)
    );
    if (!hasAI) return 'hidden';
  }

  const status = resultEditSub.value?.aiScoreStatus;
  if (resultAiRunning.value || status === 1) return 'running';
  if (status === 2) return 'completed';
  if (status === 3) return 'retry';
  return 'ready';
});

const aiButtonText = computed(() => {
  const map: Record<string, string> = { ready: '开始AI评价', running: 'AI评分中...', completed: 'AI评分已完成 ✓（可重试）', retry: '重新AI评价', hidden: '' };
  return map[aiButtonState.value] || '';
});

const aiButtonDisabled = computed(() => {
  return aiButtonState.value === 'running' || aiButtonState.value === 'hidden';
});

const templateAllowsAI = computed(() => {
  if (resultEditReadOnly.value) return true;
  const tid = resultEditTemplateId.value;
  const tpl = tid ? templates.value.find(t => t.id === tid) : null;
  if (!tpl) return true;
  return tpl.evalMethod !== 1;
});

function showAiForItem(item: any) {
  if (!templateAllowsAI.value) return false;
  return item.evalType === 0 || item.evalType === 2;
}

function showManualForItem(item: any) {
  return item.evalType === 1 || item.evalType === 2;
}

const evalTagType = (t: number) => t === 0 ? 'primary' : t === 1 ? 'warning' : 'success';

const resultEditTotalScore = computed(() => {
  if (!resultEditResults.value.length) return 0;
  // ===== 层级1：计算各维度综合得分 =====
  const dimScore: Record<string, number> = {};
  const dimWeight: Record<string, number> = {};

  resultEditResults.value.forEach(item => {
    const dim = item.dimension || 'other';
    if (dim === 'precheck') return;

    let score = 0;
    const auto = item.autoScore != null ? Number(item.autoScore) : null;
    const manual = item._manualScore != null ? Number(item._manualScore) : null;

    if (item.evalType === 0) {
      if (auto != null) score = auto;
    } else if (item.evalType === 1) {
      if (manual != null) score = manual;
    } else if (item.evalType === 2) {
      if (auto != null && manual != null) {
        const aw = Number(item.aiWeight || 0.6);
        const mw = Number(item.manualWeight || 0.4);
        score = auto * aw + manual * mw;
      } else if (auto != null) {
        score = auto * Number(item.aiWeight || 0.6);
      } else if (manual != null) {
        score = manual;
      }
    }
    score = Math.max(0, Math.min(100, score));
    const w = Number(item.weight || 0);

    if (!dimScore[dim]) { dimScore[dim] = 0; dimWeight[dim] = 0; }
    dimScore[dim] += score * w;
    dimWeight[dim] += w;
  });

  // ===== 层级2：全局维度加权总分 =====
  const tpl = resultEditTemplateId.value ? templates.value.find(t => t.id === resultEditTemplateId.value) : null;
  const getDimW = (key: string) => {
    if (!tpl) return 0.25;
    const v = (tpl as any)['weight' + key.charAt(0).toUpperCase() + key.slice(1)];
    return v != null ? Number(v) : 0.25;
  };
  const dimKeys = ['completion', 'tech', 'innovation', 'document'];
  let total = 0;
  for (const dim of dimKeys) {
    if (!dimWeight[dim] || dimWeight[dim] === 0) continue;
    const dimAvg = dimScore[dim] / dimWeight[dim];  // 维度内加权平均
    total += dimAvg * getDimW(dim);
  }
  return Math.max(0, Math.min(100, total));
});

// 当前总分 = 加权计算分 + 主观评分（±20），再 clamp 0~100
const resultFinalTotal = computed(() => {
  const subj = Number(resultSubjectiveScore.value || 0);
  return Math.max(0, Math.min(100, resultEditTotalScore.value + subj));
});

function validateSubjective(): boolean {
  const s = Number(resultSubjectiveScore.value || 0);
  if (Number.isNaN(s) || s < -20 || s > 20) {
    ElNotification({ type: 'warning', title: '主观评分超出范围（±20），请修改', message: '', position: 'top-right', duration: 3000 });
    return false;
  }
  if (resultFinalTotal.value < 0 || resultFinalTotal.value > 100) {
    ElNotification({ type: 'warning', title: '总分超出范围（0~100），请调整主观评分', message: '', position: 'top-right', duration: 3000 });
    return false;
  }
  return true;
}

async function saveResultDraft() {
  if (!resultEditSub.value) return;
  if (resultEditSub.value.approvalStatus === 2) { ElNotification({ type: 'warning', title: '评价已审批，不可保存', message: '', position: 'top-right', duration: 3000 }); return; }
  if (!validateSubjective()) return;
  for (const item of resultEditResults.value) {
    if (item.id == null) continue;
    const hasData = item._manualScore != null || item._manualComment;
    if (hasData) {
      try { await submitManualScore({ resultId: item.id, manualScore: item._manualScore, manualComment: item._manualComment || '' }); }
      catch { /* skip */ }
    }
  }
  resultEditSub.value.approvalStatus = 1;
  resultEditSub.value.draftTemplateId = resultEditTemplateId.value;
  originalSubjectiveScore.value = resultSubjectiveScore.value;
  originalSubjectiveReason.value = resultSubjectiveReason.value;
  ElNotification({ type: 'success', title: '已保存', message: '', position: 'top-right', duration: 3000 });
}

async function submitResultFinal() {
  if (resultEditSub.value?.approvalStatus === 2) { ElNotification({ type: 'warning', title: '已审批，不可重复提交', message: '', position: 'top-right', duration: 3000 }); return; }

  if (!validateSubjective()) return;

  if (hasUnsavedChanges.value) { await saveResultDraft(); }

  if (!resultEditSub.value) return;
  for (const item of resultEditResults.value) {
    if (item.id == null) continue;
    const hasData = item._manualScore != null || item._manualComment;
    if (hasData) {
      try {
        await submitManualScore({
          resultId: item.id,
          manualScore: item._manualScore,
          manualComment: item._manualComment || '',
        });
      } catch { /* skip */ }
    }
  }
  try {
    await submitEvaluationResult(resultEditSub.value.id, resultTeacherComment.value, resultSubjectiveScore.value, resultSubjectiveReason.value);
    resultEditSub.value.approvalStatus = 2;
    resultEditSub.value.submitted = 1;
    resultEditSub.value.totalScore = resultFinalTotal.value;
    resultTeacherComment.value = '';
  } catch (e: any) { ElNotification({ type: 'error', title: '提交失败', message: e?.message || '', position: 'top-right', duration: 3000 }); return; }
  ElNotification({ type: 'success', title: '评价已提交', message: '', position: 'top-right', duration: 3000 });
  resultEditSub.value = null; resultEditResults.value = [];
  resultSubjectiveScore.value = null; resultSubjectiveReason.value = '';
  originalSubjectiveScore.value = null; originalSubjectiveReason.value = '';
}

async function runResultAI() {
  const tpl = resultEditTemplateId.value ? templates.value.find(t => t.id === resultEditTemplateId.value) : null;
  if (!resultEditSub.value || !resultEditTemplateId.value) return;
  if (tpl && tpl.evalMethod === 1) return;
  resultAiRunning.value = true;
  resultAiEver.value = true;
  const aiNotif = ElNotification({ type: 'info', title: 'AI 评价中，请稍候...', message: '期间可填写其他内容，但不可保存或提交', position: 'top-right', duration: 0 });
  try {
    resultAiFailed.value = false;
    const resp = await triggerAutoScore(resultEditSub.value.id, resultEditTemplateId.value);
    const results = resp.results || [];
    const summary = resp.statusSummary;

    const oldItems = resultEditResults.value;
    resultEditResults.value = results.map(item => {
      const old = oldItems.find(o => o.indicatorId === item.indicatorId);
      return {
        ...item,
        _manualScore: old ? old._manualScore : item.manualScore,
        _manualComment: old ? (old._manualComment || '') : (item.manualComment || ''),
        _saving: false,
      };
    });

    if (summary) {
      const { contentEmpty, totalAiIndicators, aiFailed, aiScoredPositive, aiScoredZero } = summary;

      if (contentEmpty) {
        resultAiFailed.value = true;
        ElNotification({ type: 'warning', title: '提交内容为空', message: '文件解析后未获得有效内容，请检查提交文件是否正确、是否为可解析格式', position: 'top-right', duration: 5000 });
      } else if (aiFailed === totalAiIndicators && totalAiIndicators > 0) {
        resultAiFailed.value = true;
        ElNotification({ type: 'error', title: 'AI评价全部失败', message: `所有 ${totalAiIndicators} 个指标AI评价均失败，请检查AI服务配置或重试`, position: 'top-right', duration: 5000 });
      } else if (aiFailed > 0) {
        resultAiFailed.value = true;
        ElNotification({ type: 'warning', title: 'AI评价部分失败', message: `${totalAiIndicators} 个指标中 ${aiFailed} 个失败，${aiScoredPositive} 个获得有效评分。请手动补充评分或重试`, position: 'top-right', duration: 5000 });
      } else if (aiScoredPositive === 0 && aiScoredZero > 0) {
        resultAiFailed.value = true;
        ElNotification({ type: 'warning', title: 'AI评价完成：所有指标评分为0', message: 'AI认为提交内容与评价指标不匹配，所有指标评分为0。请检查提交内容或手动评分', position: 'top-right', duration: 5000 });
      } else {
        ElNotification({ type: 'success', title: 'AI评价完成', message: `${totalAiIndicators} 个指标中 ${aiScoredPositive} 个获得有效评分（>0）`, position: 'top-right', duration: 3000 });
      }
    } else {
      const hasAnyScore = results.some(r => r.autoScore != null && parseFloat(r.autoScore) > 0);
      if (!hasAnyScore) {
        resultAiFailed.value = true;
        ElNotification({ type: 'warning', title: 'AI评价完成但未获得有效评分，请检查提交内容或重试', message: '', position: 'top-right', duration: 3000 });
      } else {
        ElNotification({ type: 'success', title: 'AI评价完成', message: '', position: 'top-right', duration: 3000 });
      }
    }
  } catch {
    resultAiFailed.value = true;
    ElNotification({ type: 'error', title: 'API调用失败，请检查AI服务配置', message: '', position: 'top-right', duration: 3000 });
  } finally {
    aiNotif.close();
    resultAiRunning.value = false;
  }
}

function getTemplateModel() {
  return 'GLM-4-Flash（文本）/ GLM-4V-Flash（视觉）';
}

function onSubjectiveChange(v: number | null) {
  if (v != null && (v < -20 || v > 20)) {
    ElNotification({ type: 'warning', title: '主观评分范围为 ±20 分', message: '', position: 'top-right', duration: 3000 });
  }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.indicator-header { display: flex; justify-content: flex-end; align-items: center; margin-bottom: 8px; }
.file-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 2px; }
.file-count { font-size: 12px; color: #909399; margin-left: 6px; }
.pagination-wrap {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  margin-top: 20px; flex-wrap: wrap;
}
.pagination-wrap .el-button + .el-button { margin-left: 0; }
.page-info { font-size: 13px; color: #909399; margin-left: 16px; }

/* ====== 评价详细页新布局 ====== */

/* 顶部操作栏 */
.eval-topbar {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 14px; margin-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}
.eval-topbar-left { display: flex; align-items: center; gap: 6px; }
.eval-student { font-size: 18px; font-weight: 600; }
.eval-sep { color: #c0c4cc; margin: 0 4px; }
.eval-task { font-size: 14px; color: #606266; }
.eval-topbar-right { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

/* 教师综合评价 */
.eval-section-label { font-size: 13px; color: #909399; margin-bottom: 6px; font-weight: 500; }
.eval-teacher-comment { margin-bottom: 16px; }

/* 全局调整栏 */
.eval-adjust-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 16px; background: #f2f2f7;
  border-radius: 14px; margin-bottom: 20px;
}
.eval-adjust-label { font-size: 14px; color: #1c1c1e; font-weight: 600; white-space: nowrap; }
.eval-adjust-hint { font-size: 12px; color: #aeaeb2; white-space: nowrap; }
.eval-total-text { font-size: 13px; color: #8e8e93; }
.eval-total-score { font-size: 26px; font-weight: 700; color: #007aff; }

/* 评价指标分隔 */
.eval-section-divider {
  font-size: 15px; font-weight: 700; color: #1c1c1e; letter-spacing: 0.3px;
  padding-bottom: 10px; margin-bottom: 16px;
  border-bottom: 1px solid #ececf1;
}

/* ====== iOS 风格维度卡片网格 ====== */
.ios-dim-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  align-items: start;
}
@media (max-width: 900px) {
  .ios-dim-grid { grid-template-columns: 1fr; }
}

.ios-dim-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #ececf1;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04), 0 4px 14px rgba(0, 0, 0, 0.04);
  padding: 16px 16px 6px;
}
.ios-dim-card-head {
  display: flex; align-items: center; gap: 10px;
  padding-bottom: 12px; margin-bottom: 12px;
  border-bottom: 1px solid #f0f0f3;
}
.ios-dim-title { font-size: 16px; font-weight: 700; color: #1c1c1e; letter-spacing: 0.3px; }
.ios-dim-badge {
  font-size: 12px; font-weight: 600; color: #007aff;
  background: rgba(0, 122, 255, 0.1);
  padding: 2px 10px; border-radius: 20px;
}
.ios-dim-badge-muted { color: #8e8e93; background: rgba(142, 142, 147, 0.12); }
.ios-dim-scored { font-size: 12px; color: #8e8e93; margin-left: auto; }

.ios-ind-list { display: flex; flex-direction: column; gap: 12px; }
.ios-ind-item {
  background: #f7f7fa;
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 6px;
}
.ios-ind-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.ios-ind-name { font-weight: 600; font-size: 14px; color: #1c1c1e; }
.ios-ind-weight { font-size: 12px; color: #8e8e93; }
.ios-ind-mix { font-size: 11px; color: #8e8e93; margin-left: auto; }

.ios-score-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 10px; margin-bottom: 8px;
}
.ios-score-ai { background: rgba(0, 122, 255, 0.08); }
.ios-score-tag { font-size: 12px; font-weight: 600; color: #007aff; white-space: nowrap; }
.ios-score-tag.ai { min-width: 24px; }
.ios-score-tag.manual { color: #ff9500; }
.ios-score-val { font-size: 15px; font-weight: 700; color: #1c1c1e; }
.ios-score-comment { font-size: 12px; color: #636366; flex: 1; line-height: 1.5; }
.ios-score-pending { font-size: 12px; color: #aeaeb2; font-style: italic; }

.ios-manual-block { display: flex; flex-direction: column; gap: 8px; }
.ios-manual-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ios-input-score { width: 96px; }
.ios-input-adjust { width: 80px; }
.ios-comment-input { width: 100%; }

/* iOS 圆角输入控件 */
.ios-dim-card :deep(.el-input__wrapper),
.ios-dim-card :deep(.el-textarea__inner) {
  border-radius: 10px;
}
.ios-dim-card :deep(.el-input-number) { width: 100%; }

/* 历史记录 */
.recent-list { display: flex; flex-direction: column; gap: 4px; }
.recent-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; border-radius: 6px; cursor: pointer;
  transition: background 0.2s;
}
.recent-item:hover { background: #f5f7fa; }
.recent-item-left { display: flex; flex-direction: column; gap: 2px; }
.recent-student { font-weight: 500; font-size: 14px; }
.recent-task { font-size: 12px; color: #909399; }
.recent-item-right { display: flex; align-items: center; gap: 12px; }
.recent-time { font-size: 12px; color: #c0c4cc; }

.dim-total-warn {
  color: #f56c6c;
  font-weight: 600;
}
.dim-weight-zero {
  font-size: 13px; color: #c0c4cc;
}
</style>
