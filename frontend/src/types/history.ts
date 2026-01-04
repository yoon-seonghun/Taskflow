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
  workerUsername?: string
  workerName?: string
  createdAt: string
  createdByName?: string
  requestDate?: string
  completedAt?: string
  updatedAt?: string
  deletedAt?: string
  previousStatus?: PreviousStatus
}

export interface ItemHistorySearchRequest {
  boardId?: number
  result?: HistoryResult
  workerUsername?: string
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
  createdBy: string          // 생성자 USERNAME
  createdByName?: string     // 생성자 표시명
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

// 관리 이력 (감사 로그)
export type AuditTargetType = 'BOARD' | 'ITEM' | 'BOARD_SHARE' | 'ITEM_SHARE'
export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'TRANSFER' | 'SHARE' | 'UNSHARE'

export interface AuditLog {
  logId: number
  targetType: AuditTargetType
  targetId: number
  targetName?: string
  action: AuditAction
  actorUsername?: string      // 수행자 USERNAME
  actorName?: string          // 수행자 표시명
  description?: string
  relatedUsername?: string    // 관련 사용자 USERNAME (공유/이관 대상)
  relatedUserName?: string    // 관련 사용자 표시명
  createdAt: string
}

export interface AuditLogSearchRequest {
  targetType?: AuditTargetType
  action?: AuditAction
  actorId?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}
