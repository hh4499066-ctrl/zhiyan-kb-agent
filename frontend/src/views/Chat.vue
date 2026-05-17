<template>
  <div :class="['page chat-page', isChatSidebarCollapse ? 'chat-sidebar-collapsed' : '']">
    <aside class="panel sessions">
      <div class="sessions-head">
        <h3>会话</h3>
        <el-button type="primary" @click="newSession">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>
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

    <button class="chat-sidebar-toggle" type="button" @click="isChatSidebarCollapse = !isChatSidebarCollapse">
      <el-icon>
        <ArrowRight v-if="isChatSidebarCollapse" />
        <ArrowLeft v-else />
      </el-icon>
    </button>

    <section :class="['chat-main panel', messages.length ? 'has-messages' : 'empty-state']">
      <div class="chat-topline">
        <div>
          <h2>智研 AI</h2>
          <p>带引用来源、置信度和上下文记忆的企业 AI 助手</p>
        </div>
        <div class="trust-pills">
          <el-tag round type="success">RAG 检索增强</el-tag>
          <el-tag round>引用来源</el-tag>
        </div>
      </div>

      <div class="messages">
        <div v-if="!messages.length" class="empty-chat">
          <span class="empty-kicker">智研 AI</span>
          <strong>今天想了解什么？</strong>
          <span>可以从一个研发问题、流程规范或排障场景开始。</span>
        </div>
        <div v-for="m in messages" :key="m.id" :class="['chat-bubble', m.role === 'USER' ? 'chat-user' : 'chat-ai']">
          <template v-if="m.role === 'ASSISTANT'">
            <span class="ai-mark" aria-label="DeepSeek">
              <img :src="deepseekLogo" alt="" />
            </span>
              <div class="ai-body">
              <div v-if="m.thinking" class="thinking-indicator">
                <span></span>
                <span></span>
                <span></span>
                <em>正在思考</em>
              </div>
              <div v-else class="message-content" v-html="renderAssistantMessage(m.content)"></div>
              <div v-if="m.refs?.length" class="refs">
                <b>引用来源</b>
                <div v-for="r in m.refs" :key="r.chunkId">
                  《{{ r.documentTitle }}》：{{ r.chunkContent.slice(0, 120) }}...（{{ Number(r.score).toFixed(2) }}）
                </div>
              </div>
            </div>
          </template>
          <div v-else class="message-content">{{ m.content }}</div>
        </div>
      </div>

      <div class="input">
        <div class="composer">
          <textarea
            v-model="question"
            class="composer-textarea"
            :placeholder="inputPlaceholder"
            rows="1"
            @keydown="handleQuestionKeydown"
          ></textarea>
          <div class="composer-toolbar">
            <div class="composer-actions">
              <div :class="['model-picker', messages.length ? 'open-up' : 'open-down']">
                <button class="composer-speed" type="button" @click="isModelMenuOpen = !isModelMenuOpen">
                  <span>{{ selectedModelLabel }}</span>
                  <el-icon><ArrowDown /></el-icon>
                </button>
                <div v-if="isModelMenuOpen" class="model-menu">
                  <div class="model-menu-title">DeepSeek V4</div>
                  <button
                    v-for="option in modelOptions"
                    :key="option.value"
                    :class="['model-menu-item', selectedModel === option.value ? 'active' : '']"
                    type="button"
                    @click="selectChatModel(option.value)"
                  >
                    <span>
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                    <el-icon v-if="selectedModel === option.value"><Check /></el-icon>
                  </button>
                </div>
              </div>
              <button class="composer-send" type="button" :disabled="loading || !question.trim()" @click="ask" aria-label="发送">
                <el-icon><Promotion /></el-icon>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowLeft, ArrowRight, Check, Plus, Promotion } from '@element-plus/icons-vue'
import { http } from '../api'
import deepseekLogo from '../assets/deepseek-logo-icon.svg'

