<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">文档管理</h2>
        <p class="page-subtitle">上传、解析并向量化企业知识文档。</p>
      </div>
      <el-upload :http-request="upload" :show-file-list="false">
        <el-button type="primary"><el-icon><Upload /></el-icon>上传文档</el-button>
      </el-upload>
    </div>
    <div class="toolbar">
      <el-select v-model="spaceId" placeholder="知识空间" style="width:220px" @change="load">
        <el-option v-for="s in spaces" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="success" @click="router.push({ path: '/chat', query: { spaceId } })">空间问答</el-button>
      <el-button @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
      <el-button type="danger" :disabled="!selectedRows.length" @click="removeSelected"><el-icon><Delete /></el-icon>批量删除</el-button>
    </div>
    <div class="stat-strip">
      <article class="mini-stat">
        <span>文档总量</span>
        <strong>{{ rows.length }}</strong>
        <small>上传后进入解析流水线</small>
      </article>
      <article class="mini-stat blue">
        <span>已解析</span>
        <strong>{{ parsedCount }}</strong>
        <small>可被摘要和分块</small>
      </article>
      <article class="mini-stat amber">
        <span>已向量化</span>
        <strong>{{ vectorizedCount }}</strong>
        <small>可参与混合检索</small>
      </article>
    </div>
    <section v-loading="loading" class="panel">
      <el-table :data="pagedRows" stripe @selection-change="selectedRows = $event">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="80" />
        <el-table-column label="解析" width="116"><template #default="{ row }"><StatusTag :value="row.parseStatus" /></template></el-table-column>
        <el-table-column label="向量" width="124"><template #default="{ row }"><StatusTag :value="row.vectorStatus" /></template></el-table-column>
        <el-table-column prop="keywords" label="关键词" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <ActionIconButton tooltip="详情" :icon="View" @click="detail(row)" />
              <ActionIconButton tooltip="Chunk" :icon="Files" type="info" @click="chunks(row)" />
              <ActionIconButton tooltip="AI 摘要" :icon="Memo" type="success" @click="summary(row)" />
              <ActionIconButton tooltip="生成 FAQ" :icon="Tickets" type="warning" @click="faq(row)" />
              <ActionIconButton tooltip="删除" :icon="Delete" type="danger" @click="remove(row)" />
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span>已选择 {{ selectedRows.length }} 项</span>
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" :total="rows.length" />
      </div>
    </section>
    <el-drawer v-model="drawer" size="46%" :title="drawerTitle">
      <pre class="doc-text">{{ drawerText }}</pre>
      <el-table v-if="chunkRows.length" :data="chunkRows">
        <el-table-column prop="chunkIndex" label="#" width="80" />
        <el-table-column prop="content" label="内容" />
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Files, Memo, Refresh, Tickets, Upload, View } from '@element-plus/icons-vue'
import ActionIconButton from '../components/ActionIconButton.vue'
import StatusTag from '../components/StatusTag.vue'
import { http } from '../api'

const route = useRoute()
const router = useRouter()
const spaces = ref<any[]>([])
const rows = ref<any[]>([])
const selectedRows = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const spaceId = ref<number>()
const drawer = ref(false)
const drawerTitle = ref('')
const drawerText = ref('')
const chunkRows = ref<any[]>([])
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
const parsedCount = computed(() => rows.value.filter((row) => row.parseStatus === 'PARSED').length)
const vectorizedCount = computed(() => rows.value.filter((row) => row.vectorStatus === 'VECTORIZED').length)

async function loadSpaces() {
  const data = await http.get('/spaces', { params: { page: 1, size: 100 } })
  spaces.value = data.records || data
  if (!spaceId.value) spaceId.value = Number(route.query.spaceId || spaces.value[0]?.id)
}
async function load() {
  loading.value = true
  try {
    const data = await http.get('/documents', { params: { spaceId: spaceId.value, page: 1, size: 100 } })
    rows.value = data.records || data
    page.value = 1
  } finally {
    loading.value = false
  }
}
async function upload(option: any) {
  const fd = new FormData()
  fd.append('file', option.file)
  fd.append('spaceId', String(spaceId.value))
  await http.post('/documents/upload', fd)
  ElMessage.success('上传并解析完成')
  await load()
}
function detail(row: any) { drawerTitle.value = row.title; drawerText.value = row.contentText || row.summary; chunkRows.value = []; drawer.value = true }
async function chunks(row: any) { drawerTitle.value = `${row.title} - Chunk`; drawerText.value = ''; chunkRows.value = await http.get(`/documents/${row.id}/chunks`); drawer.value = true }
async function summary(row: any) { const data = await http.post(`/documents/${row.id}/ai-summary`); drawerTitle.value = 'AI 摘要'; drawerText.value = data.summary; chunkRows.value = []; drawer.value = true; await load() }
async function faq(row: any) { await http.post(`/documents/${row.id}/generate-faq`); ElMessage.success('FAQ 已生成') }
async function remove(row: any) { await http.delete(`/documents/${row.id}`); await load() }
async function removeSelected() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 个文档？`, '批量删除', { type: 'warning' })
  for (const row of selectedRows.value) await http.delete(`/documents/${row.id}`)
  selectedRows.value = []
  await load()
}
onMounted(async () => { await loadSpaces(); await load() })
</script>

<style scoped>
.doc-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #374151;
}
</style>
