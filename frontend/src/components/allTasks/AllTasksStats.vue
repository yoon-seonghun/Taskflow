<script setup lang="ts">
/**
 * 전체 업무 통계 컴포넌트
 * - 출처별/상태별/우선순위별 통계 표시
 * - 클릭 시 해당 필터로 전환
 */
import { computed } from 'vue'
import type { AllItemsStats, SourceType } from '@/types/allTasks'
import { sourceTypeLabels, sourceTypeColors } from '@/types/allTasks'

interface Props {
  stats: AllItemsStats | null
  /** 현재 선택된 출처 타입 */
  selectedSourceType?: SourceType
  /** 로딩 상태 */
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selectedSourceType: 'ALL',
  loading: false
})

const emit = defineEmits<{
  (e: 'select-source', sourceType: SourceType): void
}>()

// 출처별 통계 카드
const sourceStats = computed(() => {
  if (!props.stats) return []

  return [
    {
      sourceType: 'ALL' as SourceType,
      label: '전체',
      count: props.stats.totalCount,
      icon: 'all'
    },
    {
      sourceType: 'OWNED' as SourceType,
      label: sourceTypeLabels.OWNED,
      count: props.stats.ownedCount,
      icon: 'owned'
    },
    {
      sourceType: 'BOARD_SHARED' as SourceType,
      label: sourceTypeLabels.BOARD_SHARED,
      count: props.stats.boardSharedCount,
      icon: 'board_shared'
    },
    {
      sourceType: 'ITEM_SHARED' as SourceType,
      label: sourceTypeLabels.ITEM_SHARED,
      count: props.stats.itemSharedCount,
      icon: 'item_shared'
    },
    {
      sourceType: 'ASSIGNED' as SourceType,
      label: sourceTypeLabels.ASSIGNED,
      count: props.stats.assignedCount,
      icon: 'assigned'
    }
  ]
})

// 상태별 통계
const statusStats = computed(() => {
  if (!props.stats?.byStatus) return []

  const statusLabels: Record<string, string> = {
    NOT_STARTED: '시작전',
    IN_PROGRESS: '진행중',
    PENDING: '대기중',
    COMPLETED: '완료',
    DELETED: '삭제'
  }

  const statusColors: Record<string, string> = {
    NOT_STARTED: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
    IN_PROGRESS: 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400',
    PENDING: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/50 dark:text-yellow-400',
    COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400',
    DELETED: 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-400'
  }

  return Object.entries(props.stats.byStatus).map(([status, count]) => ({
    status,
    label: statusLabels[status] || status,
    count,
    colorClass: statusColors[status] || 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
  }))
})

// 우선순위별 통계
const priorityStats = computed(() => {
  if (!props.stats?.byPriority) return []

  const priorityLabels: Record<string, string> = {
    URGENT: '긴급',
    HIGH: '높음',
    MEDIUM: '보통',
    LOW: '낮음'
  }

  const priorityColors: Record<string, string> = {
    URGENT: 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-400',
    HIGH: 'bg-orange-100 text-orange-700 dark:bg-orange-900/50 dark:text-orange-400',
    MEDIUM: 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400',
    LOW: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
  }

  return Object.entries(props.stats.byPriority).map(([priority, count]) => ({
    priority,
    label: priorityLabels[priority] || priority,
    count,
    colorClass: priorityColors[priority] || 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
  }))
})

// 카드 색상 클래스 가져오기
function getCardColorClass(sourceType: SourceType, isSelected: boolean): string {
  if (isSelected) {
    const colors = sourceTypeColors[sourceType]
    return `${colors.bg} ${colors.darkBg} ring-2 ring-offset-2 ring-gray-400 dark:ring-gray-500`
  }
  return 'bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-750'
}

// 출처 선택
function selectSource(sourceType: SourceType): void {
  emit('select-source', sourceType)
}
</script>

<template>
  <div class="space-y-4">
    <!-- 출처별 통계 카드 -->
    <div class="flex flex-wrap gap-2">
      <button
        v-for="stat in sourceStats"
        :key="stat.sourceType"
        type="button"
        :class="[
          'flex items-center gap-2 px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-700 transition-all cursor-pointer',
          getCardColorClass(stat.sourceType, selectedSourceType === stat.sourceType)
        ]"
        @click="selectSource(stat.sourceType)"
      >
        <!-- 아이콘 -->
        <span class="flex-shrink-0">
          <!-- 전체 -->
          <svg v-if="stat.icon === 'all'" class="w-4 h-4 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <!-- 내 보드 -->
          <svg v-else-if="stat.icon === 'owned'" class="w-4 h-4 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          <!-- 공유 보드 -->
          <svg v-else-if="stat.icon === 'board_shared'" class="w-4 h-4 text-blue-500 dark:text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          <!-- 공유 업무 -->
          <svg v-else-if="stat.icon === 'item_shared'" class="w-4 h-4 text-purple-500 dark:text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
          </svg>
          <!-- 배당 업무 -->
          <svg v-else-if="stat.icon === 'assigned'" class="w-4 h-4 text-green-500 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
          </svg>
        </span>

        <!-- 라벨과 카운트 -->
        <span class="text-sm font-medium text-gray-700 dark:text-gray-300">
          {{ stat.label }}
        </span>
        <span class="text-sm font-bold text-gray-900 dark:text-white">
          {{ loading ? '-' : stat.count }}
        </span>
      </button>
    </div>

    <!-- 상태별/우선순위별 요약 (접이식) -->
    <div class="flex flex-wrap gap-4 text-xs">
      <!-- 상태별 -->
      <div v-if="statusStats.length > 0" class="flex items-center gap-1">
        <span class="text-gray-500 dark:text-gray-400 mr-1">상태:</span>
        <span
          v-for="stat in statusStats"
          :key="stat.status"
          :class="['px-1.5 py-0.5 rounded font-medium', stat.colorClass]"
        >
          {{ stat.label }} {{ stat.count }}
        </span>
      </div>

      <!-- 우선순위별 -->
      <div v-if="priorityStats.length > 0" class="flex items-center gap-1">
        <span class="text-gray-500 dark:text-gray-400 mr-1">우선순위:</span>
        <span
          v-for="stat in priorityStats"
          :key="stat.priority"
          :class="['px-1.5 py-0.5 rounded font-medium', stat.colorClass]"
        >
          {{ stat.label }} {{ stat.count }}
        </span>
      </div>
    </div>
  </div>
</template>
