<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="spaceId" placeholder="知识空间" style="width:220px" @change="load">
        <el-option v-for="s in spaces" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-upload :http-request="upload" :show-file-list="false">
        <el-button type="primary">上传文档</el-button>
      </el-upload>
      <el-button type="success" @click="router.push({ path: '/chat', query: { spaceId } })">空间问答</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <section class="panel">
      <el-table :data="rows" stripe>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="fileType" label="类型" width="80" />
        <el-table-column prop="parseStatus" label="解析" width="100" />
        <el-table-column prop="vectorStatus" label="向量" width="100" />
        <el-table-column prop="keywords" label="关键词" />
        <el-table-column label="操作" width="360">
          <template #default="{ row }">
            <el-button size="small" @click="detail(row)">详情</el-button>
            <el-button size="small" @click="chunks(row)">Chunk</el-button>
            <el-button size="small" @click="summary(row)">AI 摘要</el-button>
            <el-button size="small" @click="faq(row)">生成 FAQ</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
    <el-drawer v-model="drawer" size="46%" :title="drawerTitle">
      <pre class="doc-text">{{ drawerText }}</pre>
      <el-table v-if="chunkRows.length" :data="chunkRows"><el-table-column prop="chunkIndex" label="#" width="80" /><el-table-column prop="content" label="内容" /></el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '../api'

const route = useRoute()
const router = useRouter()
const spaces = ref<any[]>([])
const rows = ref<any[]>([])
const spaceId = ref<number>()
const drawer = ref(false)
const drawerTitle = ref('')
const drawerText = ref('')
const chunkRows = ref<any[]>([])

async function loadSpaces() { spaces.value = await http.get('/spaces'); if (!spaceId.value) spaceId.value = Number(route.query.spaceId || spaces.value[0]?.id) }
async function load() { rows.value = await http.get('/documents', { params: { spaceId: spaceId.value } }) }
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
onMounted(async () => { await loadSpaces(); await load() })
</script>

<style scoped>
.doc-text { white-space: pre-wrap; line-height: 1.8; color: #374151; }
</style>
