<script setup lang="ts">
/**
 * 사용자 목록 컴포넌트
 * - 사용자 테이블 표시
 * - 선택, 활성/비활성화, 삭제 기능
 */
import { computed } from 'vue'
import Badge from '@/components/common/Badge.vue'
import type { User } from '@/types/user'

interface Props {
  users: User[]
  selectedUserId?: number | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selectedUserId: null,
  loading: false
})

const emit = defineEmits<{
  (e: 'select', user: User): void
  (e: 'toggle-status', user: User): void
  (e: 'delete', user: User): void
}>()

// 빈 데이터 여부
const isEmpty = computed(() => props.users.length === 0)

// 날짜 포맷팅
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return '-'
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 행 클릭 (선택)
function handleRowClick(user: User) {
  emit('select', user)
}

// 상태 토글
function handleToggleStatus(event: Event, user: User) {
  event.stopPropagation()
  emit('toggle-status', user)
}

// 삭제
function handleDelete(event: Event, user: User) {
  event.stopPropagation()
  emit('delete', user)
}
</script>

<template>
  <div class="user-list-wrapper">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="py-12 text-center">
      <svg class="animate-spin h-8 w-8 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">사용자 목록을 불러오는 중...</p>
    </div>

    <!-- 빈 상태 -->
    <div v-else-if="isEmpty" class="py-12 text-center">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">등록된 사용자가 없습니다.</p>
    </div>

    <!-- 사용자 테이블 -->
    <table v-else class="user-table">
      <thead>
        <tr>
          <th class="w-[100px]">사용자 이름</th>
          <th class="w-[100px]">아이디</th>
          <th class="w-[150px]">이메일</th>
          <th class="w-[100px]">부서</th>
          <th class="w-[70px]">상태</th>
          <th class="w-[130px]">등록일시</th>
          <th class="w-[80px]">관리</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="user in users"
          :key="user.userId"
          class="cursor-pointer"
          :class="{ 'bg-primary-50': selectedUserId === user.userId }"
          @click="handleRowClick(user)"
        >
          <td class="font-medium text-gray-900">
            {{ user.userName || user.name }}
          </td>
          <td class="text-gray-600">
            {{ user.username }}
          </td>
          <td class="text-gray-600 truncate max-w-[150px]" :title="user.email">
            {{ user.email || '-' }}
          </td>
          <td class="text-gray-600">
            {{ user.departmentName || '-' }}
          </td>
          <td>
            <Badge
              :variant="user.useYn === 'Y' ? 'success' : 'default'"
              size="sm"
            >
              {{ user.useYn === 'Y' ? '활성' : '비활성' }}
            </Badge>
          </td>
          <td class="text-gray-500">
            {{ formatDateTime(user.createdAt) }}
          </td>
          <td>
            <div class="flex items-center gap-2">
              <!-- 상태 토글 버튼 -->
              <button
                type="button"
                class="action-btn"
                :title="user.useYn === 'Y' ? '비활성화' : '활성화'"
                @click="handleToggleStatus($event, user)"
              >
                <svg v-if="user.useYn === 'Y'" class="w-4 h-4 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                </svg>
                <svg v-else class="w-4 h-4 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </button>

              <!-- 삭제 버튼 -->
              <button
                type="button"
                class="action-btn"
                title="삭제"
                @click="handleDelete($event, user)"
              >
                <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.user-list-wrapper {
  @apply overflow-x-auto;
}

.user-table {
  @apply w-full text-sm;
}

.user-table thead {
  @apply bg-gray-50 border-y border-gray-200;
}

.user-table th {
  @apply px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider;
}

.user-table tbody {
  @apply divide-y divide-gray-100;
}

.user-table td {
  @apply px-4 py-3 whitespace-nowrap;
}

.user-table tbody tr {
  @apply hover:bg-gray-50 transition-colors;
}

.action-btn {
  @apply p-1.5 rounded hover:bg-gray-100 transition-colors;
}
</style>
