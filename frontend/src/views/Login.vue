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
        <div class="hero-chart">
          <div class="chart-head">
            <span>问答质量趋势</span>
            <b>92%</b>
          </div>
          <div class="chart-line">
            <i style="height: 42%"></i>
            <i style="height: 62%"></i>
            <i style="height: 50%"></i>
            <i style="height: 76%"></i>
            <i style="height: 68%"></i>
            <i style="height: 88%"></i>
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
      <el-form-item label="密码">
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
      <div v-if="showDemoAccounts" class="samples">
        <span>演示账号：admin / manager / zhangsan / newcomer</span>
        <b>密码均为 123456</b>
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
  grid-template-columns: minmax(0, 680px) 420px;
  align-items: center;
  justify-content: center;
  gap: 72px;
  padding: 72px 40px;
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
  gap: 34px;
  color: #fff;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 34px;
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
  max-width: 640px;
  margin: 0;
  color: #fff;
  font-size: clamp(40px, 5vw, 64px);
  font-weight: 850;
  line-height: 1.05;
}

.hero-copy p {
  max-width: 560px;
  margin: 22px 0 0;
  color: #b8c3d1;
  font-size: 17px;
  line-height: 1.8;
}

.hero-dashboard {
  width: min(100%, 560px);
  padding: 24px;
  border: 1px solid #1d3c52;
  border-radius: 8px;
  background: #0d2235;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.32);
}

.hero-chart {
  padding: 22px;
  border: 1px solid #214a5e;
  border-radius: 8px;
  background: #102c40;
}

.chart-head,
.insight-grid {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.chart-head span,
.insight-grid span {
  color: #94a3b8;
  font-size: 13px;
}

.chart-head b {
  color: #12bfae;
  font-size: 24px;
}

.chart-line {
  height: 154px;
  display: flex;
  align-items: end;
  gap: 18px;
  margin-top: 28px;
  border-bottom: 1px solid #214a5e;
}

.chart-line i {
  width: 32px;
  border-radius: 6px 6px 0 0;
  background: linear-gradient(180deg, #12bfae, #2563eb);
  animation: bar-grow 620ms var(--ease-out) both;
}

.insight-grid {
  margin-top: 18px;
}

.insight-grid div {
  flex: 1;
  padding: 22px;
  border: 1px solid #214a5e;
  border-radius: 8px;
  background: #102c40;
}

.insight-grid strong {
  display: block;
  margin-top: 8px;
  color: #12bfae;
  font-size: 32px;
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

.login-button {
  width: 100%;
  height: 48px;
  margin-top: 4px;
}

.samples {
  margin-top: 18px;
  color: #647084;
  font-size: 13px;
  line-height: 1.7;
}

.samples span,
.samples b {
  display: block;
}

.samples b {
  color: #111827;
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

@keyframes bar-grow {
  from {
    transform: scaleY(0.4);
    transform-origin: bottom;
    opacity: 0;
  }
  to {
    transform: scaleY(1);
    transform-origin: bottom;
    opacity: 1;
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
