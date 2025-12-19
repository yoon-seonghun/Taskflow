/**
 * 이력 관리 타입 정의
 */

export type HistoryResult = 'COMPLETED' | 'DELETED'

export type PreviousStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'PENDING'

export interface ItemHistory {
  itemId: number
  boardId: number
  boardName?: string
  title: string
  result: HistoryResult
  workerId?: number
  workerName?: string
  createdAt: string
  createdByName?: string
  startTime?: string
  completedAt?: string
  updatedAt?: string
  deletedAt?: string
  previousStatus?: PreviousStatus
}

export interface ItemHistorySearchRequest {
  boardId?: number
  result?: HistoryResult
  workerId?: number
  startDate?: string
  endDate?: string
  keyword?: string
  page?: number
  size?: number
  sort?: string
}

export interface TemplateHistory {
  templateId: number
  content: string
  status: string
  createdBy: number
  createdByName?: string
  createdAt: string
  updatedAt?: string
  updatedByName?: string
}

export interface TemplateHistorySearchRequest {
  status?: string
  createdBy?: number
  startDate?: string
  endDate?: string
  keyword?: string
  page?: number
  size?: number
  sort?: string
}
