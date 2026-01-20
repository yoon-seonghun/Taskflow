import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/api/notification'
import type { NotificationResponse } from '@/types/notification'

/**
 * 알림 스토어
 */
export const useNotificationStore = defineStore('notification', () => {
  // State
  const notifications = ref<NotificationResponse[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const totalElements = ref(0)
  const totalPages = ref(0)
  const currentPage = ref(0)

  // Getters
  const unreadNotifications = computed(() =>
    notifications.value.filter(n => !n.isRead)
  )

  const hasUnread = computed(() => unreadCount.value > 0)

  // Actions
  /**
   * 알림 목록 조회
   */
  async function fetchNotifications(page: number = 0, size: number = 20, unreadOnly: boolean = false) {
    loading.value = true
    try {
      const response = await notificationApi.getNotifications(page, size, unreadOnly)
      if (response.success && response.data) {
        notifications.value = response.data.content
        totalElements.value = response.data.totalElements
        totalPages.value = response.data.totalPages
        currentPage.value = response.data.number
      }
    } catch (error) {
      console.error('알림 목록 조회 실패:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 읽지 않은 알림 수 조회
   */
  async function fetchUnreadCount() {
    try {
      const response = await notificationApi.getUnreadCount()
      if (response.success && response.data) {
        unreadCount.value = response.data.count
      }
    } catch (error) {
      console.error('읽지 않은 알림 수 조회 실패:', error)
    }
  }

  /**
   * 알림 읽음 처리
   */
  async function markAsRead(notificationId: number) {
    try {
      const response = await notificationApi.markAsRead(notificationId)
      if (response.success) {
        // 로컬 상태 업데이트
        const notification = notifications.value.find(n => n.notificationId === notificationId)
        if (notification && !notification.isRead) {
          notification.isRead = true
          notification.readAt = new Date().toISOString()
          unreadCount.value = Math.max(0, unreadCount.value - 1)
        }
      }
    } catch (error) {
      console.error('알림 읽음 처리 실패:', error)
    }
  }

  /**
   * 전체 읽음 처리
   */
  async function markAllAsRead() {
    try {
      const response = await notificationApi.markAllAsRead()
      if (response.success) {
        // 로컬 상태 업데이트
        notifications.value.forEach(n => {
          if (!n.isRead) {
            n.isRead = true
            n.readAt = new Date().toISOString()
          }
        })
        unreadCount.value = 0
      }
    } catch (error) {
      console.error('전체 읽음 처리 실패:', error)
    }
  }

  /**
   * 새 알림 추가 (SSE용)
   */
  function addNotification(notification: NotificationResponse) {
    notifications.value.unshift(notification)
    if (!notification.isRead) {
      unreadCount.value++
    }
  }

  /**
   * 초기화
   */
  function reset() {
    notifications.value = []
    unreadCount.value = 0
    totalElements.value = 0
    totalPages.value = 0
    currentPage.value = 0
  }

  return {
    // State
    notifications,
    unreadCount,
    loading,
    totalElements,
    totalPages,
    currentPage,

    // Getters
    unreadNotifications,
    hasUnread,

    // Actions
    fetchNotifications,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
    addNotification,
    reset
  }
})
