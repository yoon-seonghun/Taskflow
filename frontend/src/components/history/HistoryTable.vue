<script setup lang="ts">
/**
 * 이력 테이블 컴포넌트
 * - 작업 처리 이력 / 작업 등록 이력 / 관리 이력 표시
 */
import { computed } from 'vue'
import Badge from '@/components/common/Badge.vue'
import type { ItemHistory, TemplateHistory, AuditLog, AuditAction, AuditTargetType } from '@/types/history'
import type { HistoryType } from './HistorySwitch.vue'

interface Props {
  type: HistoryType
  itemHistories?: ItemHistory[]
  templateHistories?: TemplateHistory[]
  managementHistories?: AuditLog[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  itemHistories: () => [],
  templateHistories: () => [],
  managementHistories: () => [],
  loading: false
})

const emit = defineEmits<{
  (e: 'openItem', itemId: number, boardId: number): void
}>()

// 날짜 포맷팅
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  // 유효하지 않은 날짜 처리
  if (isNaN(date.getTime())) return '-'
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 작업 결과 뱃지 설정
function getResultBadge(result: string) {
  switch (result) {
    case 'COMPLETED':
      return { label: '완료', variant: 'success' as const }
    case 'DELETED':
      return { label: '삭제', variant: 'danger' as const }
    default:
      return { label: result, variant: 'default' as const }
  }
}

// 템플릿 상태 뱃지 설정
function getStatusBadge(status: string) {
  switch (status) {
    case 'ACTIVE':
      return { label: '활성', variant: 'success' as const }
    case 'INACTIVE':
      return { label: '비활성', variant: 'default' as const }
    default:
      return { label: status, variant: 'default' as const }
  }
}

// 관리 이력 - 액션 뱃지 설정
function getActionBadge(action: AuditAction) {
  switch (action) {
    case 'CREATE':
      return { label: '생성', variant: 'success' as const }
    case 'UPDATE':
      return { label: '수정', variant: 'info' as const }
    case 'DELETE':
      return { label: '삭제', variant: 'danger' as const }
    case 'TRANSFER':
      return { label: '이관', variant: 'warning' as const }
    case 'SHARE':
      return { label: '공유', variant: 'primary' as const }
    case 'UNSHARE':
      return { label: '공유해제', variant: 'default' as const }
    default:
      return { label: action, variant: 'default' as const }
  }
}

// 관리 이력 - 대상 타입 라벨
function getTargetTypeLabel(targetType: AuditTargetType) {
  switch (targetType) {
    case 'BOARD':
      return '보드'
    case 'ITEM':
      return '업무'
    case 'BOARD_SHARE':
      return '보드 공유'
    case 'ITEM_SHARE':
      return '업무 공유'
    default:
      return targetType
  }
}

// 빈 데이터 여부
const isEmpty = computed(() => {
  if (props.type === 'item') {
    return props.itemHistories.length === 0
  } else if (props.type === 'template') {
    return props.templateHistories.length === 0
  } else if (props.type === 'management') {
    return props.managementHistories.length === 0
  }
  return props.templateHistories.length === 0
})

// 빈 데이터 메시지
const emptyMessage = computed(() => {
  switch (props.type) {
    case 'item':
      return '작업 처리 이력이 없습니다.'
    case 'template':
      return '작업 등록 이력이 없습니다.'
    case 'management':
      return '관리 이력이 없습니다.'
    default:
      return '이력이 없습니다.'
  }
})
</script>

