<template>
  <div class="page onboarding-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">新人学习路径</h2>
        <p class="page-subtitle">把角色、部门和知识空间转化成可执行的 7 天入职计划。</p>
      </div>
    </div>
    <section class="panel wizard-panel">
      <div class="wizard-controls">
        <label>
          <span>目标岗位</span>
          <el-select v-model="roleType">
            <el-option v-for="r in roles" :key="r" :label="r" :value="r" />
          </el-select>
        </label>
        <el-button type="primary" :loading="loading" @click="generate">
          <el-icon><Guide /></el-icon>
          AI 生成
        </el-button>
      </div>
      <div v-if="!plan" class="learning-grid">
        <article v-for="(day, index) in previewDays" :key="day" class="day-card">
          <span>Day {{ index + 1 }}</span>
          <h3>{{ day }}</h3>
          <p>推荐 3 篇文档 · 1 个练习任务 · AI 伴学问答</p>
        </article>
      </div>
      <div v-else class="generated-plan">
        <div class="generated-head">
          <div>
            <h2>{{ plan.title }}</h2>
            <p class="muted">{{ plan.description }}</p>
          </div>
          <el-tag round type="success">已生成</el-tag>
        </div>
        <el-alert title="推荐文档" :description="plan.recommendedDocuments" type="success" :closable="false" />
        <pre class="plan">{{ plan.planContent }}</pre>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Guide } from '@element-plus/icons-vue'
import { http } from '../api'

const roles = ['后端', '前端', '测试', '运维', '产品']
const previewDays = ['环境准备', '研发规范', '接口联调', '知识检索', '权限模型', '上线流程', '复盘总结']
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
.wizard-panel {
  padding: 28px;
}

.wizard-controls {
  display: flex;
  align-items: end;
  gap: 18px;
  margin-bottom: 28px;
  padding: 22px;
  border: 1px solid #dce4ee;
  border-radius: 8px;
  background: #f9fbfd;
}

.wizard-controls label {
  display: grid;
  gap: 8px;
  min-width: 240px;
}

.wizard-controls span {
  color: #647084;
  font-size: 12px;
  font-weight: 800;
}

.learning-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.day-card {
  min-height: 154px;
  padding: 18px;
  border: 1px solid #dce4ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--shadow-soft);
  transition: transform 180ms var(--ease-out), box-shadow 180ms var(--ease-out), border-color 180ms var(--ease-out);
}

.day-card:hover {
  border-color: rgba(18, 191, 174, 0.42);
  box-shadow: var(--shadow-lift);
  transform: translateY(-3px);
}

.day-card span {
  display: inline-flex;
  padding: 7px 10px;
  border-radius: 8px;
  background: #e6fbf7;
  color: #087d75;
  font-size: 12px;
  font-weight: 850;
}

.day-card h3 {
  margin: 18px 0 10px;
  color: #111827;
  font-size: 17px;
}

.day-card p {
  margin: 0;
  color: #647084;
  font-size: 12px;
  line-height: 1.7;
}

.generated-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.generated-head h2 {
  margin: 0;
  color: #111827;
}

.plan {
  margin-top: 16px;
  white-space: pre-wrap;
  line-height: 1.9;
  font-size: 15px;
  background: #f9fbfd;
  padding: 18px;
  border: 1px solid #dce4ee;
  border-radius: 8px;
}

@media (max-width: 1180px) {
  .learning-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .wizard-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .learning-grid {
    grid-template-columns: 1fr;
  }
}
</style>
