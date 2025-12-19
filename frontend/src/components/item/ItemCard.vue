<script setup lang="ts">
/**
 * 아이템 카드 컴포넌트 (칸반용)
 * - 드래그 가능
 * - 클릭 시 상세 패널 오픈
 * - 주요 정보 표시
 * Compact UI 적용
 */
import { computed } from 'vue'
import { Badge } from '@/components/common'
import { isItemOverdue, getOverdueDays } from '@/utils/item'
import type { Item, ItemStatus, Priority } from '@/types/item'

interface Props {
  item: Item
  dragging?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  dragging: false
})

const emit = defineEmits<{
  (e: 'click', item: Item): void
  (e: 'dragstart', event: DragEvent, item: Item): void
  (e: 'dragend', event: DragEvent): void
}>()

// 우선순위 색상
const priorityColors: Record<Priority, { bg: string; text: string; border: string }> = {
  URGENT: { bg: 'bg-red-50', text: 'text-red-700', border: 'border-l-red-500' },
  HIGH: { bg: 'bg-orange-50', text: 'text-orange-700', border: 'border-l-orange-500' },
  NORMAL: { bg: 'bg-blue-50', text: 'text-blue-700', border: 'border-l-blue-500' },
  LOW: { bg: 'bg-gray-50', text: 'text-gray-600', border: 'border-l-gray-400' }
}

// 우선순위 라벨
const priorityLabels: Record<Priority, string> = {
  URGENT: '긴급',
  HIGH: '높음',
  NORMAL: '보통',
  LOW: '낮음'
}

// 상태 라벨
const statusLabels: Record<ItemStatus, string> = {
  NOT_STARTED: '시작전',
  IN_PROGRESS: '진행중',
  PENDING: '대기',
  COMPLETED: '완료',
  DELETED: '삭제'
}

// 지연 여부
const isOverdue = computed(() => isItemOverdue(props.item))
const overdueDays = computed(() => getOverdueDays(props.item))

// 카드 스타일
const cardClasses = computed(() => [
  'bg-white rounded-lg border shadow-sm cursor-grab active:cursor-grabbing',
  'hover:shadow-md transition-shadow duration-150',
  'border-l-4',
  priorityColors[props.item.priority].border,
  props.dragging ? 'opacity-50 ring-2 ring-primary-500' : ''
])

// 날짜 포맷
function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric'
  })
}

// 드래그 시작
function handleDragStart(event: DragEvent) {
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(props.item.itemId))
  }
  emit('dragstart', event, props.item)
}

// 드래그 종료
function handleDragEnd(event: DragEvent) {
  emit('dragend', event)
}

// 카드 클릭
function handleClick() {
  emit('click', props.item)
}
</script>

<template>
  <div
    :class="cardClasses"
    draggable="true"
    @dragstart="handleDragStart"
    @dragend="handleDragEnd"
    @click="handleClick"
  >
    <div class="p-3">
      <!-- 제목 -->
      <h4 class="text-[13px] font-medium text-gray-900 line-clamp-2 mb-2">
        {{ item.title }}
      </h4>

      <!-- 지연 표시 -->
      <div
        v-if="isOverdue"
        class="flex items-center gap-1 mb-2 px-1.5 py-1 bg-red-50 border border-red-200 rounded text-[11px] text-red-700"
      >
        <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        <span class="font-medium">{{ overdueDays }}일 지연</span>
      </div>

      <!-- 메타 정보 -->
      <div class="flex flex-wrap items-center gap-2 text-[11px]">
        <!-- 우선순위 -->
        <span
          :class="[
            'px-1.5 py-0.5 rounded text-[11px] font-medium',
            priorityColors[item.priority].bg,
            priorityColors[item.priority].text
          ]"
        >
          {{ priorityLabels[item.priority] }}
        </span>

        <!-- 그룹 -->
        <span
          v-if="item.groupName"
          class="px-1.5 py-0.5 rounded text-[11px] bg-gray-100 text-gray-600"
          :style="item.groupColor ? { backgroundColor: `${item.groupColor}20`, color: item.groupColor } : {}"
        >
          {{ item.groupName }}
        </span>
      </div>

      <!-- 하단 정보 -->
      <div class="flex items-center justify-between mt-3 pt-2 border-t border-gray-100">
        <!-- 담당자 -->
        <div class="flex items-center gap-1.5">
          <div class="w-5 h-5 rounded-full bg-gray-200 flex items-center justify-center">
            <svg class="w-3 h-3 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </div>
          <span class="text-[11px] text-gray-600 truncate max-w-[80px]">
            {{ item.assigneeName || '미지정' }}
          </span>
        </div>

        <!-- 우측 정보 -->
        <div class="flex items-center gap-2">
          <!-- 마감일 -->
          <span v-if="item.dueDate" class="text-[11px] text-gray-500 flex items-center gap-0.5">
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            {{ formatDate(item.dueDate) }}
          </span>

          <!-- 댓글 수 -->
          <span v-if="item.commentCount > 0" class="text-[11px] text-gray-500 flex items-center gap-0.5">
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            {{ item.commentCount }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
