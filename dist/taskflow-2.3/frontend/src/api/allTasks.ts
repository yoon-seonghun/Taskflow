/**
 * 전체 업무 API (v2.1)
 * - GET /api/items/all - 전체 업무 목록 조회
 * - GET /api/items/all/stats - 전체 업무 통계 조회
 */
import { get } from './client'
import type {
  AllItemsPageResponse,
  AllItemsSearchRequest,
  AllItemsStats
} from '@/types/allTasks'

/**
 * 프론트엔드 요청 파라미터를 백엔드 형식으로 변환
 * - sortField + sortDirection → sort (예: "createdAt,desc")
 */
function convertToApiParams(params?: AllItemsSearchRequest): Record<string, unknown> | undefined {
  if (!params) return undefined

  const { sortField, sortDirection, ...rest } = params

  // sort 파라미터 생성 (백엔드 형식: "field,direction")
  const sort = sortField && sortDirection
    ? `${sortField},${sortDirection}`
    : sortField
      ? `${sortField},desc`
      : undefined

  return {
    ...rest,
    ...(sort && { sort })
  }
}

export const allTasksApi = {
  /**
   * 전체 업무 목록 조회
   * - 소유 보드 + 공유 보드 + 개별 공유/배당 업무
   */
  getAllItems(params?: AllItemsSearchRequest) {
    return get<AllItemsPageResponse>('/items/all', convertToApiParams(params))
  },

  /**
   * 전체 업무 통계 조회
   * - 출처별, 상태별, 우선순위별 개수
   */
  getStats(params?: Partial<AllItemsSearchRequest>) {
    return get<AllItemsStats>('/items/all/stats', params)
  }
}
