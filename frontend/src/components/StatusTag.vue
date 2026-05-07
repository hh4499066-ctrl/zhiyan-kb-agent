<template>
  <el-tag :type="meta.type" effect="light" round>
    {{ meta.label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value: string | boolean | number | null | undefined
}>()

const meta = computed(() => {
  const value = String(props.value ?? '').toUpperCase()
  const map: Record<string, { label: string; type: 'success' | 'warning' | 'danger' | 'info' | 'primary' }> = {
    ENABLED: { label: '启用', type: 'success' },
    DISABLED: { label: '禁用', type: 'danger' },
    NORMAL: { label: '正常', type: 'success' },
    DELETED: { label: '已删除', type: 'danger' },
    PENDING: { label: '待处理', type: 'warning' },
    RESOLVED: { label: '已解决', type: 'success' },
    IGNORED: { label: '已忽略', type: 'info' },
    PUBLIC: { label: '公开', type: 'success' },
    DEPARTMENT: { label: '部门', type: 'primary' },
    PRIVATE: { label: '私有', type: 'warning' },
    PARSED: { label: '已解析', type: 'success' },
    PARSING: { label: '解析中', type: 'warning' },
    FAILED: { label: '失败', type: 'danger' },
    VECTORIZED: { label: '已向量化', type: 'success' },
    VECTORING: { label: '向量化中', type: 'warning' },
    TRUE: { label: '是', type: 'danger' },
    FALSE: { label: '否', type: 'success' }
  }
  return map[value] || { label: String(props.value ?? '-'), type: 'info' as const }
})
</script>
