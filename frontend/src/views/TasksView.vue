<script setup lang="ts">
/**
 * 업무 페이지 (메인)
 * - 보드 선택 및 뷰 타입 전환
 * - 상태별 필터 탭 (전체, 대기, 진행중, 지연, 보류)
 * - 테이블/칸반/리스트 뷰 표시
 * - 신규 업무 등록
 * - 슬라이드오버 패널로 상세 보기
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useBoardStore } from '@/stores/board'
import { useItemStore } from '@/stores/item'
import { useToast } from '@/composables/useToast'
import { Button, Select, Spinner } from '@/components/common'
import ItemTable from '@/components/item/ItemTable.vue'
import ItemKanban from '@/components/item/ItemKanban.vue'
import ItemList from '@/components/item/ItemList.vue'
import NewItemInput from '@/components/item/NewItemInput.vue'
import CompletedItemsCollapse from '@/components/item/CompletedItemsCollapse.vue'
import { isItemOverdue } from '@/utils/item'
import type { Item } from '@/types/item'

const boardStore = useBoardStore()
const itemStore = useItemStore()
const toast = useToast()

// 상태
const isLoading = ref(false)

// 필터 타입
type StatusFilter = 'all' | 'not_started' | 'in_progress' | 'overdue' | 'pending'
const statusFilter = ref<StatusFilter>('all')

// 현재 선택된 보드
const currentBoardId = computed(() => boardStore.currentBoardId)

// 보드 목록
const boardOptions = computed(() =>
  boardStore.activeBoards.map(b => ({
    value: b.boardId,
    label: b.boardName
  }))
)

// 뷰 타입 옵션
const viewTypeOptions = [
  { value: 'table', label: '테이블' },
  { value: 'kanban', label: '칸반' },
  { value: 'list', label: '리스트' }
]

// 현재 뷰 타입
const viewType = computed({
  get: () => boardStore.viewType,
  set: (value) => boardStore.setViewType(value)
})

// 활성 아이템 (완료/삭제 제외)
const activeItems = computed(() => itemStore.activeItems)

// 필터별 카운트
const filterCounts = computed(() => {
  const items = activeItems.value
  return {
    all: items.length,
    not_started: items.filter(i => i.status === 'NOT_STARTED').length,
    in_progress: items.filter(i => i.status === 'IN_PROGRESS').length,
    overdue: items.filter(i => isItemOverdue(i)).length,
    pending: items.filter(i => i.status === 'PENDING').length
  }
})

// 필터 탭 옵션
const filterTabs = computed(() => [
  { value: 'all', label: '전체', count: filterCounts.value.all },
  { value: 'not_started', label: '대기', count: filterCounts.value.not_started },
  { value: 'in_progress', label: '진행중', count: filterCounts.value.in_progress },
  { value: 'overdue', label: '지연', count: filterCounts.value.overdue, warning: true },
  { value: 'pending', label: '보류', count: filterCounts.value.pending }
])

// 필터링된 아이템 (ItemTable 등에 전달)
const filteredItems = computed(() => {
  const items = activeItems.value

  switch (statusFilter.value) {
    case 'not_started':
      return items.filter(i => i.status === 'NOT_STARTED')
    case 'in_progress':
      return items.filter(i => i.status === 'IN_PROGRESS')
    case 'overdue':
      return items.filter(i => isItemOverdue(i))
    case 'pending':
      return items.filter(i => i.status === 'PENDING')
    default:
      return items
  }
})

// 보드 목록 로드
async function loadBoards() {
  isLoading.value = true
  try {
    await boardStore.fetchBoards()
    // 첫 번째 보드 선택
    if (boardStore.boards.length > 0 && !currentBoardId.value) {
      await selectBoard(boardStore.boards[0].boardId)
    }
  } catch (error) {
    toast.error('보드 목록을 불러오는데 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

// 보드 선택
async function selectBoard(boardId: number) {
  const board = boardStore.getBoardById(boardId)
  if (board) {
    boardStore.setCurrentBoard(board)
    await boardStore.fetchBoard(boardId)
  }
}

// 아이템 클릭 핸들러
function handleItemClick(item: Item) {
  // SlideOver 패널에서 처리됨
}

// 신규 업무 등록 완료 핸들러
function handleItemCreated(itemId: number) {
  // 필요 시 추가 처리
}

// 보드 변경 핸들러
function handleBoardChange(boardId: number | string | null) {
  if (boardId && typeof boardId === 'number') {
    selectBoard(boardId)
  }
}

// 뷰 타입 변경 핸들러
function handleViewTypeChange(type: string | number | null) {
  if (type && typeof type === 'string') {
    boardStore.setViewType(type as 'table' | 'kanban' | 'list')
  }
}

// 필터 변경 핸들러
function handleFilterChange(filter: StatusFilter) {
  statusFilter.value = filter
}

onMounted(() => {
  boardStore.loadViewType()
  loadBoards()
})
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 페이지 헤더 -->
    <div class="flex-shrink-0 pb-4">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <!-- 좌측: 보드 선택 및 뷰 타입 -->
        <div class="flex items-center gap-3">
          <!-- 보드 선택 -->
          <Select
            :model-value="currentBoardId"
            :options="boardOptions"
            placeholder="보드 선택"
            class="w-48"
            @update:model-value="handleBoardChange"
          />

          <!-- 뷰 타입 선택 -->
          <div class="flex items-center bg-gray-100 rounded-lg p-0.5">
            <button
              v-for="option in viewTypeOptions"
              :key="option.value"
              class="px-3 py-1.5 text-[13px] rounded-md transition-colors"
              :class="viewType === option.value
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'"
              @click="handleViewTypeChange(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 필터 탭 -->
    <div v-if="currentBoardId && !isLoading" class="flex-shrink-0 mb-4">
      <div class="flex items-center gap-1 p-1 bg-gray-100 rounded-lg">
        <button
          v-for="tab in filterTabs"
          :key="tab.value"
          class="flex items-center gap-1.5 px-3 py-1.5 text-[13px] font-medium rounded-md transition-colors"
          :class="[
            statusFilter === tab.value
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
          ]"
          @click="handleFilterChange(tab.value as StatusFilter)"
        >
          <span>{{ tab.label }}</span>
          <span
            class="px-1.5 py-0.5 text-[11px] rounded-full"
            :class="[
              statusFilter === tab.value
                ? tab.warning ? 'bg-red-100 text-red-700' : 'bg-primary-100 text-primary-700'
                : tab.warning && tab.count > 0 ? 'bg-red-100 text-red-600' : 'bg-gray-200 text-gray-600'
            ]"
          >
            {{ tab.count }}
          </span>
          <!-- 지연 경고 아이콘 -->
          <svg
            v-if="tab.warning && tab.count > 0 && statusFilter !== tab.value"
            class="w-3.5 h-3.5 text-red-500"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 신규 업무 등록 -->
    <div v-if="currentBoardId" class="flex-shrink-0 mb-4">
      <NewItemInput
        :board-id="currentBoardId"
        @created="handleItemCreated"
      />
    </div>

    <!-- 메인 컨텐츠 -->
    <div class="flex-1 min-h-0">
      <!-- 로딩 상태 -->
      <div v-if="isLoading" class="h-full flex items-center justify-center">
        <Spinner size="lg" />
      </div>

      <!-- 보드 미선택 상태 -->
      <div v-else-if="!currentBoardId" class="h-full flex items-center justify-center">
        <div class="text-center">
          <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2" />
          </svg>
          <p class="text-gray-500">보드를 선택해주세요.</p>
        </div>
      </div>

      <!-- 테이블 뷰 -->
      <ItemTable
        v-else-if="viewType === 'table'"
        :board-id="currentBoardId"
        :filter="statusFilter"
        @item-click="handleItemClick"
      />

      <!-- 칸반 뷰 -->
      <ItemKanban
        v-else-if="viewType === 'kanban'"
        :board-id="currentBoardId"
        :filter="statusFilter"
        @item-click="handleItemClick"
      />

      <!-- 리스트 뷰 -->
      <ItemList
        v-else-if="viewType === 'list'"
        :board-id="currentBoardId"
        :filter="statusFilter"
        @item-click="handleItemClick"
      />
    </div>

    <!-- 완료/삭제 업무 Hidden 영역 -->
    <div v-if="currentBoardId && !isLoading && statusFilter === 'all'" class="flex-shrink-0">
      <CompletedItemsCollapse />
    </div>
  </div>
</template>
