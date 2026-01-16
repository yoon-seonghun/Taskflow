<script setup lang="ts">
/**
 * 하위 업무 카드 컴포넌트 (v2.2)
 * - 칸반 뷰에서 하위 업무 카드 표시
 * - 컴팩트 모드 (부모 카드 내 인라인) / 일반 모드 (독립 카드)
 */
import { computed } from 'vue'
import type { Item, ItemStatus } from '@/types/item'
import ChildProgressBar from './ChildProgressBar.vue'

interface Props {
  item: Item
  depth?: number              // 업무 깊이
  compact?: boolean           // 컴팩트 모드 (부모 카드 내부 표시)
  showParentInfo?: boolean    // 부모 정보 표시
  draggable?: boolean         // 드래그 가능 여부
}

const props = withDefaults(defineProps<Props>(), {
  depth: 1,
  compact: false,
  showParentInfo: false,
  draggable: true
})

const emit = defineEmits<{
  (e: 'click', item: Item): void
  (e: 'complete', itemId: number): void
}>()

// 깊이별 스타일
const depthStyles = computed(() => {
  switch (props.depth) {
    case 1:
      return {
        margin: 'ml-4',
        border: 'border-l-[3px] border-blue-400',
        bg: 'bg-blue-50/50'
      }
    case 2:
      return {
        margin: 'ml-8',
        border: 'border-l-[3px] border-purple-400',
        bg: 'bg-purple-50/50'
      }
    default:
      return {
        margin: '',
        border: '',
        bg: 'bg-white'
      }
  }
})

// 완료 여부
const isCompleted = computed(() => props.item.status === 'COMPLETED')

// 상태 색상
const statusDotColor = computed(() => {
  const colors: Record<ItemStatus, string> = {
    NOT_STARTED: 'bg-gray-400',
    IN_PROGRESS: 'bg-blue-500',
    PENDING: 'bg-yellow-500',
    COMPLETED: 'bg-green-500',
    DELETED: 'bg-red-400'
  }
  return colors[props.item.status]
})

// 날짜 포맷
function formatDate(date: string | null | undefined): string {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

// 핸들러
function handleClick() {
  emit('click', props.item)
}

function handleComplete(e: Event) {
  e.stopPropagation()
  emit('complete', props.item.itemId)
}
</script>

<template>
  <!-- 컴팩트 모드: 부모 카드 내부에 축약 표시 -->
  <div
    v-if="compact"
    class="sub-task-card-compact flex items-center gap-2 py-1 px-2 rounded
           bg-gray-50 hover:bg-gray-100 cursor-pointer transition-colors"
    @click="handleClick"
  >
    <!-- 깊이 표시 -->
    <div
      class="w-1 h-4 rounded-full flex-shrink-0"
      :class="depth === 1 ? 'bg-blue-400' : 'bg-purple-400'"
    />

    <!-- 체크박스 -->
    <button
      class="flex-shrink-0 w-3.5 h-3.5 border rounded-sm transition-colors"
      :class="isCompleted
        ? 'bg-green-500 border-green-500 text-white'
        : 'border-gray-300 hover:border-green-500'"
      @click="handleComplete"
    >
      <svg v-if="isCompleted" class="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
      </svg>
    </button>

    <!-- 제목 -->
    <span
      class="flex-1 text-[12px] truncate"
      :class="isCompleted ? 'line-through text-gray-400' : 'text-gray-700'"
    >
      {{ item.title || item.content }}
    </span>

    <!-- 상태 점 -->
    <span
      v-if="!isCompleted"
      class="w-2 h-2 rounded-full flex-shrink-0"
      :class="statusDotColor"
    />
  </div>

  <!-- 일반 모드: 독립 카드 -->
  <div
    v-else
    class="sub-task-card rounded-lg shadow-sm border border-gray-200
           hover:shadow-md cursor-pointer transition-all"
    :class="[depthStyles.margin, depthStyles.border, depthStyles.bg]"
    :draggable="draggable"
    @click="handleClick"
  >
    <!-- 카드 헤더 -->
    <div class="flex items-start gap-2 p-3">
      <!-- 하위 업무 아이콘 -->
      <svg
        class="w-4 h-4 mt-0.5 flex-shrink-0"
        :class="depth === 1 ? 'text-blue-500' : 'text-purple-500'"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M9 5l7 7-7 7"
        />
      </svg>

      <!-- 제목 -->
      <span
        class="flex-1 text-[13px] leading-snug"
        :class="isCompleted ? 'line-through text-gray-400' : 'text-gray-800'"
      >
        {{ item.title || item.content }}
      </span>

      <!-- 상태 점 -->
      <span
        class="w-2.5 h-2.5 rounded-full flex-shrink-0 mt-1"
        :class="statusDotColor"
        :title="item.status"
      />
    </div>

    <!-- 카드 바디 -->
    <div class="flex items-center justify-between gap-2 px-3 pb-2">
      <!-- 담당자 -->
      <div v-if="item.assigneeName" class="flex items-center gap-1">
        <div class="w-5 h-5 rounded-full bg-gray-200 flex items-center justify-center">
          <span class="text-[10px] text-gray-600 font-medium">
            {{ item.assigneeName?.charAt(0) }}
          </span>
        </div>
        <span class="text-[11px] text-gray-500">{{ item.assigneeName }}</span>
      </div>

      <div class="flex-1" />

      <!-- 마감일 -->
      <span v-if="item.dueDate" class="text-[11px] text-gray-400">
        {{ formatDate(item.dueDate) }}
      </span>
    </div>

    <!-- 부모 정보 (배당받은 업무) -->
    <div
      v-if="showParentInfo && item.parentInfo"
      class="flex items-center gap-1 px-3 pb-2 text-[11px] text-gray-500"
    >
      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18" />
      </svg>
      <span class="truncate">{{ item.parentInfo.title }}</span>
    </div>

    <!-- 하위 업무 진행률 -->
    <div
      v-if="(item.childCount ?? 0) > 0"
      class="px-3 pb-2"
    >
      <ChildProgressBar
        :child-count="item.childCount ?? 0"
        :completed-child-count="item.completedChildCount ?? 0"
        size="xs"
        :show-count="true"
      />
    </div>
  </div>
</template>
