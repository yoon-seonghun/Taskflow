import { get, post, put, del } from './client'
import type {
  Item,
  ItemCreateRequest,
  ItemUpdateRequest,
  CrossBoardSearchRequest,
  ItemPageResponse,
  CrossBoardStats,
  ItemTransferRequest,
  ItemShare,
  ItemPropertySortRequest,
  ItemReorderRequest,
  // v2.2: 하위 업무 관련 타입
  SubTaskCreateRequest,
  SubTaskReorderRequest,
  AncestorResponse,
  ItemCompleteRequest,
  IncompleteChildrenResponse
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

  /**
   * 공유받은 업무 목록 조회
   */
  getSharedItems(params?: CrossBoardSearchRequest) {
    return get<ItemPageResponse>('/items/shared', params)
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
  updateItemShare(itemId: number, username: string, data: ShareUpdateRequest) {
    return put<void>(`/items/${itemId}/shares/${username}`, data)
  },

  /**
   * 업무 공유 제거
   */
  removeItemShare(itemId: number, username: string) {
    return del<void>(`/items/${itemId}/shares/${username}`)
  },

  // =============================================
  // 아이템 속성 순서 변경
  // =============================================

  /**
   * 아이템 속성 순서 변경
   */
  updatePropertySortOrders(boardId: number, itemId: number, data: ItemPropertySortRequest) {
    return put<void>(`/boards/${boardId}/items/${itemId}/properties/sort`, data)
  },

  // =============================================
  // 아이템 순서 변경 (Drag & Drop)
  // =============================================

  /**
   * 아이템 순서 변경
   */
  reorderItem(boardId: number, data: ItemReorderRequest) {
    return put<void>(`/boards/${boardId}/items/reorder`, data)
  },

  // =============================================
  // v2.2: 하위 업무 (Sub-Task) API
  // =============================================

  /**
   * 하위 업무 목록 조회
   */
  getChildren(boardId: number, parentItemId: number, params?: { includeCompleted?: boolean; includeDeleted?: boolean }) {
    return get<Item[]>(`/boards/${boardId}/items/${parentItemId}/children`, params)
  },

  /**
   * 업무 트리 조회 (부모 + 모든 하위)
   */
  getItemTree(boardId: number, itemId: number, params?: { maxDepth?: number; includeCompleted?: boolean }) {
    return get<Item>(`/boards/${boardId}/items/${itemId}/tree`, params)
  },

  /**
   * 상위 계층 조회 (Breadcrumb용)
   */
  getAncestors(boardId: number, itemId: number) {
    return get<AncestorResponse[]>(`/boards/${boardId}/items/${itemId}/ancestors`)
  },

  /**
   * 부모 업무 조회
   */
  getParent(boardId: number, itemId: number) {
    return get<Item | null>(`/boards/${boardId}/items/${itemId}/parent`)
  },

  /**
   * 루트 업무 목록 조회 (보드별)
   */
  getRootItems(boardId: number, params?: { includeCompleted?: boolean; includeDeleted?: boolean }) {
    return get<Item[]>(`/boards/${boardId}/items/root`, params)
  },

  /**
   * 하위 업무 등록
   */
  createSubTask(boardId: number, parentItemId: number, data: SubTaskCreateRequest) {
    return post<Item>(`/boards/${boardId}/items/${parentItemId}/subtask`, data)
  },

  /**
   * 하위 업무 순서 변경
   */
  reorderChildren(boardId: number, parentItemId: number, data: SubTaskReorderRequest) {
    return put<void>(`/boards/${boardId}/items/${parentItemId}/subtask/reorder`, data)
  },

  /**
   * 업무 완료 처리 (하위 업무 체크 포함)
   * 미완료 하위 업무가 있으면 IncompleteChildrenResponse 반환
   * 완료 성공 시 Item 반환
   */
  completeItemWithChildren(boardId: number, itemId: number, data?: ItemCompleteRequest) {
    return put<Item | IncompleteChildrenResponse>(`/boards/${boardId}/items/${itemId}/complete-with-children`, data || {})
  },

  /**
   * 미완료 하위 업무 조회
   */
  getIncompleteChildren(boardId: number, itemId: number) {
    return get<IncompleteChildrenResponse>(`/boards/${boardId}/items/${itemId}/incomplete-children`)
  }
}
