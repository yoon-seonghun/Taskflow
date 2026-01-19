import { get, post, put, del } from './client'
import type {
  Todo,
  TodoShare,
  TodoCreateRequest,
  TodoUpdateRequest,
  TodoShareRequest,
  TodoShareUpdateRequest,
  TodoTransferRequest,
  TodoBulkTransferRequest,
  TodoReorderRequest,
  TodoCountResponse
} from '@/types/todo'

export const todoApi = {
  /**
   * 내 Todo 목록 조회
   */
  getTodos(params?: {
    includeCompleted?: boolean
    dueDateFrom?: string
    dueDateTo?: string
  }) {
    return get<Todo[]>('/todos', params)
  },

  /**
   * Todo 상세 조회
   */
  getTodo(todoId: number) {
    return get<Todo>(`/todos/${todoId}`)
  },

  /**
   * Todo 생성
   */
  createTodo(data: TodoCreateRequest) {
    return post<Todo>('/todos', data)
  },

  /**
   * Todo 수정
   */
  updateTodo(todoId: number, data: TodoUpdateRequest) {
    return put<Todo>(`/todos/${todoId}`, data)
  },

  /**
   * 완료 상태 토글
   */
  toggleComplete(todoId: number) {
    return put<Todo>(`/todos/${todoId}/complete`)
  },

  /**
   * Todo 삭제
   */
  deleteTodo(todoId: number) {
    return del<void>(`/todos/${todoId}`)
  },

  /**
   * Todo 이관
   */
  transferTodo(todoId: number, data: TodoTransferRequest) {
    return put<void>(`/todos/${todoId}/transfer`, data)
  },

  /**
   * Todo 순서 변경
   */
  reorderTodos(data: TodoReorderRequest) {
    return put<void>('/todos/reorder', data)
  },

  /**
   * 오늘 마감 Todo 목록
   */
  getTodayTodos() {
    return get<Todo[]>('/todos/today')
  },

  /**
   * 지연 Todo 목록
   */
  getOverdueTodos() {
    return get<Todo[]>('/todos/overdue')
  },

  /**
   * 완료된 Todo 목록
   */
  getCompletedTodos(params?: {
    completedDateFrom?: string
    completedDateTo?: string
  }) {
    return get<Todo[]>('/todos/completed', params)
  },

  /**
   * 이관받은 Todo 목록
   */
  getTransferredTodos() {
    return get<Todo[]>('/todos/transferred')
  },

  /**
   * 공유받은 Todo 목록
   */
  getSharedTodos() {
    return get<TodoShare[]>('/todos/shared')
  },

  /**
   * Todo 수 조회
   */
  getTodoCount(userId?: number) {
    return get<TodoCountResponse>('/todos/count', userId !== undefined ? { userId } : undefined)
  },

  /**
   * 일괄 이관 (사용자 삭제용)
   */
  transferAllTodos(data: TodoBulkTransferRequest) {
    return put<{ transferredCount: number }>('/todos/transfer/bulk', data)
  },

  /**
   * 사용자의 모든 Todo 삭제 (사용자 삭제용)
   */
  deleteAllTodos(userId: number) {
    return del<{ deletedCount: number }>(`/todos/by-user/${userId}`)
  },

  // ============================================
  // Todo 공유 API
  // ============================================

  /**
   * Todo 공유 목록 조회
   */
  getShares(todoId: number) {
    return get<TodoShare[]>(`/todos/${todoId}/shares`)
  },

  /**
   * Todo 공유 추가
   */
  addShare(todoId: number, data: TodoShareRequest) {
    return post<TodoShare>(`/todos/${todoId}/shares`, data)
  },

  /**
   * 공유 권한 변경
   */
  updateSharePermission(shareId: number, data: TodoShareUpdateRequest) {
    return put<TodoShare>(`/todos/shares/${shareId}`, data)
  },

  /**
   * 공유 삭제
   */
  deleteShare(shareId: number) {
    return del<void>(`/todos/shares/${shareId}`)
  },

  /**
   * 특정 사용자 공유 삭제
   */
  deleteShareByUser(todoId: number, userId: number) {
    return del<void>(`/todos/${todoId}/shares/${userId}`)
  }
}
