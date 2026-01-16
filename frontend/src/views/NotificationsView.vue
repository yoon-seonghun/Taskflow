<script setup lang="ts">
/**
 * 알림 전체보기 페이지 (v2.2.1)
 * - 알림 목록 페이징
 * - 읽음/안읽음 필터
 * - 전체 읽음 처리
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { storeToRefs } from 'pinia'
import Pagination from '@/components/common/Pagination.vue'

const router = useRouter()
const notificationStore = useNotificationStore()
const { notifications, loading, totalElements, totalPages, currentPage, unreadCount } = storeToRefs(notificationStore)

// 필터 상태
const filterTab = ref<'all' | 'unread'>('all')
const pageSize = ref(20)

// 필터 적용된 조회
const unreadOnly = computed(() => filterTab.value === 'unread')

// 알림 조회
async function loadNotifications(page: number = 0) {
  await notificationStore.fetchNotifications(page, pageSize.value, unreadOnly.value)
}

// 페이지 변경
function handlePageChange(page: number) {
  loadNotifications(page)
}

// 필터 변경
function handleFilterChange(tab: 'all' | 'unread') {
  filterTab.value = tab
  loadNotifications(0)
}

// 전체 읽음 처리
async function handleMarkAllAsRead() {
  await notificationStore.markAllAsRead()
  if (filterTab.value === 'unread') {
    // 안읽음 필터 상태면 목록 새로고침
    loadNotifications(0)
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
}

// 시간 포맷 (상대 시간)
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

  return date.toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' })
}

// 알림 아이콘 (SVG path)
function getNotificationIcon(type: string): string {
  switch (type) {
    case 'ITEM_ASSIGNED':
      return 'M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z'
    case 'ITEM_SHARED':
      return 'M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z'
    case 'ITEM_COMPLETED':
      return 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'
    case 'ITEM_UPDATED':
      return 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z'
    default:
      return 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9'
  }
}

// 알림 색상
function getNotificationColor(type: string): { icon: string; bg: string } {
  switch (type) {
    case 'ITEM_ASSIGNED':
      return { icon: 'text-green-600', bg: 'bg-green-100' }
    case 'ITEM_SHARED':
      return { icon: 'text-blue-600', bg: 'bg-blue-100' }
    case 'ITEM_COMPLETED':
      return { icon: 'text-purple-600', bg: 'bg-purple-100' }
    case 'ITEM_UPDATED':
      return { icon: 'text-yellow-600', bg: 'bg-yellow-100' }
    default:
      return { icon: 'text-gray-600', bg: 'bg-gray-100' }
  }
}

// 알림 타입 라벨
function getTypeLabel(type: string): string {
  switch (type) {
    case 'ITEM_ASSIGNED': return '배정'
    case 'ITEM_SHARED': return '공유'
    case 'ITEM_COMPLETED': return '완료'
    case 'ITEM_UPDATED': return '업데이트'
    default: return '알림'
  }
}

// 초기 로드
onMounted(() => {
  loadNotifications(0)
  notificationStore.fetchUnreadCount()
})

// 필터 변경 감지
watch(filterTab, () => {
  loadNotifications(0)
})
</script>

<template>
  <div class="notifications-view">
    <!-- 헤더 -->
    <div class="header-section">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-xl font-semibold text-gray-900">알림</h1>
          <p class="text-sm text-gray-500 mt-1">
            총 {{ totalElements }}건
            <span v-if="unreadCount > 0" class="text-primary-600">
              (읽지 않은 알림 {{ unreadCount }}건)
            </span>
          </p>
        </div>

        <!-- 전체 읽음 버튼 -->
        <button
          v-if="unreadCount > 0"
          class="btn-secondary"
          @click="handleMarkAllAsRead"
        >
          <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          전체 읽음
        </button>
      </div>

      <!-- 필터 탭 -->
      <div class="flex gap-2 mt-4">
        <button
          :class="[
            'filter-tab',
            filterTab === 'all' ? 'filter-tab-active' : 'filter-tab-inactive'
          ]"
          @click="handleFilterChange('all')"
        >
          전체
        </button>
        <button
          :class="[
            'filter-tab',
            filterTab === 'unread' ? 'filter-tab-active' : 'filter-tab-inactive'
          ]"
          @click="handleFilterChange('unread')"
        >
          읽지 않음
          <span v-if="unreadCount > 0" class="ml-1 px-1.5 py-0.5 text-xs bg-red-500 text-white rounded-full">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </button>
      </div>
    </div>

    <!-- 알림 목록 -->
    <div class="notification-list">
      <!-- 로딩 -->
      <div v-if="loading" class="flex items-center justify-center py-12">
        <svg class="w-8 h-8 animate-spin text-primary-600" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      </div>

      <!-- 빈 상태 -->
      <div v-else-if="notifications.length === 0" class="empty-state">
        <svg class="w-12 h-12 mx-auto text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        <p class="text-gray-500">
          {{ filterTab === 'unread' ? '읽지 않은 알림이 없습니다.' : '알림이 없습니다.' }}
        </p>
      </div>

      <!-- 알림 항목 -->
      <div v-else class="divide-y divide-gray-100">
        <div
          v-for="notification in notifications"
          :key="notification.notificationId"
          class="notification-item"
          :class="{ 'notification-unread': !notification.isRead }"
          @click="handleNotificationClick(notification)"
        >
          <!-- 아이콘 -->
          <div
            class="notification-icon"
            :class="[getNotificationColor(notification.notificationType).bg]"
          >
            <svg
              class="w-5 h-5"
              :class="getNotificationColor(notification.notificationType).icon"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                :d="getNotificationIcon(notification.notificationType)"
              />
            </svg>
          </div>

          <!-- 내용 -->
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between gap-2">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-0.5">
                  <span
                    class="inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full"
                    :class="[getNotificationColor(notification.notificationType).bg, getNotificationColor(notification.notificationType).icon]"
                  >
                    {{ getTypeLabel(notification.notificationType) }}
                  </span>
                  <span v-if="!notification.isRead" class="w-2 h-2 bg-blue-500 rounded-full" />
                </div>
                <p class="text-sm font-medium text-gray-900 truncate">{{ notification.title }}</p>
                <p v-if="notification.message" class="text-sm text-gray-500 truncate mt-0.5">
                  {{ notification.message }}
                </p>
              </div>
              <div class="flex-shrink-0 text-right">
                <p class="text-xs text-gray-400">{{ formatTime(notification.createdAt) }}</p>
                <p v-if="notification.createdByName" class="text-xs text-gray-400 mt-0.5">
                  {{ notification.createdByName }}
                </p>
              </div>
            </div>
          </div>

          <!-- 화살표 -->
          <svg class="w-5 h-5 text-gray-300 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </div>
    </div>

    <!-- 페이지네이션 -->
    <div v-if="totalPages > 1" class="pagination-section">
      <Pagination
        :current-page="currentPage"
        :total-pages="totalPages"
        @change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.notifications-view {
  @apply max-w-4xl mx-auto p-4;
}

.header-section {
  @apply bg-white rounded-lg border border-gray-200 p-4 mb-4;
}

.notification-list {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden;
}

.notification-item {
  @apply flex items-center gap-4 px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors;
}

.notification-unread {
  @apply bg-blue-50/50;
}

.notification-icon {
  @apply w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0;
}

.empty-state {
  @apply text-center py-12;
}

.pagination-section {
  @apply mt-4 flex justify-center;
}

.filter-tab {
  @apply inline-flex items-center px-4 py-2 text-sm font-medium rounded-lg transition-colors;
}

.filter-tab-active {
  @apply bg-primary-600 text-white;
}

.filter-tab-inactive {
  @apply bg-gray-100 text-gray-700 hover:bg-gray-200;
}

.btn-secondary {
  @apply inline-flex items-center px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors;
}
</style>
