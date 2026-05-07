<template>
  <div class="page">
    <div class="toolbar"><el-button type="primary" @click="open()">新增记忆</el-button></div>
    <section class="panel">
      <el-table :data="rows">
        <el-table-column prop="memoryType" label="类型" width="160" />
        <el-table-column prop="content" label="内容" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }"><el-button size="small" @click="open(row)">编辑</el-button><el-button size="small" type="danger" @click="remove(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </section>
    <el-dialog v-model="visible" title="长期记忆" width="540px">
      <el-form label-width="80px">
        <el-form-item label="类型"><el-select v-model="form.memoryType"><el-option v-for="t in types" :key="t" :label="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
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
const types = ['PREFERENCE', 'IDENTITY', 'PROJECT', 'OTHER']
async function load() { rows.value = await http.get('/memories') }
function open(row?: any) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, row || { memoryType: 'PREFERENCE' }); visible.value = true }
async function save() { form.id ? await http.put(`/memories/${form.id}`, form) : await http.post('/memories', form); visible.value = false; await load() }
async function remove(row: any) { await http.delete(`/memories/${row.id}`); await load() }
onMounted(load)
</script>
