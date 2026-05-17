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
    ENABLED: { label: 'Enabled', type: 'success' },
    DISABLED: { label: 'Disabled', type: 'danger' },
    NORMAL: { label: 'Normal', type: 'success' },
    DELETED: { label: 'Deleted', type: 'danger' },
    PENDING: { label: 'Pending', type: 'warning' },
    RESOLVED: { label: 'Resolved', type: 'success' },
    IGNORED: { label: 'Ignored', type: 'info' },
    PUBLIC: { label: 'Public', type: 'success' },
    DEPARTMENT: { label: 'Department', type: 'primary' },
    PRIVATE: { label: 'Private', type: 'warning' },
    UPLOADED: { label: 'Uploaded', type: 'info' },
    PARSED: { label: 'Parsed', type: 'success' },
    PARSING: { label: 'Parsing', type: 'warning' },
    SUCCESS: { label: 'Success', type: 'success' },
    FAILED: { label: 'Failed', type: 'danger' },
    VECTORIZED: { label: 'Vectorized', type: 'success' },
    VECTORING: { label: 'Vectoring', type: 'warning' },
    PROCESSING: { label: 'Processing', type: 'warning' },
    TRUE: { label: 'Yes', type: 'danger' },
    FALSE: { label: 'No', type: 'success' }
  }
  return map[value] || { label: String(props.value ?? '-'), type: 'info' as const }
})
</script>
