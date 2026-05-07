<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">操作日志</h2>
        <p class="page-subtitle">查询系统关键操作和请求参数。</p>
      </div>
    </div>
    <div class="toolbar">
      <el-input v-model="moduleName" placeholder="模块名，如 documents/chat" style="width: 260px" clearable />
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
    </div>
    <section class="panel">
      <el-table :data="rows" stripe>
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="userId" label="用户 ID" width="90" />
        <el-table-column prop="moduleName" label="模块" width="130" />
        <el-table-column prop="operation" label="操作" min-width="220" />
        <el-table-column prop="ip" label="IP" width="150" />
        <el-table-column prop="detail" label="参数" show-overflow-tooltip />
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { http } from '../api'

const moduleName = ref('')
const rows = ref<any[]>([])

async function load() {
  const data = await http.get('/operation-logs', { params: { moduleName: moduleName.value || undefined, size: 100 } })
  rows.value = data.records || []
}

onMounted(load)
</script>
