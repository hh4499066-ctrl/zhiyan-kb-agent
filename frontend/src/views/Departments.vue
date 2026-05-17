<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">部门管理</h2>
        <p class="page-subtitle">维护组织结构，用于知识空间权限和统计归属。</p>
      </div>
      <el-button type="primary" @click="open()"><el-icon><Plus /></el-icon>新增部门</el-button>
    </div>
    <div class="toolbar">
      <el-button type="danger" :disabled="!selectedRows.length" @click="removeSelected"><el-icon><Delete /></el-icon>批量删除</el-button>
    </div>
    <div class="stat-strip">
      <article class="mini-stat">
        <span>部门数</span>
        <strong>{{ rows.length }}</strong>
        <small>组织结构覆盖</small>
      </article>
      <article class="mini-stat blue">
        <span>启用部门</span>
        <strong>{{ enabledCount }}</strong>
        <small>参与权限与统计归属</small>
      </article>
      <article class="mini-stat amber">
        <span>已选择</span>
        <strong>{{ selectedRows.length }}</strong>
        <small>可执行批量操作</small>
      </article>
    </div>
    <section v-loading="loading" class="panel">
      <el-table :data="pagedRows" row-key="id" @selection-change="selectedRows = $event">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="name" label="部门名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
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
    <el-dialog v-model="visible" title="部门" width="480px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="上级 ID"><el-input-number v-model="form.parentId" :min="0" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
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
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const visible = ref(false)
const form = reactive<any>({})
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
const enabledCount = computed(() => rows.value.filter((row) => row.status !== 'DISABLED').length)
async function load() { loading.value = true; try { rows.value = await http.get('/departments'); page.value = 1 } finally { loading.value = false } }
function open(row?: any) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, row || { parentId: 0, status: 'ENABLED' }); visible.value = true }
async function save() { form.id ? await http.put(`/departments/${form.id}`, form) : await http.post('/departments', form); visible.value = false; await load() }
async function remove(row: any) { await http.delete(`/departments/${row.id}`); await load() }
async function removeSelected() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 个部门？`, '批量删除', { type: 'warning' })
  for (const row of selectedRows.value) await http.delete(`/departments/${row.id}`)
  selectedRows.value = []
  await load()
}
onMounted(load)
</script>
