import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const user = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  async function login(username, password) {
    const res = await api.post('/auth/login', { username, password })
    if (res.code === 200) {
      token.value = res.data.token
      user.value = res.data.user
      return { ok: true }
    }
    return { ok: false, message: res.message }
  }

  async function register(data) {
    const res = await api.post('/auth/register', data)
    return { ok: res.code === 200, message: res.message }
  }

  function logout() {
    token.value = ''
    user.value = null
  }

  return { token, user, isLoggedIn, isAdmin, login, register, logout }
}, {
  persist: { key: 'study-room-user', paths: ['token', 'user'] }
})