const route = useRoute()
const spaces = ref<any[]>([])
const sessions = ref<any[]>([])
const spaceId = ref<number>(0)
const sessionId = ref(`web-${Date.now()}`)
const question = ref('')
const loading = ref(false)
const messages = ref<any[]>([])
const viewingHistory = ref(false)
const isChatSidebarCollapse = ref(false)
const isModelMenuOpen = ref(false)
const selectedModel = ref('deepseek-v4-flash')
const modelOptions = [
  { label: '快速', value: 'deepseek-v4-flash', description: '低延迟回答' },
  { label: '专业', value: 'deepseek-v4-pro', description: '复杂问题与代码任务' }
]
const inputPlaceholder = computed(() => viewingHistory.value ? '' : '请输入你的问题，例如：项目启动失败怎么处理？')
const selectedModelLabel = computed(() => modelOptions.find((option) => option.value === selectedModel.value)?.label || '快速')

function newSession() {
  sessionId.value = `web-${Date.now()}`
  messages.value = []
  question.value = ''
  viewingHistory.value = false
}

async function loadSessions() {
  const data = await http.get('/chat/sessions', { params: { page: 1, size: 50 } })
  sessions.value = data.records || data
}

async function loadSession(session: any) {
  sessionId.value = session.sessionId
  spaceId.value = session.spaceId || 0
  question.value = ''
  viewingHistory.value = true
  const data = await http.get('/chat/records', { params: { sessionId: session.sessionId, page: 1, size: 100 } })
  const records = data.records || data
  messages.value = records.flatMap((r: any) => [
    { id: `${r.id}-q`, role: 'USER', content: r.question },
    { id: `${r.id}-a`, role: 'ASSISTANT', content: r.answer, refs: safeRefs(r.referencesJson) }
  ])
}

async function ask() {
  if (!question.value.trim()) return
  const q = question.value
  const userMessageId = Date.now()
  const thinkingMessageId = userMessageId + 1
  viewingHistory.value = false
  messages.value.push({ id: userMessageId, role: 'USER', content: q })
  messages.value.push({ id: thinkingMessageId, role: 'ASSISTANT', content: '', thinking: true })
  question.value = ''
  loading.value = true
  try {
    const data = await http.post('/chat/ask', {
      spaceId: spaceId.value || null,
      sessionId: sessionId.value,
      question: q,
      useMemory: true,
      topK: 5,
      model: selectedModel.value
    })
    const thinkingMessage = messages.value.find((message) => message.id === thinkingMessageId)
    if (thinkingMessage) {
      Object.assign(thinkingMessage, { content: data.answer, refs: data.references, thinking: false })
    }
    sessionId.value = data.sessionId
    if (data.spaceId) spaceId.value = data.spaceId
    await loadSessions()
  } catch (error) {
    messages.value = messages.value.filter((message) => message.id !== thinkingMessageId)
    ElMessage.error('回答生成失败，请稍后重试')
    throw error
  } finally {
    loading.value = false
  }
}

function selectChatModel(command: string) {
  selectedModel.value = command
  isModelMenuOpen.value = false
}

function handleQuestionKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return
  event.preventDefault()
  ask()
}

function renderAssistantMessage(content: string) {
  return escapeHtml(content || '')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function safeRefs(json: string) {
  try {
    return JSON.parse(json || '[]')
  } catch {
    return []
  }
}

onMounted(async () => {
  const spaceData = await http.get('/spaces', { params: { page: 1, size: 100 } })
  spaces.value = spaceData.records || spaceData
  const querySpaceId = Number(route.query.spaceId)
  spaceId.value = spaces.value.some((s) => s.id === querySpaceId) ? querySpaceId : 0
  await loadSessions()
})
</script>

<style scoped>
.chat-page {
  --chat-sidebar-width: 282px;
  --chat-sidebar-toggle-width: 32px;
  position: relative;
  display: flex;
  gap: 24px;
  height: calc(100vh - 78px);
  min-height: 640px;
  transition: gap 280ms var(--ease-out);
}

.chat-sidebar-collapsed {
  gap: 0;
}

.sessions {
  position: relative;
  z-index: 11;
  width: var(--chat-sidebar-width);
  flex: 0 0 var(--chat-sidebar-width);
  min-height: 0;
  overflow: auto;
  transition: width 280ms var(--ease-out), flex-basis 280ms var(--ease-out), padding 280ms var(--ease-out), border-width 280ms var(--ease-out), opacity 180ms var(--ease-out);
}

.chat-sidebar-collapsed .sessions {
  width: 0;
  flex-basis: 0;
  min-width: 0;
  padding-right: 0;
  padding-left: 0;
  border-width: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
}

.sessions-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 20px;
}

