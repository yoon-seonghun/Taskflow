/**
 * Todo(개인 할 일) 타입 정의
 */

export type TodoPriority = 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW'
export type RepeatType = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'
export type TodoSharePermission = 'VIEW' | 'EDIT'

/**
 * Todo 인터페이스
 */
export interface Todo {
  todoId: number
  ownerUserId: number
  ownerUserName?: string
  ownerUsername?: string
  title: string
  description?: string
  priority: TodoPriority
  dueDate?: string
  dueTime?: string
  isCompleted: boolean
  completedAt?: string
  sortOrder: number
  repeatType?: RepeatType
  repeatInterval?: number
  repeatDays?: string
  repeatEndDate?: string
  nextDueDate?: string
  parentTodoId?: number
  transferredFromUserId?: number
  transferredFromUserName?: string
  transferredAt?: string
  createdAt: string
  createdBy: string
  updatedAt?: string
  updatedBy?: string
  shareCount?: number
  isOverdue?: boolean
  isDueToday?: boolean
  shares?: TodoShare[]
}

/**
 * Todo 공유 인터페이스
 */
export interface TodoShare {
  shareId: number
  todoId: number
  todoTitle?: string
  sharedUserId: number
  sharedUserName?: string
  sharedUsername?: string
  departmentName?: string
  sharedByUserId: number
  sharedByUserName?: string
  sharedByUsername?: string
  permission: TodoSharePermission
  createdAt: string
  createdBy: string
  // Todo 관련 필드
  todoDueDate?: string
  todoDueTime?: string
  todoPriority?: TodoPriority
  todoOwnerUserName?: string
}

/**
 * Todo 생성 요청
 */
export interface TodoCreateRequest {
  title: string
  description?: string
  priority?: TodoPriority
  dueDate?: string
  dueTime?: string
  sortOrder?: number
  repeatType?: RepeatType
  repeatInterval?: number
  repeatDays?: string
  repeatEndDate?: string
}

/**
 * Todo 수정 요청
 */
export interface TodoUpdateRequest {
  title?: string
  description?: string
  priority?: TodoPriority
  dueDate?: string
  dueTime?: string
  isCompleted?: boolean
  sortOrder?: number
  repeatType?: RepeatType
  repeatInterval?: number
  repeatDays?: string
  repeatEndDate?: string
}

/**
 * Todo 공유 요청
 */
export interface TodoShareRequest {
  sharedUsername: string
  permission?: TodoSharePermission
}

/**
 * Todo 공유 권한 수정 요청
 */
export interface TodoShareUpdateRequest {
  permission: TodoSharePermission
}

/**
 * Todo 이관 요청
 */
export interface TodoTransferRequest {
  toUsername: string
  todoIds?: number[]
}

/**
 * Todo 일괄 이관 요청 (사용자 삭제용)
 */
export interface TodoBulkTransferRequest {
  fromUserId: number
  toUserId: number
  todoIds?: number[]
}

/**
 * Todo 순서 변경 요청
 */
export interface TodoReorderRequest {
  todoIds: number[]
}

/**
 * Todo 수 응답
 */
export interface TodoCountResponse {
  total: number
  active: number
}

/**
 * 우선순위 옵션
 */
export const PRIORITY_OPTIONS: { value: TodoPriority; label: string; color: string }[] = [
  { value: 'URGENT', label: '긴급', color: 'red' },
  { value: 'HIGH', label: '높음', color: 'orange' },
  { value: 'NORMAL', label: '보통', color: 'blue' },
  { value: 'LOW', label: '낮음', color: 'gray' }
]

/**
 * 반복 타입 옵션
 */
export const REPEAT_TYPE_OPTIONS: { value: RepeatType; label: string }[] = [
  { value: 'NONE', label: '반복 없음' },
  { value: 'DAILY', label: '매일' },
  { value: 'WEEKLY', label: '매주' },
  { value: 'MONTHLY', label: '매월' },
  { value: 'YEARLY', label: '매년' }
]

/**
 * 요일 옵션 (WEEKLY 반복용)
 */
export const WEEKDAY_OPTIONS: { value: string; label: string }[] = [
  { value: '1', label: '월' },
  { value: '2', label: '화' },
  { value: '3', label: '수' },
  { value: '4', label: '목' },
  { value: '5', label: '금' },
  { value: '6', label: '토' },
  { value: '7', label: '일' }
]

/**
 * 우선순위 레벨 반환
 */
export function getPriorityLevel(priority: TodoPriority): number {
  const levels: Record<TodoPriority, number> = {
    URGENT: 4,
    HIGH: 3,
    NORMAL: 2,
    LOW: 1
  }
  return levels[priority] || 0
}

/**
 * 우선순위 색상 반환
 */
export function getPriorityColor(priority: TodoPriority): string {
  const colors: Record<TodoPriority, string> = {
    URGENT: 'bg-red-500',
    HIGH: 'bg-orange-500',
    NORMAL: 'bg-blue-500',
    LOW: 'bg-gray-400'
  }
  return colors[priority] || 'bg-gray-400'
}
