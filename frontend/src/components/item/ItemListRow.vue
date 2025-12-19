<script setup lang="ts">
/**
 * 리스트 항목 컴포넌트
 * - 단순 목록 형태
 * - 주요 정보만 표시
 * - 모바일 최적화
 * Compact UI: height 48px (모바일 터치 영역 고려)
 */
import { computed } from 'vue'
import { isItemOverdue, getOverdueDays } from '@/utils/item'
import type { Item, ItemStatus, Priority } from '@/types/item'

interface Props {
  item: Item
  selected?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selected: false
})

const emit = defineEmits<{
  (e: 'click', item: Item): void
  (e: 'complete', itemId: number): void
  (e: 'delete', itemId: number): void
}>()

// 상태 설정
const statusConfig: Record<ItemStatus, { label: string; color: string; bg: string }> = {
  NOT_STARTED: { label: '시작전', color: 'text-gray-600', bg: 'bg-gray-100' },
  IN_PROGRESS: { label: '진행중', color: 'text-blue-600', bg: 'bg-blue-100' },
  PENDING: { label: '대기', color: 'text-yellow-700', bg: 'bg-yellow-100' },
  COMPLETED: { label: '완료', color: 'text-green-600', bg: 'bg-green-100' },
  DELETED: { label: '삭제', color: 'text-red-600', bg: 'bg-red-100' }
}

// 지연 여부
const isOverdue = computed(() => isItemOverdue(props.item))
const overdueDays = computed(() => getOverdueDays(props.item))

// 우선순위 설정
const priorityConfig: Record<Priority, { color: string }> = {
  URGENT: { color: 'border-l-red-500' },
  HIGH: { color: 'border-l-orange-500' },
  NORMAL: { color: 'border-l-blue-500' },
  LOW: { color: 'border-l-gray-400' }
}

// 완료/삭제 여부
const isInactive = computed(() =>
  props.item.status === 'COMPLETED' || props.item.status === 'DELETED'
)

// 행 클래스
const rowClasses = computed(() => [
  'flex items-center px-3 py-2.5 bg-white border-b border-gray-100',
  'hover:bg-gray-50 active:bg-gray-100 transition-colors cursor-pointer',
  'border-l-4',
  priorityConfig[props.item.priority].color,
  props.selected ? 'bg-primary-50 border-primary-500' : '',
  isInactive.value ? 'opacity-60' : ''
])

// 날짜 포맷
function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diffTime = date.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays === 0) return '오늘'
  if (diffDays === 1) return '내일'
  if (diffDays === -1) return '어제'
  if (diffDays > 0 && diffDays <= 7) return `${diffDays}일 후`
  if (diffDays < 0 && diffDays >= -7) return `${Math.abs(diffDays)}일 전`

  return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

// 클릭 핸들러
function handleClick() {
  emit('click', props.item)
}

// 완료 처리
function handleComplete(event: Event) {
  event.stopPropagation()
  emit('complete', props.item.itemId)
}

// 삭제 처리
function handleDelete(event: Event) {
  event.stopPropagation()
  emit('delete', props.item.itemId)
}
</script>

<template>
  <div :class="rowClasses" @click="handleClick">
    <!-- 체크박스 (완료 버튼) -->
    <button
      v-if="!isInactive"
      class="flex-shrink-0 w-5 h-5 mr-3 rounded border-2 border-gray-300 hover:border-green-500 hover:bg-green-50 transition-colors flex items-center justify-center"
      @click="handleComplete"
    >
      <svg class="w-3 h-3 text-transparent hover:text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
      </svg>
    </button>
    <div
      v-else
      class="flex-shrink-0 w-5 h-5 mr-3 rounded bg-gray-200 flex items-center justify-center"
    >
      <svg class="w-3 h-3 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
      </svg>
    </div>

    <!-- 메인 컨텐츠 -->
    <div class="flex-1 min-w-0">
      <!-- 제목 -->
      <div class="flex items-center gap-2">
        <h4
          class="text-[14px] text-gray-900 truncate"
          :class="{ 'line-through': isInactive }"
        >
          {{ item.title }}
        </h4>
        <!-- 지연 표시 -->
        <span
          v-if="isOverdue"
          class="flex-shrink-0 inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-medium text-red-700 bg-red-100 rounded"
        >
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          {{ overdueDays }}일 지연
        </span>
      </div>

      <!-- 메타 정보 -->
      <div class="flex items-center gap-3 mt-0.5">
        <!-- 상태 -->
        <span
          :class="[
            'text-[11px] px-1.5 py-0.5 rounded',
            statusConfig[item.status].bg,
            statusConfig[item.status].color
          ]"
        >
          {{ statusConfig[item.status].label }}
        </span>

        <!-- 담당자 -->
        <span v-if="item.assigneeName" class="text-[12px] text-gray-500 truncate max-w-[100px]">
          {{ item.assigneeName }}
        </span>

        <!-- 마감일 -->
        <span v-if="item.dueDate" class="text-[12px] text-gray-400">
          {{ formatDate(item.dueDate) }}
        </span>

        <!-- 댓글 -->
        <span v-if="item.commentCount > 0" class="text-[12px] text-gray-400 flex items-center gap-0.5">
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          {{ item.commentCount }}
        </span>
      </div>
    </div>

    <!-- 우측 액션 -->
    <div class="flex-shrink-0 flex items-center gap-1 ml-2">
      <!-- 그룹 태그 -->
      <span
        v-if="item.groupName"
        class="hidden sm:inline-block text-[11px] px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 max-w-[80px] truncate"
        :style="item.groupColor ? { backgroundColor: `${item.groupColor}20`, color: item.groupColor } : {}"
      >
        {{ item.groupName }}
      </span>

      <!-- 화살표 아이콘 (모바일 터치 힌트) -->
      <svg class="w-5 h-5 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
      </svg>
    </div>
  </div>
</template>
