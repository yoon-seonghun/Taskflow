import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'
import router from '@/router'
import { useUiStore } from '@/stores/ui'
import { pinia } from '@/plugins/pinia'

// =============================================
// 케이스 변환 유틸리티
// =============================================

/**
 * 문자열을 snake_case에서 camelCase로 변환
 */
function toCamelCase(str: string): string {
  return str.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase())
}

/**
 * 문자열을 camelCase에서 snake_case로 변환
 */
function toSnakeCase(str: string): string {
  return str.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`)
}

/**
 * 객체의 모든 키를 camelCase에서 snake_case로 변환 (재귀)
 * 요청 데이터 변환용
 */
function convertKeysToSnakeCase(obj: unknown): unknown {
  if (obj === null || obj === undefined) {
    return obj
  }

  if (Array.isArray(obj)) {
    return obj.map(item => convertKeysToSnakeCase(item))
  }

  if (typeof obj === 'object') {
    const converted: Record<string, unknown> = {}
    for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
      // 프론트엔드-백엔드 필드명 매핑: 'userName' -> 'name'
      let snakeKey = toSnakeCase(key)
      if (key === 'userName') {
        snakeKey = 'name'
      }
      converted[snakeKey] = convertKeysToSnakeCase(value)
    }
    return converted
  }

  return obj
}

/**
 * 객체의 모든 키를 snake_case에서 camelCase로 변환 (재귀)
 * 추가로 백엔드-프론트엔드 필드명 매핑 처리
 */
function convertKeysToCamelCase(obj: unknown): unknown {
  if (obj === null || obj === undefined) {
    return obj
  }

  if (Array.isArray(obj)) {
    return obj.map(item => convertKeysToCamelCase(item))
  }

  if (typeof obj === 'object') {
    const converted: Record<string, unknown> = {}
    for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
      const camelKey = toCamelCase(key)
      converted[camelKey] = convertKeysToCamelCase(value)
    }

    // 백엔드-프론트엔드 필드명 매핑: 'name' -> 'userName' (사용자 객체용)
    // User 객체에서 name 필드를 userName으로도 사용할 수 있도록 복사
    if ('name' in converted && 'userId' in converted) {
      converted['userName'] = converted['name']
    }

    return converted
  }

  return obj
}

/**
 * Axios 클라이언트 인스턴스
 */
const client: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  withCredentials: true, // 쿠키 전송 활성화 (Refresh Token용)
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 토큰 갱신 중 여부
 */
let isRefreshing = false

/**
 * 토큰 갱신 대기 요청 큐
 */
let refreshSubscribers: Array<(token: string) => void> = []

/**
 * 토큰 갱신 완료 후 대기 중인 요청 처리
 */
function onRefreshed(token: string) {
  refreshSubscribers.forEach((callback) => callback(token))
  refreshSubscribers = []
}

/**
 * 토큰 갱신 대기 요청 추가
 */
function addRefreshSubscriber(callback: (token: string) => void) {
  refreshSubscribers.push(callback)
}

/**
 * 요청 인터셉터 - 토큰 자동 첨부 및 camelCase → snake_case 변환
 */
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 토큰 첨부
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 요청 데이터 키를 snake_case로 변환
    if (config.data && typeof config.data === 'object') {
      config.data = convertKeysToSnakeCase(config.data)
    }

    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 응답 인터셉터 - snake_case → camelCase 변환, 에러 처리 및 토큰 갱신
 */
client.interceptors.response.use(
  (response: AxiosResponse) => {
    // 응답 데이터의 키를 camelCase로 변환
    if (response.data) {
      response.data = convertKeysToCamelCase(response.data)
    }
    return response
  },
  async (error) => {
    const originalRequest = error.config

    // 401 에러 - 토큰 만료
    if (error.response?.status === 401 && !originalRequest._retry) {
      // 토큰 갱신 요청 자체가 실패한 경우
      if (originalRequest.url?.includes('/auth/refresh')) {
        localStorage.removeItem('accessToken')
        router.push({ name: 'Login' })
        return Promise.reject(error)
      }

      // 토큰 갱신 중인 경우 대기
      if (isRefreshing) {
        return new Promise((resolve) => {
          addRefreshSubscriber((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(client(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 토큰 갱신 시도
        const response = await client.post<ApiResponse<{ accessToken: string }>>('/auth/refresh')

        if (response.data.success && response.data.data) {
          const newToken = response.data.data.accessToken
          localStorage.setItem('accessToken', newToken)

          // 대기 중인 요청들 처리
          onRefreshed(newToken)

          // 원래 요청 재시도
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return client(originalRequest)
        }
      } catch (refreshError) {
        // 토큰 갱신 실패 - 로그아웃 처리
        localStorage.removeItem('accessToken')
        router.push({ name: 'Login' })
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 403 에러 - 권한 없음
    if (error.response?.status === 403) {
      console.error('접근 권한이 없습니다.')
      const uiStore = useUiStore(pinia)
      uiStore.showError('접근 권한이 없습니다.')
    }

    // 서버 에러
    if (error.response?.status >= 500) {
      console.error('서버 오류가 발생했습니다.')
      const uiStore = useUiStore(pinia)
      const message = error.response?.data?.message || '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
      uiStore.showError(message)
    }

    return Promise.reject(error)
  }
)

// =============================================
// API 헬퍼 함수
// =============================================

/**
 * GET 요청
 */
export async function get<T>(url: string, params?: object): Promise<ApiResponse<T>> {
  const response = await client.get<ApiResponse<T>>(url, { params })
  return response.data
}

/**
 * POST 요청
 */
export async function post<T>(url: string, data?: object): Promise<ApiResponse<T>> {
  const response = await client.post<ApiResponse<T>>(url, data)
  return response.data
}

/**
 * PUT 요청
 */
export async function put<T>(url: string, data?: object): Promise<ApiResponse<T>> {
  const response = await client.put<ApiResponse<T>>(url, data)
  return response.data
}

/**
 * DELETE 요청 (옵션으로 body 데이터 지원)
 */
export async function del<T>(url: string, data?: object): Promise<ApiResponse<T>> {
  const response = await client.delete<ApiResponse<T>>(url, { data })
  return response.data
}

/**
 * PATCH 요청
 */
export async function patch<T>(url: string, data?: object): Promise<ApiResponse<T>> {
  const response = await client.patch<ApiResponse<T>>(url, data)
  return response.data
}

export default client
