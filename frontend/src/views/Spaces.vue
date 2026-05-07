<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索知识空间" style="width:260px" clearable />
      <el-button type="primary" @click="load">搜索</el-button>
      <el-button @click="open()">新增空间</el-button>
    </div>
    <div class="grid">
      <el-card v-for="s in rows" :key="s.id" shadow="never">
        <h3>{{ s.name }}</h3>
        <p class="muted">{{ s.description }}</p>
        <el-descriptions :column="2" size="small">
          <el-descriptions-item label="文档">{{ s.documentCount }}</el-descriptions-item>
          <el-descriptions-item label="问答">{{ s.qaCount }}</el-descriptions-item>
          <el-descriptions-item label="可见">{{ s.visibility }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ s.status }}</el-descriptions-item>
        </el-descriptions>
        <template #footer>
          <el-button size="small" @click="open(s)">编辑</el-button>
          <el-button size="small" type="primary" @click="$router.push({ path: '/documents', query: { spaceId: s.id } })">进入空间</el-button>
        </template>
      </el-card>
    </div>
    <el-dialog v-model="visible" title="知识空间" width="560px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="可见范围"><el-select v-model="form.visibility"><el-option label="PUBLIC" value="PUBLIC" /><el-option label="DEPARTMENT" value="DEPARTMENT" /><el-option label="PRIVATE" value="PRIVATE" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { http } from '../api'

const rows = ref<any[]>([])
const keyword = ref('')
const visible = ref(false)
const form = reactive<any>({})
async function load() { rows.value = await http.get('/spaces', { params: { keyword: keyword.value } }) }
function open(row?: any) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, row || { visibility: 'PUBLIC', status: 'NORMAL' }); visible.value = true }
async function save() { form.id ? await http.put(`/spaces/${form.id}`, form) : await http.post('/spaces', form); visible.value = false; await load() }
onMounted(load)
</script>
