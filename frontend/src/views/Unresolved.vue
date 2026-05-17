<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">未解决问题</h2>
        <p class="page-subtitle">沉淀无法回答的问题，驱动知识补齐。</p>
      </div>
      <el-button @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
    </div>
    <div class="toolbar">
      <el-select v-model="status" style="width:180px" @change="load">
        <el-option label="全部" value="" />
        <el-option label="待处理" value="PENDING" />
        <el-option label="已解决" value="RESOLVED" />
        <el-option label="已忽略" value="IGNORED" />
      </el-select>
    </div>
    <div class="stat-strip">
      <article class="mini-stat amber">
        <span>待处理</span>
        <strong>{{ pendingCount }}</strong>
        <small>需要补充或关联知识</small>
      </article>
      <article class="mini-stat">
        <span>已解决</span>
        <strong>{{ resolvedCount }}</strong>
        <small>完成知识闭环</small>
      </article>
      <article class="mini-stat rose">
        <span>已忽略</span>
        <strong>{{ ignoredCount }}</strong>
        <small>非知识库问题</small>
      </article>
    </div>
    <section v-loading="loading" class="panel">
      <el-table :data="pagedRows">
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="116"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column label="操作" width="104" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <ActionIconButton tooltip="标记已解决" :icon="CircleCheck" @click="resolve(row)" />
              <ActionIconButton tooltip="忽略" :icon="Remove" type="warning" @click="ignore(row)" />
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span>共 {{ rows.length }} 条问题</span>
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" :total="rows.length" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CircleCheck, Refresh, Remove } from '@element-plus/icons-vue'
import ActionIconButton from '../components/ActionIconButton.vue'
import StatusTag from '../components/StatusTag.vue'
import { http } from '../api'

const rows = ref<any[]>([])
const status = ref('')
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
const pendingCount = computed(() => rows.value.filter((row) => row.status === 'PENDING').length)
const resolvedCount = computed(() => rows.value.filter((row) => row.status === 'RESOLVED').length)
const ignoredCount = computed(() => rows.value.filter((row) => row.status === 'IGNORED').length)
async function load() { loading.value = true; try { rows.value = await http.get('/unresolved', { params: { status: status.value || undefined } }); page.value = 1 } finally { loading.value = false } }
async function resolve(row: any) { await http.put(`/unresolved/${row.id}/resolve`, { resolveNote: '已补充或关联知识文档' }); await load() }
async function ignore(row: any) { await http.put(`/unresolved/${row.id}/ignore`); await load() }
onMounted(load)
</script>
