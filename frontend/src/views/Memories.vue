<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">长期记忆</h2>
        <p class="page-subtitle">记录用户偏好、身份和项目背景，辅助连续对话。</p>
      </div>
      <el-button type="primary" @click="open()"><el-icon><Plus /></el-icon>新增记忆</el-button>
    </div>
    <div class="stat-strip">
      <article class="mini-stat">
        <span>记忆总量</span>
        <strong>{{ rows.length }}</strong>
        <small>跨会话个性化上下文</small>
      </article>
      <article class="mini-stat blue">
        <span>身份记忆</span>
        <strong>{{ identityCount }}</strong>
        <small>用于角色化回答</small>
      </article>
      <article class="mini-stat amber">
        <span>项目记忆</span>
        <strong>{{ projectCount }}</strong>
        <small>用于研发背景召回</small>
      </article>
    </div>
    <section v-loading="loading" class="panel">
      <el-table :data="pagedRows">
        <el-table-column label="类型" width="160">
          <template #default="{ row }"><StatusTag :value="row.memoryType" /></template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
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
        <span>共 {{ rows.length }} 条记忆</span>
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" :total="rows.length" />
      </div>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import ActionIconButton from '../components/ActionIconButton.vue'
import StatusTag from '../components/StatusTag.vue'
import { http } from '../api'

const rows = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const visible = ref(false)
const form = reactive<any>({})
const types = ['PREFERENCE', 'IDENTITY', 'PROJECT', 'OTHER']
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
const identityCount = computed(() => rows.value.filter((row) => row.memoryType === 'IDENTITY').length)
const projectCount = computed(() => rows.value.filter((row) => row.memoryType === 'PROJECT').length)
async function load() { loading.value = true; try { rows.value = await http.get('/memories'); page.value = 1 } finally { loading.value = false } }
function open(row?: any) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, row || { memoryType: 'PREFERENCE' }); visible.value = true }
async function save() { form.id ? await http.put(`/memories/${form.id}`, form) : await http.post('/memories', form); visible.value = false; await load() }
async function remove(row: any) { await http.delete(`/memories/${row.id}`); await load() }
onMounted(load)
</script>
