<template>
  <div class="page chat-page">
    <aside class="panel sessions">
      <el-button type="primary" class="full" @click="newSession">
        <el-icon><Plus /></el-icon>
        新建对话
      </el-button>
      <el-divider />
      <label class="field-label">检索范围</label>
      <el-select v-model="spaceId" placeholder="全库自动检索" class="full">
        <el-option label="全库自动检索" :value="0" />
        <el-option v-for="s in spaces" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <p class="hint">不知道问题属于哪个空间时保持全库检索；选择空间后只会限定该知识空间。</p>
      <el-button class="full" @click="clear">
        <el-icon><Delete /></el-icon>
        清空当前上下文
      </el-button>
      <el-divider />
      <div class="history-title">历史对话</div>
      <div class="history">
        <button
          v-for="s in sessions"
          :key="s.sessionId"
          :class="['session-item', s.sessionId === sessionId ? 'active' : '']"
          type="button"
          @click="loadSession(s)"
        >
          <span>{{ s.title }}</span>
          <small>{{ s.updateTime }}</small>
        </button>
        <el-empty v-if="!sessions.length" description="暂无历史" :image-size="70" />
      </div>
    </aside>

    <section class="chat-main panel">
      <div class="chat-topline">
        <div>
          <h2>智能问答</h2>
          <p>自动召回相关文档，并保留当前会话上下文。</p>
        </div>
        <el-tag round type="success">RAG 检索增强</el-tag>
      </div>

      <div class="messages">
        <el-empty v-if="!messages.length" description="输入问题后，系统会自动在知识库中检索相关文档" />
        <div v-for="m in messages" :key="m.id" :class="['chat-bubble', m.role === 'USER' ? 'chat-user' : 'chat-ai']">
          {{ m.content }}
          <div v-if="m.refs?.length" class="refs">
            <b>引用来源</b>
            <div v-for="r in m.refs" :key="r.chunkId">
              《{{ r.documentTitle }}》：{{ r.chunkContent.slice(0, 120) }}...（{{ Number(r.score).toFixed(2) }}）
            </div>
          </div>
        </div>
      </div>

      <div class="input">
        <el-input v-model="question" type="textarea" :rows="3" :placeholder="inputPlaceholder" @keydown.ctrl.enter="ask" />
        <el-button type="primary" :loading="loading" @click="ask">
          <el-icon><Promotion /></el-icon>
          发送
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete, Plus, Promotion } from '@element-plus/icons-vue'
import { http } from '../api'

const route = useRoute()
const spaces = ref<any[]>([])
const sessions = ref<any[]>([])
const spaceId = ref<number>(0)
const sessionId = ref(`web-${Date.now()}`)
const question = ref('')
const loading = ref(false)
const messages = ref<any[]>([])
const viewingHistory = ref(false)
const inputPlaceholder = computed(() => viewingHistory.value ? '' : '请输入你的问题，例如：项目启动失败怎么处理？')

function newSession() {
  sessionId.value = `web-${Date.now()}`
  messages.value = []
  question.value = ''
  viewingHistory.value = false
}

async function clear() {
  await http.delete(`/chat/sessions/${sessionId.value}/memory`)
  messages.value = []
  ElMessage.success('已清空当前上下文')
}

async function loadSessions() {
  sessions.value = await http.get('/chat/sessions')
}

async function loadSession(session: any) {
  sessionId.value = session.sessionId
  spaceId.value = session.spaceId || 0
  question.value = ''
  viewingHistory.value = true
  const records = await http.get('/chat/records', { params: { sessionId: session.sessionId } })
  messages.value = records.flatMap((r: any) => [
    { id: `${r.id}-q`, role: 'USER', content: r.question },
    { id: `${r.id}-a`, role: 'ASSISTANT', content: r.answer, refs: safeRefs(r.referencesJson) }
  ])
}

async function ask() {
  if (!question.value.trim()) return
  const q = question.value
  viewingHistory.value = false
  messages.value.push({ id: Date.now(), role: 'USER', content: q })
  question.value = ''
  loading.value = true
  try {
    const data = await http.post('/chat/ask', {
      spaceId: spaceId.value || null,
      sessionId: sessionId.value,
      question: q,
      useMemory: true,
      topK: 5
    })
    messages.value.push({ id: Date.now() + 1, role: 'ASSISTANT', content: data.answer, refs: data.references })
    sessionId.value = data.sessionId
    if (data.spaceId) spaceId.value = data.spaceId
    await loadSessions()
  } finally {
    loading.value = false
  }
}

function safeRefs(json: string) {
  try {
    return JSON.parse(json || '[]')
  } catch {
    return []
  }
}

onMounted(async () => {
  spaces.value = await http.get('/spaces')
  const querySpaceId = Number(route.query.spaceId)
  spaceId.value = spaces.value.some((s) => s.id === querySpaceId) ? querySpaceId : 0
  await loadSessions()
})
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  height: calc(100vh - 78px);
  min-height: 640px;
}

.sessions {
  min-height: 0;
  overflow: auto;
}

.full {
  width: 100%;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  color: #475569;
  font-size: 13px;
  font-weight: 750;
}

.hint {
  margin: 10px 0 12px;
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
}

.history-title {
  margin-bottom: 10px;
  color: #132033;
  font-weight: 800;
}

.history {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-item {
  width: 100%;
  text-align: left;
  border: 1px solid #dfe8ee;
  background: #fff;
  border-radius: 8px;
  padding: 11px 12px;
  color: #1f2937;
  transition: border-color 200ms ease, background-color 200ms ease, box-shadow 200ms ease;
}

.session-item:hover,
.session-item.active {
  border-color: #10b6a6;
  background: #effbf8;
  box-shadow: 0 8px 20px rgba(16, 182, 166, 0.12);
}

.session-item span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 650;
}

.session-item small {
  display: block;
  margin-top: 4px;
  color: #667085;
}

.chat-main {
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 14px;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

.chat-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  border-bottom: 1px solid #e4edf1;
}

.chat-topline h2 {
  margin: 0;
  color: #132033;
  font-size: 20px;
}

.chat-topline p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.messages {
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px 20px;
}

.input {
  display: grid;
  grid-template-columns: 1fr 108px;
  gap: 10px;
  padding: 14px;
  border-top: 1px solid #e4edf1;
  background: rgba(248, 252, 252, 0.9);
}

.refs {
  margin-top: 10px;
  color: #4b5563;
  font-size: 13px;
  border-top: 1px solid rgba(226, 232, 240, 0.75);
  padding-top: 8px;
}

@media (max-width: 900px) {
  .chat-page {
    grid-template-columns: 1fr;
    height: auto;
  }

  .input {
    grid-template-columns: 1fr;
  }
}
</style>
