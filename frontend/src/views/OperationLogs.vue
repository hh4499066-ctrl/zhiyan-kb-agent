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
    <section v-loading="loading" class="panel">
      <el-table :data="pagedRows" stripe>
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="userId" label="用户 ID" width="90" />
        <el-table-column prop="moduleName" label="模块" width="130" />
        <el-table-column prop="operation" label="操作" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="150" />
        <el-table-column prop="detail" label="参数" min-width="260" show-overflow-tooltip />
      </el-table>
      <div class="table-footer">
        <span>共 {{ rows.length }} 条日志</span>
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" :total="rows.length" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { http } from '../api'

const moduleName = ref('')
const rows = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

async function load() {
  loading.value = true
  try {
    const data = await http.get('/operation-logs', { params: { moduleName: moduleName.value || undefined, size: 100 } })
    rows.value = data.records || []
    page.value = 1
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