.sessions-head h3 {
  margin: 0;
  color: #111827;
  font-size: 17px;
}

.chat-sidebar-toggle {
  position: absolute;
  top: 50%;
  left: calc(var(--chat-sidebar-width) + 24px);
  z-index: 10;
  width: var(--chat-sidebar-toggle-width);
  height: 42px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid #dce4ee;
  border-left: 0;
  border-radius: 0 999px 999px 0;
  background: #fff;
  color: #647084;
  box-shadow: 0 10px 24px rgba(7, 20, 38, 0.12);
  transform: translateY(-50%);
  transition: left 280ms var(--ease-out), border-color 180ms var(--ease-out), color 180ms var(--ease-out), box-shadow 180ms var(--ease-out);
}

.chat-sidebar-toggle:hover {
  border-color: #12bfae;
  color: #087d75;
}

.chat-sidebar-collapsed .chat-sidebar-toggle {
  left: 0;
}

.history-title {
  margin: 6px 0 10px;
  color: #111827;
  font-weight: 850;
}

.history {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-item {
  width: 100%;
  text-align: left;
  border: 1px solid #dce4ee;
  background: #f9fbfd;
  border-radius: 8px;
  padding: 12px;
  color: #111827;
  transition: transform 160ms var(--ease-out), border-color 160ms var(--ease-out), background-color 160ms var(--ease-out), box-shadow 160ms var(--ease-out);
}

.session-item:hover,
.session-item.active {
  border-color: #12bfae;
  background: #e6fbf7;
  box-shadow: 0 10px 22px rgba(18, 191, 174, 0.12);
  transform: translateY(-2px);
}

.session-item span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 750;
}

.session-item small {
  display: block;
  margin-top: 4px;
  color: #647084;
}

.chat-main {
  position: relative;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 0;
  flex: 1 1 auto;
  width: 0;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

.chat-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 24px 30px;
  border-bottom: 1px solid #dce4ee;
}

.chat-topline h2 {
  margin: 0;
  color: #111827;
  font-size: 20px;
  font-weight: 850;
}

.chat-topline p {
  margin: 5px 0 0;
  color: #647084;
  font-size: 13px;
}

.trust-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.messages {
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 24px 30px 136px;
  transition: padding-bottom 420ms var(--ease-out);
}

.empty-state .messages {
  padding-bottom: 260px;
}

.empty-chat {
  display: grid;
  gap: 7px;
  place-items: center;
  min-height: 42%;
  color: #647084;
  text-align: center;
  transform: translateY(-10px);
  transition: transform 420ms var(--ease-out), opacity 260ms var(--ease-out);
}

.has-messages .empty-chat {
  opacity: 0;
  transform: translateY(-38px);
}

.empty-chat strong {
  color: #162033;
  font-size: 22px;
  font-weight: 850;
  letter-spacing: 0;
}

.empty-chat span:last-child {
  max-width: 420px;
  color: #718096;
  font-size: 14px;
  line-height: 1.7;
}

.empty-kicker {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border: 1px solid rgba(18, 191, 174, 0.24);
  border-radius: 999px;
  background: rgba(236, 253, 248, 0.72);
  color: #087d75;
  font-size: 12px;
  font-weight: 800;
}

.input {
  position: absolute;
  z-index: 20;
  left: 16px;
  right: 16px;
  bottom: 14px;
  width: auto;
  padding: 0;
  border-top: 0;
  background: transparent;
  box-shadow: none;
  transform: translate(0, 0);
  transition: left 520ms var(--ease-out), right 520ms var(--ease-out), bottom 520ms var(--ease-out), width 520ms var(--ease-out), transform 520ms var(--ease-out);
}

.empty-state .input {
  left: 50%;
  right: auto;
  bottom: 50%;
  width: min(760px, calc(100% - 72px));
  transform: translate(-50%, 58%);
}

.composer {
  display: grid;
  gap: 14px;
  min-height: 92px;
  padding: 18px 22px 14px;
  border: 1px solid rgba(18, 191, 174, 0.44);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(236, 253, 248, 0.98), rgba(247, 254, 252, 0.98));
  color: #0f172a;
  box-shadow: 0 16px 34px rgba(18, 191, 174, 0.14);
}

