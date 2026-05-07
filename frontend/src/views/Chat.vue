<template>
  <div class="page chat-page">
    <aside class="panel sessions">
      <el-button type="primary" style="width:100%" @click="newSession">新建对话</el-button>
      <el-divider />
      <div class="muted label">检索范围</div>
      <el-select v-model="spaceId" placeholder="全库自动检索" style="width:100%">
        <el-option label="全库自动检索" :value="0" />
        <el-option v-for="s in spaces" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <div class="hint">不知道问题在哪个空间时保持“全库自动检索”。选择空间只是限制检索范围。</div>
      <el-button style="width:100%;margin-top:10px" @click="clear">清空当前上下文</el-button>
      <el-divider />
      <div class="history-title">历史对话</div>
      <div class="history">
        <button
          v-for="s in sessions"
          :key="s.sessionId"
          :class="['session-item', s.sessionId === sessionId ? 'active' : '']"
          @click="loadSession(s)"
        >
          <span>{{ s.title }}</span>
          <small>{{ s.updateTime }}</small>
        </button>
        <el-empty v-if="!sessions.length" description="暂无历史" :image-size="70" />
      </div>
    </aside>
    <section class="chat-main">
      <div class="messages">
        <el-empty v-if="!messages.length" description="输入问题后，系统会自动在全库中检索相关文档" />
        <div v-for="m in messages" :key="m.id" :class="['chat-bubble', m.role === 'USER' ? 'chat-user' : 'chat-ai']">
          {{ m.content }}
          <div v-if="m.refs?.length" class="refs">
            <b>引用来源</b>
            <div v-for="r in m.refs" :key="r.chunkId">《{{ r.documentTitle }}》：{{ r.chunkContent.slice(0, 120) }}...（{{ Number(r.score).toFixed(2) }}）</div>
          </div>
        </div>
      </div>
      <div class="input">
        <el-input v-model="question" type="textarea" :rows="3" :placeholder="inputPlaceholder" @keydown.ctrl.enter="ask" />
        <el-button type="primary" :loading="loading" @click="ask">发送</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
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
const inputPlaceholder = computed(() => viewingHistory.value ? '' : '请输入你的问题，例如：项目启动失败怎么办？')

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
.chat-page { display: grid; grid-template-columns: 300px 1fr; gap: 14px; height: calc(100vh - 60px); }
.sessions { height: fit-content; max-height: calc(100vh - 96px); overflow: auto; }
.label { margin-bottom: 8px; font-size: 13px; }
.hint { margin-top: 8px; color: #6b7280; font-size: 12px; line-height: 1.5; }
.history-title { font-weight: 700; margin-bottom: 8px; }
.history { display: flex; flex-direction: column; gap: 8px; }
.session-item { text-align: left; border: 1px solid #e5e7eb; background:#fff; border-radius:8px; padding:10px; cursor:pointer; color:#1f2937; }
.session-item.active { border-color:#0f766e; background:#ecfdf5; }
.session-item span { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.session-item small { display:block; margin-top:4px; color:#6b7280; }
.chat-main { display: grid; grid-template-rows: 1fr auto; gap: 12px; min-height: 0; }
.messages { overflow: auto; display: flex; flex-direction: column; gap: 12px; padding: 10px; }
.input { display: grid; grid-template-columns: 1fr 110px; gap: 10px; background:#fff; border-top:1px solid #e5e7eb; padding: 12px; }
.refs { margin-top: 10px; color: #4b5563; font-size: 13px; border-top: 1px solid #e5e7eb; padding-top: 8px; }
@media (max-width: 820px) { .chat-page { grid-template-columns: 1fr; height:auto; } .input { grid-template-columns: 1fr; } }
</style>
