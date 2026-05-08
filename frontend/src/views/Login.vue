<template>
  <main class="login-screen">
    <div class="grid-bg"></div>
    <section class="login-hero" aria-label="智研知识库登录">
      <div class="brand-mark">
        <span>Z</span>
      </div>
      <h1>智研知识库</h1>
      <p>企业研发知识库与 AI 智能协作平台</p>
    </section>

    <el-form class="login-card" @submit.prevent="submit">
      <div class="login-card__header">
        <h2>欢迎回来</h2>
        <p>登录账号后继续管理知识资产</p>
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
        登录
      </el-button>
      <div v-if="showDemoAccounts" class="samples">
        演示账号：admin / manager / zhangsan / newcomer，密码均为 123456
      </div>
    </el-form>

    <footer class="login-footer">© 2026 Zhiyan KB Agent. All rights reserved.</footer>
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
const showDemoAccounts = import.meta.env.DEV || import.meta.env.VITE_SHOW_DEMO_ACCOUNTS === 'true'
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
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 88px 20px 64px;
  overflow: hidden;
  background:
    radial-gradient(circle at 86% 12%, rgba(42, 220, 197, 0.24), transparent 28%),
    radial-gradient(circle at 12% 86%, rgba(42, 220, 197, 0.2), transparent 28%),
    linear-gradient(135deg, #f7fbfb 0%, #edf8f6 52%, #fbfefe 100%);
}

.grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(18, 107, 113, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(18, 107, 113, 0.06) 1px, transparent 1px);
  background-size: 76px 76px;
  mask-image: linear-gradient(to bottom, transparent, black 12%, black 88%, transparent);
}

.login-hero,
.login-card,
.login-footer {
  position: relative;
  z-index: 1;
}

.login-hero {
  text-align: center;
  margin-bottom: 30px;
}

.brand-mark {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  margin: 0 auto 18px;
  border: 8px solid #061426;
  border-radius: 18px;
  background: linear-gradient(145deg, #061426, #0d2036);
  box-shadow: 0 18px 44px rgba(12, 151, 139, 0.25);
}

.brand-mark span {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border: 5px solid #2ddfca;
  border-left-color: #8297ff;
  border-right-color: #8297ff;
  border-radius: 12px;
  color: #ffffff;
  font-size: 20px;
  font-weight: 900;
  transform: rotate(-8deg);
}

.login-hero h1 {
  margin: 0;
  color: #0aa99b;
  font-size: 38px;
  font-weight: 850;
  line-height: 1.15;
}

.login-hero p {
  margin: 10px 0 0;
  color: #667085;
  font-size: 16px;
}

.login-card {
  width: min(100%, 520px);
  padding: 38px;
  border: 1px solid rgba(214, 232, 236, 0.9);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 28px 80px rgba(14, 73, 81, 0.14);
  backdrop-filter: blur(18px);
}

.login-card__header {
  text-align: center;
  margin-bottom: 26px;
}

.login-card h2 {
  margin: 0;
  color: #132033;
  font-size: 28px;
  font-weight: 850;
}

.login-card p {
  margin: 8px 0 0;
  color: #667085;
}

.login-button {
  width: 100%;
  height: 48px;
  margin-top: 4px;
}

.samples {
  margin-top: 18px;
  color: #667085;
  font-size: 13px;
  line-height: 1.7;
  text-align: center;
}

.login-footer {
  margin-top: 30px;
  color: #98a2b3;
  font-size: 13px;
}

@media (max-width: 560px) {
  .login-screen {
    padding: 42px 16px 34px;
  }

  .login-hero h1 {
    font-size: 32px;
  }

  .login-card {
    padding: 26px 20px;
  }
}
</style>
