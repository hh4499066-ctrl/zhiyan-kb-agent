import axios from 'axios'
import { ElMessage } from 'element-plus'

export const http: any = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((config: any) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response: any) => {
    const body = response.data
    if (body?.code && body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      if (body.code === 401) location.href = '/login'
      return Promise.reject(new Error(body.message))
    }
    return body?.data ?? body
  },
  (error: any) => {
    ElMessage.error(error?.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  }
)
