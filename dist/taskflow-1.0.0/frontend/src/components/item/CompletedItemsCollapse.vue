<script setup lang="ts">
/**
 * 완료/삭제 업무 축소 영역 컴포넌트
 * - 당일 완료/삭제된 업무만 표시
 * - 맨 하단 축소 상태로 표시
 * - 클릭 시 확장/축소
 * Compact UI 적용
 */
import { ref, computed } from 'vue'
import { useItemStore } from '@/stores/item'
import { Badge } from '@/components/common'
import type { Item } from '@/types/item'

const itemStore = useItemStore()

// 확장/축소 상태
const isExpanded = ref(false)

// 당일 완료/삭제 아이템
const todayItems = computed(() => itemStore.todayCompletedItems)

// 아이템 수
const itemCount = computed(() => todayItems.value.length)

// 완료/삭제 구분
const completedCount = computed(() =>
  todayItems.value.filter(item => item.status === 'COMPLETED').length
)
const deletedCount = computed(() =>
  todayItems.value.filter(item => item.status === 'DELETED').length
)

// 확장/축소 토글
function toggleExpand() {
  isExpanded.value = !isExpanded.value
}

// 처리 시간 포맷
function formatTime(item: Item): string {
  const dateStr = item.status === 'COMPLETED' ? item.completedAt : item.deletedAt
  if (!dateStr) return '-'

  const date = new Date(dateStr)
  return date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 상태 배지 색상
function getStatusVariant(status: string): 'success' | 'danger' {
  return status === 'COMPLETED' ? 'success' : 'danger'
}

// 상태 텍스트
function getStatusText(status: string): string {
  return status === 'COMPLETED' ? '완료' : '삭제'
}
</script>

<template>
  <div v-if="itemCount > 0" class="completed-items-collapse">
    <!-- 헤더 (클릭 시 확장/축소) -->
    <button
      class="w-full flex items-center justify-between px-4 py-2.5 bg-gray-50 hover:bg-gray-100 border border-gray-200 rounded-lg transition-colors"
      @click="toggleExpand"
    >
      <div class="flex items-center gap-2">
        <!-- 확장/축소 아이콘 -->
        <svg
          class="w-4 h-4 text-gray-500 transition-transform duration-200"
          :class="{ 'rotate-90': isExpanded }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>

        <!-- 텍스트 -->
        <span class="text-[13px] font-medium text-gray-600">
          오늘 처리된 업무
        </span>

        <!-- 카운트 배지 -->
        <span class="px-2 py-0.5 text-[11px] font-medium bg-gray-200 text-gray-600 rounded-full">
          {{ itemCount }}건
        </span>

        <!-- 완료/삭제 구분 표시 -->
        <div class="flex items-center gap-1.5 ml-2">
          <span v-if="completedCount > 0" class="text-[11px] text-green-600">
            완료 {{ completedCount }}
          </span>
          <span v-if="completedCount > 0 && deletedCount > 0" class="text-gray-300">|</span>
          <span v-if="deletedCount > 0" class="text-[11px] text-red-500">
            삭제 {{ deletedCount }}
          </span>
        </div>
      </div>

      <!-- 힌트 텍스트 -->
      <span class="text-[11px] text-gray-400">
        {{ isExpanded ? '접기' : '펼치기' }}
      </span>
    </button>

    <!-- 확장된 목록 -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 -translate-y-2"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-2"
    >
      <div
        v-if="isExpanded"
        class="mt-2 border border-gray-200 rounded-lg overflow-hidden"
      >
        <!-- 테이블 헤더 -->
        <div class="grid grid-cols-12 gap-2 px-4 py-2 bg-gray-50 border-b border-gray-200 text-[11px] font-medium text-gray-500 uppercase tracking-wider">
          <div class="col-span-7">작업 내용</div>
          <div class="col-span-2 text-center">결과</div>
          <div class="col-span-3 text-right">처리 시간</div>
        </div>

        <!-- 아이템 목록 -->
        <div class="max-h-60 overflow-y-auto">
          <div
            v-for="item in todayItems"
            :key="item.itemId"
            class="grid grid-cols-12 gap-2 px-4 py-2.5 border-b border-gray-100 last:border-b-0 hover:bg-gray-50 transition-colors"
          >
            <!-- 작업 내용 -->
            <div class="col-span-7 flex items-center gap-2 min-w-0">
              <span class="text-[13px] text-gray-700 truncate">
                {{ item.title }}
              </span>
            </div>

            <!-- 결과 (완료/삭제) -->
            <div class="col-span-2 flex items-center justify-center">
              <Badge
                :variant="getStatusVariant(item.status)"
                size="sm"
              >
                {{ getStatusText(item.status) }}
              </Badge>
            </div>

            <!-- 처리 시간 -->
            <div class="col-span-3 flex items-center justify-end">
              <span class="text-[12px] text-gray-500">
                {{ formatTime(item) }}
              </span>
            </div>
          </div>
        </div>

        <!-- 빈 상태 (표시될 일 없음 - 안전장치) -->
        <div
          v-if="todayItems.length === 0"
          class="px-4 py-6 text-center text-[13px] text-gray-400"
        >
          오늘 처리된 업무가 없습니다.
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.completed-items-collapse {
  @apply mt-4;
}
</style>
