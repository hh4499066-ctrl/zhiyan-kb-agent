<template>
  <main class="login">
    <section class="brand">
      <h1>智研库</h1>
      <p>企业研发知识库与 AI 智能协作体平台</p>
      <div class="samples">
        演示账号：admin / manager / zhangsan / newcomer，密码均为 123456
      </div>
    </section>
    <el-form class="login-box" @submit.prevent="submit">
      <h2>登录系统</h2>
      <el-form-item>
        <el-input v-model="form.username" size="large" placeholder="用户名" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="form.password" size="large" type="password" placeholder="密码" show-password />
      </el-form-item>
      <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">登录</el-button>
    </el-form>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

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
.login {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(320px, 1fr) 420px;
  background: linear-gradient(135deg, #eef7f5, #f7f8fb 55%, #fff);
}
.brand {
  padding: 12vh 8vw;
}
.brand h1 {
  font-size: 56px;
  margin: 0 0 16px;
  color: #0f766e;
  letter-spacing: 0;
}
.brand p {
  font-size: 22px;
  color: #243447;
}
.samples {
  margin-top: 28px;
  color: #475569;
}
.login-box {
  align-self: center;
  margin-right: 8vw;
  padding: 28px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.12);
}
@media (max-width: 820px) {
  .login {
    grid-template-columns: 1fr;
    padding: 20px;
  }
  .brand {
    padding: 42px 8px 10px;
  }
  .brand h1 {
    font-size: 42px;
  }
  .login-box {
    margin: 0;
  }
}
</style>
