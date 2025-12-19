<script setup lang="ts">
/**
 * 페이지네이션 컴포넌트
 * - 페이지 번호 표시
 * - 이전/다음 네비게이션
 * - Compact UI 적용
 */
import { computed } from 'vue'

interface Props {
  currentPage: number
  totalPages: number
  siblingCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  siblingCount: 1
})

const emit = defineEmits<{
  (e: 'change', page: number): void
}>()

// 표시할 페이지 번호 계산
const pages = computed(() => {
  const total = props.totalPages
  const current = props.currentPage
  const siblings = props.siblingCount

  // 총 표시할 버튼 수: 시작(1) + 끝(1) + 현재 + 좌우 siblings + 구분자(2)
  const totalNumbers = siblings * 2 + 5

  // 총 페이지가 적으면 모두 표시
  if (total <= totalNumbers) {
    return Array.from({ length: total }, (_, i) => i)
  }

  const leftSiblingIndex = Math.max(current - siblings, 1)
  const rightSiblingIndex = Math.min(current + siblings, total - 2)

  const showLeftDots = leftSiblingIndex > 2
  const showRightDots = rightSiblingIndex < total - 3

  const result: (number | 'dots')[] = []

  // 첫 페이지
  result.push(0)

  // 왼쪽 dots
  if (showLeftDots) {
    result.push('dots')
  } else {
    // 1부터 leftSibling까지 추가
    for (let i = 1; i < leftSiblingIndex; i++) {
      result.push(i)
    }
  }

  // 중간 페이지들 (현재 페이지 주변)
  for (let i = leftSiblingIndex; i <= rightSiblingIndex; i++) {
    result.push(i)
  }

  // 오른쪽 dots
  if (showRightDots) {
    result.push('dots')
  } else {
    // rightSibling부터 끝 전까지 추가
    for (let i = rightSiblingIndex + 1; i < total - 1; i++) {
      result.push(i)
    }
  }

  // 마지막 페이지
  if (total > 1) {
    result.push(total - 1)
  }

  return result
})

function goToPage(page: number) {
  if (page >= 0 && page < props.totalPages && page !== props.currentPage) {
    emit('change', page)
  }
}

function goToPrev() {
  if (props.currentPage > 0) {
    emit('change', props.currentPage - 1)
  }
}

function goToNext() {
  if (props.currentPage < props.totalPages - 1) {
    emit('change', props.currentPage + 1)
  }
}

const canGoPrev = computed(() => props.currentPage > 0)
const canGoNext = computed(() => props.currentPage < props.totalPages - 1)
</script>

<template>
  <nav class="flex items-center gap-1" aria-label="Pagination">
    <!-- 이전 버튼 -->
    <button
      type="button"
      class="p-1.5 rounded text-gray-600 transition-colors"
      :class="[
        canGoPrev
          ? 'hover:bg-gray-100 cursor-pointer'
          : 'opacity-40 cursor-not-allowed'
      ]"
      :disabled="!canGoPrev"
      @click="goToPrev"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
    </button>

    <!-- 페이지 번호 -->
    <template v-for="(page, index) in pages" :key="index">
      <!-- 구분자 -->
      <span v-if="page === 'dots'" class="px-2 text-gray-400 text-[13px]">
        ...
      </span>

      <!-- 페이지 번호 버튼 -->
      <button
        v-else
        type="button"
        class="min-w-[28px] h-7 px-2 rounded text-[13px] font-medium transition-colors"
        :class="[
          page === currentPage
            ? 'bg-primary-500 text-white'
            : 'text-gray-600 hover:bg-gray-100'
        ]"
        @click="goToPage(page)"
      >
        {{ page + 1 }}
      </button>
    </template>

    <!-- 다음 버튼 -->
    <button
      type="button"
      class="p-1.5 rounded text-gray-600 transition-colors"
      :class="[
        canGoNext
          ? 'hover:bg-gray-100 cursor-pointer'
          : 'opacity-40 cursor-not-allowed'
      ]"
      :disabled="!canGoNext"
      @click="goToNext"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
      </svg>
    </button>
  </nav>
</template>
