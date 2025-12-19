/**
 * 인증 관련 composable
 * - 로그인/로그아웃
 * - 토큰 관리
 * - 사용자 정보 관리
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { useToast } from './useToast'
import type { LoginRequest } from '@/types/user'

export function useAuth() {
  const router = useRouter()
  const authStore = useAuthStore()
  const toast = useToast()

  const loginError = ref<string | null>(null)
  const isLoggingIn = ref(false)
  const isLoggingOut = ref(false)

  // Computed
  const isAuthenticated = computed(() => authStore.isAuthenticated)
  const currentUser = computed(() => authStore.user)
  const currentUserId = computed(() => authStore.currentUserId)
  const currentUserName = computed(() => authStore.currentUserName)
  const isInitialized = computed(() => authStore.initialized)

  /**
   * 로그인
   */
  async function login(credentials: LoginRequest): Promise<boolean> {
    loginError.value = null
    isLoggingIn.value = true
    authStore.setLoading(true)

    try {
      const response = await authApi.login(credentials)

      if (response.success && response.data) {
        const { accessToken, user } = response.data
        authStore.setAuth(accessToken, user)

        toast.success(`${user.userName}님, 환영합니다!`)

        // 메인 페이지로 이동
        await router.push({ name: 'Tasks' })

        return true
      } else {
        loginError.value = response.message || '로그인에 실패했습니다.'
        return false
      }
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '로그인 중 오류가 발생했습니다.'
      loginError.value = message
      toast.error(message)
      return false
    } finally {
      isLoggingIn.value = false
      authStore.setLoading(false)
    }
  }

  /**
   * 로그아웃
   */
  async function logout(): Promise<void> {
    isLoggingOut.value = true

    try {
      // 서버에 로그아웃 요청 (Refresh Token 쿠키 삭제)
      await authApi.logout()
    } catch (error) {
      // 서버 로그아웃 실패해도 클라이언트는 로그아웃 처리
      console.error('Logout API error:', error)
    } finally {
      // 클라이언트 상태 초기화
      authStore.clearAuth()
      isLoggingOut.value = false

      // 로그인 페이지로 이동
      await router.push({ name: 'Login' })

      toast.info('로그아웃되었습니다.')
    }
  }

  /**
   * 토큰 갱신
   */
  async function refreshToken(): Promise<boolean> {
    try {
      const response = await authApi.refresh()

      if (response.success && response.data) {
        authStore.setToken(response.data.accessToken)
        return true
      }

      return false
    } catch (error) {
      console.error('Token refresh failed:', error)
      return false
    }
  }

  /**
   * 현재 사용자 정보 조회
   */
  async function fetchCurrentUser(): Promise<boolean> {
    if (!authStore.accessToken) return false

    try {
      const response = await authApi.getCurrentUser()

      if (response.success && response.data) {
        authStore.setUser(response.data)
        return true
      }

      return false
    } catch (error) {
      console.error('Fetch current user failed:', error)
      return false
    }
  }

  /**
   * 앱 초기화 시 인증 상태 확인
   * - 저장된 토큰이 있으면 유효성 검증
   * - 유효하면 사용자 정보 로드
   */
  async function initializeAuth(): Promise<void> {
    if (authStore.initialized) return

    authStore.setLoading(true)

    try {
      if (authStore.accessToken) {
        // 토큰이 만료되었으면 갱신 시도
        if (authStore.needsTokenRefresh()) {
          const refreshed = await refreshToken()
          if (!refreshed) {
            // 갱신 실패 시 로그아웃
            authStore.clearAuth()
            return
          }
        }

        // 사용자 정보 로드
        const success = await fetchCurrentUser()
        if (!success) {
          // 사용자 정보 로드 실패 시 로그아웃
          authStore.clearAuth()
        }
      }
    } catch (error) {
      console.error('Auth initialization failed:', error)
      authStore.clearAuth()
    } finally {
      authStore.setInitialized(true)
      authStore.setLoading(false)
    }
  }

  /**
   * 토큰 만료 시 처리
   * - 토큰 갱신 시도
   * - 실패 시 로그아웃
   */
  async function handleTokenExpired(): Promise<boolean> {
    const refreshed = await refreshToken()

    if (!refreshed) {
      authStore.clearAuth()
      await router.push({ name: 'Login', query: { expired: 'true' } })
      toast.warning('세션이 만료되었습니다. 다시 로그인해주세요.')
      return false
    }

    return true
  }

  /**
   * 비밀번호 유효성 검사
   */
  function validatePassword(password: string): { valid: boolean; message: string } {
    if (password.length < 8) {
      return { valid: false, message: '비밀번호는 최소 8자 이상이어야 합니다.' }
    }

    if (!/[A-Z]/.test(password)) {
      return { valid: false, message: '비밀번호에 대문자가 포함되어야 합니다.' }
    }

    if (!/[a-z]/.test(password)) {
      return { valid: false, message: '비밀번호에 소문자가 포함되어야 합니다.' }
    }

    if (!/[0-9]/.test(password)) {
      return { valid: false, message: '비밀번호에 숫자가 포함되어야 합니다.' }
    }

    if (!/[!@#$%^&*]/.test(password)) {
      return { valid: false, message: '비밀번호에 특수문자(!@#$%^&*)가 포함되어야 합니다.' }
    }

    return { valid: true, message: '' }
  }

  return {
    // State
    loginError,
    isLoggingIn,
    isLoggingOut,
    // Computed
    isAuthenticated,
    currentUser,
    currentUserId,
    currentUserName,
    isInitialized,
    // Methods
    login,
    logout,
    refreshToken,
    fetchCurrentUser,
    initializeAuth,
    handleTokenExpired,
    validatePassword
  }
}
