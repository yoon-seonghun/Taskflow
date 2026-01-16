<script setup lang="ts">
/**
 * 부모 업무 정보 툴팁 컴포넌트 (v2.2)
 * - 하위 업무의 부모/루트 정보를 툴팁으로 표시
 * - 업무 설명과 상위 경로 표시
 */
import { computed } from 'vue'
import type { ItemStatus } from '@/types/item'

interface ParentInfo {
  itemId: number
  title: string
  status: ItemStatus
}

interface Props {
  parent?: ParentInfo | null      // 직접 부모 정보
  root?: ParentInfo | null        // 최상위 업무 정보
  description?: string            // 업무 설명
  placement?: 'top' | 'bottom' | 'left' | 'right'
}

const props = withDefaults(defineProps<Props>(), {
  placement: 'top'
})

const emit = defineEmits<{
  (e: 'navigate', item: ParentInfo): void
}>()

// 표시 여부 (부모 또는 설명이 있을 때만)
const hasContent = computed(() => {
  return props.description || props.parent || props.root
})

// 상태별 색상
const statusColors: Record<ItemStatus, { bg: string; text: string }> = {
  NOT_STARTED: { bg: 'bg-gray-100', text: 'text-gray-600' },
  IN_PROGRESS: { bg: 'bg-blue-100', text: 'text-blue-700' },
  PENDING: { bg: 'bg-yellow-100', text: 'text-yellow-700' },
  COMPLETED: { bg: 'bg-green-100', text: 'text-green-700' },
  DELETED: { bg: 'bg-red-100', text: 'text-red-600' }
}

// 상태 라벨
const statusLabels: Record<ItemStatus, string> = {
  NOT_STARTED: '시작전',
  IN_PROGRESS: '진행중',
  PENDING: '보류',
  COMPLETED: '완료',
  DELETED: '삭제됨'
}

// 부모와 루트가 다른 경우만 둘 다 표시
const showBothParentAndRoot = computed(() => {
  return props.root && props.parent && props.root.itemId !== props.parent.itemId
})

function handleNavigate(item: ParentInfo) {
  emit('navigate', item)
}
</script>

<template>
  <div v-if="hasContent" class="parent-info-tooltip group relative inline-flex">
    <!-- 트리거 아이콘 -->
    <button
      class="flex items-center justify-center w-5 h-5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded transition-colors"
      title="상위 업무 정보"
    >
      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z"
        />
      </svg>
    </button>

    <!-- 툴팁 내용 -->
    <div
      class="absolute z-50 invisible opacity-0 group-hover:visible group-hover:opacity-100
             transition-all duration-200 delay-300
             bg-white rounded-lg shadow-lg border border-gray-200
             min-w-[220px] max-w-[320px] p-3"
      :class="{
        'bottom-full left-1/2 -translate-x-1/2 mb-2': placement === 'top',
        'top-full left-1/2 -translate-x-1/2 mt-2': placement === 'bottom',
        'right-full top-1/2 -translate-y-1/2 mr-2': placement === 'left',
        'left-full top-1/2 -translate-y-1/2 ml-2': placement === 'right'
      }"
    >
      <!-- 설명 섹션 -->
      <div v-if="description" class="tooltip-section">
        <span class="block text-[10px] font-semibold text-gray-400 uppercase tracking-wide mb-1">
          설명
        </span>
        <p class="text-[13px] text-gray-700 leading-relaxed line-clamp-3">
          {{ description }}
        </p>
      </div>

      <!-- 구분선 -->
      <div
        v-if="description && (parent || root)"
        class="my-2 border-t border-gray-100"
      />

      <!-- 상위 업무 정보 섹션 -->
      <div v-if="parent || root" class="tooltip-section">
        <span class="block text-[10px] font-semibold text-gray-400 uppercase tracking-wide mb-2">
          상위 업무
        </span>

        <!-- 경로 표시 -->
        <div class="flex flex-wrap items-center gap-1">
          <!-- 루트 업무 (부모와 다른 경우) -->
          <button
            v-if="showBothParentAndRoot && root"
            class="inline-flex items-center gap-1 px-2 py-1 text-[12px] rounded
                   bg-blue-50 text-blue-700 hover:bg-blue-100 transition-colors"
            @click="handleNavigate(root)"
          >
            <span class="truncate max-w-[100px]">{{ root.title }}</span>
          </button>

          <!-- 화살표 구분자 -->
          <svg
            v-if="showBothParentAndRoot"
            class="w-3 h-3 text-gray-400 flex-shrink-0"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>

          <!-- 부모 업무 -->
          <button
            v-if="parent"
            class="inline-flex items-center gap-1 px-2 py-1 text-[12px] rounded
                   bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors"
            @click="handleNavigate(parent)"
          >
            <span class="truncate max-w-[120px]">{{ parent.title }}</span>
          </button>
        </div>

        <!-- 상태 표시 -->
        <div v-if="parent" class="mt-2 flex items-center gap-2">
          <span class="text-[11px] text-gray-500">상태:</span>
          <span
            class="inline-flex items-center px-1.5 py-0.5 text-[11px] rounded"
            :class="[statusColors[parent.status].bg, statusColors[parent.status].text]"
          >
            {{ statusLabels[parent.status] }}
          </span>
        </div>
      </div>

      <!-- 툴팁 화살표 -->
      <div
        class="absolute w-2 h-2 bg-white border border-gray-200 transform rotate-45"
        :class="{
          'top-full left-1/2 -translate-x-1/2 -mt-1 border-t-0 border-l-0': placement === 'top',
          'bottom-full left-1/2 -translate-x-1/2 -mb-1 border-b-0 border-r-0': placement === 'bottom',
          'left-full top-1/2 -translate-y-1/2 -ml-1 border-l-0 border-b-0': placement === 'left',
          'right-full top-1/2 -translate-y-1/2 -mr-1 border-r-0 border-t-0': placement === 'right'
        }"
      />
    </div>
  </div>
</template>
