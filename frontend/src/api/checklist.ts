import { get, post, put, del } from './client'
import type {
  Checklist,
  ChecklistCreateRequest,
  ChecklistUpdateRequest,
  ChecklistReorderRequest,
  ChecklistProgressResponse
} from '@/types/checklist'

export const checklistApi = {
  /**
   * 업무별 체크리스트 목록 조회
   */
  getChecklists(itemId: number) {
    return get<Checklist[]>(`/items/${itemId}/checklists`)
  },

  /**
   * 체크리스트 진행률 조회
   */
  getProgress(itemId: number) {
    return get<ChecklistProgressResponse>(`/items/${itemId}/checklists/progress`)
  },

  /**
   * 체크리스트 생성
   */
  createChecklist(itemId: number, data: ChecklistCreateRequest) {
    return post<Checklist>(`/items/${itemId}/checklists`, data)
  },

  /**
   * 체크리스트 수정
   */
  updateChecklist(checklistId: number, data: ChecklistUpdateRequest) {
    return put<Checklist>(`/checklists/${checklistId}`, data)
  },

  /**
   * 완료 상태 토글
   */
  toggleComplete(checklistId: number) {
    return put<Checklist>(`/checklists/${checklistId}/complete`)
  },

  /**
   * 체크리스트 삭제
   */
  deleteChecklist(checklistId: number) {
    return del<void>(`/checklists/${checklistId}`)
  },

  /**
   * 체크리스트 순서 변경
   */
  reorderChecklists(itemId: number, data: ChecklistReorderRequest) {
    return put<void>(`/items/${itemId}/checklists/reorder`, data)
  },

  /**
   * 내 담당 체크리스트 목록
   */
  getMyChecklists() {
    return get<Checklist[]>('/checklists/my')
  }
}