<template>
  <div class="history-table-wrapper">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="py-12 text-center">
      <svg class="animate-spin h-8 w-8 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">이력을 불러오는 중...</p>
    </div>

    <!-- 빈 상태 -->
    <div v-else-if="isEmpty" class="py-12 text-center">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">
        {{ emptyMessage }}
      </p>
    </div>

    <!-- 작업 처리 이력 테이블 -->
    <table v-else-if="type === 'item'" class="history-table">
      <thead>
        <tr>
          <th class="w-[300px]">작업내용</th>
          <th class="w-[80px]">작업결과</th>
          <th class="w-[100px]">작업자</th>
          <th class="w-[140px]">등록시간</th>
          <th class="w-[140px]">시작시간</th>
          <th class="w-[140px]">완료시간</th>
          <th class="w-[140px]">수정시간</th>
          <th class="w-[140px]">삭제시간</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in itemHistories" :key="item.itemId" class="group">
          <td class="font-medium text-gray-900">
            <div class="flex items-center gap-2">
              <span class="truncate flex-1" :title="item.title">
                {{ item.title }}
              </span>
              <!-- Notion 스타일 '열기' 버튼 (hover 시 표시) -->
              <button
                class="flex-shrink-0 opacity-0 group-hover:opacity-100 transition-opacity px-1.5 py-0.5 text-[11px] text-gray-500 hover:text-primary-600 hover:bg-primary-50 rounded"
                title="상세 패널 열기"
                @click="emit('openItem', item.itemId, item.boardId)"
              >
                열기
              </button>
            </div>
          </td>
          <td>
            <Badge :variant="getResultBadge(item.result).variant" size="sm">
              {{ getResultBadge(item.result).label }}
            </Badge>
          </td>
          <td>{{ item.workerName || '-' }}</td>
          <td>{{ formatDateTime(item.createdAt) }}</td>
          <td>{{ formatDateTime(item.startTime) }}</td>
          <td>{{ formatDateTime(item.completedAt) }}</td>
          <td>{{ formatDateTime(item.updatedAt) }}</td>
          <td>{{ formatDateTime(item.deletedAt) }}</td>
        </tr>
      </tbody>
    </table>

    <!-- 작업 등록 이력 테이블 -->
    <table v-else-if="type === 'template'" class="history-table">
      <thead>
        <tr>
          <th class="w-[400px]">작업내용</th>
          <th class="w-[100px]">등록자</th>
          <th class="w-[160px]">등록시간</th>
          <th class="w-[160px]">수정시간</th>
          <th class="w-[80px]">상태</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="template in templateHistories" :key="template.templateId">
          <td class="font-medium text-gray-900">
            <div class="truncate max-w-[400px]" :title="template.content">
              {{ template.content }}
            </div>
          </td>
          <td>{{ template.createdByName || '-' }}</td>
          <td>{{ formatDateTime(template.createdAt) }}</td>
          <td>{{ formatDateTime(template.updatedAt) }}</td>
          <td>
            <Badge :variant="getStatusBadge(template.status).variant" size="sm">
              {{ getStatusBadge(template.status).label }}
            </Badge>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 관리 이력 테이블 -->
    <table v-else-if="type === 'management'" class="history-table">
      <thead>
        <tr>
          <th class="w-[100px]">대상유형</th>
          <th class="w-[80px]">액션</th>
          <th class="w-[200px]">대상</th>
          <th class="w-[250px]">내용</th>
          <th class="w-[100px]">수행자</th>
          <th class="w-[120px]">관련사용자</th>
          <th class="w-[160px]">일시</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="log in managementHistories" :key="log.logId">
          <td>
            <span class="text-xs px-2 py-1 bg-gray-100 rounded text-gray-600">
              {{ getTargetTypeLabel(log.targetType) }}
            </span>
          </td>
          <td>
            <Badge :variant="getActionBadge(log.action).variant" size="sm">
              {{ getActionBadge(log.action).label }}
            </Badge>
          </td>
          <td class="font-medium text-gray-900">
            <div class="truncate max-w-[200px]" :title="log.targetName || String(log.targetId)">
              {{ log.targetName || `#${log.targetId}` }}
            </div>
          </td>
          <td>
            <div class="truncate max-w-[250px] text-gray-600" :title="log.description || ''">
              {{ log.description || '-' }}
            </div>
          </td>
          <td>{{ log.actorName || '-' }}</td>
          <td>{{ log.relatedUserName || '-' }}</td>
          <td>{{ formatDateTime(log.createdAt) }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.history-table-wrapper {
  @apply overflow-x-auto;
}

.history-table {
  @apply w-full text-sm;
}

.history-table thead {
  @apply bg-gray-50 border-y border-gray-200;
}

.history-table th {
  @apply px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider;
}

.history-table tbody {
  @apply divide-y divide-gray-100;
}

.history-table td {
  @apply px-4 py-3 text-gray-600 whitespace-nowrap;
}

.history-table tbody tr {
  @apply hover:bg-gray-50 transition-colors;
}
</style>
