<script setup lang="ts">
/**
 * 업무 페이지 (메인)
 * - 보드 선택 및 뷰 타입 전환
 * - 상태별 필터 탭 (전체, 대기, 진행중, 지연, 보류)
 * - 테이블/칸반/리스트 뷰 표시
 * - 신규 업무 등록
 * - 슬라이드오버 패널로 상세 보기
 * - v2.0: 보드 속성 패널 항상 표시
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useBoardStore } from '@/stores/board'
import { useItemStore } from '@/stores/item'
import { useCategoryStore } from '@/stores/category'
import { useToast } from '@/composables/useToast'
import { boardApi } from '@/api/board'
import { Button, Select, Spinner, EntityEditModal } from '@/components/common'
import ItemTable from '@/components/item/ItemTable.vue'
import ItemKanban from '@/components/item/ItemKanban.vue'
import ItemList from '@/components/item/ItemList.vue'
import NewItemInput from '@/components/item/NewItemInput.vue'
import CompletedItemsCollapse from '@/components/item/CompletedItemsCollapse.vue'
import { isItemOverdue } from '@/utils/item'
import type { Item } from '@/types/item'
import type { BoardCategory, BoardProperty } from '@/types/board'
import type { Category } from '@/types/category'

const boardStore = useBoardStore()
const itemStore = useItemStore()
const categoryStore = useCategoryStore()
const toast = useToast()

// 상태
const isLoading = ref(false)

// 보드 수정 모달 상태
const showBoardEditModal = ref(false)
const categories = ref<Category[]>([])
const existingBoardCategories = ref<BoardCategory[]>([])
const existingBoardProperties = ref<BoardProperty[]>([])
const boardFormData = ref({
  boardName: '',
  description: '',
  color: '#3B82F6',
  categoryIds: [] as number[],
  propertyIds: [] as number[],
  propertyDefaults: {} as Record<number, string>
})

// 색상 옵션
const colorOptions = [
  '#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6',
  '#EC4899', '#6366F1', '#14B8A6', '#F97316', '#6B7280'
]

// v2.0: 보드 카테고리
const boardCategories = ref<BoardCategory[]>([])

// 현재 보드의 기본 카테고리 정보
const currentCategory = computed(() => {
  if (boardCategories.value.length === 0) return null
  // 기본 카테고리가 있으면 사용, 없으면 첫 번째 카테고리 사용
  const defaultCat = boardCategories.value.find(c => c.isDefault)
  return defaultCat || boardCategories.value[0]
})

const currentCategoryName = computed(() => currentCategory.value?.categoryName || '')
const currentCategoryColor = computed(() => currentCategory.value?.categoryColor || '#6B7280')

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
    // v2.0: 보드 카테고리 로드
    await loadBoardCategories(boardId)
  }
}

// v2.0: 보드 카테고리 로드
async function loadBoardCategories(boardId: number) {
  try {
    const response = await boardApi.getBoardCategories(boardId)
    if (response.success && response.data) {
      boardCategories.value = response.data
    }
  } catch (error) {
    console.error('Failed to load board categories:', error)
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

// 카테고리 목록 로드
async function loadCategories() {
  try {
    await categoryStore.fetchAccessibleCategories()
    categories.value = categoryStore.activeAccessibleCategories
  } catch (error) {
    console.error('Failed to load categories:', error)
  }
}

// 보드 수정 모달 열기
async function openBoardEditModal() {
  const board = boardStore.currentBoard
  if (!board) return

  // 먼저 데이터 초기화
  let categoryIds: number[] = []
  let propertyIds: number[] = []
  const propertyDefaults: Record<number, string> = {}

  // 기존 카테고리와 속성 로드 (모달 열기 전에 완료)
  try {
    const [catRes, propRes] = await Promise.all([
      boardApi.getBoardCategories(board.boardId),
      boardApi.getBoardProperties(board.boardId)
    ])

    if (catRes.success && catRes.data) {
      existingBoardCategories.value = catRes.data
      categoryIds = catRes.data.map(c => c.categoryId)
    } else {
      existingBoardCategories.value = []
    }

    if (propRes.success && propRes.data) {
      existingBoardProperties.value = propRes.data
      propertyIds = propRes.data.map(p => p.propertyId)
      propRes.data.forEach(p => {
        if (p.defaultValue) {
          propertyDefaults[p.propertyId] = p.defaultValue
        }
      })
    } else {
      existingBoardProperties.value = []
    }
  } catch (error) {
    console.error('Failed to load board categories/properties:', error)
    existingBoardCategories.value = []
    existingBoardProperties.value = []
  }

  // 데이터 로드 완료 후 formData 설정
  boardFormData.value = {
    boardName: board.boardName,
    description: board.description || '',
    color: board.color || '#3B82F6',
    categoryIds,
    propertyIds,
    propertyDefaults
  }

  // 모달 열기 (데이터가 이미 준비된 상태)
  showBoardEditModal.value = true
}

// 보드 수정 저장 (EntityEditModal에서 호출)
async function handleBoardUpdateSave(data: {
  name: string
  description: string
  color: string
  categoryIds: number[]
  categoryId: number | null
  propertyIds: number[]
  propertyDefaults: Record<number, string>
}) {
  const board = boardStore.currentBoard
  if (!board) return
  if (!data.name.trim()) {
    toast.error('보드 이름을 입력해주세요.')
    return
  }

  const boardId = board.boardId

  try {
    // 1. 기본 정보 수정
    const success = await boardStore.updateBoard(boardId, {
      boardName: data.name,
      boardDescription: data.description,
      color: data.color
    })

    if (!success) {
      toast.error(boardStore.error || '보드 수정에 실패했습니다.')
      return
    }

    // 2. 카테고리 변경 처리
    const existingCatIds = new Set(existingBoardCategories.value.map(c => c.categoryId))
    const newCatIds = new Set(data.categoryIds)
    const catsToRemove = [...existingCatIds].filter(id => !newCatIds.has(id))
    const catsToAdd = [...newCatIds].filter(id => !existingCatIds.has(id))

    await Promise.all([
      ...catsToRemove.map(catId => boardApi.removeBoardCategory(boardId, catId)),
      ...catsToAdd.map(catId => boardApi.addBoardCategory(boardId, catId))
    ])

    // 3. 속성 변경 처리
    const existingPropIds = new Set(existingBoardProperties.value.map(p => p.propertyId))
    const newPropIds = new Set(data.propertyIds)
    const propsToRemove = [...existingPropIds].filter(id => !newPropIds.has(id))
    const propsToAdd = [...newPropIds].filter(id => !existingPropIds.has(id))

    await Promise.all([
      ...propsToRemove.map(propId => boardApi.removeBoardProperty(boardId, propId)),
      ...propsToAdd.map(propId => boardApi.addBoardProperty(boardId, propId, {
        defaultValue: data.propertyDefaults[propId] || undefined
      }))
    ])

    // 4. 기존 속성 기본값 업데이트
    const existingProps = existingBoardProperties.value.filter(p => newPropIds.has(p.propertyId))
    await Promise.all(
      existingProps
        .filter(p => p.defaultValue !== data.propertyDefaults[p.propertyId])
        .map(p => boardApi.updateBoardProperty(boardId, p.propertyId, {
          defaultValue: data.propertyDefaults[p.propertyId] || undefined
        }))
    )

    toast.success('보드가 수정되었습니다.')
    showBoardEditModal.value = false

    // 보드 카테고리 다시 로드
    await loadBoardCategories(boardId)
    // 보드 정보 갱신
    await boardStore.fetchBoard(boardId)
  } catch (error) {
    console.error('Failed to update board:', error)
    toast.error('보드 수정에 실패했습니다.')
  }
}

// 보드 변경 시 카테고리 로드
watch(currentBoardId, async (newBoardId) => {
  if (newBoardId) {
    await loadBoardCategories(newBoardId)
  } else {
    boardCategories.value = []
  }
}, { immediate: true })

onMounted(() => {
  boardStore.loadViewType()
  loadBoards()
  loadCategories()
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

          <!-- v2.0: 카테고리명 + 변경 버튼 그룹 -->
          <div v-if="currentBoardId" class="flex items-center gap-2 flex-shrink-0">
            <!-- 카테고리 라벨 -->
            <div
              v-if="currentCategoryName"
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border flex-shrink-0"
              :style="{
                backgroundColor: currentCategoryColor + '15',
                borderColor: currentCategoryColor + '40'
              }"
            >
              <div
                class="w-3 h-3 rounded flex-shrink-0"
                :style="{ backgroundColor: currentCategoryColor }"
              />
              <span
                class="text-sm font-medium whitespace-nowrap"
                :style="{ color: currentCategoryColor }"
              >
                {{ currentCategoryName }}
              </span>
            </div>

            <!-- 변경 버튼 -->
            <button
              type="button"
              class="px-3 py-1.5 text-sm font-medium text-blue-700 bg-blue-50 border border-blue-200 rounded-lg hover:bg-blue-100 transition-colors whitespace-nowrap flex-shrink-0"
              @click="openBoardEditModal"
            >
              변경
            </button>
          </div>

          <!-- 뷰 타입 선택 -->
          <div class="inline-flex rounded-lg border border-gray-200 bg-gray-50 p-1">
            <button
              type="button"
              class="flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-md transition-all duration-150 whitespace-nowrap"
              :class="viewType === 'table'
                ? 'bg-white text-primary-700 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'"
              @click="handleViewTypeChange('table')"
            >
              <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M3 10h18M3 14h18M3 6h18M3 18h18M10 6v12M17 6v12" />
              </svg>
              <span>테이블</span>
            </button>
            <button
              type="button"
              class="flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-md transition-all duration-150 whitespace-nowrap"
              :class="viewType === 'kanban'
                ? 'bg-white text-primary-700 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'"
              @click="handleViewTypeChange('kanban')"
            >
              <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2" />
              </svg>
              <span>칸반</span>
            </button>
            <button
              type="button"
              class="flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-md transition-all duration-150 whitespace-nowrap"
              :class="viewType === 'list'
                ? 'bg-white text-primary-700 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'"
              @click="handleViewTypeChange('list')"
            >
              <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M4 6h16M4 10h16M4 14h16M4 18h16" />
              </svg>
              <span>리스트</span>
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

    <!-- v2.0: 보드 수정 모달 -->
    <EntityEditModal
      v-model="showBoardEditModal"
      mode="board"
      title="보드 수정"
      :categories="categories"
      :color-options="colorOptions"
      name-label="보드 이름"
      :initial-name="boardFormData.boardName"
      :initial-description="boardFormData.description"
      :initial-color="boardFormData.color"
      :initial-category-ids="boardFormData.categoryIds"
      :initial-property-ids="boardFormData.propertyIds"
      :initial-property-defaults="boardFormData.propertyDefaults"
      @save="handleBoardUpdateSave"
      @cancel="showBoardEditModal = false"
    />
  </div>
</template>
