<script setup lang="ts">
/**
 * 전체 업무 통계 컴포넌트
 * - 상태별 통계 카드 (메인)
 * - 출처별/우선순위별 통계 (보조)
 * - 우선순위는 현재 표시된 아이템 기준으로 계산
 */
import { computed } from 'vue'
import type { AllItemsStats, SourceType, AllItemResponse } from '@/types/allTasks'
import type { ItemStatus } from '@/types/item'
import { sourceTypeLabels } from '@/types/allTasks'

interface Props {
  stats: AllItemsStats | null
  /** 현재 표시된 아이템 목록 (우선순위 계산용) */
  items?: AllItemResponse[]
  /** 현재 선택된 출처 타입 */
  selectedSourceType?: SourceType
  /** 현재 선택된 상태 */
  selectedStatus?: ItemStatus | null
  /** 로딩 상태 */
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
  selectedSourceType: 'ALL',
  selectedStatus: null,
  loading: false
})

const emit = defineEmits<{
  (e: 'select-source', sourceType: SourceType): void
  (e: 'select-status', status: ItemStatus | null): void
}>()

// 상태별 통계 카드 (메인)
const statusCards = computed(() => {
  if (!props.stats?.byStatus) return []

  const statusConfig: Array<{
    status: ItemStatus | 'ALL'
    label: string
    icon: string
    bgClass: string
    textClass: string
    iconClass: string
  }> = [
    {
      status: 'ALL',
      label: '전체',
      icon: 'all',
      bgClass: 'bg-white dark:bg-gray-800',
      textClass: 'text-gray-700 dark:text-gray-300',
      iconClass: 'text-gray-500 dark:text-gray-400'
    },
    {
      status: 'IN_PROGRESS',
      label: '진행중',
      icon: 'progress',
      bgClass: 'bg-blue-50 dark:bg-blue-900/30',
      textClass: 'text-blue-700 dark:text-blue-400',
      iconClass: 'text-blue-500 dark:text-blue-400'
    },
    {
      status: 'NOT_STARTED',
      label: '시작전',
      icon: 'not_started',
      bgClass: 'bg-gray-50 dark:bg-gray-800',
      textClass: 'text-gray-700 dark:text-gray-300',
      iconClass: 'text-gray-500 dark:text-gray-400'
    },
    {
      status: 'PENDING',
      label: '대기중',
      icon: 'pending',
      bgClass: 'bg-yellow-50 dark:bg-yellow-900/30',
      textClass: 'text-yellow-700 dark:text-yellow-400',
      iconClass: 'text-yellow-500 dark:text-yellow-400'
    },
    {
      status: 'COMPLETED',
      label: '완료',
      icon: 'completed',
      bgClass: 'bg-green-50 dark:bg-green-900/30',
      textClass: 'text-green-700 dark:text-green-400',
      iconClass: 'text-green-500 dark:text-green-400'
    }
  ]

  return statusConfig.map(config => ({
    ...config,
    count: config.status === 'ALL'
      ? props.stats?.totalCount ?? 0
      : props.stats?.byStatus?.[config.status] ?? 0
  }))
})

// 출처별 통계 (보조 - 작은 배지)
const sourceStats = computed(() => {
  if (!props.stats) return []

  return [
    {
      sourceType: 'OWNED' as SourceType,
      label: sourceTypeLabels.OWNED,
      count: props.stats.ownedCount
    },
    {
      sourceType: 'BOARD_SHARED' as SourceType,
      label: sourceTypeLabels.BOARD_SHARED,
      count: props.stats.boardSharedCount
    },
    {
      sourceType: 'ITEM_SHARED' as SourceType,
      label: sourceTypeLabels.ITEM_SHARED,
      count: props.stats.itemSharedCount
    },
    {
      sourceType: 'ASSIGNED' as SourceType,
      label: sourceTypeLabels.ASSIGNED,
      count: props.stats.assignedCount
    }
  ]
})

// 상태별 통계 (이전 호환성)
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

