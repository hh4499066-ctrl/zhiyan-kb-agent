<template>
  <el-tooltip :content="tooltip" placement="top">
    <el-button :type="type" :disabled="disabled" text circle size="small" @click="$emit('click')">
      <el-icon><component :is="icon" /></el-icon>
    </el-button>
  </el-tooltip>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(defineProps<{
  tooltip: string
  icon: Component
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  disabled?: boolean
}>(), {
  type: 'primary',
  disabled: false
})

defineEmits<{
  click: []
}>()
</script>
