<template>
  <div ref="layoutRoot" class="shell-host">
  <el-container class="shell">
    <el-aside
      :class="['aside', isMainNavCollapse ? 'aside-collapsed' : '']"
      :width="isMainNavCollapse ? '76px' : '252px'"
    >
      <div class="logo" data-motion="page-item">
        <div class="logo-mark">Z</div>
        <div class="logo-copy">
          <strong>智研知识库</strong>
          <span>AI Knowledge Base</span>
        </div>
      </div>
      <el-menu
        class="nav-menu"
        router
        :default-active="$route.path"
        :collapse="isMainNavCollapse"
        :collapse-transition="false"
      >
        <el-menu-item
          v-for="item in menus"
          :key="item.path"
          :index="item.path"
          class="nav-item"
          data-motion="page-item"
          :style="{ '--nav-accent': item.accent }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <div v-if="!isMainNavCollapse" class="mini-card">
          <span>知识库健康度</span>
          <strong>98%</strong>
        </div>
        <button class="main-nav-toggle" type="button" @click="isMainNavCollapse = !isMainNavCollapse">
          <el-icon>
            <Expand v-if="isMainNavCollapse" />
            <Fold v-else />
          </el-icon>
        </button>
      </div>
    </el-aside>

    <el-container class="content-shell">
      <el-header class="header" data-motion="page-item">
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
        <router-view v-slot="{ Component, route }">
          <transition name="route-fade" mode="out-in">
            <keep-alive>
              <component :is="Component" :key="route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import {
  ArrowDown,
  Box,
  ChatSquare,
  Connection,
  DataBoard,
  Expand,
  Fold,
  FolderOpened,
  HelpFilled,
  OfficeBuilding,
  Operation,
  Opportunity,
  Stopwatch,
  UserFilled,
  WarningFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '../store'
import { useAnimeMotion } from '../composables/useAnimeMotion'

const auth = useAuthStore()
const layoutRoot = ref<HTMLElement>()
const motion = useAnimeMotion()
const isMainNavCollapse = ref(false)
const allMenus = [
  { path: '/dashboard', title: '仪表盘', icon: DataBoard, accent: '#12bfae' },
  { path: '/chat', title: '智能问答', icon: ChatSquare, accent: '#38bdf8' },
  { path: '/records', title: '问答记录', icon: Stopwatch, accent: '#60a5fa' },
  { path: '/memories', title: '长期记忆', icon: Connection, accent: '#a78bfa' },
  { path: '/spaces', title: '知识空间', icon: Box, accent: '#34d399' },
  { path: '/onboarding', title: '新人助手', icon: Opportunity, accent: '#fbbf24' },
  { path: '/users', title: '用户管理', icon: UserFilled, accent: '#93c5fd', roles: ['admin'] },
  { path: '/departments', title: '部门管理', icon: OfficeBuilding, accent: '#c4b5fd', roles: ['admin'] },
  { path: '/documents', title: '文档管理', icon: FolderOpened, accent: '#22c55e' },
  { path: '/faqs', title: 'FAQ 管理', icon: HelpFilled, accent: '#f59e0b' },
  { path: '/unresolved', title: '未解决问题', icon: WarningFilled, accent: '#fb7185' },
  { path: '/operation-logs', title: '操作日志', icon: Operation, accent: '#94a3b8', roles: ['admin'] }
]

const menus = computed(() => allMenus.filter((m) => !m.roles || m.roles.includes(auth.user?.role || '')))
const roleName = computed(() => ({
  admin: '管理员',
  kb_manager: '知识库管理员',
  employee: '员工',
  newcomer: '新人'
}[auth.user?.role || 'employee']))
const avatarText = computed(() => (auth.user?.realName || auth.user?.username || 'U').slice(0, 1).toUpperCase())

onMounted(() => {
  motion.enterPage(layoutRoot)
})

watch(isMainNavCollapse, async () => {
  await nextTick()
  motion.pulse(layoutRoot.value?.querySelector<HTMLElement>('.logo-mark') || undefined)
})
</script>

<style scoped>
.shell-host {
  min-height: 100vh;
}

.shell {
  min-height: 100vh;
  background: #f6f8fb;
}

.aside {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  flex: 0 0 auto;
  border-right: 1px solid #1d3c52;
  background: #071426;
  overflow: hidden;
  transition: width 280ms var(--ease-out);
}

.logo {
  height: 78px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 22px;
  color: #fff;
  overflow: hidden;
  transition: padding 280ms var(--ease-out), justify-content 280ms var(--ease-out);
}

