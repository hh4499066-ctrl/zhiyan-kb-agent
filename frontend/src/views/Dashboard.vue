<template>
  <div class="page">
    <div class="grid">
      <el-card v-for="s in stats" :key="s.label" class="stat" shadow="never">
        <div class="muted">{{ s.label }}</div>
        <h2>{{ s.value }}</h2>
      </el-card>
    </div>
    <el-alert
      v-if="aiConfig"
      style="margin-top: 16px"
      :title="`AI 模式：${aiConfig.mode === 'real' ? 'DeepSeek 真实调用' : 'Mock 演示模式'}，模型：${aiConfig.chatModel || 'mock'}`"
      :description="aiConfig.mode === 'real' ? `Base URL：${aiConfig.baseUrl}，Key 已配置：${aiConfig.apiKeyConfigured ? '是' : '否'}` : '当前无需 API Key，适合离线演示。设置 ZHIYAN_AI_MODE=real 和 DEEPSEEK_API_KEY 后可切换真实大模型。'"
      type="info"
      :closable="false"
    />
    <div class="grid" style="margin-top: 16px">
      <section class="panel"><div ref="qaChart" style="height: 300px"></div></section>
      <section class="panel"><div ref="deptChart" style="height: 300px"></div></section>
    </div>
    <section class="panel" style="margin-top: 16px">
      <h3>热门问题 Top 10</h3>
      <el-table :data="overview.topQuestions || []">
        <el-table-column type="index" width="80" />
        <el-table-column label="问题" prop="question">
          <template #default="{ row }">{{ typeof row === 'string' ? row : row.question }}</template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, nextTick, onMounted, ref } from 'vue'
import { http } from '../api'

const overview = ref<any>({})
const aiConfig = ref<any>()
const qaChart = ref<HTMLDivElement>()
const deptChart = ref<HTMLDivElement>()
const stats = computed(() => [
  { label: '知识空间', value: overview.value.spaceCount || 0 },
  { label: '文档总数', value: overview.value.documentCount || 0 },
  { label: 'Chunk 总数', value: overview.value.chunkCount || 0 },
  { label: '问答次数', value: overview.value.qaCount || 0 },
  { label: '今日问答', value: overview.value.todayQaCount || 0 },
  { label: '未解决问题', value: overview.value.unresolvedCount || 0 },
  { label: '满意度', value: `${overview.value.satisfactionRate || 0}%` }
])

onMounted(async () => {
  overview.value = await http.get('/dashboard/overview')
  aiConfig.value = await http.get('/ai-config').catch(() => null)
  await nextTick()
  echarts.init(qaChart.value!).setOption({
    title: { text: '问答趋势' },
    xAxis: { type: 'category', data: overview.value.qaTrend?.map((x: any) => x.date) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: overview.value.qaTrend?.map((x: any) => x.count), color: '#0f766e' }]
  })
  echarts.init(deptChart.value!).setOption({
    title: { text: '部门文档贡献' },
    xAxis: { type: 'category', data: overview.value.departmentContribution?.map((x: any) => x.name) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: overview.value.departmentContribution?.map((x: any) => x.count), color: '#2563eb' }]
  })
})
</script>
