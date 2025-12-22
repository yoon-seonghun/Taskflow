import { get, post, put, del } from './client'
import type {
  Item,
  ItemCreateRequest,
  ItemUpdateRequest,
  CrossBoardSearchRequest,
  ItemPageResponse,
  CrossBoardStats,
  ItemTransferRequest,
  ItemShare
} from '@/types/item'
import type { Share, ShareRequest, ShareUpdateRequest } from '@/types/share'

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
  },

  // =============================================
  // 업무 이관 API
  // =============================================

  /**
   * 업무 이관
   */
  transferItem(boardId: number, itemId: number, data: ItemTransferRequest) {
    return put<Item>(`/boards/${boardId}/items/${itemId}/transfer`, data)
  },

  /**
   * 이관 가능 여부 확인
   */
  canTransfer(boardId: number, itemId: number) {
    return get<boolean>(`/boards/${boardId}/items/${itemId}/can-transfer`)
  },

  /**
   * 공유 가능 여부 확인
   */
  canShare(boardId: number, itemId: number) {
    return get<boolean>(`/boards/${boardId}/items/${itemId}/can-share`)
  },

  /**
   * 업무 권한 조회
   */
  getItemPermission(boardId: number, itemId: number) {
    return get<string>(`/boards/${boardId}/items/${itemId}/permission`)
  },

  // =============================================
  // 업무 공유 API
  // =============================================

  /**
   * 업무 공유 목록 조회
   */
  getItemShares(itemId: number) {
    return get<Share[]>(`/items/${itemId}/shares`)
  },

  /**
   * 업무 공유 추가
   */
  addItemShare(itemId: number, data: ShareRequest) {
    return post<void>(`/items/${itemId}/shares`, data)
  },

  /**
   * 업무 공유 권한 변경
   */
  updateItemShare(itemId: number, userId: number, data: ShareUpdateRequest) {
    return put<void>(`/items/${itemId}/shares/${userId}`, data)
  },

  /**
   * 업무 공유 제거
   */
  removeItemShare(itemId: number, userId: number) {
    return del<void>(`/items/${itemId}/shares/${userId}`)
  }
}
