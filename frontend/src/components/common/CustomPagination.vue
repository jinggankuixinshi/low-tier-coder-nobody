<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  currentPage: number
  totalPages: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'page-change', page: number): void
}>()

const jumpVisible = ref(false)
const jumpPageNum = ref(1)

const visiblePages = computed(() => {
  const pages = props.totalPages
  if (pages <= 10) {
    return Array.from({ length: pages }, (_, i) => i + 1)
  }
  const cp = props.currentPage
  if (cp <= 5) return [1, 2, 3, 4, 5, 6, -1, pages]
  if (cp >= pages - 4) return [1, -1, pages - 5, pages - 4, pages - 3, pages - 2, pages - 1, pages]
  return [1, -1, cp - 2, cp - 1, cp, cp + 1, cp + 2, -1, pages]
})

function showJumpInput() {
  jumpVisible.value = true
  jumpPageNum.value = 1
}

function doJump() {
  if (jumpPageNum.value >= 1 && jumpPageNum.value <= props.totalPages) {
    emit('page-change', jumpPageNum.value)
  }
  jumpVisible.value = false
}
</script>

<template>
  <div class="custom-pagination">
    <el-button :disabled="currentPage <= 1" @click="emit('page-change', currentPage - 1)" size="small">上一页</el-button>
    <el-button
      v-for="p in visiblePages"
      :key="p"
      :type="p === currentPage ? 'primary' : ''"
      size="small"
      :disabled="p === -1"
      @click="p !== -1 && emit('page-change', p)"
    >
      {{ p === -1 ? '...' : p }}
    </el-button>
    <el-button :disabled="currentPage >= totalPages" @click="emit('page-change', currentPage + 1)" size="small">下一页</el-button>
    <el-button size="small" @click="showJumpInput">跳转</el-button>
    <span class="page-info">共 {{ total }} 条 / {{ totalPages }} 页</span>

    <el-dialog v-model="jumpVisible" title="跳转到" width="300px" append-to-body>
      <el-input-number v-model="jumpPageNum" :min="1" :max="totalPages" />
      <template #footer>
        <el-button @click="jumpVisible = false">取消</el-button>
        <el-button type="primary" @click="doJump">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.custom-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 16px 0;
}
.page-info {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}
</style>
