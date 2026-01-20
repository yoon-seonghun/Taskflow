import { get, post, put, del } from './client'
import type { AssignmentRequest, AssignmentResponse, ItemAccessInfo } from '@/types/assignment'

/**
 * 업무 배정 API
 */
export const assignmentApi = {
  /**
   * 업무 담당자 배정
   */
  assignItem(boardId: number, itemId: number, data: AssignmentRequest) {
    return post<AssignmentResponse>(`/boards/${boardId}/items/${itemId}/assign`, data)
  },

  /**
   * 업무 배정 취소
   */
  cancelAssignment(boardId: number, itemId: number, username: string) {
    return del<void>(`/boards/${boardId}/items/${itemId}/assign/${username}`)
  },

  /**
   * 배정 권한 수정
   */
  updateAssignmentPermission(boardId: number, itemId: number, username: string, permissionLevel: string) {
    return put<AssignmentResponse>(`/boards/${boardId}/items/${itemId}/assign/${username}`, {
      permissionLevel
    })
  },

  /**
   * 업무 접근 권한 정보 조회
   */
  getAccessInfo(boardId: number, itemId: number) {
    return get<ItemAccessInfo>(`/boards/${boardId}/items/${itemId}/access-info`)
  }
}
