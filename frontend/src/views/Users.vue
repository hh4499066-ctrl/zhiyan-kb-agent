<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-subtitle">维护账号、角色和基础联系方式。</p>
      </div>
      <el-button type="primary" @click="open()"><el-icon><Plus /></el-icon>新增用户</el-button>
    </div>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名或姓名" style="width: 260px" clearable />
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>搜索</el-button>
      <el-button :disabled="!selectedRows.length" @click="exportSelected"><el-icon><Download /></el-icon>导出所选</el-button>
    </div>
    <div class="stat-strip">
      <article class="mini-stat">
        <span>用户总数</span>
        <strong>{{ rows.length }}</strong>
        <small>当前筛选结果</small>
      </article>
      <article class="mini-stat blue">
        <span>管理员</span>
        <strong>{{ adminCount }}</strong>
        <small>拥有系统级权限</small>
      </article>
      <article class="mini-stat amber">
        <span>已停用</span>
        <strong>{{ disabledCount }}</strong>
        <small>需要复核的账号</small>
      </article>
    </div>
    <section v-loading="loading" class="panel">
      <el-table :data="pagedRows" stripe @selection-change="selectedRows = $event">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="role" label="角色" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="138" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <ActionIconButton tooltip="编辑" :icon="Edit" @click="open(row)" />
              <ActionIconButton :tooltip="row.status === 'ENABLED' ? '禁用' : '启用'" :icon="SwitchButton" type="warning" @click="status(row)" />
              <ActionIconButton tooltip="重置密码" :icon="Key" type="info" @click="reset(row)" />
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span>已选择 {{ selectedRows.length }} 项</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="rows.length"
        />
      </div>
    </section>
    <el-dialog v-model="visible" title="用户信息" width="520px">
      <el-form label-width="90px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码"><el-input v-model="form.password" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role"><el-option v-for="r in roles" :key="r" :label="r" :value="r" /></el-select>
        </el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Edit, Key, Plus, Search, SwitchButton } from '@element-plus/icons-vue'
import ActionIconButton from '../components/ActionIconButton.vue'
import StatusTag from '../components/StatusTag.vue'
import { http } from '../api'

const keyword = ref('')
const rows = ref<any[]>([])
const selectedRows = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const visible = ref(false)
const roles = ['admin', 'kb_manager', 'employee', 'newcomer']
const form = reactive<any>({})
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
const adminCount = computed(() => rows.value.filter((row) => row.role === 'admin').length)
const disabledCount = computed(() => rows.value.filter((row) => row.status === 'DISABLED').length)

async function load() {
  loading.value = true
  try {
    const data = await http.get('/users', { params: { keyword: keyword.value, size: 100 } })
    rows.value = data.records || []
    page.value = 1
  } finally {
    loading.value = false
  }
}
function open(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { role: 'employee', status: 'ENABLED', password: '' })
  visible.value = true
}
async function save() {
  if (form.id) await http.put(`/users/${form.id}`, form)
  else await http.post('/users', form)
  visible.value = false
  await load()
}
async function status(row: any) {
  await http.put(`/users/${row.id}/status`, { status: row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED' })
  await load()
}
async function reset(row: any) {
  const { value } = await ElMessageBox.prompt('请输入至少 8 位临时密码', '重置密码', { inputType: 'password' })
  await http.put(`/users/${row.id}/reset-password`, { password: value })
  ElMessage.success('密码已重置')
}
function exportSelected() {
  const lines = ['用户名,姓名,角色,邮箱,状态', ...selectedRows.value.map((r) => [r.username, r.realName, r.role, r.email, r.status].map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`).join(','))]
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = 'users.csv'
  link.click()
  URL.revokeObjectURL(link.href)
}
onMounted(load)
</script>
