<script setup lang="ts">
/**
 * 알림 드롭다운 컴포넌트
 * - 헤더에서 알림 아이콘 클릭 시 표시
 * - 최근 알림 목록 표시
 * - 읽음 처리
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { storeToRefs } from 'pinia'

const router = useRouter()
const notificationStore = useNotificationStore()
const { notifications, unreadCount, loading, hasUnread } = storeToRefs(notificationStore)

// State
const isOpen = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

// 드롭다운 토글
function toggleDropdown() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    notificationStore.fetchNotifications(0, 10)
  }
}

// 외부 클릭 감지
function handleClickOutside(event: MouseEvent) {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

// 알림 클릭
async function handleNotificationClick(notification: any) {
  // 읽음 처리
  if (!notification.isRead) {
    await notificationStore.markAsRead(notification.notificationId)
  }

  // 관련 페이지로 이동
  if (notification.relatedUrl) {
    router.push(notification.relatedUrl)
  }

  isOpen.value = false
}

// 전체 읽음 처리
async function handleMarkAllAsRead() {
  await notificationStore.markAllAsRead()
}

// 시간 포맷
function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return '방금 전'
  if (diffMins < 60) return `${diffMins}분 전`
  if (diffHours < 24) return `${diffHours}시간 전`
  if (diffDays < 7) return `${diffDays}일 전`

  return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

// 알림 아이콘
function getNotificationIcon(type: string): string {
  switch (type) {
    case 'ITEM_ASSIGNED':
      return 'M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z'
    case 'ITEM_SHARED':
      return 'M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z'
    case 'ITEM_COMPLETED':
      return 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'
    default:
      return 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9'
  }
}

// 알림 색상
function getNotificationColor(type: string): string {
  switch (type) {
    case 'ITEM_ASSIGNED':
      return 'text-green-600 bg-green-100 dark:text-green-400 dark:bg-green-900/50'
    case 'ITEM_SHARED':
      return 'text-blue-600 bg-blue-100 dark:text-blue-400 dark:bg-blue-900/50'
    case 'ITEM_COMPLETED':
      return 'text-purple-600 bg-purple-100 dark:text-purple-400 dark:bg-purple-900/50'
    default:
      return 'text-gray-600 bg-gray-100 dark:text-gray-400 dark:bg-gray-700'
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  notificationStore.fetchUnreadCount()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div ref="dropdownRef" class="relative">
    <!-- 알림 버튼 -->
    <button
      @click="toggleDropdown"
      class="relative p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:bg-gray-700 rounded-full transition-colors"
    >
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
      </svg>
      <!-- 읽지 않은 알림 배지 -->
      <span
        v-if="hasUnread"
        class="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 text-[10px] font-bold text-white bg-red-500 rounded-full flex items-center justify-center"
      >
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>

    <!-- 드롭다운 패널 -->
    <Transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="opacity-0 translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition ease-in duration-150"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 translate-y-1"
    >
      <div
        v-if="isOpen"
        class="absolute right-0 mt-2 w-80 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 z-50"
      >
        <!-- 헤더 -->
        <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100 dark:border-gray-700">
          <h3 class="font-medium text-gray-900 dark:text-white">
            알림
            <span v-if="hasUnread" class="ml-1 text-sm text-gray-500 dark:text-gray-400">({{ unreadCount }})</span>
          </h3>
          <button
            v-if="hasUnread"
            @click="handleMarkAllAsRead"
            class="text-xs text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
          >
            모두 읽음
          </button>
        </div>

        <!-- 알림 목록 -->
        <div class="max-h-80 overflow-y-auto">
          <!-- 로딩 -->
          <div v-if="loading" class="flex justify-center py-8">
            <svg class="w-6 h-6 text-gray-400 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>

          <!-- 빈 상태 -->
          <div v-else-if="notifications.length === 0" class="py-8 text-center text-gray-500 dark:text-gray-400 text-sm">
            알림이 없습니다
          </div>

          <!-- 알림 항목 -->
          <div v-else>
            <button
              v-for="notification in notifications"
              :key="notification.notificationId"
              @click="handleNotificationClick(notification)"
              class="w-full px-4 py-3 flex items-start gap-3 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-left"
              :class="{ 'bg-blue-50/50 dark:bg-blue-900/20': !notification.isRead }"
            >
              <!-- 아이콘 -->
              <div
                class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center"
                :class="getNotificationColor(notification.notificationType)"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="getNotificationIcon(notification.notificationType)" />
                </svg>
              </div>

              <!-- 내용 -->
              <div class="flex-1 min-w-0">
                <p class="text-sm text-gray-900 dark:text-white" :class="{ 'font-medium': !notification.isRead }">
                  {{ notification.title }}
                </p>
                <p v-if="notification.message" class="text-xs text-gray-500 dark:text-gray-400 mt-0.5 truncate">
                  {{ notification.message }}
                </p>
                <p class="text-xs text-gray-400 dark:text-gray-500 mt-1">
                  {{ formatTime(notification.createdAt) }}
                </p>
              </div>

              <!-- 읽지 않음 표시 -->
              <div v-if="!notification.isRead" class="flex-shrink-0 w-2 h-2 bg-blue-500 rounded-full mt-2"></div>
            </button>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="px-4 py-2 border-t border-gray-100 dark:border-gray-700 text-center">
          <router-link
            to="/notifications"
            class="text-sm text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
            @click="isOpen = false"
          >
            전체 알림 보기
          </router-link>
        </div>
      </div>
    </Transition>
  </div>
</template>
