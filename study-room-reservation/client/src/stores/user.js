import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const user = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  async function login(username, password) {
    // 登录时不带旧 token，避免 Spring Security 拦截
    delete api.defaults.headers.common['Authorization']
    const res = await api.post('/auth/login', { username, password })
    if (res.code === 200) {
      token.value = res.data.token
      user.value = res.data.user
      api.defaults.headers.common['Authorization'] = `Bearer ${res.data.token}`
      return { ok: true }
    }
    return { ok: false, message: res.message }
  }

  async function register(data) {
    delete api.defaults.headers.common['Authorization']
    const res = await api.post('/auth/register', data)
    return { ok: res.code === 200, message: res.message }
  }

  function logout() {
    token.value = ''
    user.value = null
    delete api.defaults.headers.common['Authorization']
  }

  // 恢复登录状态
  function restoreAuth() {
    if (token.value) {
      api.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
    }
  }

  return { token, user, isLoggedIn, isAdmin, login, register, logout, restoreAuth }
}, {
  persist: { key: 'study-room-user', paths: ['token', 'user'] }
})
