<template>
  <div class="page">
    <section class="panel">
      <div class="toolbar">
        <el-select v-model="roleType" style="width:200px">
          <el-option v-for="r in roles" :key="r" :label="r" :value="r" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="generate">生成学习路径</el-button>
      </div>
      <el-empty v-if="!plan" description="请选择岗位并生成新人学习计划" />
      <div v-else>
        <h2>{{ plan.title }}</h2>
        <p class="muted">{{ plan.description }}</p>
        <el-alert title="推荐文档" :description="plan.recommendedDocuments" type="success" :closable="false" />
        <pre class="plan">{{ plan.planContent }}</pre>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { http } from '../api'

const roles = ['后端', '前端', '测试', '运维', '产品']
const roleType = ref('后端')
const loading = ref(false)
const plan = ref<any>()
async function generate() {
  loading.value = true
  try { plan.value = await http.post('/onboarding/generate-plan', { roleType: roleType.value }) }
  finally { loading.value = false }
}
</script>

<style scoped>
.plan { white-space: pre-wrap; line-height: 1.9; font-size: 15px; background:#f8fafc; padding:16px; border-radius:8px; }
</style>
