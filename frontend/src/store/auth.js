import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const saved = JSON.parse(localStorage.getItem('user') || 'null')
  if (saved) {
    saved.role = (saved.role || '').toLowerCase()
  }
  const user = ref(saved)

  const role = computed(() => user.value?.role || '')
  const username = computed(() => user.value?.username || '')
  const isAdmin = computed(() => role.value === 'admin')
  const isOperator = computed(() => role.value === 'operator' || isAdmin.value)
  const isViewer = computed(() => role.value === 'viewer')
  const canWrite = computed(() => isAdmin.value || (role.value === 'operator'))
  const canDelete = computed(() => isAdmin.value)
  const canAdmin = computed(() => isAdmin.value)

  function loginSuccess(res) {
    // 后端返回格式: { code, message, data: { token, role, ... } }
    const payload = res.data || res
    token.value = payload.access_token || payload.token
    const rawUser = payload.user || { id: payload.userId, username: payload.username, role: payload.role }
    user.value = {
      ...rawUser,
      role: (rawUser.role || '').toLowerCase()
    }
    localStorage.setItem('token', token.value)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, role, username, isAdmin, isOperator, isViewer, canWrite, canDelete, canAdmin, loginSuccess, logout }
})
