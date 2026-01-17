/**
 * 체크리스트 타입 정의
 */

/**
 * 체크리스트 인터페이스
 */
export interface Checklist {
  checklistId: number
  itemId: number
  title: string
  isCompleted: boolean
  completedAt?: string
  completedBy?: string
  completedByName?: string
  sortOrder: number
  dueDate?: string
  assigneeUsername?: string
  assigneeName?: string
  createdAt: string
  createdBy: string
  updatedAt?: string
  updatedBy?: string
  isOverdue?: boolean
  isDueToday?: boolean
}

/**
 * 체크리스트 생성 요청
 */
export interface ChecklistCreateRequest {
  title: string
  dueDate?: string
  assigneeUsername?: string
  sortOrder?: number
}

/**
 * 체크리스트 수정 요청
 */
export interface ChecklistUpdateRequest {
  title?: string
  isCompleted?: boolean
  dueDate?: string
  assigneeUsername?: string
  sortOrder?: number
}

/**
 * 체크리스트 순서 변경 요청
 */
export interface ChecklistReorderRequest {
  checklistIds: number[]
}

/**
 * 체크리스트 진행률 응답
 */
export interface ChecklistProgressResponse {
  completed: number
  total: number
}
