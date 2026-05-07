import { defineStore } from 'pinia'
import { http } from './api'

export interface LoginUser {
  id: number
  username: string
  realName: string
  role: 'admin' | 'kb_manager' | 'employee' | 'newcomer'
  departmentId: number
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user') || 'null') as LoginUser | null
  }),
  actions: {
    async login(username: string, password: string) {
      const data = await http.post('/auth/login', { username, password })
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
      this.user = data.user
    },
    async loadMe() {
      this.user = await http.get('/auth/me')
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    logout() {
      http.post('/auth/logout').catch(() => undefined)
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      this.user = null
      location.href = '/login'
    }
  }
})
