<script setup lang="ts">
defineProps<{
  icon: string
  value: number | string
  label: string
  color?: string
  bgColor?: string
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()
</script>

<template>
  <el-card shadow="hover" class="stat-card" :style="bgColor ? { backgroundColor: bgColor } : {}" @click="emit('click')">
    <div class="stat-content">
      <el-icon :size="32" :color="color || '#409EFF'"><component :is="icon" /></el-icon>
      <div class="stat-value">{{ value }}</div>
      <div class="stat-label">{{ label }}</div>
    </div>
  </el-card>
</template>

<style scoped>
.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
}
.stat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--ios-label);
}
.stat-label {
  font-size: 14px;
  color: var(--ios-label-2);
}
</style>
