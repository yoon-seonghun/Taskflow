<script setup lang="ts">
/**
 * 캘린더 셀 아이템 컴포넌트
 * - Todo/Item을 구분하여 표시
 * - 호버 시 툴팁 표시
 * - 더블클릭 시 상세 패널 열기
 */
import { computed } from 'vue'
import type { CalendarEvent } from '@/types/calendar'

interface Props {
  event: CalendarEvent
  showTooltip?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showTooltip: true
})

const emit = defineEmits<{
  (e: 'dblclick', event: CalendarEvent): void
}>()

// Todo 배경색 (teal/cyan 계열)
const todoColors: Record<string, string> = {
  URGENT: 'bg-rose-400',
  HIGH: 'bg-amber-400',
  NORMAL: 'bg-teal-500',
  LOW: 'bg-slate-400'
}

// Item 배경색 (blue/indigo 계열)
const itemColors: Record<string, string> = {
  URGENT: 'bg-red-500',
  HIGH: 'bg-orange-500',
  NORMAL: 'bg-blue-500',
  LOW: 'bg-gray-400'
}

// Event 배경색 (purple 계열)
const eventColors: Record<string, string> = {
  URGENT: 'bg-pink-500',
  HIGH: 'bg-fuchsia-500',
  NORMAL: 'bg-purple-500',
  LOW: 'bg-violet-400'
}

// 우선순위 라벨
const priorityLabels: Record<string, string> = {
  URGENT: '긴급',
  HIGH: '높음',
  NORMAL: '보통',
  LOW: '낮음'
}

// 상태 라벨
const statusLabels: Record<string, string> = {
  NOT_STARTED: '시작전',
  IN_PROGRESS: '진행중',
  PENDING: '보류',
  COMPLETED: '완료',
  DELETED: '삭제됨'
}

// 배지 색상
const dotColor = computed(() => {
  if (props.event.type === 'TODO') {
    return todoColors[props.event.priority] || 'bg-teal-500'
  }
  if (props.event.type === 'EVENT') {
    return eventColors[props.event.priority] || 'bg-purple-500'
  }
  return itemColors[props.event.priority] || 'bg-blue-500'
})

// 호버 배경색
const hoverBgClass = computed(() => {
  if (props.event.type === 'TODO') {
    return 'hover:bg-teal-100 dark:hover:bg-teal-900/30'
  }
  if (props.event.type === 'EVENT') {
    return 'hover:bg-purple-100 dark:hover:bg-purple-900/30'
  }
  return 'hover:bg-blue-100 dark:hover:bg-blue-900/30'
})

// 텍스트 색상
const textColorClass = computed(() => {
  if (props.event.type === 'TODO') {
    return 'text-teal-700 dark:text-teal-300'
  }
  if (props.event.type === 'EVENT') {
    return 'text-purple-700 dark:text-purple-300'
  }
  return 'text-blue-700 dark:text-blue-300'
})

// 타입 라벨
const typeLabel = computed(() => {
  if (props.event.type === 'TODO') return 'Todo'
  if (props.event.type === 'EVENT') return '이벤트'
  return '업무'
})

// 완료 여부 표시
const isCompleted = computed(() => {
  if (props.event.type === 'TODO') {
    return props.event.isCompleted
  }
  return props.event.status === 'COMPLETED'
})

// 반복 이벤트 여부
const isRecurring = computed(() => props.event.isRecurring === true)

// 반복 테두리 스타일
const recurringBorderClass = computed(() => {
  if (!isRecurring.value) return ''
  if (props.event.type === 'TODO') {
    return 'border border-dashed border-teal-400 dark:border-teal-500'
  }
  if (props.event.type === 'EVENT') {
    return 'border border-dashed border-purple-400 dark:border-purple-500'
  }
  return 'border border-dashed border-blue-400 dark:border-blue-500'
})

// 반복 타입 라벨
const recurrenceLabel = computed(() => {
  if (!props.event.recurrenceType) return ''
  const labels: Record<string, string> = {
    DAILY: '매일',
    WEEKLY: '매주',
    MONTHLY: '매월',
    YEARLY: '매년',
    WEEKLY_LUNAR: '매주 (음력)',
    MONTHLY_LUNAR: '매월 (음력)',
    YEARLY_LUNAR: '매년 (음력)'
  }
  return labels[props.event.recurrenceType] || props.event.recurrenceType
})

function handleDoubleClick() {
  emit('dblclick', props.event)
}
</script>

