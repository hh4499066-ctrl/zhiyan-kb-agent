import { createRouter, createWebHistory } from 'vue-router'
import Login from './views/Login.vue'
import Layout from './views/Layout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('./views/Dashboard.vue'), meta: { title: '数据看板' } },
        { path: 'users', component: () => import('./views/Users.vue'), meta: { title: '用户管理', roles: ['admin'] } },
        { path: 'departments', component: () => import('./views/Departments.vue'), meta: { title: '部门管理', roles: ['admin'] } },
        { path: 'spaces', component: () => import('./views/Spaces.vue'), meta: { title: '知识空间' } },
        { path: 'documents', component: () => import('./views/Documents.vue'), meta: { title: '文档管理' } },
        { path: 'chat', component: () => import('./views/Chat.vue'), meta: { title: '智能问答' } },
        { path: 'memories', component: () => import('./views/Memories.vue'), meta: { title: '长期记忆' } },
        { path: 'faqs', component: () => import('./views/Faqs.vue'), meta: { title: 'FAQ 管理' } },
        { path: 'onboarding', component: () => import('./views/Onboarding.vue'), meta: { title: '新人助手' } },
        { path: 'unresolved', component: () => import('./views/Unresolved.vue'), meta: { title: '未解决问题' } },
        { path: 'records', component: () => import('./views/Records.vue'), meta: { title: '问答记录' } },
        { path: 'operation-logs', component: () => import('./views/OperationLogs.vue'), meta: { title: '操作日志', roles: ['admin'] } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.path !== '/login' && !localStorage.getItem('token')) return '/login'
  const roles = to.meta.roles as string[] | undefined
  if (roles?.length) {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    if (!roles.includes(user?.role)) return '/dashboard'
  }
})

export default router
