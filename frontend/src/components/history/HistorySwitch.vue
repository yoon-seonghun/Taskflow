<script setup lang="ts">
/**
 * 이력 타입 스위치 컴포넌트
 * - 전체 / 작업 처리 이력 / 작업 등록 이력 토글
 */

export type HistoryType = 'all' | 'item' | 'template'

interface Props {
  modelValue: HistoryType
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: HistoryType): void
}>()

function selectType(type: HistoryType) {
  emit('update:modelValue', type)
}
</script>

<template>
  <div class="history-switch">
    <button
      type="button"
      class="switch-btn"
      :class="{ active: modelValue === 'all' }"
      @click="selectType('all')"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M4 6h16M4 10h16M4 14h16M4 18h16" />
      </svg>
      전체 이력
    </button>
    <button
      type="button"
      class="switch-btn"
      :class="{ active: modelValue === 'item' }"
      @click="selectType('item')"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
      </svg>
      작업 처리 이력
    </button>
    <button
      type="button"
      class="switch-btn"
      :class="{ active: modelValue === 'template' }"
      @click="selectType('template')"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      작업 등록 이력
    </button>
  </div>
</template>

<style scoped>
.history-switch {
  @apply inline-flex rounded-lg border border-gray-200 bg-gray-50 p-1;
}

.switch-btn {
  @apply flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-md transition-all duration-150;
  @apply text-gray-600 hover:text-gray-900;
}

.switch-btn.active {
  @apply bg-white text-primary-700 shadow-sm;
}
</style>
