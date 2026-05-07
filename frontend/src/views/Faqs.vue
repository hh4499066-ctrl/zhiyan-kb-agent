<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="spaceId" placeholder="知识空间" style="width:220px" clearable @change="load">
        <el-option v-for="s in spaces" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="open()">新增 FAQ</el-button>
    </div>
    <section class="panel">
      <el-table :data="rows">
        <el-table-column prop="question" label="问题" min-width="220" />
        <el-table-column prop="answer" label="答案" />
        <el-table-column prop="createType" label="来源" width="120" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }"><el-button size="small" @click="open(row)">编辑</el-button><el-button size="small" type="danger" @click="remove(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
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
import { onMounted, reactive, ref } from 'vue'
import { http } from '../api'

const rows = ref<any[]>([])
const spaces = ref<any[]>([])
const spaceId = ref<number>()
const visible = ref(false)
const form = reactive<any>({})
async function load() { rows.value = await http.get('/faqs', { params: { spaceId: spaceId.value } }) }
function open(row?: any) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, row || { spaceId: spaceId.value || spaces.value[0]?.id, createType: 'MANUAL' }); visible.value = true }
async function save() { form.id ? await http.put(`/faqs/${form.id}`, form) : await http.post('/faqs', form); visible.value = false; await load() }
async function remove(row: any) { await http.delete(`/faqs/${row.id}`); await load() }
onMounted(async () => { spaces.value = await http.get('/spaces'); await load() })
</script>
