<script setup lang="ts">
/**
 * 전체 업무 뷰 (v2.1)
 * - 소유 보드 + 공유 보드 + 개별 공유 + 배당받은 업무 통합 조회
 * - 출처별 필터링 및 통계
 * - 트리/평면/접기 표시 모드
 */
import { ref, onMounted, computed, watch } from 'vue'
import { useAllTasksStore } from '@/stores/allTasks'
import { useSlideOver } from '@/composables/useSlideOver'
import { useToast } from '@/composables/useToast'
import { Pagination } from '@/components/common'
import {
  AllTasksFilter,
  AllTasksStats,
  AllTasksTable
} from '@/components/allTasks'
import type { AllItemResponse, AllItemsFilter, SourceType } from '@/types/allTasks'

const allTasksStore = useAllTasksStore()
const { openItemDetail } = useSlideOver()
const toast = useToast()

// 페이지 타이틀
const pageTitle = computed(() => {
  const sourceLabels: Record<SourceType, string> = {
    ALL: '전체 업무',
    OWNED: '내 보드 업무',
    BOARD_SHARED: '공유 보드 업무',
    ITEM_SHARED: '공유받은 업무',
    ASSIGNED: '배당받은 업무'
  }
  return sourceLabels[allTasksStore.filters.sourceType] || '전체 업무'
})

// 초기 로드
onMounted(async () => {
  await loadData()
})

// 데이터 로드
async function loadData(): Promise<void> {
  const success = await allTasksStore.fetchAllItems()
  if (!success && allTasksStore.error) {
    toast.error(allTasksStore.error)
  }
}

// 필터 변경 핸들러
async function handleFilterChange(newFilter: Partial<AllItemsFilter>): Promise<void> {
  // childDisplayMode 변경 시 setChildDisplayMode 호출 (expand/collapse 자동 처리)
  if (newFilter.childDisplayMode !== undefined) {
    await allTasksStore.setChildDisplayMode(newFilter.childDisplayMode)
  } else {
    await allTasksStore.updateFilter(newFilter)
  }
}

// 필터 초기화
async function handleFilterReset(): Promise<void> {
  await allTasksStore.resetFilters()
}

// 키워드 검색
async function handleSearch(keyword: string): Promise<void> {
  await allTasksStore.search(keyword)
}

// 출처 타입 선택 (통계 배지 클릭)
async function handleSourceSelect(sourceType: SourceType): Promise<void> {
  await allTasksStore.setSourceType(sourceType)
}

// 상태 선택 (통계 카드 클릭)
async function handleStatusSelect(status: string | null): Promise<void> {
  await allTasksStore.updateFilter({ status: status as any })
}

// 페이지 변경
async function handlePageChange(page: number): Promise<void> {
  await allTasksStore.setPage(page)
}

// 아이템 클릭 핸들러
function handleItemClick(item: AllItemResponse): void {
  if (item.boardId && item.itemId) {
    openItemDetail(item.itemId, item.boardId, handleItemUpdated)
  }
}

// 아이템 업데이트 핸들러 (슬라이드오버에서 수정 후 콜백)
function handleItemUpdated(updatedItem: unknown): void {
  const updated = updatedItem as AllItemResponse
  if (!updated || !updated.itemId) return

  // 로컬 업데이트
  allTasksStore.updateItemLocal(updated.itemId, updated)
}

// 아이템 확장/축소 토글
function handleToggleExpand(itemId: number): void {
  allTasksStore.toggleExpand(itemId)
}

// 모두 확장/축소
function handleExpandAll(): void {
  allTasksStore.expandAll()
}

function handleCollapseAll(): void {
  allTasksStore.collapseAll()
}

// 새로고침
async function handleRefresh(): Promise<void> {
  await loadData()
}
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 헤더 -->
    <div class="flex-shrink-0 bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 px-4 py-3">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-3">
          <h1 class="text-lg font-semibold text-gray-900 dark:text-white">
            {{ pageTitle }}
          </h1>
          <span
            v-if="allTasksStore.pagination.totalElements > 0"
            class="text-sm text-gray-500 dark:text-gray-400"
          >
            ({{ allTasksStore.pagination.totalElements }}건)
          </span>
        </div>

        <div class="flex items-center gap-2">
          <!-- 모두 확장/축소 버튼 (tree 모드에서만) -->
          <template v-if="allTasksStore.filters.childDisplayMode !== 'flat'">
            <button
              type="button"
              class="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-300 dark:hover:bg-gray-700"
              title="모두 확장"
              @click="handleExpandAll"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
              </svg>
            </button>
            <button
              type="button"
              class="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-300 dark:hover:bg-gray-700"
              title="모두 축소"
              @click="handleCollapseAll"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 9V4.5M9 9H4.5M9 9L3.75 3.75M9 15v4.5M9 15H4.5M9 15l-5.25 5.25M15 9h4.5M15 9V4.5M15 9l5.25-5.25M15 15h4.5M15 15v4.5m0-4.5l5.25 5.25" />
              </svg>
            </button>
          </template>

          <!-- 새로고침 버튼 -->
          <button
            type="button"
            class="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-300 dark:hover:bg-gray-700"
            title="새로고침"
            :disabled="allTasksStore.loading"
            @click="handleRefresh"
          >
            <svg
              class="w-4 h-4"
              :class="{ 'animate-spin': allTasksStore.loading }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
        </div>
      </div>

      <!-- 통계 카드 -->
      <AllTasksStats
        :stats="allTasksStore.stats"
        :items="allTasksStore.displayItems"
        :selected-source-type="allTasksStore.filters.sourceType"
        :selected-status="allTasksStore.filters.status"
        :loading="allTasksStore.loading"
        @select-source="handleSourceSelect"
        @select-status="handleStatusSelect"
      />
    </div>

    <!-- 필터 -->
    <div class="flex-shrink-0 bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 px-4 py-3">
      <AllTasksFilter
        :filters="allTasksStore.filters"
        :loading="allTasksStore.loading"
        @update:filters="handleFilterChange"
        @reset="handleFilterReset"
        @search="handleSearch"
      />
    </div>

    <!-- 테이블 -->
    <div class="flex-1 overflow-hidden bg-white dark:bg-gray-900">
      <AllTasksTable
        :items="allTasksStore.displayItems"
        :loading="allTasksStore.loading"
        :child-display-mode="allTasksStore.filters.childDisplayMode"
        :expanded-ids="allTasksStore.expandedItemIds"
        @item-click="handleItemClick"
        @toggle-expand="handleToggleExpand"
      />
    </div>

    <!-- 페이지네이션 -->
    <div
      v-if="allTasksStore.pagination.totalPages > 1"
      class="flex-shrink-0 bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-700 px-4 py-3"
    >
      <Pagination
        :current-page="allTasksStore.pagination.page"
        :total-pages="allTasksStore.pagination.totalPages"
        :total-elements="allTasksStore.pagination.totalElements"
        :page-size="allTasksStore.pagination.size"
        @page-change="handlePageChange"
      />
    </div>
  </div>
</template>
