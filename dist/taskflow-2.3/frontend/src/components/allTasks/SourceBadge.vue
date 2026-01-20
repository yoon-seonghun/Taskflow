<script setup lang="ts">
/**
 * 출처 배지 컴포넌트
 * - 업무의 출처(소유/공유보드/공유업무/배당)를 표시
 * - SharedItemsView와 동일한 스타일 적용
 */
import { computed } from 'vue'
import type { SourceType } from '@/types/allTasks'
import { sourceTypeLabels, sourceTypeColors } from '@/types/allTasks'

interface Props {
  /** 출처 타입 */
  sourceType: SourceType
  /** 표시 크기 */
  size?: 'sm' | 'md'
  /** 아이콘 표시 여부 */
  showIcon?: boolean
  /** 라벨 텍스트 표시 여부 */
  showLabel?: boolean
  /** 보드 색상 (보드 관련 출처인 경우) */
  boardColor?: string
  /** 공유/배당해준 사용자 이름 */
  ownerName?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'sm',
  showIcon: true,
  showLabel: false
})

// 라벨
const label = computed(() => sourceTypeLabels[props.sourceType])

// 색상 클래스
const colorClasses = computed(() => {
  const colors = sourceTypeColors[props.sourceType]
  return `${colors.bg} ${colors.text} ${colors.darkBg} ${colors.darkText}`
})

// 크기별 스타일
const sizeClasses = computed(() => {
  return props.size === 'sm'
    ? {
        badge: 'px-1.5 py-0.5 text-[10px]',
        icon: 'w-3 h-3',
        gap: 'gap-0.5'
      }
    : {
        badge: 'px-2 py-0.5 text-[11px]',
        icon: 'w-3.5 h-3.5',
        gap: 'gap-1'
      }
})

// 툴팁
const tooltip = computed(() => {
  let text = label.value
  if (props.ownerName) {
    if (props.sourceType === 'BOARD_SHARED' || props.sourceType === 'ITEM_SHARED') {
      text += ` (${props.ownerName}님이 공유)`
    } else if (props.sourceType === 'ASSIGNED') {
      text += ` (${props.ownerName}님이 배당)`
    }
  }
  return text
})
</script>

<template>
  <span
    v-if="sourceType && sourceType !== 'ALL'"
    :class="[
      'inline-flex items-center font-medium rounded flex-shrink-0',
      colorClasses,
      sizeClasses.badge,
      sizeClasses.gap
    ]"
    :title="tooltip"
  >
    <!-- 아이콘 -->
    <template v-if="showIcon">
      <!-- 내 보드 (OWNED) -->
      <svg v-if="sourceType === 'OWNED'" :class="sizeClasses.icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
      </svg>
      <!-- 공유 보드 (BOARD_SHARED) -->
      <svg v-else-if="sourceType === 'BOARD_SHARED'" :class="sizeClasses.icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
      <!-- 공유 업무 (ITEM_SHARED) -->
      <svg v-else-if="sourceType === 'ITEM_SHARED'" :class="sizeClasses.icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
      </svg>
      <!-- 배당 업무 (ASSIGNED) -->
      <svg v-else-if="sourceType === 'ASSIGNED'" :class="sizeClasses.icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
      </svg>
    </template>

    <!-- 라벨 (showLabel이 true일 때만 표시) -->
    <span v-if="showLabel">{{ label }}</span>

    <!-- 보드 색상 표시 -->
    <span
      v-if="boardColor && (sourceType === 'OWNED' || sourceType === 'BOARD_SHARED')"
      class="w-2 h-2 rounded-full"
      :class="{ 'ml-0.5': showLabel }"
      :style="{ backgroundColor: boardColor }"
    />
  </span>
</template>
