<template>
  <main class="login-screen">
    <section class="login-hero" aria-label="智研知识库登录">
      <div class="hero-copy">
        <div class="brand-row">
          <div class="brand-mark">Z</div>
          <div>
            <strong>智研知识库</strong>
            <span>AI Knowledge Base</span>
          </div>
        </div>
        <h1>企业研发知识库与 AI 智能协作体平台</h1>
        <p>集中管理项目资料、研发文档、FAQ 与新人学习路径，并通过 RAG、长期记忆和可信引用降低重复答疑成本。</p>
      </div>

      <div class="hero-dashboard" aria-hidden="true">
        <div class="knowledge-panel">
          <div class="panel-head">
            <span>Knowledge Recall</span>
            <b>92%</b>
          </div>
          <div class="knowledge-map">
            <svg viewBox="0 0 460 190" role="presentation" focusable="false">
              <defs>
                <linearGradient id="edgeGradient" x1="0" x2="1" y1="0" y2="1">
                  <stop offset="0%" stop-color="#14d7c3" />
                  <stop offset="100%" stop-color="#3b82f6" />
                </linearGradient>
              </defs>
              <path d="M86 100 C132 48 174 42 216 48 S290 48 334 48" />
              <path d="M86 100 C134 148 188 150 236 144 S302 144 354 144" />
              <path d="M214 48 C242 76 256 92 286 104 S344 108 404 104" />
              <path d="M226 144 C254 126 278 114 310 108 S362 104 404 104" />
            </svg>
            <span class="node node-primary">AI</span>
            <span class="node node-docs">Docs</span>
            <span class="node node-faq">FAQ</span>
            <span class="node node-memory">Memory</span>
            <span class="node node-search">RAG</span>
            <div class="query-card">
              <small>Query</small>
              <strong>研发规范检索</strong>
            </div>
          </div>
        </div>
        <div class="insight-grid">
          <div>
            <span>RAG 命中率</span>
            <strong>91.8%</strong>
          </div>
          <div>
            <span>待补知识</span>
            <strong>24</strong>
          </div>
        </div>
      </div>
    </section>

    <el-form class="login-card" @submit.prevent="submit">
      <div class="login-card__header">
        <h2>欢迎回来</h2>
        <p>使用企业账号进入知识协作工作台</p>
      </div>
      <el-form-item label="用户名">
        <el-input v-model="form.username" size="large" placeholder="请输入用户名" autocomplete="username">
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
      </el-form-item>
      <el-form-item label="密　码">
        <el-input
          v-model="form.password"
          size="large"
          type="password"
          placeholder="请输入密码"
          show-password
          autocomplete="current-password"
        >
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
      </el-form-item>
      <el-button class="login-button" type="primary" size="large" :loading="loading" native-type="submit">
        登录平台
      </el-button>
      <div class="samples">
        <span>演示账号：admin / manager / zhangsan / newcomer</span>
        <b>密码均为 123456</b>
      </div>
      <div class="demo-accounts">
        <span>演示账号密码</span>
        <div class="sample-grid">
          <b>admin / 123456</b>
          <b>manager / 123456</b>
          <b>zhangsan / 123456</b>
          <b>newcomer / 123456</b>
        </div>
      </div>
      <div class="security-row">
        <span>安全审计开启</span>
        <span>Mock AI Ready</span>
      </div>
    </el-form>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../store'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const showDemoAccounts = import.meta.env.VITE_SHOW_DEMO_ACCOUNTS === 'true'
const form = reactive({
  username: showDemoAccounts ? 'admin' : '',
  password: showDemoAccounts ? '123456' : ''
})

async function submit() {
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-screen {
  position: relative;
  height: 100vh;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 620px) 420px;
  align-items: center;
  justify-content: center;
  gap: 64px;
  padding: 48px 40px;
  overflow: hidden;
  background: #071426;
}

.login-screen::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(18, 191, 174, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(18, 191, 174, 0.06) 1px, transparent 1px);
  background-size: 76px 76px;
  mask-image: linear-gradient(to bottom, transparent, black 12%, black 86%, transparent);
}

.login-screen::after {
  content: "";
  position: absolute;
  width: 720px;
  height: 720px;
  right: -220px;
  top: -260px;
  background: radial-gradient(circle, rgba(18, 191, 174, 0.22), transparent 62%);
}

.login-hero,
.login-card {
  position: relative;
  z-index: 1;
}

.login-hero {
  display: grid;
  gap: 24px;
  color: #fff;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.brand-mark {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border: 1px solid #1d3c52;
  border-radius: 8px;
  background: #0f2335;
  color: #12bfae;
  font-size: 22px;
  font-weight: 900;
}

.brand-row strong,
.brand-row span {
  display: block;
}

.brand-row strong {
  font-size: 18px;
}

.brand-row span {
  margin-top: 2px;
  color: #94a3b8;
  font-size: 12px;
}

