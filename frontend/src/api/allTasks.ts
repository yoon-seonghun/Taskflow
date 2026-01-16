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

export const allTasksApi = {
  /**
   * 전체 업무 목록 조회
   * - 소유 보드 + 공유 보드 + 개별 공유/배당 업무
   */
  getAllItems(params?: AllItemsSearchRequest) {
    return get<AllItemsPageResponse>('/items/all', params)
  },

  /**
   * 전체 업무 통계 조회
   * - 출처별, 상태별, 우선순위별 개수
   */
  getStats(params?: Partial<AllItemsSearchRequest>) {
    return get<AllItemsStats>('/items/all/stats', params)
  }
}
