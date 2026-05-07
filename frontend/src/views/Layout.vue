<template>
  <el-container class="shell">
    <el-aside class="aside" width="252px">
      <div class="logo">
        <div class="logo-mark">Z</div>
        <div>
          <strong>智研知识库</strong>
          <span>AI Knowledge Base</span>
        </div>
      </div>
      <el-menu class="nav-menu" router :default-active="$route.path">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <div class="mini-card">
          <span>知识库健康度</span>
          <strong>98%</strong>
        </div>
      </div>
    </el-aside>

    <el-container class="content-shell">
      <el-header class="header">
        <div>
          <h1>{{ $route.meta.title }}</h1>
          <p>欢迎回来，这是您的企业知识平台概览。</p>
        </div>
        <div class="header-actions">
          <el-dropdown>
            <button class="user-menu" type="button">
              <span class="avatar">{{ avatarText }}</span>
              <span>
                <strong>{{ auth.user?.realName || auth.user?.username }}</strong>
                <small>{{ roleName }}</small>
              </span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="auth.logout()">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
import {
  ArrowDown,
  ChatDotRound,
  Clock,
  Collection,
  DataBoard,
  Document,
  Guide,
  Memo,
  Notebook,
  OfficeBuilding,
  Tickets,
  User,
  Warning
} from '@element-plus/icons-vue'
import { useAuthStore } from '../store'

const auth = useAuthStore()
const allMenus = [
  { path: '/dashboard', title: '仪表盘', icon: DataBoard },
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
const roleName = computed(() => ({
  admin: '管理员',
  kb_manager: '知识库管理员',
  employee: '员工',
  newcomer: '新人'
}[auth.user?.role || 'employee']))
const avatarText = computed(() => (auth.user?.realName || auth.user?.username || 'U').slice(0, 1).toUpperCase())
</script>

<style scoped>
.shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 30% 16%, rgba(23, 214, 192, 0.18), transparent 24%),
    linear-gradient(135deg, #f7fbfb 0%, #edf8f6 46%, #f8fafc 100%);
}

.aside {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e1edf1;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(18px);
}

.logo {
  height: 78px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 22px;
  color: #071426;
}

.logo-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 8px;
  background: #071426;
  color: #32dec9;
  font-weight: 900;
  box-shadow: 0 10px 28px rgba(6, 20, 38, 0.16);
}

.logo strong,
.logo span {
  display: block;
}

.logo strong {
  font-size: 18px;
}

.logo span {
  margin-top: 2px;
  color: #667085;
  font-size: 12px;
}

.nav-menu {
  flex: 1;
  border-right: 0;
  padding: 10px 12px;
  background: transparent;
}

.nav-menu :deep(.el-menu-item) {
  height: 48px;
  margin: 4px 0;
  border-radius: 8px;
  color: #536174;
  font-weight: 650;
  transition: color 200ms ease, background-color 200ms ease, transform 200ms ease, box-shadow 200ms ease;
}

.nav-menu :deep(.el-menu-item.is-active) {
  color: #079989;
  background: #e9fbf7;
}

.nav-menu :deep(.el-menu-item:hover) {
  color: #079989;
  background: #f1fbf9;
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(16, 182, 166, 0.1);
}

.aside-footer {
  padding: 16px;
  border-top: 1px solid #e6eef2;
}

.mini-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border: 1px solid #dbe9ed;
  border-radius: 8px;
  background: #f7fffd;
  color: #667085;
  font-size: 13px;
}

.mini-card strong {
  color: #079989;
  font-size: 18px;
}

.content-shell {
  min-width: 0;
}

.header {
  height: 78px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid rgba(219, 232, 238, 0.9);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(18px);
}

.header h1 {
  margin: 0;
  color: #132033;
  font-size: 22px;
  font-weight: 850;
}

.header p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-menu {
  height: 46px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 0;
  background: transparent;
  color: #132033;
}

.user-menu strong,
.user-menu small {
  display: block;
  max-width: 128px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
}

.user-menu small {
  color: #667085;
}

.avatar {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #2ddfca, #0b958a);
  color: #fff;
  font-weight: 850;
}

.main {
  min-height: calc(100vh - 78px);
  padding: 0;
  background-image:
    linear-gradient(rgba(18, 107, 113, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(18, 107, 113, 0.04) 1px, transparent 1px);
  background-size: 78px 78px;
}

@media (max-width: 980px) {
  .shell {
    display: block;
  }

  .aside {
    position: relative;
    width: 100% !important;
    height: auto;
  }

  .nav-menu {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(148px, 1fr));
  }

  .aside-footer {
    display: none;
  }

  .header {
    height: auto;
    min-height: 72px;
    flex-wrap: wrap;
    padding: 14px 16px;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