.composer-textarea {
  width: 100%;
  min-height: 28px;
  max-height: 138px;
  resize: none;
  border: 0;
  outline: 0;
  padding: 0;
  background: transparent;
  color: #0f172a;
  font: inherit;
  font-size: 15px;
  line-height: 1.7;
}

.composer-textarea::placeholder {
  color: #7a8798;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.composer-actions {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.model-picker {
  position: relative;
}

.composer-speed,
.composer-send {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: #087d75;
  font: inherit;
}

.composer-speed {
  gap: 6px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 750;
  color: #087d75;
}

.model-menu {
  position: absolute;
  right: -56px;
  z-index: 30;
  width: 320px;
  overflow: hidden;
  border: 1px solid rgba(18, 191, 174, 0.24);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(248, 254, 252, 0.98), rgba(236, 253, 248, 0.98));
  color: #0f172a;
  box-shadow: 0 18px 42px rgba(8, 125, 117, 0.18);
  animation-duration: 180ms;
  animation-timing-function: var(--ease-out);
  animation-fill-mode: both;
  transform-origin: right bottom;
}

.open-up .model-menu {
  bottom: calc(100% + 12px);
  animation-name: modelMenuUp;
  transform-origin: right bottom;
}

.open-down .model-menu {
  top: calc(100% + 12px);
  animation-name: modelMenuDown;
  transform-origin: right top;
}

@keyframes modelMenuUp {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes modelMenuDown {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.model-menu-title {
  padding: 16px 16px 8px;
  color: #087d75;
  font-size: 14px;
  font-weight: 850;
}

.model-menu-item {
  width: 100%;
  min-height: 54px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 16px;
  border: 0;
  background: transparent;
  color: #0f172a;
  text-align: left;
  transition: background-color 160ms var(--ease-out);
}

.model-menu-item:hover,
.model-menu-item.active {
  background: rgba(18, 191, 174, 0.12);
}

.model-menu-item span {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.model-menu-item strong {
  font-size: 14px;
  font-weight: 850;
}

.model-menu-item small {
  color: #647084;
  font-size: 12px;
  line-height: 1.35;
}

.model-menu-item .el-icon {
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: #12bfae;
  color: #ffffff;
  font-size: 13px;
}

.composer-send {
  width: 34px;
  height: 30px;
  border-radius: 8px;
  color: #073b3a;
  font-size: 24px;
  transition: background-color 160ms var(--ease-out), opacity 160ms var(--ease-out), transform 160ms var(--ease-out);
}

.composer-speed:hover,
.composer-send:not(:disabled):hover {
  background: rgba(18, 191, 174, 0.12);
}

.composer-send:not(:disabled):hover {
  transform: translateY(-1px);
}

.composer-send:disabled {
  cursor: not-allowed;
  opacity: 0.38;
}

.message-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.75;
}

.message-content :deep(strong) {
  font-weight: 850;
}

.message-content :deep(code) {
  padding: 2px 5px;
  border-radius: 5px;
  background: rgba(15, 23, 42, 0.08);
  color: #0f172a;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 0.92em;
}

.refs {
  margin-top: 12px;
  color: #4b5563;
  font-size: 13px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  padding-top: 10px;
}

.refs b {
  display: block;
  margin-bottom: 6px;
  color: #087d75;
}

@media (max-width: 900px) {
  .chat-page {
    display: block;
    height: auto;
  }

  .sessions {
    width: 100%;
    margin-bottom: 16px;
  }

  .chat-main {
    width: auto;
    min-height: 640px;
  }

  .chat-sidebar-collapsed .sessions {
    width: 0;
    height: 0;
    margin-bottom: 0;
  }

  .chat-sidebar-toggle,
  .chat-sidebar-collapsed .chat-sidebar-toggle {
    display: none;
  }

  .input {
    left: 12px;
    right: 12px;
    bottom: 12px;
  }

  .empty-state .input {
    width: calc(100% - 24px);
  }

  .composer {
    border-radius: 22px;
    padding: 16px;
  }

  .composer-toolbar {
    gap: 10px;
  }
}
</style>
