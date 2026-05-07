<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">问答记录</h2>
        <p class="page-subtitle">审计问答内容、置信度和用户反馈。</p>
      </div>
      <el-button @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
    </div>
    <section v-loading="loading" class="panel">
      <el-table :data="pagedRows">
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="answer" label="回答" min-width="320" show-overflow-tooltip />
        <el-table-column prop="confidence" label="置信度" width="100" />
        <el-table-column label="未解决" width="100"><template #default="{ row }"><StatusTag :value="row.unresolved" /></template></el-table-column>
        <el-table-column label="反馈" width="104" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <ActionIconButton tooltip="有帮助" :icon="CircleCheck" @click="feedback(row, true)" />
              <ActionIconButton tooltip="无帮助" :icon="CircleClose" type="warning" @click="feedback(row, false)" />
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span>共 {{ rows.length }} 条记录</span>
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" :total="rows.length" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose, Refresh } from '@element-plus/icons-vue'
import ActionIconButton from '../components/ActionIconButton.vue'
import StatusTag from '../components/StatusTag.vue'
import { http } from '../api'

const rows = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
async function load() { loading.value = true; try { rows.value = await http.get('/chat/records'); page.value = 1 } finally { loading.value = false } }
async function feedback(row: any, helpful: boolean) {
  await http.post(`/chat/records/${row.id}/feedback`, { helpful, comment: helpful ? '有帮助' : '需要补充' })
  ElMessage.success('已反馈')
}
onMounted(load)
</script>
