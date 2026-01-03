/**
 * 공유 관련 타입 정의
 */

export type SharePermission = 'VIEW' | 'EDIT' | 'FULL'

export interface Share {
  username: string           // 공유 사용자 USERNAME
  userName?: string          // 표시용 이름
  departmentName?: string
  permission: SharePermission
  createdAt?: string
}

export interface ShareRequest {
  username: string           // 공유 대상 사용자 USERNAME
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
  actorId?: number            // 백엔드 검색용 (Long 타입 유지)
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
