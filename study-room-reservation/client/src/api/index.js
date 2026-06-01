import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动注入 token
api.interceptors.request.use(
  (config) => {
    // 从 localStorage 直接读取 token（最可靠的方式）
    try {
      const raw = localStorage.getItem('study-room-user')
      if (raw) {
        const stored = JSON.parse(raw)
        if (stored.token) {
          config.headers.Authorization = `Bearer ${stored.token}`
        }
      }
    } catch {}
    return config
  },
  (err) => Promise.reject(err)
)

// 响应拦截器
api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err.response?.data?.message || '网络请求失败'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default api
