<script setup lang="ts">
/**
 * 아이템 배지 컴포넌트
 * - 공유받은 업무 표시 (공유 배지 + 공유해준 사람)
 * - 이관받은 업무 표시 (이관 배지 + 이관해준 사람)
 * - 향후 확장 가능한 구조
 */
import { computed } from 'vue'
import type { Item } from '@/types/item'

interface Props {
  item: Item
  /** 표시 크기: sm(compact) | md(normal) */
  size?: 'sm' | 'md'
  /** 최대 표시 배지 수 (나머지는 +N으로 표시) */
  maxBadges?: number
  /** 소유자 이름 표시 여부 */
  showOwnerName?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'sm',
  maxBadges: 2,
  showOwnerName: true
})

// 배지 목록 계산
interface BadgeInfo {
  type: 'shared' | 'transferred' | 'assigned'
  label: string
  ownerName?: string
  color: string
  bgColor: string
  icon: string
  title: string
}

const badges = computed<BadgeInfo[]>(() => {
  const result: BadgeInfo[] = []

  // 배당받은 업무 (v2.1) - 현재 사용자가 배당받은 경우
  if (props.item.isAssignedToMe) {
    result.push({
      type: 'assigned',
      label: '배당',
      ownerName: props.item.assignedByUserName,
      color: 'text-green-700',
      bgColor: 'bg-green-100',
      icon: 'assigned',
      title: props.item.assignedByUserName
        ? `${props.item.assignedByUserName}님이 배당`
        : '배당받은 업무'
    })
  }
  // 배당한 업무 (v2.1) - 소유자가 다른 사용자에게 배당한 경우
  else if (props.item.assignedToUsername) {
    result.push({
      type: 'assigned',
      label: '배당됨',
      ownerName: props.item.assignedToUserName,
      color: 'text-emerald-700',
      bgColor: 'bg-emerald-100',
      icon: 'assigned',
      title: props.item.assignedToUserName
        ? `${props.item.assignedToUserName}님에게 배당`
        : '배당한 업무'
    })
  }

  // 공유받은 업무
  if (props.item.isSharedToMe) {
    result.push({
      type: 'shared',
      label: '공유',
      ownerName: props.item.sharedByUserName,
      color: 'text-blue-700',
      bgColor: 'bg-blue-100',
      icon: 'share',
      title: props.item.sharedByUserName
        ? `${props.item.sharedByUserName}님이 공유`
        : '공유받은 업무'
    })
  }

  // 이관받은 업무
  if (props.item.transferredFrom) {
    result.push({
      type: 'transferred',
      label: '이관',
      ownerName: props.item.transferredByUserName,
      color: 'text-purple-700',
      bgColor: 'bg-purple-100',
      icon: 'transfer',
      title: props.item.transferredByUserName
        ? `${props.item.transferredByUserName}님이 이관`
        : '이관받은 업무'
    })
  }

  return result
})

// 표시할 배지와 숨겨진 배지 수
const visibleBadges = computed(() => badges.value.slice(0, props.maxBadges))
const hiddenCount = computed(() => Math.max(0, badges.value.length - props.maxBadges))

// 배지가 있는지 여부
const hasBadges = computed(() => badges.value.length > 0)

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
</script>

<template>
  <div v-if="hasBadges" class="flex items-center gap-1 flex-shrink-0">
    <!-- 배지들 -->
    <template v-for="badge in visibleBadges" :key="badge.type">
      <span
        :class="[
          'inline-flex items-center font-medium rounded',
          badge.color,
          badge.bgColor,
          sizeClasses.badge,
          sizeClasses.gap
        ]"
        :title="badge.title"
      >
        <!-- 배당 아이콘 -->
        <svg v-if="badge.icon === 'assigned'" :class="sizeClasses.icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
        </svg>
        <!-- 공유 아이콘 -->
        <svg v-else-if="badge.icon === 'share'" :class="sizeClasses.icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
        </svg>
        <!-- 이관 아이콘 -->
        <svg v-else-if="badge.icon === 'transfer'" :class="sizeClasses.icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
        </svg>
        <!-- 라벨 -->
        <span>{{ badge.label }}</span>
        <!-- 소유자 이름 (옵션) -->
        <span v-if="showOwnerName && badge.ownerName" class="opacity-75">
          ({{ badge.ownerName }})
        </span>
      </span>
    </template>

    <!-- 숨겨진 배지 수 -->
    <span
      v-if="hiddenCount > 0"
      :class="[
        'inline-flex items-center font-medium rounded text-gray-600 bg-gray-100',
        sizeClasses.badge
      ]"
      :title="`${hiddenCount}개 더 있음`"
    >
      +{{ hiddenCount }}
    </span>
  </div>
</template>
