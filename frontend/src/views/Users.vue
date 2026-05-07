<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名/姓名" style="width: 260px" clearable />
      <el-button type="primary" @click="load">搜索</el-button>
      <el-button @click="open()">新增用户</el-button>
    </div>
    <section class="panel">
      <el-table :data="rows" stripe>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="role" label="角色" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" @click="open(row)">编辑</el-button>
            <el-button size="small" @click="status(row)">{{ row.status === 'ENABLED' ? '禁用' : '启用' }}</el-button>
            <el-button size="small" @click="reset(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
    <el-dialog v-model="visible" title="用户信息" width="520px">
      <el-form label-width="90px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="密码" v-if="!form.id"><el-input v-model="form.password" /></el-form-item>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api'

const keyword = ref('')
const rows = ref<any[]>([])
const visible = ref(false)
const roles = ['admin', 'kb_manager', 'employee', 'newcomer']
const form = reactive<any>({})

async function load() {
  const data = await http.get('/users', { params: { keyword: keyword.value, size: 100 } })
  rows.value = data.records || []
}
function open(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { role: 'employee', status: 'ENABLED', password: '123456' })
  visible.value = true
}
async function save() {
  if (form.id) await http.put(`/users/${form.id}`, form)
  else await http.post('/users', form)
  visible.value = false
  await load()
}
async function status(row: any) {
  await http.put(`/users/${row.id}/status`, null, { params: { status: row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED' } })
  await load()
}
async function reset(row: any) {
  await http.put(`/users/${row.id}/reset-password`, null, { params: { password: '123456' } })
  ElMessage.success('已重置为 123456')
}
onMounted(load)
</script>
