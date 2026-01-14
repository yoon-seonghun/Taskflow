import { get, put } from './client'

/**
 * 업무 내용(Content) 응답 타입
 */
export interface ItemContentResponse {
  contentId: number | null
  itemId: number
  contentType: 'HTML' | 'JSON' | 'MARKDOWN'
  content: string | null
  plainText: string | null
  version: number
  createdAt: string | null
  createdBy: string | null
  updatedAt: string | null
  updatedBy: string | null
}

/**
 * 업무 내용 업데이트 요청 타입
 */
export interface ItemContentUpdateRequest {
  contentType: 'HTML' | 'JSON' | 'MARKDOWN'
  content: string
  version: number
}

/**
 * 업무 내용(Content) API
 */
export const itemContentApi = {
  /**
   * 업무 내용 조회
   * @param boardId 보드 ID
   * @param itemId 업무 ID
   */
  getContent(boardId: number, itemId: number) {
    return get<ItemContentResponse | null>(`/boards/${boardId}/items/${itemId}/content`)
  },

  /**
   * 업무 내용 저장/수정
   * @param boardId 보드 ID
   * @param itemId 업무 ID
   * @param data 업데이트 요청 데이터
   */
  updateContent(boardId: number, itemId: number, data: ItemContentUpdateRequest) {
    return put<ItemContentResponse>(`/boards/${boardId}/items/${itemId}/content`, data)
  }
}
