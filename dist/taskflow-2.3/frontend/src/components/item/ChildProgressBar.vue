<script setup lang="ts">
/**
 * 하위 업무 진행률 표시 컴포넌트 (v2.2)
 * - 기본 업무의 하위 업무 완료 진행률 표시
 * - 프로그레스 바 + 숫자 표시
 */
import { computed } from 'vue'

interface Props {
  childCount: number           // 전체 하위 업무 수
  completedChildCount: number  // 완료된 하위 업무 수
  showCount?: boolean          // 숫자 표시 여부
  size?: 'xs' | 'sm' | 'md'   // 크기
  showLabel?: boolean          // 라벨 표시 ("완료" 텍스트)
  compact?: boolean            // 컴팩트 모드
}

const props = withDefaults(defineProps<Props>(), {
  showCount: true,
  size: 'sm',
  showLabel: false,
  compact: false
})

// 진행률 계산
const percentage = computed(() => {
  if (props.childCount === 0) return 0
  return Math.round((props.completedChildCount / props.childCount) * 100)
})

// 진행 상태에 따른 색상
const progressClass = computed(() => {
  if (percentage.value === 100) return 'bg-green-500'
  if (percentage.value >= 75) return 'bg-emerald-500'
  if (percentage.value >= 50) return 'bg-blue-500'
  if (percentage.value >= 25) return 'bg-yellow-500'
  return 'bg-gray-400'
})

// 크기별 스타일
const sizeClasses = computed(() => {
  switch (props.size) {
    case 'xs':
      return {
        bar: 'h-1',
        text: 'text-[10px]',
        wrapper: 'max-w-[60px]'
      }
    case 'sm':
      return {
        bar: 'h-1.5',
        text: 'text-[11px]',
        wrapper: 'max-w-[80px]'
      }
    case 'md':
      return {
        bar: 'h-2',
        text: 'text-[12px]',
        wrapper: 'max-w-[100px]'
      }
    default:
      return {
        bar: 'h-1.5',
        text: 'text-[11px]',
        wrapper: 'max-w-[80px]'
      }
  }
})
</script>

<template>
  <div
    v-if="childCount > 0"
    class="child-progress flex items-center gap-1.5"
    :class="{ 'flex-col items-start gap-0.5': !compact && showLabel }"
  >
    <!-- 라벨 (선택적) -->
    <span
      v-if="showLabel && !compact"
      class="text-gray-500 dark:text-gray-400"
      :class="sizeClasses.text"
    >
      진행률
    </span>

    <div class="flex items-center gap-1.5 w-full">
      <!-- 프로그레스 바 -->
      <div
        class="flex-1 bg-gray-200 rounded-full overflow-hidden dark:bg-gray-700"
        :class="[sizeClasses.bar, sizeClasses.wrapper]"
        :title="`${completedChildCount}/${childCount} 완료 (${percentage}%)`"
      >
        <div
          class="h-full rounded-full transition-all duration-300 ease-out"
          :class="progressClass"
          :style="{ width: `${percentage}%` }"
        />
      </div>

      <!-- 숫자 표시 -->
      <span
        v-if="showCount"
        class="flex-shrink-0 text-gray-500 tabular-nums dark:text-gray-400"
        :class="sizeClasses.text"
      >
        {{ completedChildCount }}/{{ childCount }}
      </span>
    </div>
  </div>
</template>
