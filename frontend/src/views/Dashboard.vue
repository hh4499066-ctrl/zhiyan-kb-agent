<template>
  <div class="page dashboard-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">运营概览</h2>
        <p class="page-subtitle">跟踪知识空间、文档解析、问答质量和待补充知识。</p>
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

    <div class="metric-grid">
      <section v-for="s in stats" :key="s.label" class="metric-card">
        <div class="metric-icon" :class="s.tone">
          <el-icon><component :is="s.icon" /></el-icon>
        </div>
        <div>
          <span>{{ s.label }}</span>
          <strong>{{ s.value }}</strong>
          <small>{{ s.hint }}</small>
        </div>
      </section>
    </div>

    <section class="filter-bar">
      <div class="range-control">
        <span>时间范围：</span>
        <el-select model-value="7" style="width: 132px">
          <el-option label="近 7 天" value="7" />
          <el-option label="近 30 天" value="30" />
        </el-select>
        <el-button @click="loadData">刷新</el-button>
      </div>
      <div class="range-control">
        <span>粒度：</span>
        <el-select model-value="day" style="width: 118px">
          <el-option label="按天" value="day" />
          <el-option label="按周" value="week" />
        </el-select>
      </div>
    </section>

    <div class="chart-grid">
      <section class="panel chart-panel">
        <h3>问答趋势</h3>
        <div ref="qaChart" class="chart"></div>
      </section>
      <section class="panel chart-panel">
        <h3>部门文档贡献</h3>
        <div ref="deptChart" class="chart"></div>
      </section>
    </div>

    <div class="bottom-grid">
      <section class="panel">
        <div class="section-heading">
          <h3>热门问题 Top 10</h3>
          <el-tag round>按次数排序</el-tag>
        </div>
        <el-table :data="overview.topQuestions || []">
          <el-table-column type="index" width="72" />
          <el-table-column label="问题" prop="question" min-width="260" />
          <el-table-column label="次数" prop="count" width="96" align="right">
            <template #default="{ row }">{{ row.count || 1 }}</template>
          </el-table-column>
        </el-table>
      </section>
      <section class="panel quick-panel">
        <h3>快捷操作</h3>
        <el-button type="primary" @click="$router.push('/chat')">
          <el-icon><ChatDotRound /></el-icon>
          发起问答
        </el-button>
        <el-button @click="$router.push('/documents')">
          <el-icon><Document /></el-icon>
          管理文档
        </el-button>
        <el-button @click="$router.push('/unresolved')">
          <el-icon><Warning /></el-icon>
          处理未解决问题
        </el-button>
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
import { ChatDotRound, Collection, DataLine, Document, Files, Refresh, Tickets, TrendCharts, Warning } from '@element-plus/icons-vue'
import { http } from '../api'

const overview = ref<any>({})
const aiConfig = ref<any>()
const aiDialogVisible = ref(false)
const qaChart = ref<HTMLDivElement>()
const deptChart = ref<HTMLDivElement>()
let qaInstance: echarts.ECharts | undefined
let deptInstance: echarts.ECharts | undefined

const stats = computed(() => [
  { label: '知识空间', value: overview.value.spaceCount || 0, hint: '可检索空间', icon: Collection, tone: 'mint' },
  { label: '文档总数', value: overview.value.documentCount || 0, hint: '已纳入资产', icon: Document, tone: 'blue' },
  { label: 'Chunk 总数', value: overview.value.chunkCount || 0, hint: '语义片段', icon: Files, tone: 'violet' },
  { label: '问答次数', value: overview.value.qaCount || 0, hint: '累计请求', icon: ChatDotRound, tone: 'mint' },
  { label: '今日问答', value: overview.value.todayQaCount || 0, hint: '今日请求', icon: TrendCharts, tone: 'amber' },
  { label: '未解决问题', value: overview.value.unresolvedCount || 0, hint: '待补充知识', icon: Warning, tone: 'rose' },
  { label: '满意度', value: `${overview.value.satisfactionRate || 0}%`, hint: '反馈质量', icon: Tickets, tone: 'blue' },
  { label: '平均响应', value: '0ms', hint: '当前统计', icon: DataLine, tone: 'violet' }
])

const aiModeName = computed(() => aiConfig.value?.mode === 'real' ? 'DeepSeek 真实调用' : 'Mock 演示模式')
const aiModeTitle = computed(() => `AI 模式：${aiModeName.value}`)

async function loadData() {
  overview.value = await http.get('/dashboard/overview')
  aiConfig.value = await http.get('/ai-config').catch(() => null)
  await nextTick()
  renderCharts()
}

function renderCharts() {
  const trend = overview.value.qaTrend || []
  const departments = overview.value.departmentContribution || []

  qaInstance?.dispose()
  deptInstance?.dispose()
  qaInstance = echarts.init(qaChart.value!)
  deptInstance = echarts.init(deptChart.value!)

  qaInstance.setOption({
    grid: { top: 30, right: 16, bottom: 32, left: 38 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map((x: any) => x.date), axisLine: { lineStyle: { color: '#d8e5ea' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf2f5' } } },
    series: [{ type: 'line', smooth: true, data: trend.map((x: any) => x.count), color: '#10b6a6', areaStyle: { color: 'rgba(16, 182, 166, 0.12)' } }]
  })
  deptInstance.setOption({
    grid: { top: 30, right: 16, bottom: 32, left: 38 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: departments.map((x: any) => x.name), axisLine: { lineStyle: { color: '#d8e5ea' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf2f5' } } },
    series: [{ type: 'bar', data: departments.map((x: any) => x.count), color: '#6376f2', barWidth: 24, itemStyle: { borderRadius: [6, 6, 0, 0] } }]
  })
}

onMounted(loadData)
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
  color: #079989;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  min-height: 112px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  border: 1px solid rgba(214, 229, 234, 0.92);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-soft);
}

.metric-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 8px;
  font-size: 22px;
}

.metric-icon.mint { background: #d9fbef; color: #079989; }
.metric-icon.blue { background: #dfeaff; color: #2563eb; }
.metric-icon.violet { background: #ece6ff; color: #7c3aed; }
.metric-icon.amber { background: #fff1cc; color: #d97706; }
.metric-icon.rose { background: #ffe1e6; color: #e11d48; }

.metric-card span,
.metric-card small {
  display: block;
  color: #667085;
}

.metric-card span {
  font-size: 14px;
}

.metric-card strong {
  display: block;
  margin-top: 2px;
  color: #132033;
  font-size: 28px;
  line-height: 1.1;
}

.metric-card small {
  margin-top: 5px;
  font-size: 13px;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 22px;
  padding: 18px;
  border: 1px solid rgba(214, 229, 234, 0.92);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-soft);
}

.range-control {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #475569;
}

.chart-grid,
.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
  margin-top: 22px;
}

.chart-panel h3,
.section-heading h3,
.quick-panel h3 {
  margin: 0;
  color: #132033;
  font-size: 18px;
}

.chart {
  height: 300px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.quick-panel {
  display: grid;
  align-content: start;
  gap: 12px;
}

.quick-panel .el-button {
  justify-content: flex-start;
  width: 100%;
  margin-left: 0;
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .metric-grid,
  .chart-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar,
  .range-control {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-actions {
    justify-content: flex-start;
  }
}
</style>
