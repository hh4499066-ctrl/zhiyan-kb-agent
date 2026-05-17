<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">知识空间</h2>
        <p class="page-subtitle">按业务、部门或项目组织可检索知识。</p>
      </div>
      <el-button type="primary" @click="open()"><el-icon><Plus /></el-icon>新增空间</el-button>
    </div>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索知识空间" style="width:260px" clearable />
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>搜索</el-button>
    </div>
    <div class="stat-strip">
      <article class="mini-stat">
        <span>空间数</span>
        <strong>{{ rows.length }}</strong>
        <small>按团队、项目和权限组织</small>
      </article>
      <article class="mini-stat blue">
        <span>文档总量</span>
        <strong>{{ documentTotal }}</strong>
        <small>已归属到知识空间</small>
      </article>
      <article class="mini-stat amber">
        <span>问答次数</span>
        <strong>{{ qaTotal }}</strong>
        <small>空间内累计请求</small>
      </article>
    </div>
    <div v-loading="loading" class="space-grid">
      <el-card v-for="s in pagedRows" :key="s.id" class="space-card" shadow="never">
        <div class="space-card__head">
          <h3>{{ s.name }}</h3>
          <StatusTag :value="s.status" />
        </div>
        <el-tooltip :content="s.description || '暂无描述'" placement="top" :disabled="!(s.description && s.description.length > 42)">
          <p class="muted space-desc">{{ s.description || '暂无描述' }}</p>
        </el-tooltip>
        <el-descriptions :column="2" size="small">
          <el-descriptions-item label="文档">{{ s.documentCount }}</el-descriptions-item>
          <el-descriptions-item label="问答">{{ s.qaCount }}</el-descriptions-item>
          <el-descriptions-item label="可见"><StatusTag :value="s.visibility" /></el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :value="s.status" /></el-descriptions-item>
        </el-descriptions>
        <template #footer>
          <div class="space-actions">
            <el-button size="small" @click="open(s)">编辑</el-button>
            <el-button size="small" type="primary" @click="$router.push({ path: '/documents', query: { spaceId: s.id } })">进入空间</el-button>
          </div>
        </template>
      </el-card>
    </div>
    <div class="table-footer">
      <span>共 {{ rows.length }} 个空间</span>
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[8, 16, 32]" layout="total, sizes, prev, pager, next" :total="rows.length" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/StatusTag.vue'
import { http } from '../api'

const rows = ref<any[]>([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(8)
const loading = ref(false)
const visible = ref(false)
const form = reactive<any>({})
const pagedRows = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
const documentTotal = computed(() => rows.value.reduce((sum, row) => sum + Number(row.documentCount || 0), 0))
const qaTotal = computed(() => rows.value.reduce((sum, row) => sum + Number(row.qaCount || 0), 0))
async function load() {
  loading.value = true
  try {
    const data = await http.get('/spaces', { params: { keyword: keyword.value, page: 1, size: 100 } })
    rows.value = data.records || data
    page.value = 1
  } finally {
    loading.value = false
  }
}
function open(row?: any) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, row || { visibility: 'PUBLIC', status: 'NORMAL' }); visible.value = true }
async function save() { form.id ? await http.put(`/spaces/${form.id}`, form) : await http.post('/spaces', form); visible.value = false; await load() }
onMounted(load)
</script>

<style scoped>
.space-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  min-height: 180px;
}

.space-card {
  transition: transform 220ms ease, box-shadow 220ms ease, border-color 220ms ease;
}

.space-card:hover {
  transform: translateY(-3px);
  border-color: #bdece5;
  box-shadow: 0 16px 36px rgba(21, 60, 74, 0.14);
}

.space-card__head,
.space-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.space-card h3 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.space-desc {
  min-height: 44px;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.6;
}
</style>
