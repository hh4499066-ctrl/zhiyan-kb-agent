<template>
  <div class="page">
    <div class="toolbar"><el-button @click="load">刷新</el-button></div>
    <section class="panel">
      <el-table :data="rows">
        <el-table-column prop="question" label="问题" min-width="220" />
        <el-table-column prop="answer" label="回答" min-width="320" show-overflow-tooltip />
        <el-table-column prop="confidence" label="置信度" width="100" />
        <el-table-column prop="unresolved" label="未解决" width="100" />
        <el-table-column label="反馈" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="feedback(row, true)">有帮助</el-button>
            <el-button size="small" @click="feedback(row, false)">无帮助</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api'

const rows = ref<any[]>([])
async function load() { rows.value = await http.get('/chat/records') }
async function feedback(row: any, helpful: boolean) { await http.post(`/chat/records/${row.id}/feedback`, { helpful, comment: helpful ? '有帮助' : '需要补充' }); ElMessage.success('已反馈') }
onMounted(load)
</script>
