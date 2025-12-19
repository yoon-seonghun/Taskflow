/**
 * 라우트 가드
 * - 인증 체크
 * - 토큰 갱신
 * - 권한 검사
 */
import type { Router, RouteLocationNormalized, NavigationGuardNext } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

/**
 * 라우트 가드 설정
 */
export function setupRouteGuards(router: Router): void {
  // 전역 가드: 인증 체크
  router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore()

    // 인증이 필요하지 않은 페이지
    if (to.meta.requiresAuth === false) {
      // 이미 로그인된 상태에서 로그인 페이지 접근 시 메인으로 리다이렉트
      if (to.name === 'Login' && authStore.isAuthenticated) {
        return next({ name: 'Tasks' })
      }
      return next()
    }

    // 인증이 필요한 페이지
    if (!authStore.accessToken) {
      // 토큰이 없으면 로그인 페이지로
      return next({
        name: 'Login',
        query: { redirect: to.fullPath }
      })
    }

    // 앱 초기화가 안 되어 있으면 사용자 정보 로드
    if (!authStore.initialized) {
      await initializeAuth(authStore)
    }

    // 인증 실패 시 로그인 페이지로
    if (!authStore.isAuthenticated) {
      return next({
        name: 'Login',
        query: { redirect: to.fullPath }
      })
    }

    // 토큰 갱신이 필요한지 체크
    if (authStore.needsTokenRefresh()) {
      const refreshed = await refreshTokenSilently(authStore)
      if (!refreshed) {
        return next({
          name: 'Login',
          query: { expired: 'true', redirect: to.fullPath }
        })
      }
    }

    next()
  })

  // 라우트 변경 후: 페이지 타이틀 설정
  router.afterEach((to) => {
    const baseTitle = 'TaskFlow'
    const pageTitle = to.meta.title as string | undefined

    document.title = pageTitle ? `${pageTitle} - ${baseTitle}` : baseTitle
  })

  // 에러 핸들러
  router.onError((error) => {
    console.error('Router error:', error)

    // 청크 로드 실패 시 페이지 새로고침
    if (error.message.includes('Failed to fetch dynamically imported module')) {
      window.location.reload()
    }
  })
}

/**
 * 앱 초기화 시 인증 상태 확인
 */
async function initializeAuth(authStore: ReturnType<typeof useAuthStore>): Promise<void> {
  if (authStore.initialized) return

  authStore.setLoading(true)

  try {
    if (authStore.accessToken) {
      // 토큰이 만료되었으면 갱신 시도
      if (authStore.needsTokenRefresh()) {
        const refreshed = await refreshTokenSilently(authStore)
        if (!refreshed) {
          authStore.clearAuth()
          return
        }
      }

      // 사용자 정보 로드
      const response = await authApi.getCurrentUser()
      if (response.success && response.data) {
        authStore.setUser(response.data)
      } else {
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
 * 토큰 갱신 (silent)
 */
async function refreshTokenSilently(authStore: ReturnType<typeof useAuthStore>): Promise<boolean> {
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
 * 인증 필요 여부 체크 헬퍼
 */
export function requiresAuth(to: RouteLocationNormalized): boolean {
  return to.meta.requiresAuth !== false
}

/**
 * 권한 체크 헬퍼 (향후 확장용)
 * NOTE: DB에 역할(role) 필드 추가 후 구현 예정
 * 현재는 인증된 사용자에게 모든 권한 부여
 */
export function hasPermission(
  _to: RouteLocationNormalized,
  _authStore: ReturnType<typeof useAuthStore>
): boolean {
  // 역할 기반 권한 체크 로직 (DB 역할 필드 추가 후 활성화)
  // const requiredRoles = to.meta.roles as string[] | undefined
  // if (!requiredRoles || requiredRoles.length === 0) return true
  // return requiredRoles.some(role => authStore.user?.roles?.includes(role))
  return true
}
