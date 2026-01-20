/**
 * API 공통 타입 정의
 */

/**
 * API 응답 공통 형식
 */
export interface ApiResponse<T> {
  success: boolean
  data: T | null
  message: string | null
}

/**
 * 페이지 요청 파라미터
 */
export interface PageRequest {
  page?: number
  size?: number
  sort?: string
}

/**
 * 페이지 응답 형식
 */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

/**
 * API 에러 응답
 */
export interface ApiError {
  success: false
  data: null
  message: string
  errorCode?: string
  status?: number
}

/**
 * 정렬 방향
 */
export type SortDirection = 'asc' | 'desc'

/**
 * 정렬 파라미터
 */
export interface SortParam {
  field: string
  direction: SortDirection
}
