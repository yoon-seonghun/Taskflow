/**
 * 업무 배정 관련 타입 정의
 */

/**
 * 공유 유형
 */
export type ShareType = 'SHARE' | 'ASSIGN'

/**
 * 권한 수준
 */
export type PermissionLevel = 'VIEW' | 'EDIT' | 'FULL'

/**
 * 배정 요청
 */
export interface AssignmentRequest {
  /** 담당자 USERNAME */
  assigneeUsername: string
  /** 권한 수준 */
  permissionLevel: PermissionLevel
  /** 이메일 발송 여부 */
  sendEmail?: boolean
  /** 앱 알림 발송 여부 */
  sendNotification?: boolean
}

/**
 * 배정 응답
 */
export interface AssignmentResponse {
  /** 공유 ID */
  shareId: number
  /** 업무 ID */
  itemId: number
  /** 담당자 USERNAME */
  assigneeUsername: string
  /** 담당자 이름 */
  assigneeName: string
  /** 공유 유형 */
  shareType: ShareType
  /** 권한 수준 */
  permissionLevel: PermissionLevel
  /** 배당자 USERNAME */
  assignedBy: string
  /** 배당자 이름 */
  assignedByName: string
  /** 배당 일시 */
  assignedAt: string
  /** 이메일 발송 여부 */
  emailSent?: boolean
  /** 알림 발송 여부 */
  notificationSent?: boolean
}

/**
 * 업무 접근 권한 정보
 */
export interface ItemAccessInfo {
  /** 소유자 여부 */
  isOwner: boolean
  /** 공유 유형 (null: 소유자) */
  shareType: ShareType | null
  /** 권한 수준 (null: 소유자) */
  permissionLevel: PermissionLevel | null
  /** 배당자 USERNAME */
  assignedBy: string | null
  /** 배당자 이름 */
  assignedByName: string | null
  /** 배당 일시 */
  assignedAt: string | null
  /** 제목 편집 가능 여부 */
  canEditTitle: boolean
  /** 기본 속성 편집 가능 여부 */
  canEditBasicProperties: boolean
  /** 사용자 속성 편집 가능 여부 */
  canEditUserProperties: boolean
  /** 담당자 배정 가능 여부 */
  canAssign: boolean
  /** 삭제 가능 여부 */
  canDelete: boolean
  /** 완료 처리 가능 여부 */
  canComplete: boolean
}

/**
 * 권한 수준 옵션
 */
export const permissionLevelOptions: { value: PermissionLevel; label: string; description: string }[] = [
  {
    value: 'VIEW',
    label: '조회',
    description: '업무 내용 조회 및 댓글 작성만 가능'
  },
  {
    value: 'EDIT',
    label: '편집',
    description: '사용자 정의 속성 편집 가능'
  },
  {
    value: 'FULL',
    label: '전체',
    description: '기본 속성(상태, 우선순위 등) 편집 가능, 제목은 소유자만 수정 가능'
  }
]

/**
 * 기본 속성 목록 (FULL 권한 이상만 수정 가능)
 */
export const basicPropertyCodes = [
  'status',
  'priority',
  'requestDate',
  'dueDate',
  'categoryId',
  'groupId'
]

/**
 * 속성 편집 가능 여부 확인
 */
export function canEditProperty(accessInfo: ItemAccessInfo | null, propertyCode: string): boolean {
  if (!accessInfo) return false
  if (accessInfo.isOwner) return true

  if (propertyCode === 'title') return accessInfo.canEditTitle
  if (basicPropertyCodes.includes(propertyCode)) return accessInfo.canEditBasicProperties
  return accessInfo.canEditUserProperties
}
