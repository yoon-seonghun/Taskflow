/**
 * 알림 관련 타입 정의
 */

/**
 * 알림 유형
 */
export type NotificationType = 'ITEM_ASSIGNED' | 'ITEM_SHARED' | 'ITEM_UPDATED' | 'ITEM_COMPLETED'

/**
 * 알림 응답
 */
export interface NotificationResponse {
  /** 알림 ID */
  notificationId: number
  /** 알림 유형 */
  notificationType: NotificationType
  /** 알림 제목 */
  title: string
  /** 알림 내용 */
  message: string | null
  /** 관련 대상 유형 */
  relatedType: string | null
  /** 관련 대상 ID */
  relatedId: number | null
  /** 관련 링크 URL */
  relatedUrl: string | null
  /** 읽음 여부 */
  isRead: boolean
  /** 읽은 시간 */
  readAt: string | null
  /** 생성일시 */
  createdAt: string
  /** 발신자 USERNAME */
  createdBy: string
  /** 발신자 이름 */
  createdByName: string | null
}

/**
 * 알림 목록 페이지 응답
 */
export interface NotificationPageResponse {
  content: NotificationResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

/**
 * 알림 유형별 아이콘 및 색상
 */
export const notificationTypeConfig: Record<NotificationType, { icon: string; color: string; bgColor: string }> = {
  ITEM_ASSIGNED: {
    icon: 'user-plus',
    color: 'text-green-600',
    bgColor: 'bg-green-100'
  },
  ITEM_SHARED: {
    icon: 'share',
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
  },
  ITEM_UPDATED: {
    icon: 'edit',
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  ITEM_COMPLETED: {
    icon: 'check-circle',
    color: 'text-purple-600',
    bgColor: 'bg-purple-100'
  }
}