// 우선순위별 통계 (현재 표시된 아이템 기준으로 계산)
const priorityStats = computed(() => {
  const priorityLabels: Record<string, string> = {
    URGENT: '긴급',
    HIGH: '높음',
    NORMAL: '보통',
    LOW: '낮음'
  }

  const priorityColors: Record<string, string> = {
    URGENT: 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-400',
    HIGH: 'bg-orange-100 text-orange-700 dark:bg-orange-900/50 dark:text-orange-400',
    NORMAL: 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400',
    LOW: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
  }

  // 현재 표시된 아이템에서 우선순위별 카운트 계산
  const priorityCounts: Record<string, number> = {}

  // 재귀적으로 모든 아이템(하위 포함) 카운트
  const countItems = (items: AllItemResponse[]) => {
    for (const item of items) {
      if (item.priority) {
        priorityCounts[item.priority] = (priorityCounts[item.priority] || 0) + 1
      }
      if (item.children && item.children.length > 0) {
        countItems(item.children)
      }
    }
  }

  countItems(props.items)

  // 우선순위 순서대로 반환
  const priorityOrder = ['URGENT', 'HIGH', 'NORMAL', 'LOW']
  return priorityOrder
    .filter(priority => priorityCounts[priority] > 0)
    .map(priority => ({
      priority,
      label: priorityLabels[priority] || priority,
      count: priorityCounts[priority],
      colorClass: priorityColors[priority] || 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
    }))
})

// 상태 카드 선택 여부
function isStatusSelected(status: ItemStatus | 'ALL'): boolean {
  if (status === 'ALL') {
    return props.selectedStatus === null
  }
  return props.selectedStatus === status
}

// 상태 선택
function selectStatus(status: ItemStatus | 'ALL'): void {
  if (status === 'ALL') {
    emit('select-status', null)
  } else {
    emit('select-status', status)
  }
}

// 출처 선택
function selectSource(sourceType: SourceType): void {
  emit('select-source', sourceType)
}
</script>

<template>
  <div class="space-y-3">
    <!-- 상태별 통계 카드 (메인) -->
    <div class="flex flex-wrap gap-2">
      <button
        v-for="card in statusCards"
        :key="card.status"
        type="button"
        :class="[
          'flex items-center gap-2 px-3 py-2 rounded-lg border transition-all cursor-pointer',
          isStatusSelected(card.status)
            ? 'ring-2 ring-offset-1 ring-blue-500 dark:ring-blue-400 border-blue-300 dark:border-blue-600'
            : 'border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-750',
          card.bgClass
        ]"
        @click="selectStatus(card.status)"
      >
        <!-- 아이콘 -->
        <span class="flex-shrink-0" :class="card.iconClass">
          <!-- 전체 -->
          <svg v-if="card.icon === 'all'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <!-- 진행중 -->
          <svg v-else-if="card.icon === 'progress'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          <!-- 시작전 -->
          <svg v-else-if="card.icon === 'not_started'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <!-- 대기중 -->
          <svg v-else-if="card.icon === 'pending'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <!-- 완료 -->
          <svg v-else-if="card.icon === 'completed'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </span>

        <!-- 라벨과 카운트 -->
        <span :class="['text-sm font-medium', card.textClass]">
          {{ card.label }}
        </span>
        <span :class="['text-sm font-bold', card.textClass]">
          {{ loading ? '-' : card.count }}
        </span>
      </button>
    </div>

    <!-- 출처별/우선순위별 요약 (보조) -->
    <div class="flex flex-wrap gap-4 text-xs">
      <!-- 출처별 -->
      <div v-if="sourceStats.length > 0" class="flex items-center gap-1">
        <span class="text-gray-500 dark:text-gray-400 mr-1">출처:</span>
        <button
          v-for="stat in sourceStats"
          :key="stat.sourceType"
          type="button"
          :class="[
            'px-1.5 py-0.5 rounded font-medium transition-colors',
            selectedSourceType === stat.sourceType
              ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400 ring-1 ring-blue-400'
              : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
          ]"
          @click="selectSource(stat.sourceType)"
        >
          {{ stat.label }} {{ stat.count }}
        </button>
        <button
          v-if="selectedSourceType !== 'ALL'"
          type="button"
          class="px-1.5 py-0.5 rounded font-medium bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-600"
          @click="selectSource('ALL')"
        >
          전체
        </button>
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