<template>
  <div class="calendar-cell-item group relative">
    <!-- 이벤트 아이템 -->
    <div
      class="flex items-center gap-1 px-1 py-0.5 rounded text-xs truncate cursor-pointer transition-colors"
      :class="[hoverBgClass, recurringBorderClass]"
      @click.stop
      @dblclick="handleDoubleClick"
    >
      <!-- 타입 아이콘 (Todo: 원형, Item: 사각형, Event: 다이아몬드) -->
      <span
        :class="[
          'flex-shrink-0',
          dotColor,
          event.type === 'TODO'
            ? 'w-2 h-2 rounded-full'
            : event.type === 'EVENT'
              ? 'w-2 h-2 rotate-45'
              : 'w-2 h-2 rounded-sm'
        ]"
      />
      <!-- 제목 -->
      <span
        class="truncate"
        :class="[textColorClass, { 'line-through opacity-60': isCompleted }]"
      >
        {{ event.title }}
      </span>
    </div>

    <!-- 툴팁 (hover 시 표시) -->
    <div
      v-if="showTooltip"
      class="absolute z-50 invisible opacity-0 group-hover:visible group-hover:opacity-100
             transition-all duration-200 delay-300
             bg-white rounded-lg shadow-lg border border-gray-200
             min-w-[200px] max-w-[280px] p-2.5
             dark:bg-gray-800 dark:border-gray-700
             bottom-full left-0 mb-1"
    >
      <!-- 타입 배지 -->
      <div class="flex items-center gap-2 mb-2">
        <span
          :class="[
            'inline-flex items-center px-1.5 py-0.5 text-[10px] font-medium rounded',
            event.type === 'TODO'
              ? 'bg-teal-100 text-teal-700 dark:bg-teal-900/50 dark:text-teal-300'
              : event.type === 'EVENT'
                ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/50 dark:text-purple-300'
                : 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300'
          ]"
        >
          {{ typeLabel }}
        </span>
        <!-- 우선순위 -->
        <span
          :class="[
            'inline-flex items-center px-1.5 py-0.5 text-[10px] font-medium rounded',
            {
              'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300': event.priority === 'URGENT',
              'bg-orange-100 text-orange-700 dark:bg-orange-900/50 dark:text-orange-300': event.priority === 'HIGH',
              'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300': event.priority === 'NORMAL',
              'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-400': event.priority === 'LOW'
            }
          ]"
        >
          {{ priorityLabels[event.priority] || '보통' }}
        </span>
      </div>

      <!-- 제목 -->
      <p class="text-sm font-medium text-gray-900 dark:text-gray-100 mb-1.5 line-clamp-2">
        {{ event.title }}
      </p>

      <!-- 상태/완료 여부 -->
      <div v-if="event.type === 'ITEM' && event.status" class="flex items-center gap-1.5 text-[11px] text-gray-500 dark:text-gray-400 mb-1">
        <span>상태:</span>
        <span
          :class="[
            'px-1.5 py-0.5 rounded',
            {
              'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300': event.status === 'NOT_STARTED',
              'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400': event.status === 'IN_PROGRESS',
              'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/50 dark:text-yellow-400': event.status === 'PENDING',
              'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400': event.status === 'COMPLETED',
              'bg-red-100 text-red-600 dark:bg-red-900/50 dark:text-red-400': event.status === 'DELETED'
            }
          ]"
        >
          {{ statusLabels[event.status] || event.status }}
        </span>
      </div>

      <!-- Todo 완료 여부 -->
      <div v-if="event.type === 'TODO'" class="flex items-center gap-1.5 text-[11px] text-gray-500 dark:text-gray-400 mb-1">
        <span>완료:</span>
        <span :class="event.isCompleted ? 'text-green-600 dark:text-green-400' : 'text-gray-500'">
          {{ event.isCompleted ? '완료됨' : '미완료' }}
        </span>
      </div>

      <!-- 보드 정보 (Item인 경우) -->
      <div v-if="event.type === 'ITEM' && event.boardName" class="flex items-center gap-1.5 text-[11px] text-gray-500 dark:text-gray-400">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
        </svg>
        <span class="truncate">{{ event.boardName }}</span>
      </div>

      <!-- 그룹 정보 -->
      <div v-if="event.groupName" class="flex items-center gap-1.5 text-[11px] text-gray-500 dark:text-gray-400">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
        <span
          class="px-1.5 py-0.5 rounded text-[10px]"
          :style="{ backgroundColor: event.groupColor ? event.groupColor + '20' : '#6366f120', color: event.groupColor || '#6366f1' }"
        >
          {{ event.groupName }}
        </span>
      </div>

      <!-- 그룹 공유 표시 (본인 소유가 아닌 경우) -->
      <div v-if="event.isGroupShared && event.ownerUserName" class="flex items-center gap-1.5 text-[11px] text-amber-600 dark:text-amber-400">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
        <span>소유자: {{ event.ownerUserName }}</span>
      </div>

      <!-- 하위 업무 표시 (depth > 0인 경우) -->
      <div v-if="event.itemDepth && event.itemDepth > 0" class="flex items-center gap-1 mt-1.5 text-[10px] text-amber-600 dark:text-amber-400">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3" />
        </svg>
        <span>하위 업무 (깊이: {{ event.itemDepth }})</span>
      </div>

      <!-- 반복 이벤트 표시 -->
      <div v-if="isRecurring" class="flex items-center gap-1 mt-1.5 text-[10px] text-indigo-600 dark:text-indigo-400">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        <span>반복: {{ recurrenceLabel }}</span>
      </div>

      <!-- 안내 메시지 -->
      <p class="text-[10px] text-gray-400 dark:text-gray-500 mt-2 border-t border-gray-100 dark:border-gray-700 pt-1.5">
        더블클릭하여 상세 보기
      </p>

      <!-- 툴팁 화살표 -->
      <div class="absolute w-2 h-2 bg-white border border-gray-200 transform rotate-45 dark:bg-gray-800 dark:border-gray-700
                  top-full left-4 -mt-1 border-t-0 border-l-0" />
    </div>
  </div>
</template>