.logo-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 8px;
  border: 1px solid #1d3c52;
  background: #0f2335;
  color: #12bfae;
  font-weight: 900;
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.22);
}

.logo strong,
.logo span {
  display: block;
}

.logo-copy {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  transition: opacity 0.2s ease, width 0.3s ease;
}

.logo strong {
  font-size: 18px;
}

.logo span {
  margin-top: 2px;
  color: #94a3b8;
  font-size: 12px;
}

.nav-menu {
  flex: 1;
  border-right: 0;
  padding: 10px 12px;
  background: transparent;
  transition: width 280ms var(--ease-out), padding 280ms var(--ease-out);
}

.nav-menu:not(.el-menu--collapse) {
  width: 100%;
}

.nav-menu.el-menu--collapse {
  width: 76px;
}

.nav-menu :deep(.el-menu-item) {
  height: 48px;
  margin: 4px 0;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  align-items: center;
  justify-content: flex-start;
  column-gap: 10px;
  border-radius: 8px;
  color: #94a3b8;
  font-weight: 650;
  padding: 0 12px !important;
  transition: color 180ms var(--ease-out), background-color 180ms var(--ease-out), transform 180ms var(--ease-out), box-shadow 180ms var(--ease-out), padding 280ms var(--ease-out);
}

.nav-menu :deep(.el-menu-item .el-icon) {
  width: 28px;
  margin: 0;
  color: #8da2bd;
  transition: transform 280ms var(--ease-out);
}

.nav-menu :deep(.el-menu-item span) {
  min-width: 0;
  overflow: hidden;
  opacity: 1;
  transform: translateX(0);
  transition: opacity 180ms var(--ease-out) 80ms, transform 280ms var(--ease-out);
}

.nav-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: #112b3e;
  box-shadow: inset 0 0 0 1px #1d3c52;
}

.nav-menu :deep(.el-menu-item:hover) {
  color: #fff;
  background: #10283a;
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.18);
}

.nav-menu :deep(.el-menu-item.is-active .el-icon),
.nav-menu :deep(.el-menu-item:hover .el-icon) {
  color: var(--nav-accent, #12bfae);
  transform: scale(1.08);
}

.aside-footer {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border-top: 1px solid #1d3c52;
  transition: padding 280ms var(--ease-out);
}

.mini-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border: 1px solid #1d3c52;
  border-radius: 8px;
  background: #0f2335;
  color: #94a3b8;
  font-size: 13px;
}

.mini-card strong {
  color: #12bfae;
  font-size: 18px;
}

.main-nav-toggle {
  width: 100%;
  height: 38px;
  display: grid;
  place-items: center;
  border: 1px solid #1d3c52;
  border-radius: 8px;
  background: #0f2335;
  color: #94a3b8;
  transition: border-color 180ms var(--ease-out), color 180ms var(--ease-out), background-color 180ms var(--ease-out);
}

.main-nav-toggle:hover {
  border-color: #12bfae;
  background: #112b3e;
  color: #12bfae;
}

.aside-collapsed .logo {
  justify-content: flex-start;
  padding: 0 17px;
}

.aside-collapsed .logo-copy {
  width: 0;
  opacity: 0;
}

.aside-collapsed .nav-menu {
  padding: 10px 14px;
}

.aside-collapsed .nav-menu :deep(.el-menu-item) {
  grid-template-columns: 28px 0;
  justify-content: center;
  padding: 0 !important;
}

.aside-collapsed .nav-menu :deep(.el-menu-item span) {
  width: 0;
  opacity: 0;
  transform: translateX(-6px);
}

.aside-collapsed .aside-footer {
  padding: 12px;
}

.content-shell {
  flex: 1 1 auto;
  min-width: 0;
  transition: flex-grow 280ms var(--ease-out);
}

.header {
  height: 78px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid rgba(220, 228, 238, 0.92);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(18px);
}

.header h1 {
  margin: 0;
  color: #111827;
  font-size: 22px;
  font-weight: 850;
}

.header p {
  margin: 4px 0 0;
  color: #647084;
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
  color: #111827;
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
  color: #647084;
}

.avatar {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #e6fbf7;
  border: 1px solid #12bfae;
  color: #087d75;
  box-shadow: 0 8px 20px rgba(18, 191, 174, 0.16);
  font-weight: 850;
}

.main {
  min-height: calc(100vh - 78px);
  padding: 0;
  background-color: #f6f8fb;
  background-image:
    linear-gradient(rgba(18, 107, 113, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(18, 107, 113, 0.035) 1px, transparent 1px);
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
