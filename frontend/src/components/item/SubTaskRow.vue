<script setup lang="ts">
/**
 * 하위 업무 행 컴포넌트 (v2.2)
 * - 테이블/리스트 뷰에서 하위 업무 행 표시
 * - 깊이별 indent 및 계층 표시 라인
 */
import { computed } from 'vue'
import type { Item, ItemStatus } from '@/types/item'
import ChildProgressBar from './ChildProgressBar.vue'
import ParentInfoTooltip from './ParentInfoTooltip.vue'

interface Props {
  item: Item
  depth?: number                 // 업무 깊이 (indent 계산용)
  showParentInfo?: boolean       // 부모 정보 툴팁 표시
  showProgress?: boolean         // 하위 업무 진행률 표시
  draggable?: boolean            // 드래그 가능 여부
  readonly?: boolean             // 읽기 전용
}

const props = withDefaults(defineProps<Props>(), {
  depth: 1,
  showParentInfo: false,
  showProgress: true,
  draggable: true,
  readonly: false
})

const emit = defineEmits<{
  (e: 'click', item: Item): void
  (e: 'complete', itemId: number): void
  (e: 'delete', itemId: number): void
  (e: 'edit', item: Item): void
  (e: 'createChild', parentId: number): void
}>()

// 깊이별 indent (px)
const indentStyle = computed(() => ({
  paddingLeft: `${props.depth * 24}px`
}))

// 깊이별 왼쪽 테두리 색상
const depthBorderColor = computed(() => {
  switch (props.depth) {
    case 1: return 'border-blue-400'
    case 2: return 'border-purple-400'
    default: return 'border-gray-300'
  }
})

// 완료 여부
const isCompleted = computed(() => props.item.status === 'COMPLETED')

// 하위 업무 생성 가능 여부
const canCreateChild = computed(() => {
  return (props.item.itemDepth ?? props.depth) < 2 && !props.readonly
})

// 상태별 스타일
const statusStyles: Record<ItemStatus, { dot: string; text: string }> = {
  NOT_STARTED: { dot: 'bg-gray-400', text: 'text-gray-500' },
  IN_PROGRESS: { dot: 'bg-blue-500', text: 'text-blue-600' },
  PENDING: { dot: 'bg-yellow-500', text: 'text-yellow-600' },
  COMPLETED: { dot: 'bg-green-500', text: 'text-green-600' },
  DELETED: { dot: 'bg-red-400', text: 'text-red-500' }
}

// 상태 라벨
const statusLabels: Record<ItemStatus, string> = {
  NOT_STARTED: '시작전',
  IN_PROGRESS: '진행중',
  PENDING: '보류',
  COMPLETED: '완료',
  DELETED: '삭제됨'
}

// 날짜 포맷
function formatDate(date: string | null | undefined): string {
  if (!date) return '-'
  const d = new Date(date)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

// 핸들러
function handleClick() {
  emit('click', props.item)
}

function handleComplete(e: Event) {
  e.stopPropagation()
  if (!props.readonly) {
    emit('complete', props.item.itemId)
  }
}

function handleDelete(e: Event) {
  e.stopPropagation()
  if (!props.readonly) {
    emit('delete', props.item.itemId)
  }
}

function handleCreateChild(e: Event) {
  e.stopPropagation()
  if (canCreateChild.value) {
    emit('createChild', props.item.itemId)
  }
}
</script>

<template>
  <div
    class="sub-task-row group flex items-center h-9 border-b border-gray-100
           hover:bg-gray-50 cursor-pointer transition-colors"
    :class="[
      depthBorderColor,
      { 'border-l-3': depth > 0 }
    ]"
    :style="indentStyle"
    :draggable="draggable && !readonly"
    @click="handleClick"
  >
    <!-- 계층 연결 라인 -->
    <div class="flex items-center h-full mr-1">
      <!-- 세로선 + 꺾인선 -->
      <div class="relative w-4 h-full">
        <div
          class="absolute left-1/2 top-0 bottom-1/2 w-px bg-gray-300"
          :class="{ 'bg-blue-300': depth === 1, 'bg-purple-300': depth === 2 }"
        />
        <div
          class="absolute left-1/2 top-1/2 w-2 h-px bg-gray-300"
          :class="{ 'bg-blue-300': depth === 1, 'bg-purple-300': depth === 2 }"
        />
      </div>

      <!-- 하위 업무 아이콘 -->
      <svg class="w-3.5 h-3.5 text-gray-400 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
      </svg>
    </div>

    <!-- 완료 체크박스 -->
    <button
      class="flex-shrink-0 w-4 h-4 mr-2 border rounded transition-colors"
      :class="isCompleted
        ? 'bg-green-500 border-green-500 text-white'
        : 'border-gray-300 hover:border-green-500'"
      :disabled="readonly"
      @click="handleComplete"
    >
      <svg v-if="isCompleted" class="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
      </svg>
    </button>

    <!-- 제목 -->
    <span
      class="flex-1 text-[13px] truncate mr-2"
      :class="isCompleted ? 'line-through text-gray-400' : 'text-gray-700'"
    >
      {{ item.title || item.content }}
    </span>

    <!-- 부모 정보 툴팁 (배당받은 업무용) -->
    <ParentInfoTooltip
      v-if="showParentInfo && item.parentInfo"
      :parent="item.parentInfo"
      :root="item.rootInfo"
      :description="item.description"
      class="mr-2"
    />

    <!-- 담당자 -->
    <span
      v-if="item.assigneeName"
      class="flex-shrink-0 text-[11px] text-gray-500 mr-2"
    >
      {{ item.assigneeName }}
    </span>

    <!-- 상태 배지 -->
    <span
      v-if="item.status !== 'COMPLETED'"
      class="flex-shrink-0 inline-flex items-center gap-1 px-1.5 py-0.5 text-[10px] rounded"
      :class="statusStyles[item.status].text"
    >
      <span
        class="w-1.5 h-1.5 rounded-full"
        :class="statusStyles[item.status].dot"
      />
      {{ statusLabels[item.status] }}
    </span>

    <!-- 마감일 -->
    <span class="flex-shrink-0 text-[11px] text-gray-400 mr-2 w-10 text-right">
      {{ formatDate(item.dueDate) }}
    </span>

    <!-- 하위 업무 진행률 -->
    <ChildProgressBar
      v-if="showProgress && (item.childCount ?? 0) > 0"
      :child-count="item.childCount ?? 0"
      :completed-child-count="item.completedChildCount ?? 0"
      size="xs"
      class="mr-2"
    />

    <!-- 액션 버튼 (hover 시) -->
    <div class="flex-shrink-0 flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
      <!-- 하위 업무 추가 -->
      <button
        v-if="canCreateChild"
        class="p-1 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded transition-colors"
        title="하위 업무 추가"
        @click="handleCreateChild"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
      </button>

      <!-- 삭제 -->
      <button
        v-if="!readonly"
        class="p-1 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded transition-colors"
        title="삭제"
        @click="handleDelete"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.border-l-3 {
  border-left-width: 3px;
}
</style>
