import { get, post, put, del } from './client'
import type {
  Item,
  ItemCreateRequest,
  ItemUpdateRequest,
  CrossBoardSearchRequest,
  ItemPageResponse,
  CrossBoardStats
} from '@/types/item'

export const itemApi = {
  getItems(boardId: number, params?: object) {
    return get<ItemPageResponse>(`/boards/${boardId}/items`, params)
  },

  getItem(boardId: number, id: number) {
    return get<Item>(`/boards/${boardId}/items/${id}`)
  },

  createItem(boardId: number, data: ItemCreateRequest) {
    return post<Item>(`/boards/${boardId}/items`, data)
  },

  updateItem(boardId: number, id: number, data: ItemUpdateRequest) {
    return put<Item>(`/boards/${boardId}/items/${id}`, data)
  },

  deleteItem(boardId: number, id: number) {
    return del<void>(`/boards/${boardId}/items/${id}`)
  },

  completeItem(boardId: number, id: number) {
    return put<Item>(`/boards/${boardId}/items/${id}/complete`)
  },

  restoreItem(boardId: number, id: number) {
    return put<Item>(`/boards/${boardId}/items/${id}/restore`)
  },

  // =============================================
  // Cross-board API
  // =============================================

  /**
   * 지연 업무 목록 조회 (Cross-board)
   */
  getOverdueItems(params?: CrossBoardSearchRequest) {
    return get<ItemPageResponse>('/items/overdue', params)
  },

  /**
   * 보류 업무 목록 조회 (Cross-board)
   */
  getPendingItems(params?: CrossBoardSearchRequest) {
    return get<ItemPageResponse>('/items/pending', params)
  },

  /**
   * 활성 업무 목록 조회 (Cross-board)
   */
  getActiveItemsCrossBoard(params?: CrossBoardSearchRequest) {
    return get<ItemPageResponse>('/items/active', params)
  },

  /**
   * Cross-board 통계 조회
   */
  getCrossBoardStats() {
    return get<CrossBoardStats>('/items/stats')
  }
}
