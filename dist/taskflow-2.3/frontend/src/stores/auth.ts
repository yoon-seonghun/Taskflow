import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/user'

const ACCESS_TOKEN_KEY = 'accessToken'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(localStorage.getItem(ACCESS_TOKEN_KEY))
  const loading = ref(false)
  const initialized = ref(false)
  const lastTokenRefresh = ref<Date | null>(null)

  // Getters
  const isAuthenticated = computed(() => !!accessToken.value)
  const currentUserId = computed(() => user.value?.userId || null)
  const currentUserName = computed(() => user.value?.userName || '')
  const currentDepartmentId = computed(() => user.value?.departmentId || null)

  // Actions
  function setAuth(token: string, userData: User) {
    accessToken.value = token
    user.value = userData
    localStorage.setItem(ACCESS_TOKEN_KEY, token)
    lastTokenRefresh.value = new Date()
  }

  function setToken(token: string) {
    accessToken.value = token
    localStorage.setItem(ACCESS_TOKEN_KEY, token)
    lastTokenRefresh.value = new Date()
  }

  function setUser(userData: User) {
    user.value = userData
  }

  function clearAuth() {
    accessToken.value = null
    user.value = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    lastTokenRefresh.value = null
  }

  function setLoading(value: boolean) {
    loading.value = value
  }

  function setInitialized(value: boolean) {
    initialized.value = value
  }

  // Token validation (basic check - actual validation happens on server)
  function isTokenExpired(): boolean {
    if (!accessToken.value) return true

    try {
      // JWT 토큰에서 payload 추출 (base64 디코딩)
      const payload = JSON.parse(atob(accessToken.value.split('.')[1]))
      const exp = payload.exp * 1000 // Convert to milliseconds
      const now = Date.now()

      // 만료 5분 전부터 갱신 필요로 판단
      return now >= exp - 5 * 60 * 1000
    } catch {
      return true
    }
  }

  // 토큰 갱신이 필요한지 확인
  function needsTokenRefresh(): boolean {
    if (!accessToken.value) return false
    return isTokenExpired()
  }

  return {
    // State
    user,
    accessToken,
    loading,
    initialized,
    lastTokenRefresh,
    // Getters
    isAuthenticated,
    currentUserId,
    currentUserName,
    currentDepartmentId,
    // Actions
    setAuth,
    setToken,
    setUser,
    clearAuth,
    setLoading,
    setInitialized,
    isTokenExpired,
    needsTokenRefresh
  }
})
