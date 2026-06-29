import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isAuthenticated = computed(() => !!token.value)

  function persist(res) {
    token.value = res.token
    user.value = { username: res.username, role: res.role }
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  async function login(username, password) {
    const { data } = await http.post('/api/auth/login', { username, password })
    persist(data)
  }

  async function register(username, email, password) {
    const { data } = await http.post('/api/auth/register', { username, email, password })
    persist(data)
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isAuthenticated, login, register, logout }
})
