<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="status" style="width:180px" @change="load"><el-option label="全部" value="" /><el-option label="待处理" value="PENDING" /><el-option label="已解决" value="RESOLVED" /><el-option label="已忽略" value="IGNORED" /></el-select>
      <el-button @click="load">刷新</el-button>
    </div>
    <section class="panel">
      <el-table :data="rows">
        <el-table-column prop="question" label="问题" />
        <el-table-column prop="reason" label="原因" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="resolve(row)">标记已解决</el-button>
            <el-button size="small" @click="ignore(row)">忽略</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../api'

const rows = ref<any[]>([])
const status = ref('')
async function load() { rows.value = await http.get('/unresolved', { params: { status: status.value || undefined } }) }
async function resolve(row: any) { await http.put(`/unresolved/${row.id}/resolve`, { resolveNote: '已补充或关联知识文档' }); await load() }
async function ignore(row: any) { await http.put(`/unresolved/${row.id}/ignore`); await load() }
onMounted(load)
</script>
