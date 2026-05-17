<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">FAQ 管理</h2>
        <p class="page-subtitle">维护高频问题与标准答案，提升自动回答质量。</p>
      </div>
      <el-button type="primary" @click="open()"><el-icon><Plus /></el-icon>新增 FAQ</el-button>
    </div>
    <div class="toolbar">
      <el-select v-model="spaceId" placeholder="知识空间" style="width:220px" clearable @change="load">
        <el-option v-for="s in spaces" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="danger" :disabled="!selectedRows.length" @click="removeSelected"><el-icon><Delete /></el-icon>批量删除</el-button>
    </div>
    <div class="stat-strip">
      <article class="mini-stat">
        <span>FAQ 总量</span>
        <strong>{{ rows.length }}</strong>
        <small>标准答案资产</small>
      </article>
      <article class="mini-stat blue">
        <span>AI 生成</span>
        <strong>{{ aiCount }}</strong>
        <small>来自文档或问题沉淀</small>
      </article>
      <article class="mini-stat amber">
        <span>已选择</span>
        <strong>{{ selectedRows.length }}</strong>
        <small>可批量维护</small>
      </article>
    </div>
    <section v-loading="loading" class="panel">
      <el-table :data="pagedRows" @selection-change="selectedRows = $event">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案" min-width="260" show-overflow-tooltip />
        <el-table-column label="来源" width="120">
          <template #default="{ row }"><StatusTag :value="row.createType" /></template>
        </el-table-column>
        <el-table-column label="操作" width="104" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <ActionIconButton tooltip="编辑" :icon="Edit" @click="open(row)" />
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
    <el-dialog v-model="visible" title="FAQ" width="620px">
      <el-form label-width="80px">
        <el-form-item label="空间"><el-select v-model="form.spaceId"><el-option v-for="s in spaces" :key="s.id" :label="s.name" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="问题"><el-input v-model="form.question" /></el-form-item>
        <el-form-item label="答案"><el-input v-model="form.answer" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tags" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import ActionIconButton from '../components/ActionIconButton.vue'
import StatusTag from '../components/StatusTag.vue'
import { http } from '../api'

const rows = ref<any[]>([])
const selectedRows = ref<any[]>([])
const spaces = ref<any[]>([])
const spaceId = ref<number>()
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const visible = ref(false)
const form = reactive<any>({})
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
const aiCount = computed(() => rows.value.filter((row) => String(row.createType || '').toUpperCase() !== 'MANUAL').length)
async function load() { loading.value = true; try { rows.value = await http.get('/faqs', { params: { spaceId: spaceId.value } }); page.value = 1 } finally { loading.value = false } }
function open(row?: any) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, row || { spaceId: spaceId.value || spaces.value[0]?.id, createType: 'MANUAL' }); visible.value = true }
async function save() { form.id ? await http.put(`/faqs/${form.id}`, form) : await http.post('/faqs', form); visible.value = false; await load() }
async function remove(row: any) { await http.delete(`/faqs/${row.id}`); await load() }
async function removeSelected() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 条 FAQ？`, '批量删除', { type: 'warning' })
  for (const row of selectedRows.value) await http.delete(`/faqs/${row.id}`)
  selectedRows.value = []
  await load()
}
onMounted(async () => {
  const data = await http.get('/spaces', { params: { page: 1, size: 100 } })
  spaces.value = data.records || data
  await load()
})
</script>
