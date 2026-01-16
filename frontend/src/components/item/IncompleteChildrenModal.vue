<script setup lang="ts">
/**
 * 미완료 하위 업무 확인 모달 (v2.2)
 * - 부모 업무 완료 시 미완료 하위 업무가 있으면 표시
 * - 강제 완료 옵션 제공
 */
import { computed } from 'vue'
import type { IncompleteChildrenResponse, ItemStatus } from '@/types/item'

interface Props {
  show: boolean
  incompleteChildren: IncompleteChildrenResponse | null
  parentTitle?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'forceComplete'): void
  (e: 'cancel'): void
}>()

// 하위 업무 목록
const children = computed(() => props.incompleteChildren?.children ?? [])

// 미완료 하위 업무 수
const incompleteCount = computed(() => props.incompleteChildren?.incompleteChildCount ?? 0)

// 상태별 색상
const statusColors: Record<ItemStatus, string> = {
  NOT_STARTED: 'bg-gray-100 text-gray-600',
  IN_PROGRESS: 'bg-blue-100 text-blue-600',
  PENDING: 'bg-yellow-100 text-yellow-600',
  COMPLETED: 'bg-green-100 text-green-600',
  DELETED: 'bg-red-100 text-red-600'
}

// 상태 라벨
const statusLabels: Record<ItemStatus, string> = {
  NOT_STARTED: '시작전',
  IN_PROGRESS: '진행중',
  PENDING: '보류',
  COMPLETED: '완료',
  DELETED: '삭제'
}

// 깊이 표시 (들여쓰기)
function getDepthPadding(depth: number): string {
  return `${(depth - 1) * 16}px`
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="show"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        @click.self="$emit('cancel')"
      >
        <!-- 배경 오버레이 -->
        <div class="absolute inset-0 bg-black/50" @click="$emit('cancel')" />

        <!-- 모달 컨텐츠 -->
        <div class="relative bg-white rounded-lg shadow-xl w-full max-w-md max-h-[80vh] flex flex-col">
          <!-- 헤더 -->
          <div class="flex items-center justify-between px-4 py-3 border-b border-gray-200">
            <div class="flex items-center gap-2">
              <svg class="w-5 h-5 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <h3 class="text-lg font-semibold text-gray-900">미완료 하위 업무</h3>
            </div>
            <button
              class="p-1 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors"
              @click="$emit('close')"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 본문 -->
          <div class="flex-1 overflow-y-auto p-4">
            <!-- 안내 메시지 -->
            <div class="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p class="text-sm text-yellow-800">
                <strong>"{{ parentTitle }}"</strong> 업무에 미완료 하위 업무가 <strong>{{ incompleteCount }}개</strong> 있습니다.
              </p>
              <p class="mt-1 text-xs text-yellow-600">
                강제 완료 시 모든 하위 업무도 함께 완료 처리됩니다.
              </p>
            </div>

            <!-- 미완료 하위 업무 목록 -->
            <div class="space-y-1">
              <div
                v-for="child in children"
                :key="child.itemId"
                class="flex items-center gap-2 p-2 bg-gray-50 rounded hover:bg-gray-100 transition-colors"
                :style="{ paddingLeft: getDepthPadding(child.itemDepth) }"
              >
                <!-- 깊이 표시 아이콘 -->
                <svg
                  v-if="child.itemDepth > 1"
                  class="w-3 h-3 text-gray-400 flex-shrink-0"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>

                <!-- 제목 -->
                <span class="flex-1 text-sm text-gray-700 truncate" :title="child.title">
                  {{ child.title }}
                </span>

                <!-- 상태 배지 -->
                <span
                  class="flex-shrink-0 px-1.5 py-0.5 text-[10px] font-medium rounded"
                  :class="statusColors[child.status]"
                >
                  {{ statusLabels[child.status] }}
                </span>
              </div>
            </div>
          </div>

          <!-- 푸터 -->
          <div class="flex items-center justify-end gap-2 px-4 py-3 border-t border-gray-200 bg-gray-50">
            <button
              class="px-4 py-2 text-sm text-gray-700 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
              @click="$emit('cancel')"
            >
              취소
            </button>
            <button
              class="px-4 py-2 text-sm text-white bg-primary-600 hover:bg-primary-700 rounded-lg transition-colors"
              @click="$emit('forceComplete')"
            >
              강제 완료
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .relative,
.modal-leave-active .relative {
  transition: transform 0.2s ease;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
}
</style>
