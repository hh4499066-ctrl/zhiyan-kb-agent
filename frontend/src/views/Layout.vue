<template>
  <el-container class="shell">
    <el-aside width="236px" class="aside">
      <div class="logo">智研库</div>
      <el-menu router :default-active="$route.path" background-color="#111827" text-color="#d1d5db" active-text-color="#5eead4">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>智研库</el-breadcrumb-item>
          <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="user">
          <span>{{ auth.user?.realName }}（{{ roleName }}）</span>
          <el-button text @click="auth.logout()">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { DataBoard, User, OfficeBuilding, Collection, Document, ChatDotRound, Tickets, Guide, Warning, Clock, Memo, Notebook } from '@element-plus/icons-vue'
import { useAuthStore } from '../store'

const auth = useAuthStore()
const allMenus = [
  { path: '/dashboard', title: '数据看板', icon: DataBoard },
  { path: '/users', title: '用户管理', icon: User, roles: ['admin'] },
  { path: '/departments', title: '部门管理', icon: OfficeBuilding, roles: ['admin'] },
  { path: '/spaces', title: '知识空间', icon: Collection },
  { path: '/documents', title: '文档管理', icon: Document },
  { path: '/chat', title: '智能问答', icon: ChatDotRound },
  { path: '/memories', title: '长期记忆', icon: Memo },
  { path: '/faqs', title: 'FAQ 管理', icon: Tickets },
  { path: '/onboarding', title: '新人助手', icon: Guide },
  { path: '/unresolved', title: '未解决问题', icon: Warning },
  { path: '/records', title: '问答记录', icon: Clock },
  { path: '/operation-logs', title: '操作日志', icon: Notebook, roles: ['admin'] }
]
const menus = computed(() => allMenus.filter((m) => !m.roles || m.roles.includes(auth.user?.role || '')))
const roleName = computed(() => ({ admin: '管理员', kb_manager: '知识库管理员', employee: '员工', newcomer: '新人' }[auth.user?.role || 'employee']))
</script>

<style scoped>
.shell {
  min-height: 100vh;
}
.aside {
  background: #111827;
}
.logo {
  height: 58px;
  display: flex;
  align-items: center;
  padding-left: 22px;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}
.main {
  background: #f5f7fb;
  padding: 0;
}
.user {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
