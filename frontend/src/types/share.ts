/**
 * 공유 관련 타입 정의
 */

export type SharePermission = 'VIEW' | 'EDIT' | 'FULL'

export interface Share {
  userId: number
  loginId?: string
  userName?: string
  departmentName?: string
  permission: SharePermission
  createdAt?: string
}

export interface ShareRequest {
  userId: number
  permission: SharePermission
}

export interface ShareUpdateRequest {
  permission: SharePermission
}

// 감사 로그 관련
export type AuditTargetType = 'BOARD' | 'ITEM' | 'BOARD_SHARE' | 'ITEM_SHARE'
export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'TRANSFER' | 'SHARE' | 'UNSHARE'

export interface AuditLog {
  logId: number
  targetType: AuditTargetType
  targetId: number
  targetName?: string
  action: AuditAction
  actorId: number
  actorName?: string
  description?: string
  relatedUserId?: number
  relatedUserName?: string
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

export interface AuditLogPageResponse {
  content: AuditLog[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
