<script setup lang="ts">
interface Props {
  title: string
  icon?: string
}

defineProps<Props>()
</script>

<template>
  <div class="page-header">
    <h2>
      <el-icon v-if="icon" :size="22"><component :is="icon" /></el-icon>
      {{ title }}
    </h2>
    <div class="page-header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-header-actions {
  display: flex;
  gap: 8px;
}
</style>
