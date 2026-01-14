import { get, put } from './client'
import type { NotificationResponse, NotificationPageResponse } from '@/types/notification'

/**
 * 알림 API
 */
export const notificationApi = {
  /**
   * 내 알림 목록 조회
   */
  getNotifications(page: number = 0, size: number = 20, unreadOnly: boolean = false) {
    return get<NotificationPageResponse>('/notifications', {
      params: { page, size, unreadOnly }
    })
  },

  /**
   * 읽지 않은 알림 수 조회
   */
  getUnreadCount() {
    return get<{ count: number }>('/notifications/unread-count')
  },

  /**
   * 알림 읽음 처리
   */
  markAsRead(notificationId: number) {
    return put<NotificationResponse>(`/notifications/${notificationId}/read`)
  },

  /**
   * 전체 읽음 처리
   */
  markAllAsRead() {
    return put<{ updatedCount: number }>('/notifications/read-all')
  }
}
