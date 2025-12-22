<script setup lang="ts">
/**
 * 공유 사용자 목록 컴포넌트
 * - 공유된 사용자 테이블 표시
 * - 권한 표시 및 제거 기능
 */
import { computed } from 'vue'
import Badge from '@/components/common/Badge.vue'
import type { BoardShare, BoardPermission } from '@/types/board'

interface Props {
  shares: BoardShare[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  (e: 'remove', share: BoardShare): void
}>()

// 빈 데이터 여부
const isEmpty = computed(() => props.shares.length === 0)

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

// 권한 뱃지 설정
function getPermissionBadge(permission: BoardPermission) {
  switch (permission) {
    case 'VIEW':
      return { label: '조회', variant: 'default' as const }
    case 'EDIT':
      return { label: '편집', variant: 'primary' as const }
    case 'FULL':
      return { label: '전체', variant: 'success' as const }
    default:
      return { label: permission, variant: 'default' as const }
  }
}

// 제거
function handleRemove(share: BoardShare) {
  emit('remove', share)
}
</script>

<template>
  <div class="share-user-list-wrapper">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="py-12 text-center">
      <svg class="animate-spin h-8 w-8 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">공유 사용자를 불러오는 중...</p>
    </div>

    <!-- 빈 상태 -->
    <div v-else-if="isEmpty" class="py-12 text-center">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">공유된 사용자가 없습니다.</p>
      <p class="mt-1 text-xs text-gray-400">사용자를 검색하여 공유 목록에 추가하세요.</p>
    </div>

    <!-- 공유 사용자 테이블 -->
    <table v-else class="share-table">
      <thead>
        <tr>
          <th class="w-[200px]">사용자</th>
          <th class="w-[100px]">권한</th>
          <th class="w-[160px]">공유일시</th>
          <th class="w-[80px]">관리</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="share in shares" :key="share.boardShareId">
          <td>
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center flex-shrink-0">
                <span class="text-sm font-medium text-primary-700">
                  {{ share.userName?.charAt(0) || '?' }}
                </span>
              </div>
              <div>
                <div class="font-medium text-gray-900">{{ share.userName || '-' }}</div>
                <div class="text-xs text-gray-500">ID: {{ share.userId }}</div>
              </div>
            </div>
          </td>
          <td>
            <Badge :variant="getPermissionBadge(share.permission).variant" size="sm">
              {{ getPermissionBadge(share.permission).label }}
            </Badge>
          </td>
          <td class="text-gray-500">
            {{ formatDateTime(share.createdAt) }}
          </td>
          <td>
            <button
              type="button"
              class="action-btn text-red-500 hover:text-red-700 hover:bg-red-50"
              title="공유 해제"
              @click="handleRemove(share)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M13 7a4 4 0 11-8 0 4 4 0 018 0zM9 14a6 6 0 00-6 6v1h12v-1a6 6 0 00-6-6zM21 12h-6" />
              </svg>
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.share-user-list-wrapper {
  @apply overflow-x-auto;
}

.share-table {
  @apply w-full text-sm;
}

.share-table thead {
  @apply bg-gray-50 border-y border-gray-200;
}

.share-table th {
  @apply px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider;
}

.share-table tbody {
  @apply divide-y divide-gray-100;
}

.share-table td {
  @apply px-4 py-3 whitespace-nowrap;
}

.share-table tbody tr {
  @apply hover:bg-gray-50 transition-colors;
}

.action-btn {
  @apply p-1.5 rounded transition-colors;
}
</style>
