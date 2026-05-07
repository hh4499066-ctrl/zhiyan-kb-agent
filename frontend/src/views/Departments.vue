<template>
  <div class="page">
    <div class="toolbar"><el-button type="primary" @click="open()">新增部门</el-button></div>
    <section class="panel">
      <el-table :data="rows" row-key="id">
        <el-table-column prop="name" label="部门名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="open(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
    <el-dialog v-model="visible" title="部门" width="480px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="上级ID"><el-input-number v-model="form.parentId" :min="0" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { http } from '../api'

const rows = ref<any[]>([])
const visible = ref(false)
const form = reactive<any>({})
async function load() { rows.value = await http.get('/departments') }
function open(row?: any) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, row || { parentId: 0, status: 'ENABLED' }); visible.value = true }
async function save() { form.id ? await http.put(`/departments/${form.id}`, form) : await http.post('/departments', form); visible.value = false; await load() }
async function remove(row: any) { await http.delete(`/departments/${row.id}`); await load() }
onMounted(load)
</script>
