<template>
  <div ref="dashboardRoot" class="page dashboard-page">
    <div class="page-header" data-motion="page-item">
      <div>
        <h2 class="page-title">知识运营驾驶舱</h2>
        <p class="page-subtitle">把空间、文档、问答和沉淀问题集中到一张可行动视图。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="aiConfig" class="ai-mode-button" @click="aiDialogVisible = true">
          {{ aiModeTitle }}
        </el-button>
        <el-button type="primary" @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="metric-grid">
      <section v-for="s in featuredStats" :key="s.label" class="metric-card" :class="s.tone">
        <span>{{ s.label }}</span>
        <div class="metric-value">
          <strong>{{ s.value }}</strong>
          <em :class="['trend', s.trend >= 0 ? 'up' : 'down']">{{ s.trend >= 0 ? '+' : '-' }}{{ Math.abs(s.trend) }}%</em>
        </div>
        <small>{{ s.hint }}</small>
      </section>
    </div>

    <div class="chart-grid">
      <section class="panel chart-panel">
        <div class="section-heading">
          <div>
            <h3>问答质量趋势</h3>
            <p>近 7 天 · 自动刷新</p>
          </div>
          <el-tag round>RAG</el-tag>
        </div>
        <div ref="qaChart" class="chart"></div>
      </section>
      <section class="panel chart-panel">
        <div class="section-heading">
          <div>
            <h3>知识沉淀趋势</h3>
            <p>部门文档贡献与活跃度</p>
          </div>
          <el-tag round type="success">Live</el-tag>
        </div>
        <div ref="deptChart" class="chart"></div>
      </section>
    </div>

    <div class="bottom-grid">
      <section class="panel">
        <div class="section-heading">
          <div>
            <h3>热门问题 Top 10</h3>
            <p>按最近问答次数排序</p>
          </div>
          <el-tag round>高频</el-tag>
        </div>
        <el-table :data="overview.topQuestions || []">
          <el-table-column type="index" width="72" />
          <el-table-column label="问题" prop="question" min-width="260" />
          <el-table-column label="次数" prop="count" width="96" align="right">
            <template #default="{ row }">{{ row.count || 1 }}</template>
          </el-table-column>
        </el-table>
      </section>
      <section class="panel priority-panel">
        <div class="section-heading">
          <div>
            <h3>今日优先处理</h3>
            <p>低置信、无命中和知识缺口</p>
          </div>
        </div>
        <div class="priority-list">
          <button v-for="item in priorities" :key="item.title" type="button" @click="$router.push(item.to)">
            <span :class="item.tone">{{ item.label }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.desc }}</small>
          </button>
        </div>
      </section>
    </div>

    <el-dialog v-model="aiDialogVisible" title="AI 模式详情" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="模式">{{ aiModeName }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ aiConfig?.chatModel || 'mock' }}</el-descriptions-item>
        <el-descriptions-item label="Base URL">{{ aiConfig?.baseUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Key 已配置">{{ aiConfig?.apiKeyConfigured ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="aiDialogVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { http } from '../api'
import { useAnimeMotion } from '../composables/useAnimeMotion'

const overview = ref<any>({})
const aiConfig = ref<any>()
const aiDialogVisible = ref(false)
const dashboardRoot = ref<HTMLElement>()
const qaChart = ref<HTMLDivElement>()
const deptChart = ref<HTMLDivElement>()
let qaInstance: echarts.ECharts | undefined
let deptInstance: echarts.ECharts | undefined
const motion = useAnimeMotion()

const loading = ref(false)
const allStats = computed(() => [
  { label: '知识空间', value: overview.value.spaceCount || 0, hint: '可检索空间', tone: 'teal', trend: 4 },
  { label: '文档总量', value: overview.value.documentCount || 0, hint: '已纳入资产', tone: 'blue', trend: 8 },
  { label: '问答次数', value: overview.value.qaCount || 0, hint: '累计请求', tone: 'violet', trend: 12 },
  { label: '未解决问题', value: overview.value.unresolvedCount || 0, hint: '待补充知识', tone: 'amber', trend: -3 },
  { label: 'Chunk 总数', value: overview.value.chunkCount || 0, hint: '语义片段', tone: 'blue', trend: 6 },
  { label: '今日问答', value: overview.value.todayQaCount || 0, hint: '今日请求', tone: 'teal', trend: 5 },
  { label: '满意度', value: `${overview.value.satisfactionRate || 0}%`, hint: '反馈质量', tone: 'blue', trend: 2 }
])
const featuredStats = computed(() => allStats.value.slice(0, 4))

const priorities = computed(() => [
  { label: '高优先级', title: '处理未解决问题', desc: `${overview.value.unresolvedCount || 0} 条待补充`, tone: 'rose', to: '/unresolved' },
  { label: '待审核', title: '复盘问答质量', desc: '查看低分反馈与引用来源', tone: 'blue', to: '/records' },
  { label: '知识沉淀', title: '维护 FAQ', desc: '从文档生成候选答案', tone: 'teal', to: '/faqs' },
  { label: '训练路径', title: '生成新人计划', desc: '面向岗位推荐文档', tone: 'amber', to: '/onboarding' }
])

const aiModeName = computed(() => aiConfig.value?.mode === 'real' ? 'DeepSeek 真实调用' : 'Mock 演示模式')
const aiModeTitle = computed(() => `AI 模式：${aiModeName.value}`)

async function loadData() {
  loading.value = true
  try {
    overview.value = await http.get('/dashboard/overview')
    aiConfig.value = await http.get('/ai-config').catch(() => null)
    await nextTick()
    renderCharts()
    motion.enterDashboard(dashboardRoot)
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  const trend = overview.value.qaTrend || []
  const departments = overview.value.departmentContribution || []

  qaInstance?.dispose()
  deptInstance?.dispose()
  if (!qaChart.value || !deptChart.value) return
  qaInstance = echarts.init(qaChart.value)
  deptInstance = echarts.init(deptChart.value)

  qaInstance.setOption({
    animationDuration: 620,
    animationEasing: 'cubicOut',
    grid: { top: 18, right: 16, bottom: 32, left: 38 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map((x: any) => x.date), axisLine: { lineStyle: { color: '#dce4ee' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf2f5', type: 'dashed' } } },
    series: [{
      name: '问答次数',
      type: 'line',
      smooth: true,
      data: trend.map((x: any) => x.count),
      color: '#12bfae',
      areaStyle: { color: 'rgba(18, 191, 174, 0.12)' },
      symbolSize: 8
    }]
  })
  deptInstance.setOption({
    animationDuration: 620,
    animationEasing: 'cubicOut',
    grid: { top: 18, right: 16, bottom: 32, left: 38 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: departments.map((x: any) => x.name), axisLine: { lineStyle: { color: '#dce4ee' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf2f5', type: 'dashed' } } },
    series: [{
      name: '文档数',
      type: 'bar',
      data: departments.map((x: any) => x.count),
      color: '#2563eb',
      barWidth: 24,
      itemStyle: { borderRadius: [6, 6, 0, 0] }
    }]
  })
}

onMounted(async () => {
  await motion.enterPage(dashboardRoot)
  await loadData()
})
onBeforeUnmount(() => {
  qaInstance?.dispose()
  deptInstance?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  padding-top: 28px;
}

.page-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-mode-button {
  border-color: #bdece5;
  background: #effbf8;
  color: #087d75;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px;
}

.metric-card {
  position: relative;
  min-height: 118px;
  padding: 20px 22px;
  overflow: hidden;
  border: 1px solid rgba(220, 228, 238, 0.92);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-soft);
  transition: transform 180ms var(--ease-out), box-shadow 180ms var(--ease-out), border-color 180ms var(--ease-out);
  animation: card-in 220ms var(--ease-out) both;
}

.metric-card:hover {
  transform: translateY(-3px);
}

.metric-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: #12bfae;
}

.metric-card.blue::before { background: #2563eb; }
.metric-card.violet::before { background: #7057ff; }
.metric-card.amber::before { background: #f59e0b; }

.metric-card span,
.metric-card small {
  display: block;
  color: #647084;
}

.metric-card span {
  font-size: 13px;
  font-weight: 700;
}

.metric-card strong {
  color: #111827;
  font-size: 32px;
  line-height: 1.1;
}

.metric-value {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.trend {
  padding: 5px 9px;
  border-radius: 8px;
  background: #e6fbf7;
  color: #087d75;
  font-size: 12px;
  font-style: normal;
  font-weight: 850;
}

.trend.down {
  background: #fff7e6;
  color: #b45309;
}

.metric-card small {
  margin-top: 10px;
  font-size: 12px;
}

.chart-grid,
.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.85fr);
  gap: 22px;
  margin-top: 22px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.section-heading h3 {
  margin: 0;
  color: #111827;
  font-size: 17px;
  font-weight: 850;
}

.section-heading p {
  margin: 5px 0 0;
  color: #647084;
  font-size: 12px;
}

.chart {
  height: 280px;
}

.priority-list {
  display: grid;
  gap: 12px;
}

.priority-list button {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  column-gap: 14px;
  row-gap: 4px;
  width: 100%;
  padding: 14px;
  border: 1px solid #dce4ee;
  border-radius: 8px;
  background: #f9fbfd;
  text-align: left;
  transition: transform 160ms var(--ease-out), border-color 160ms var(--ease-out), background-color 160ms var(--ease-out);
}

.priority-list button:hover {
  border-color: rgba(18, 191, 174, 0.45);
  background: #fff;
  transform: translateY(-2px);
}

.priority-list span {
  grid-row: span 2;
  align-self: center;
  padding: 7px 9px;
  border-radius: 8px;
  background: #eaf1ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 850;
  text-align: center;
}

.priority-list .rose { background: #fff1f2; color: #e5484d; }
.priority-list .teal { background: #e6fbf7; color: #087d75; }
.priority-list .amber { background: #fff7e6; color: #b45309; }

.priority-list strong {
  color: #111827;
  font-size: 14px;
}

.priority-list small {
  color: #647084;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1180px) {
  .metric-grid,
  .chart-grid,
  .bottom-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .metric-grid,
  .chart-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .page-actions {
    justify-content: flex-start;
  }
}
</style>
