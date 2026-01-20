/**
 * 전역 에러 핸들러 composable
 * - API 에러 처리 (상태 코드별)
 * - 네트워크 에러 처리
 * - 토스트 알림
 * - 콘솔 로깅
 */
import { useRouter } from 'vue-router'
import { useToast } from './useToast'
import { useAuthStore } from '@/stores/auth'
import {
  AppError,
  ApiError,
  NetworkError,
  ValidationError,
  AuthError,
  ErrorCode,
  ERROR_MESSAGES,
  HTTP_STATUS,
  HTTP_STATUS_TO_ERROR_CODE,
  isAppError,
  isApiError,
  isNetworkError,
  isValidationError,
  isAuthError,
  toAppError
} from '@/utils/errorTypes'

// 환경 설정
const isDev = import.meta.env.DEV
const LOG_ERRORS = import.meta.env.VITE_LOG_ERRORS !== 'false'

// 에러 로그 타입
interface ErrorLog {
  timestamp: string
  error: AppError
  context?: string
  userAgent: string
  url: string
}

// 에러 로그 저장소 (개발용)
const errorLogs: ErrorLog[] = []
const MAX_ERROR_LOGS = 100

export function useErrorHandler() {
  const toast = useToast()
  const router = useRouter()
  const authStore = useAuthStore()

  /**
   * 콘솔 로깅
   */
  function logError(error: AppError, context?: string) {
    if (!LOG_ERRORS) return

    const logPrefix = context ? `[${context}]` : '[Error]'

    if (isDev) {
      console.group(`${logPrefix} ${error.code}`)
      console.error('Message:', error.message)
      console.error('Code:', error.code)
      if (error.statusCode) console.error('Status:', error.statusCode)
      if (error.context) console.error('Context:', error.context)
      if (error.originalError) console.error('Original:', error.originalError)
      if (isApiError(error)) {
        console.error('URL:', error.url)
        console.error('Method:', error.method)
        console.error('Response:', error.responseData)
      }
      console.error('Stack:', error.stack)
      console.groupEnd()
    } else {
      console.error(`${logPrefix}`, error.message)
    }

    // 에러 로그 저장
    const logEntry: ErrorLog = {
      timestamp: new Date().toISOString(),
      error,
      context,
      userAgent: navigator.userAgent,
      url: window.location.href
    }
    errorLogs.push(logEntry)
    if (errorLogs.length > MAX_ERROR_LOGS) {
      errorLogs.shift()
    }
  }

  /**
   * 토스트 알림 표시
   */
  function showErrorToast(error: AppError) {
    const message = error.message || ERROR_MESSAGES[error.code] || ERROR_MESSAGES[ErrorCode.UNKNOWN_ERROR]
    toast.error(message)
  }

  /**
   * 인증 에러 처리
   */
  function handleAuthError(error: AuthError) {
    logError(error, 'Auth')

    // 로그아웃 처리
    authStore.logout()

    // 토스트 표시
    showErrorToast(error)

    // 리다이렉트
    if (error.shouldRedirect) {
      const currentPath = router.currentRoute.value.fullPath
      const redirectTo = error.redirectTo || '/login'

      // 현재 경로를 저장하여 로그인 후 돌아올 수 있도록
      if (currentPath !== redirectTo && currentPath !== '/') {
        router.push({
          path: redirectTo,
          query: { redirect: currentPath }
        })
      } else {
        router.push(redirectTo)
      }
    }
  }

  /**
   * API 에러 처리 (상태 코드별)
   */
  function handleApiError(error: ApiError) {
    logError(error, 'API')

    switch (error.statusCode) {
      case HTTP_STATUS.UNAUTHORIZED:
        // 401: 인증 필요 → 로그인 페이지로 이동
        handleAuthError(new AuthError(error.message, ErrorCode.UNAUTHORIZED, {
          shouldRedirect: true,
          originalError: error
        }))
        break

      case HTTP_STATUS.FORBIDDEN:
        // 403: 권한 없음
        toast.error('접근 권한이 없습니다.')
        break

      case HTTP_STATUS.NOT_FOUND:
        // 404: 리소스 없음
        toast.warning('요청한 데이터를 찾을 수 없습니다.')
        break

      case HTTP_STATUS.CONFLICT:
        // 409: 충돌 (동시 수정)
        toast.warning('다른 사용자가 수정 중입니다. 새로고침 후 다시 시도해주세요.')
        break

      case HTTP_STATUS.BAD_REQUEST:
      case HTTP_STATUS.UNPROCESSABLE_ENTITY:
        // 400, 422: 유효성 검증 실패
        showErrorToast(error)
        break

      case HTTP_STATUS.INTERNAL_SERVER_ERROR:
      case HTTP_STATUS.BAD_GATEWAY:
      case HTTP_STATUS.SERVICE_UNAVAILABLE:
      case HTTP_STATUS.GATEWAY_TIMEOUT:
        // 5xx: 서버 에러
        toast.error('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
        break

      default:
        showErrorToast(error)
    }
  }

  /**
   * 네트워크 에러 처리
   */
  function handleNetworkError(error: NetworkError) {
    logError(error, 'Network')

    if (error.isOffline || !navigator.onLine) {
      toast.error('인터넷 연결을 확인해주세요.')
    } else if (error.isTimeout) {
      toast.error('요청 시간이 초과되었습니다. 다시 시도해주세요.')
    } else {
      toast.error('네트워크 오류가 발생했습니다.')
    }
  }

  /**
   * 검증 에러 처리
   */
  function handleValidationError(error: ValidationError) {
    logError(error, 'Validation')

    if (error.errors && Object.keys(error.errors).length > 0) {
      // 필드별 에러가 있는 경우 첫 번째 에러 표시
      const firstField = Object.keys(error.errors)[0]
      const firstError = error.errors[firstField][0]
      toast.warning(firstError || error.message)
    } else {
      toast.warning(error.message)
    }
  }

  /**
   * 메인 에러 핸들러
   */
  function handleError(error: unknown, context?: string): AppError {
    // AppError로 변환
    const appError = toAppError(error, context)

    // 타입별 처리
    if (isAuthError(error)) {
      handleAuthError(error)
    } else if (isApiError(error)) {
      handleApiError(error)
    } else if (isNetworkError(error)) {
      handleNetworkError(error)
    } else if (isValidationError(error)) {
      handleValidationError(error)
    } else {
      // 일반 에러
      logError(appError, context)
      showErrorToast(appError)
    }

    return appError
  }

  /**
   * Axios 에러를 ApiError로 변환
   */
  function fromAxiosError(axiosError: {
    response?: {
      status: number
      statusText?: string
      data?: { message?: string; error?: string }
    }
    request?: unknown
    message?: string
    code?: string
    config?: { url?: string; method?: string }
  }, context?: string): AppError {
    // 응답이 있는 경우 (서버 에러)
    if (axiosError.response) {
      const { status, statusText, data } = axiosError.response
      const message = data?.message || data?.error || statusText || '요청 처리 중 오류가 발생했습니다.'

      return new ApiError(message, status, {
        url: axiosError.config?.url,
        method: axiosError.config?.method?.toUpperCase(),
        responseData: data,
        context
      })
    }

    // 요청은 보냈지만 응답이 없는 경우 (네트워크 에러)
    if (axiosError.request) {
      const isTimeout = axiosError.code === 'ECONNABORTED' || axiosError.message?.includes('timeout')
      return new NetworkError(
        isTimeout ? '요청 시간이 초과되었습니다.' : '서버에 연결할 수 없습니다.',
        {
          isTimeout,
          isOffline: !navigator.onLine,
          context
        }
      )
    }

    // 요청 설정 중 에러
    return new AppError(
      axiosError.message || '요청 생성 중 오류가 발생했습니다.',
      ErrorCode.UNKNOWN_ERROR,
      { context }
    )
  }

  /**
   * 에러 로그 가져오기 (디버깅용)
   */
  function getErrorLogs(): ErrorLog[] {
    return [...errorLogs]
  }

  /**
   * 에러 로그 클리어 (디버깅용)
   */
  function clearErrorLogs() {
    errorLogs.length = 0
  }

  /**
   * 에러 리포트 생성 (향후 에러 리포팅 서비스 연동용)
   */
  function createErrorReport(error: AppError): Record<string, unknown> {
    return {
      ...error.toJSON(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      referrer: document.referrer,
      screenSize: `${window.screen.width}x${window.screen.height}`,
      viewportSize: `${window.innerWidth}x${window.innerHeight}`,
      localStorage: Object.keys(localStorage).length,
      sessionStorage: Object.keys(sessionStorage).length,
      cookiesEnabled: navigator.cookieEnabled,
      onLine: navigator.onLine
    }
  }

  return {
    // 메인 핸들러
    handleError,

    // 타입별 핸들러
    handleApiError,
    handleNetworkError,
    handleValidationError,
    handleAuthError,

    // 유틸리티
    fromAxiosError,
    showErrorToast,
    logError,

    // 디버깅
    getErrorLogs,
    clearErrorLogs,
    createErrorReport,

    // 에러 생성 헬퍼
    createApiError: (message: string, statusCode: number, options?: Parameters<typeof ApiError['prototype']['constructor']>[2]) =>
      new ApiError(message, statusCode, options),
    createNetworkError: (message?: string, options?: Parameters<typeof NetworkError['prototype']['constructor']>[1]) =>
      new NetworkError(message, options),
    createValidationError: (message: string, options?: Parameters<typeof ValidationError['prototype']['constructor']>[1]) =>
      new ValidationError(message, options),
    createAuthError: (message: string, code?: ErrorCode, options?: Parameters<typeof AuthError['prototype']['constructor']>[2]) =>
      new AuthError(message, code, options)
  }
}

/**
 * 전역 에러 핸들러 생성 (Vue 외부에서 사용)
 * 주의: toast 함수를 외부에서 주입받아야 합니다.
 *
 * @example
 * // main.ts에서 설정
 * import { useToast } from '@/composables/useToast'
 * const toast = useToast()
 * const globalErrorHandler = createGlobalErrorHandler((msg) => toast.error(msg))
 *
 * app.config.errorHandler = globalErrorHandler
 */
export function createGlobalErrorHandler(
  showToast?: (message: string) => void
) {
  return function handleGlobalError(error: unknown, context?: string) {
    const appError = toAppError(error, context)

    // 콘솔 로깅
    if (isDev) {
      console.group(`[Global Error] ${appError.code}`)
      console.error('Message:', appError.message)
      console.error('Context:', context)
      console.error('Stack:', appError.stack)
      console.groupEnd()
    } else {
      console.error(`[Global Error]`, appError.message)
    }

    // 토스트 표시 (주입된 함수가 있을 때만)
    if (showToast) {
      showToast(appError.message || '오류가 발생했습니다.')
    }

    return appError
  }
}
