<script setup lang="ts">
/**
 * 아이템 테이블 뷰 컴포넌트
 * - 테이블 형식으로 아이템 목록 표시
 * - 컬럼 헤더 클릭 시 속성 관리
 * - 인라인 편집 지원
 * - 최소 15개 항목 표시 (Compact UI)
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useItemStore } from '@/stores/item'
import { usePropertyStore } from '@/stores/property'
import { useBoardStore } from '@/stores/board'
import { useDepartmentStore } from '@/stores/department'
import { useSlideOver } from '@/composables/useSlideOver'
import { useSortableDrag, createSubTaskCanDrop } from '@/composables/useSortableDrag'
import type { UserOption, DepartmentOption } from '@/components/common'
import { useToast } from '@/composables/useToast'
import { userApi } from '@/api/user'
import { boardApi } from '@/api/board'
import type { User } from '@/types/user'
import type { BoardCategory } from '@/types/board'
import { useConfirm } from '@/composables/useConfirm'
import { Spinner, EmptyState } from '@/components/common'
import PropertyHeader from '@/components/property/PropertyHeader.vue'
import ItemRow from './ItemRow.vue'
import SimpleCategoryModal from './SimpleCategoryModal.vue'
import { isItemOverdue } from '@/utils/item'
import type { Item, ItemUpdateRequest } from '@/types/item'
import type { PropertyDef } from '@/types/property'

type StatusFilter = 'all' | 'not_started' | 'in_progress' | 'overdue' | 'pending'

interface Props {
  boardId: number
  filter?: StatusFilter
  groupId?: number | null
}

const props = withDefaults(defineProps<Props>(), {
  filter: 'all',
  groupId: null
})

const emit = defineEmits<{
  (e: 'itemClick', item: Item): void
}>()

const itemStore = useItemStore()
const propertyStore = usePropertyStore()
const boardStore = useBoardStore()
const departmentStore = useDepartmentStore()
const { openItemDetail } = useSlideOver()
const toast = useToast()
const confirm = useConfirm()

// 모든 활성 사용자 목록
const allUsers = ref<User[]>([])

// 보드 카테고리 목록 (v2.0)
const boardCategories = ref<BoardCategory[]>([])

// 담당자 선택용 사용자 목록 (모든 활성 사용자)
const sharedUsers = computed<UserOption[]>(() => {
  return allUsers.value.map(u => ({
    username: u.username,
    userName: u.name || u.userName || u.username,
    departmentId: u.departmentId,
    departmentName: u.departmentName
  }))
})

// 부서 목록 (담당자 필터용)
const departments = computed<DepartmentOption[]>(() => {
  return departmentStore.activeFlatDepartments.map(d => ({
    departmentId: d.departmentId,
    departmentName: d.departmentName
  }))
})

// 상태
const selectedItemId = ref<number | null>(null)
const isLoading = ref(false)

// v2.0: 카테고리 변경 시 속성편집 모달 상태
const showPropertyModal = ref(false)
const modalTargetItem = ref<Item | null>(null)
const pendingCategoryId = ref<number | null>(null)

// 컬럼 너비 (기본값)
const propertyWidths = ref<Record<number, number>>({})

// 고정 컬럼 너비 관리 (localStorage 저장)
const COLUMN_WIDTHS_KEY = 'taskflow_column_widths'
const columnWidths = ref({
  title: 250,
  status: 70,
  priority: 60,
  assignee: 70,
  requestDate: 90,
  dueDate: 90,
  category: 80,
  comments: 50,
  actions: 70
})

// 최소/최대 컬럼 너비
const columnMinWidths = {
  title: 150,
  status: 50,
  priority: 50,
  assignee: 50,
  requestDate: 80,
  dueDate: 80,
  category: 60,
  comments: 40,
  actions: 60
}

// 리사이즈 상태
const isResizing = ref(false)
const resizingColumn = ref<string | null>(null)
const startX = ref(0)
const startWidth = ref(0)

// 컬럼 폭 로드 (localStorage)
function loadColumnWidths() {
  try {
    const saved = localStorage.getItem(COLUMN_WIDTHS_KEY)
    if (saved) {
      const parsed = JSON.parse(saved)
      columnWidths.value = { ...columnWidths.value, ...parsed }
    }
  } catch (e) {
    console.error('Failed to load column widths:', e)
  }
}

// 컬럼 폭 저장 (localStorage)
function saveColumnWidths() {
  try {
    localStorage.setItem(COLUMN_WIDTHS_KEY, JSON.stringify(columnWidths.value))
  } catch (e) {
    console.error('Failed to save column widths:', e)
  }
}

// 리사이즈 시작
function startResize(event: MouseEvent, column: string) {
  event.preventDefault()
  event.stopPropagation()

  isResizing.value = true
  resizingColumn.value = column
  startX.value = event.clientX
  startWidth.value = columnWidths.value[column as keyof typeof columnWidths.value]

  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'

  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
}

// 리사이즈 중
function handleResize(event: MouseEvent) {
  if (!isResizing.value || !resizingColumn.value) return

  const diff = event.clientX - startX.value
  const newWidth = Math.max(
    columnMinWidths[resizingColumn.value as keyof typeof columnMinWidths] || 50,
    startWidth.value + diff
  )

  columnWidths.value[resizingColumn.value as keyof typeof columnWidths.value] = newWidth
}

// 리사이즈 종료
function stopResize() {
  isResizing.value = false
  resizingColumn.value = null

  document.body.style.cursor = ''
  document.body.style.userSelect = ''

  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)

  saveColumnWidths()
}

// 컴포넌트 마운트 시 컬럼 폭 로드
onMounted(() => {
  loadColumnWidths()
})

// 기존 defaultColumnWidths (호환성 유지)
const defaultColumnWidths = columnWidths

// 드래그 중 로컬 순서 (useSortableDrag에서 사용)
const localItems = ref<Item[] | null>(null)

// v2.2: 확장된 아이템 ID 목록 (하위 업무 표시용)
const expandedItemIds = ref<Set<number>>(new Set())

// v2.2: 확장/축소 토글
function toggleExpand(itemId: number) {
  if (expandedItemIds.value.has(itemId)) {
    expandedItemIds.value.delete(itemId)
  } else {
    expandedItemIds.value.add(itemId)
  }
  // Set 변경 감지를 위해 새 Set 생성
  expandedItemIds.value = new Set(expandedItemIds.value)
}

// v2.2: 아이템 확장 여부 확인
function isExpanded(itemId: number): boolean {
  return expandedItemIds.value.has(itemId)
}

// v2.2: 루트 아이템 목록 (필터 적용 + sortOrder 정렬)
const rootItems = computed(() => {
  let items = itemStore.activeRootItems

  // 그룹 필터 적용
  if (props.groupId !== null && props.groupId !== undefined) {
    items = items.filter(i => i.groupId === props.groupId)
  }

  // 상태 필터 적용
  let filtered: Item[]
  switch (props.filter) {
    case 'not_started':
      filtered = items.filter(i => i.status === 'NOT_STARTED')
      break
    case 'in_progress':
      filtered = items.filter(i => i.status === 'IN_PROGRESS')
      break
    case 'overdue':
      filtered = items.filter(i => isItemOverdue(i))
      break
    case 'pending':
      filtered = items.filter(i => i.status === 'PENDING')
      break
    default:
      filtered = [...items]
  }

  // sortOrder 기준 정렬 (낮은 순서가 먼저)
  return [...filtered].sort((a, b) => (a.sortOrder ?? 999999) - (b.sortOrder ?? 999999))
})

// v2.2: 특정 부모의 하위 업무 가져오기 (활성 상태만)
function getActiveChildren(parentItemId: number): Item[] {
  return itemStore.getChildrenFromCache(parentItemId)
    .filter(item => item.status !== 'COMPLETED' && item.status !== 'DELETED')
    .sort((a, b) => (a.sortOrder ?? 999999) - (b.sortOrder ?? 999999))
}

// v2.2: 평탄화된 아이템 목록 (루트 + 확장된 부모의 하위 업무)
const flattenedItems = computed(() => {
  const result: Item[] = []

  function addItemWithChildren(item: Item) {
    result.push(item)

    // 확장된 아이템의 하위 업무 추가
    if (expandedItemIds.value.has(item.itemId) && (item.childCount ?? 0) > 0) {
      const children = getActiveChildren(item.itemId)
      for (const child of children) {
        addItemWithChildren(child) // 재귀적으로 하위 업무도 처리
      }
    }
  }

  for (const rootItem of rootItems.value) {
    addItemWithChildren(rootItem)
  }

  return result
})

// 아이템 목록 (필터 적용 + sortOrder 정렬)
const items = computed({
  get() {
    // 드래그 중에는 로컬 순서 사용
    if (localItems.value) {
      return localItems.value
    }

    // v2.2: 평탄화된 계층 구조 사용
    return flattenedItems.value
  },
  set(newItems: Item[]) {
    // useSortableDrag에서 드래그 중 로컬 순서 업데이트
    localItems.value = newItems
  }
})

// v2.2: 하위 업무 D&D 제약 적용
const subTaskCanDrop = createSubTaskCanDrop<Item>((message) => toast.warning(message))

// 드래그 앤 드롭 정렬 설정
const {
  draggedIndex,
  dragOverIndex,
  isDragging,
  isReordering,
  handleDragStart,
  handleDragOver,
  handleDragLeave,
  handleDragEnd: originalHandleDragEnd,
  handleDrop,
  getDragClass
} = useSortableDrag<Item>({
  items: items,
  itemKey: 'itemId',
  // v2.2: 하위 업무 D&D 제약 - 같은 부모 내에서만 이동 가능
  canDrop: subTaskCanDrop,
  onReorder: async (reorderedItems, fromIndex, toIndex) => {
    const movedItem = reorderedItems[toIndex]
    if (!movedItem) {
      // 로컬 상태 초기화
      localItems.value = null
      return
    }

    // 새 위치(인덱스)를 그대로 newOrder로 사용
    // 백엔드에서 다른 아이템들의 순서도 함께 조정함
    const newSortOrder = toIndex

    const success = await itemStore.reorderItem(props.boardId, movedItem.itemId, newSortOrder)

    // 로컬 상태 초기화 (store에서 갱신된 데이터 사용)
    localItems.value = null

    if (!success) {
      toast.error('순서 변경에 실패했습니다.')
    }
  }
})

// 드래그 종료 시 로컬 상태 초기화
function handleDragEnd(event?: DragEvent) {
  originalHandleDragEnd(event)
  localItems.value = null
}

// 완료/삭제된 아이템 (당일)
const todayCompletedItems = computed(() => itemStore.todayCompletedItems)

// 표시할 속성 목록 (v2.0: 기본 속성만 표시, GLOBAL/MANAGER/USER 속성 제외)
const visibleProperties = computed(() => {
  return propertyStore.sortedProperties.filter(prop => {
    // ownerType이 GLOBAL, MANAGER, USER인 경우 제외
    if (prop.ownerType === 'GLOBAL' || prop.ownerType === 'MANAGER' || prop.ownerType === 'USER') {
      return false
    }
    return true
  })
})

// 로딩 상태
const loading = computed(() => itemStore.loading || isLoading.value)

// 데이터 로드
async function loadData() {
  isLoading.value = true
  try {
    // 모든 활성 사용자 로드
    const userResponse = await userApi.getUsers({ useYn: 'Y', size: 1000 })
    if (userResponse.success && userResponse.data?.content) {
      allUsers.value = userResponse.data.content
    }

    // 보드 카테고리 로드 (v2.0)
    const categoryResponse = await boardApi.getBoardCategories(props.boardId)
    if (categoryResponse.success && categoryResponse.data) {
      boardCategories.value = categoryResponse.data
    }

    await Promise.all([
      itemStore.fetchItems(props.boardId),
      propertyStore.fetchProperties(props.boardId),
      departmentStore.fetchFlatDepartments({ useYn: 'Y' })
    ])

    // 속성 너비 초기화
    visibleProperties.value.forEach(prop => {
      if (!propertyWidths.value[prop.propertyId]) {
        propertyWidths.value[prop.propertyId] = 150
      }
    })
  } catch (error) {
    toast.error('데이터를 불러오는데 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

// 아이템 클릭 핸들러
function handleItemClick(item: Item) {
  selectedItemId.value = item.itemId
  openItemDetail(item.itemId, props.boardId)
  emit('itemClick', item)
}

// 아이템 업데이트 핸들러 (null 클리어 지원)
async function handleItemUpdate(itemId: number, field: string, value: unknown) {
  const data: Record<string, unknown> = { [field]: value }

  // null/undefined일 경우 clear_fields 추가 (snake_case)
  if (value === null || value === undefined) {
    data.clear_fields = [field]
  }

  const result = await itemStore.updateItem(props.boardId, itemId, data)
  if (!result) {
    toast.error('업데이트에 실패했습니다.')
  }
}

// 속성값 업데이트 핸들러
async function handlePropertyUpdate(itemId: number, propertyId: number, value: unknown) {
  const success = await itemStore.updateItemProperty(props.boardId, itemId, propertyId, value)
  if (!success) {
    toast.error('속성값 업데이트에 실패했습니다.')
  }
}

// 완료 처리 핸들러
async function handleComplete(itemId: number) {
  const success = await itemStore.completeItem(props.boardId, itemId)
  if (success) {
    toast.success('완료 처리되었습니다.')
  } else {
    toast.error('완료 처리에 실패했습니다.')
  }
}

// 삭제 처리 핸들러
async function handleDelete(itemId: number) {
  const confirmed = await confirm.show({
    title: '업무 삭제',
    message: '이 업무를 삭제하시겠습니까?',
    confirmText: '삭제',
    confirmType: 'danger'
  })

  if (confirmed) {
    const success = await itemStore.deleteItem(props.boardId, itemId)
    if (success) {
      toast.success('삭제되었습니다.')
    } else {
      toast.error('삭제에 실패했습니다.')
    }
  }
}

// 복원 처리 핸들러
async function handleRestore(itemId: number) {
  const success = await itemStore.restoreItem(props.boardId, itemId)
  if (success) {
    toast.success('복원되었습니다.')
  } else {
    toast.error('복원에 실패했습니다.')
  }
}

// 속성 이름 변경
// NOTE: 속성 관리 기능은 설정 > 코드 관리 메뉴에서 제공
function handlePropertyRename(propertyId: number) {
  toast.info('속성 이름 변경 기능 - 추후 구현')
}

// 속성 타입 변경
function handlePropertyTypeChange(propertyId: number) {
  toast.info('속성 타입 변경 기능 - 추후 구현')
}

// 속성 왼쪽 이동
async function handlePropertyMoveLeft(propertyId: number) {
  const properties = [...visibleProperties.value]
  const index = properties.findIndex(p => p.propertyId === propertyId)
  if (index > 0) {
    const newSortOrder = properties[index - 1].sortOrder ?? (index - 1)
    const currentSortOrder = properties[index].sortOrder ?? index
    await propertyStore.reorderProperty(propertyId, newSortOrder)
    await propertyStore.reorderProperty(properties[index - 1].propertyId, currentSortOrder)
  }
}

// 속성 오른쪽 이동
async function handlePropertyMoveRight(propertyId: number) {
  const properties = [...visibleProperties.value]
  const index = properties.findIndex(p => p.propertyId === propertyId)
  if (index < properties.length - 1) {
    const newSortOrder = properties[index + 1].sortOrder ?? (index + 1)
    const currentSortOrder = properties[index].sortOrder ?? index
    await propertyStore.reorderProperty(propertyId, newSortOrder)
    await propertyStore.reorderProperty(properties[index + 1].propertyId, currentSortOrder)
  }
}

// 속성 숨기기
async function handlePropertyHide(propertyId: number) {
  const success = await propertyStore.togglePropertyVisibility(propertyId)
  if (success) {
    toast.success('속성이 숨겨졌습니다.')
  }
}

// 속성 삭제
async function handlePropertyDelete(propertyId: number) {
  const confirmed = await confirm.show({
    title: '속성 삭제',
    message: '이 속성을 삭제하시겠습니까? 해당 속성의 모든 데이터가 삭제됩니다.',
    confirmText: '삭제',
    confirmType: 'danger'
  })

  if (confirmed) {
    const success = await propertyStore.deleteProperty(propertyId)
    if (success) {
      toast.success('속성이 삭제되었습니다.')
    } else {
      toast.error('속성 삭제에 실패했습니다.')
    }
  }
}

// 새 속성 추가
function handleAddProperty() {
  toast.info('새 속성 추가 기능 - 추후 구현')
}

// 정렬 핸들러
function handleSort(propertyId: number, direction: 'asc' | 'desc') {
  toast.info(`정렬: ${direction} - 추후 구현`)
}

// v2.0: 카테고리 변경 핸들러 (모달 오픈 또는 직접 클리어)
async function handleCategoryChange(item: Item, newCategoryId: number | null) {
  if (newCategoryId === null) {
    // null인 경우 바로 클리어 (모달 없이)
    await handleItemUpdate(item.itemId, 'categoryId', null)
    toast.success('카테고리가 해제되었습니다.')
    return
  }
  // 카테고리 선택 시 모달 열기
  modalTargetItem.value = item
  pendingCategoryId.value = newCategoryId
  showPropertyModal.value = true
}

// v2.0: 간단한 카테고리 모달에서 적용
async function handleCategoryApply(categoryId: number | null) {
  if (!modalTargetItem.value) return

  const updateData: ItemUpdateRequest = {
    categoryId: categoryId ?? undefined
  }

  const result = await itemStore.updateItem(props.boardId, modalTargetItem.value.itemId, updateData)
  if (result) {
    toast.success('카테고리가 수정되었습니다.')
  } else {
    toast.error('수정에 실패했습니다.')
  }

  // 모달 상태 초기화
  modalTargetItem.value = null
  pendingCategoryId.value = null
}

// v2.0: 카테고리 모달 취소
function handleCategoryCancel() {
  modalTargetItem.value = null
  pendingCategoryId.value = null
}

// boardId 변경 시 데이터 재로드
watch(() => props.boardId, () => {
  loadData()
}, { immediate: true })
</script>

<template>
  <div class="flex flex-col h-full bg-white rounded-lg border border-gray-200 overflow-hidden dark:bg-gray-800 dark:border-gray-700">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <Spinner size="lg" />
    </div>

    <!-- 빈 상태 -->
    <template v-else-if="items.length === 0">
      <EmptyState
        title="업무가 없습니다"
        description="새 업무를 추가해보세요."
        icon="clipboard"
        class="flex-1"
      />
    </template>

    <!-- 테이블 -->
    <template v-else>
      <div class="flex-1 overflow-auto">
        <!-- 테이블 헤더 -->
        <div class="sticky top-0 z-10 flex bg-gray-50 border-b border-gray-200 dark:bg-gray-900 dark:border-gray-700">
          <!-- 드래그 핸들 헤더 (빈 공간) -->
          <div class="w-6 h-8 flex-shrink-0 bg-gray-50 dark:bg-gray-900"></div>
          <!-- 제목 헤더 -->
          <div
            class="px-2 h-8 flex items-center text-[13px] font-medium text-gray-600 border-r border-gray-200 relative group dark:text-gray-300 dark:border-gray-700"
            :style="{ width: `${columnWidths.title}px`, minWidth: `${columnMinWidths.title}px` }"
          >
            <svg class="w-4 h-4 mr-1.5 text-gray-400 flex-shrink-0 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
            </svg>
            <span class="truncate">업무내용</span>
            <!-- 리사이즈 핸들 -->
            <div
              class="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary-400 transition-colors"
              :class="{ 'bg-primary-400': resizingColumn === 'title' }"
              @mousedown="startResize($event, 'title')"
            />
          </div>

          <!-- 상태 헤더 -->
          <div
            class="px-2 h-8 flex items-center text-[13px] font-medium text-gray-600 border-r border-gray-200 bg-gray-50 relative group dark:bg-gray-900 dark:text-gray-300 dark:border-gray-700"
            :style="{ width: `${columnWidths.status}px`, minWidth: `${columnMinWidths.status}px` }"
          >
            <span class="truncate">상태</span>
            <div
              class="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary-400 transition-colors"
              :class="{ 'bg-primary-400': resizingColumn === 'status' }"
              @mousedown="startResize($event, 'status')"
            />
          </div>

          <!-- 우선순위 헤더 -->
          <div
            class="px-2 h-8 flex items-center text-[13px] font-medium text-gray-600 border-r border-gray-200 bg-gray-50 relative group dark:bg-gray-900 dark:text-gray-300 dark:border-gray-700"
            :style="{ width: `${columnWidths.priority}px`, minWidth: `${columnMinWidths.priority}px` }"
          >
            <span class="truncate">우선순위</span>
            <div
              class="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary-400 transition-colors"
              :class="{ 'bg-primary-400': resizingColumn === 'priority' }"
              @mousedown="startResize($event, 'priority')"
            />
          </div>

          <!-- 담당자 헤더 -->
          <div
            class="px-2 h-8 flex items-center text-[13px] font-medium text-gray-600 border-r border-gray-200 bg-gray-50 relative group dark:bg-gray-900 dark:text-gray-300 dark:border-gray-700"
            :style="{ width: `${columnWidths.assignee}px`, minWidth: `${columnMinWidths.assignee}px` }"
          >
            <span class="truncate">담당자</span>
            <div
              class="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary-400 transition-colors"
              :class="{ 'bg-primary-400': resizingColumn === 'assignee' }"
              @mousedown="startResize($event, 'assignee')"
            />
          </div>

          <!-- 요청일 헤더 -->
          <div
            class="px-2 h-8 flex items-center text-[13px] font-medium text-gray-600 border-r border-gray-200 bg-gray-50 relative group dark:bg-gray-900 dark:text-gray-300 dark:border-gray-700"
            :style="{ width: `${columnWidths.requestDate}px`, minWidth: `${columnMinWidths.requestDate}px` }"
          >
            <span class="truncate">요청일</span>
            <div
              class="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary-400 transition-colors"
              :class="{ 'bg-primary-400': resizingColumn === 'requestDate' }"
              @mousedown="startResize($event, 'requestDate')"
            />
          </div>

          <!-- 마감일 헤더 -->
          <div
            class="px-2 h-8 flex items-center text-[13px] font-medium text-gray-600 border-r border-gray-200 bg-gray-50 relative group dark:bg-gray-900 dark:text-gray-300 dark:border-gray-700"
            :style="{ width: `${columnWidths.dueDate}px`, minWidth: `${columnMinWidths.dueDate}px` }"
          >
            <span class="truncate">마감일</span>
            <div
              class="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary-400 transition-colors"
              :class="{ 'bg-primary-400': resizingColumn === 'dueDate' }"
              @mousedown="startResize($event, 'dueDate')"
            />
          </div>

          <!-- 카테고리 헤더 -->
          <div
            class="px-2 h-8 flex items-center text-[13px] font-medium text-gray-600 border-r border-gray-200 bg-gray-50 relative group dark:bg-gray-900 dark:text-gray-300 dark:border-gray-700"
            :style="{ width: `${columnWidths.category}px`, minWidth: `${columnMinWidths.category}px` }"
          >
            <span class="truncate">카테고리</span>
            <div
              class="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary-400 transition-colors"
              :class="{ 'bg-primary-400': resizingColumn === 'category' }"
              @mousedown="startResize($event, 'category')"
            />
          </div>

          <!-- 동적 속성 헤더 -->
          <PropertyHeader
            v-for="property in visibleProperties"
            :key="property.propertyId"
            :property="property"
            :width="propertyWidths[property.propertyId] || 150"
            @rename="handlePropertyRename"
            @change-type="handlePropertyTypeChange"
            @move-left="handlePropertyMoveLeft"
            @move-right="handlePropertyMoveRight"
            @hide="handlePropertyHide"
            @delete="handlePropertyDelete"
            @add-property="handleAddProperty"
            @sort="handleSort"
          />

          <!-- 댓글 헤더 -->
          <div
            class="px-2 h-8 flex items-center justify-center text-[13px] font-medium text-gray-600 border-r border-gray-200 bg-gray-50 relative dark:bg-gray-900 dark:text-gray-300 dark:border-gray-700"
            :style="{ width: `${columnWidths.comments}px`, minWidth: `${columnMinWidths.comments}px` }"
          >
            <svg class="w-4 h-4 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <div
              class="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary-400 transition-colors"
              :class="{ 'bg-primary-400': resizingColumn === 'comments' }"
              @mousedown="startResize($event, 'comments')"
            />
          </div>

          <!-- 액션 헤더 -->
          <div
            class="px-2 h-8 flex items-center justify-center text-[13px] font-medium text-gray-600 bg-gray-50 dark:bg-gray-900 dark:text-gray-300"
            :style="{ width: `${columnWidths.actions}px`, minWidth: `${columnMinWidths.actions}px` }"
          >
            액션
          </div>
        </div>

        <!-- 테이블 바디 -->
        <div class="divide-y divide-gray-200 dark:divide-gray-700">
          <ItemRow
            v-for="(item, index) in items"
            :key="item.itemId"
            :item="item"
            :properties="visibleProperties"
            :property-widths="propertyWidths"
            :column-widths="columnWidths"
            :shared-users="sharedUsers"
            :departments="departments"
            :board-categories="boardCategories"
            :selected="selectedItemId === item.itemId"
            :draggable="true"
            :drag-class="getDragClass(index)"
            :is-drag-over="dragOverIndex === index && draggedIndex !== index"
            :expanded="isExpanded(item.itemId)"
            :has-children="(item.childCount ?? 0) > 0"
            @click="handleItemClick"
            @update="handleItemUpdate"
            @update-property="handlePropertyUpdate"
            @complete="handleComplete"
            @delete="handleDelete"
            @restore="handleRestore"
            @category-change="handleCategoryChange"
            @toggle-expand="toggleExpand"
            @dragstart="handleDragStart(index, $event)"
            @dragover="handleDragOver(index, $event)"
            @dragleave="handleDragLeave"
            @dragend="handleDragEnd($event)"
            @drop="handleDrop(index, $event)"
          />
        </div>

        <!-- 당일 완료/삭제된 아이템 (축소 가능) -->
        <div v-if="todayCompletedItems.length > 0" class="border-t border-gray-200 dark:border-gray-700">
          <details class="group">
            <summary class="px-4 py-2 text-[13px] text-gray-500 cursor-pointer hover:bg-gray-50 flex items-center gap-2 dark:text-gray-400 dark:hover:bg-gray-700">
              <svg
                class="w-4 h-4 transition-transform group-open:rotate-90"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
              오늘 처리된 업무 ({{ todayCompletedItems.length }}건)
            </summary>
            <div class="bg-gray-50 dark:bg-gray-900/50">
              <ItemRow
                v-for="item in todayCompletedItems"
                :key="item.itemId"
                :item="item"
                :properties="visibleProperties"
                :property-widths="propertyWidths"
                :column-widths="columnWidths"
                :shared-users="sharedUsers"
                :departments="departments"
                :board-categories="boardCategories"
                :selected="selectedItemId === item.itemId"
                :expanded="false"
                :has-children="(item.childCount ?? 0) > 0"
                @click="handleItemClick"
                @update="handleItemUpdate"
                @update-property="handlePropertyUpdate"
                @complete="handleComplete"
                @delete="handleDelete"
                @restore="handleRestore"
                @category-change="handleCategoryChange"
              />
            </div>
          </details>
        </div>
      </div>
    </template>

    <!-- v2.0: 간단한 카테고리 변경 모달 (cap_3.jpg 스타일) -->
    <SimpleCategoryModal
      v-if="modalTargetItem"
      v-model="showPropertyModal"
      :board-id="boardId"
      :initial-category-id="pendingCategoryId"
      :item-title="modalTargetItem.title"
      @apply="handleCategoryApply"
      @cancel="handleCategoryCancel"
    />
  </div>
</template>