.hero-copy h1 {
  max-width: 600px;
  margin: 0;
  color: transparent;
  font-size: clamp(40px, 5vw, 58px);
  font-weight: 850;
  line-height: 1.05;
  background: linear-gradient(112deg, #ffffff 0%, #dffdf8 36%, #48d6c8 64%, #7aa7ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  text-shadow: 0 18px 44px rgba(18, 191, 174, 0.16);
}

.hero-copy p {
  max-width: 540px;
  margin: 18px 0 0;
  color: #b8c3d1;
  font-size: 16px;
  line-height: 1.7;
}

.hero-dashboard {
  width: min(100%, 520px);
  padding: 18px;
  border: 1px solid #1d3c52;
  border-radius: 8px;
  background: #0d2235;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.32);
}

.knowledge-panel {
  position: relative;
  overflow: hidden;
  padding: 18px;
  border: 1px solid rgba(20, 215, 195, 0.28);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(20, 215, 195, 0.08), rgba(59, 130, 246, 0.05)),
    #102c40;
}

.knowledge-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.07) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.78), transparent);
  pointer-events: none;
}

.panel-head,
.insight-grid {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.panel-head span,
.insight-grid span {
  color: #94a3b8;
  font-size: 13px;
}

.panel-head b {
  color: #12bfae;
  font-size: 22px;
}

.knowledge-map {
  position: relative;
  height: 160px;
  margin-top: 10px;
}

.knowledge-map svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.knowledge-map path {
  fill: none;
  stroke: url(#edgeGradient);
  stroke-width: 2;
  stroke-linecap: round;
  opacity: 0.68;
  stroke-dasharray: 7 9;
  animation: flow-line 3.2s linear infinite;
}

.node {
  position: absolute;
  display: grid;
  place-items: center;
  border: 1px solid rgba(20, 215, 195, 0.38);
  border-radius: 8px;
  background: rgba(7, 20, 38, 0.88);
  color: #dffdf8;
  box-shadow: 0 12px 28px rgba(18, 191, 174, 0.14);
  font-size: 12px;
  font-weight: 850;
}

.node-primary {
  left: 34px;
  top: 62px;
  width: 64px;
  height: 48px;
  background: linear-gradient(135deg, #12bfae, #2563eb);
  color: #fff;
  font-size: 18px;
}

.node-docs {
  left: 136px;
  top: 14px;
  width: 70px;
  height: 36px;
}

.node-faq {
  left: 146px;
  bottom: 16px;
  width: 62px;
  height: 36px;
}

.node-memory {
  left: 238px;
  bottom: 18px;
  width: 88px;
  height: 36px;
}

.node-search {
  right: 18px;
  top: 68px;
  width: 66px;
  height: 40px;
}

.query-card {
  position: absolute;
  left: 226px;
  top: 14px;
  min-width: 122px;
  padding: 10px 12px;
  border: 1px solid rgba(59, 130, 246, 0.42);
  border-radius: 8px;
  background: rgba(8, 24, 44, 0.9);
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.14);
}

.query-card small {
  display: block;
  color: #7dd3fc;
  font-size: 11px;
  font-weight: 800;
}

.query-card strong {
  display: block;
  margin-top: 4px;
  color: #fff;
  font-size: 14px;
}

.insight-grid {
  margin-top: 14px;
}

.insight-grid div {
  flex: 1;
  padding: 16px 18px;
  border: 1px solid #214a5e;
  border-radius: 8px;
  background: #102c40;
}

.insight-grid strong {
  display: block;
  margin-top: 6px;
  color: #12bfae;
  font-size: 28px;
}

.login-card {
  width: 420px;
  padding: 44px;
  border: 1px solid rgba(220, 228, 238, 0.96);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(18px);
}

.login-card__header {
  margin-bottom: 28px;
}

.login-card h2 {
  margin: 0;
  color: #111827;
  font-size: 30px;
  font-weight: 850;
}

.login-card p {
  margin: 8px 0 0;
  color: #647084;
}

.login-card :deep(.el-form-item__label) {
  white-space: nowrap;
  word-break: keep-all;
}

.login-button {
  width: 100%;
  height: 48px;
  margin-top: 4px;
}

.samples {
  display: none;
}

.demo-accounts {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.demo-accounts span {
  display: block;
  margin-bottom: 10px;
  color: #334155;
  font-size: 13px;
  font-weight: 850;
}

.sample-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 12px;
}

.sample-grid b {
  min-width: 0;
  padding: 8px 10px;
  border-radius: 7px;
  background: #eef6ff;
  color: #0f3a66;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.security-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.security-row span {
  padding: 7px 10px;
  border-radius: 8px;
  background: #eaf1ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
}

@keyframes flow-line {
  from {
    stroke-dashoffset: 0;
  }
  to {
    stroke-dashoffset: -32;
  }
}

@media (max-width: 980px) {
  .login-screen {
    grid-template-columns: 1fr;
    gap: 30px;
    padding: 42px 18px;
  }

  .hero-dashboard {
    display: none;
  }

  .login-card {
    width: min(100%, 420px);
    padding: 30px 22px;
  }
}
</style>
