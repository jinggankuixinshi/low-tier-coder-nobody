<script setup lang="ts">
import { computed } from 'vue'
import { useFileTag } from '@/composables/useFileTag'
import { useFormatSize } from '@/composables/useFormatSize'

const props = defineProps<{
  fileNames: string | string[]
  fileSizes?: string | null
  maxTags?: number
}>()

const { getExt, fileTagType, parseArray } = useFileTag()
const { formatSize } = useFormatSize()

const namesArr = computed(() => parseArray(props.fileNames))
const sizesArr = computed(() => {
  if (!props.fileSizes) return []
  try { return JSON.parse(props.fileSizes) as number[] } catch { return [] }
})

const displayNames = computed(() => {
  return props.maxTags ? namesArr.value.slice(0, props.maxTags) : namesArr.value
})

const moreCount = computed(() => Math.max(0, namesArr.value.length - (props.maxTags || Infinity)))
</script>

<template>
  <div class="file-tags">
    <el-tooltip
      v-for="(name, idx) in displayNames"
      :key="idx"
      :content="`${name} (${sizesArr[idx] !== undefined ? formatSize(sizesArr[idx]) : '?'})`"
      placement="top"
    >
      <el-tag :type="fileTagType(name)" size="small">{{ name.length > 30 ? name.slice(0, 28) + '...' : name }}</el-tag>
    </el-tooltip>
    <el-tag v-if="moreCount > 0" size="small" type="info">+{{ moreCount }} 个</el-tag>
  </div>
</template>

<style scoped>
.file-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
