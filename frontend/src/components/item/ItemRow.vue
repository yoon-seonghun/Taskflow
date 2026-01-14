<script setup lang="ts">
/**
 * 아이템 테이블 행 컴포넌트
 * - 인라인 편집 지원
 * - 완료/삭제 버튼
 * - 행 클릭 시 상세 패널 오픈
 * Compact UI: height 36px, font 13px
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { Select, DatePicker, Badge, UserSelect } from '@/components/common'
import type { UserOption, DepartmentOption } from '@/components/common'
import { isItemOverdue, getOverdueDays } from '@/utils/item'
import type { Item, ItemStatus, Priority } from '@/types/item'
import type { PropertyDef, PropertyOption } from '@/types/property'
import type { BoardCategory } from '@/types/board'
import ItemBadges from './ItemBadges.vue'

interface ColumnWidths {
  title: number
  status: number
  priority: number
  assignee: number
  requestDate: number
  dueDate: number
  category: number
  comments: number
  actions: number
}

interface Props {
  item: Item
  properties: PropertyDef[]
  propertyWidths: Record<number, number>
  columnWidths?: ColumnWidths
  sharedUsers?: UserOption[]
  departments?: DepartmentOption[]
  boardCategories?: BoardCategory[]
  selected?: boolean
  isEditing?: boolean
  // 드래그 앤 드롭 관련
  draggable?: boolean
  dragClass?: Record<string, boolean>
  isDragOver?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selected: false,
  isEditing: false,
  boardCategories: () => [],
  columnWidths: () => ({
    title: 250,
    status: 70,
    priority: 60,
    assignee: 70,
    requestDate: 90,
    dueDate: 90,
    category: 80,
    comments: 50,
    actions: 70
  }),
  draggable: false,
  dragClass: () => ({}),
  isDragOver: false
})

const emit = defineEmits<{
  (e: 'click', item: Item): void
  (e: 'update', itemId: number, field: string, value: unknown): void
  (e: 'updateProperty', itemId: number, propertyId: number, value: unknown): void
  (e: 'complete', itemId: number): void
  (e: 'delete', itemId: number): void
  (e: 'restore', itemId: number): void
  (e: 'categoryChange', item: Item, newCategoryId: number | null): void  // v2.0: 카테고리 변경 시 모달 오픈
  // 드래그 앤 드롭 이벤트
  (e: 'dragstart', event: DragEvent): void
  (e: 'dragover', event: DragEvent): void
  (e: 'dragleave', event: DragEvent): void
  (e: 'dragend', event: DragEvent): void
  (e: 'drop', event: DragEvent): void
}>()

// 카테고리 옵션 (보드에 연결된 카테고리만 사용)
const categoryOptions = computed(() => {
  return props.boardCategories.map(c => ({
    value: c.categoryId,
    label: c.categoryName,
    color: c.categoryColor
  }))
})

// 인라인 편집 상태
const editingField = ref<string | null>(null)
const editValue = ref<unknown>(null)

// 속성 편집 상태
const editingPropertyId = ref<number | null>(null)
const editPropertyValue = ref<unknown>(null)

// 상태 옵션
const statusOptions = [
  { value: 'NOT_STARTED', label: '시작전', color: '#6B7280' },
  { value: 'IN_PROGRESS', label: '진행중', color: '#3B82F6' },
  { value: 'PENDING', label: '보류', color: '#F59E0B' },
  { value: 'COMPLETED', label: '완료', color: '#10B981' },
  { value: 'DELETED', label: '삭제', color: '#EF4444' }
]

// 우선순위 옵션
const priorityOptions = [
  { value: 'URGENT', label: '긴급', color: '#EF4444' },
  { value: 'HIGH', label: '높음', color: '#F97316' },
  { value: 'NORMAL', label: '보통', color: '#3B82F6' },
  { value: 'LOW', label: '낮음', color: '#6B7280' }
]

// 상태 배지 색상
const statusColors: Record<ItemStatus, string> = {
  NOT_STARTED: 'gray',
  IN_PROGRESS: 'blue',
  PENDING: 'yellow',
  COMPLETED: 'green',
  DELETED: 'red'
}

// 지연 여부
const isOverdue = computed(() => isItemOverdue(props.item))
const overdueDays = computed(() => getOverdueDays(props.item))

// 우선순위 배지 색상
const priorityColors: Record<Priority, string> = {
  URGENT: 'red',
  HIGH: 'orange',
  NORMAL: 'blue',
  LOW: 'gray'
}

// 완료/삭제된 아이템 여부
const isInactive = computed(() =>
  props.item.status === 'COMPLETED' || props.item.status === 'DELETED'
)

// 행 클래스 (Compact UI: 32px)
const rowClasses = computed(() => [
  'flex items-center h-8 border-b border-gray-200 hover:bg-gray-50 transition-colors cursor-pointer',
  props.selected ? 'bg-primary-50' : '',
  isInactive.value ? 'opacity-60' : '',
  props.dragClass?.['sortable-dragging'] ? 'opacity-50 bg-gray-100' : '',
  props.isDragOver ? 'border-t-2 border-t-primary-500' : ''
])

// 셀 클릭 핸들러 (토글 지원)
function handleCellClick(event: Event, field: string) {
  event.stopPropagation()

  // 같은 필드 클릭 시 토글
  if (editingField.value === field) {
    editingField.value = null
    editValue.value = null
    return
  }

  // 다른 필드 클릭 시 기존 편집 종료 후 새 편집 시작
  startEdit(field)
}

// 인라인 편집 시작
function startEdit(field: string) {
  if (isInactive.value) return

  editingField.value = field

  switch (field) {
    case 'title':
      editValue.value = props.item.title
      break
    case 'status':
      editValue.value = props.item.status
      break
    case 'priority':
      editValue.value = props.item.priority
      break
    case 'requestDate':
      editValue.value = props.item.requestDate
      break
    case 'dueDate':
      editValue.value = props.item.dueDate
      break
    case 'assigneeUsername':
      editValue.value = props.item.assigneeUsername
      break
  }
}

// 편집 완료
function finishEdit() {
  if (!editingField.value) return

  const field = editingField.value
  const value = editValue.value

  // 변경 사항이 있을 때만 업데이트
  if (value !== getFieldValue(field)) {
    emit('update', props.item.itemId, field, value)
  }

  editingField.value = null
  editValue.value = null
}

// 편집 취소
function cancelEdit() {
  editingField.value = null
  editValue.value = null
}

// 속성 편집 시작 (TEXT, NUMBER 타입용)
function startPropertyEdit(propertyId: number) {
  if (isInactive.value) return
  editingPropertyId.value = propertyId
  editPropertyValue.value = getPropertyValue(propertyId)
}

// 속성 편집 완료
function finishPropertyEdit(propertyId: number) {
  if (editingPropertyId.value !== propertyId) return

  const originalValue = getPropertyValue(propertyId)
  if (editPropertyValue.value !== originalValue) {
    emit('updateProperty', props.item.itemId, propertyId, editPropertyValue.value)
  }

  editingPropertyId.value = null
  editPropertyValue.value = null
}

// 속성 편집 취소
function cancelPropertyEdit() {
  editingPropertyId.value = null
  editPropertyValue.value = null
}

// 필드 값 가져오기
function getFieldValue(field: string): unknown {
  switch (field) {
    case 'title': return props.item.title
    case 'status': return props.item.status
    case 'priority': return props.item.priority
    case 'requestDate': return props.item.requestDate
    case 'dueDate': return props.item.dueDate
    case 'assigneeUsername': return props.item.assigneeUsername
    default: return null
  }
}

// Select 변경 핸들러
function handleSelectChange(field: string, value: string | number | null) {
  editValue.value = value
  emit('update', props.item.itemId, field, value)
  editingField.value = null
}

// v2.0: 카테고리 변경 핸들러 (속성편집 모달 오픈)
function handleCategoryChange(value: string | number | null) {
  emit('categoryChange', props.item, value as number | null)
}

// 날짜 변경 핸들러
function handleDateChange(field: string, value: string | null) {
  editValue.value = value
  emit('update', props.item.itemId, field, value)
  editingField.value = null
}

// 속성값 변경 핸들러
function handlePropertyChange(propertyId: number, value: unknown) {
  emit('updateProperty', props.item.itemId, propertyId, value)
}

// TB_ITEM status를 option_id로 매핑
const statusToOptionId: Record<string, number> = {
  'NOT_STARTED': 10,
  'IN_PROGRESS': 11,
  'COMPLETED': 12,
  'DELETED': 13,
  'PENDING': 10  // 대기 상태는 시작전으로 매핑
}

// TB_ITEM priority를 option_id로 매핑
const priorityToOptionId: Record<string, number> = {
  'URGENT': 20,
  'HIGH': 21,
  'NORMAL': 22,
  'LOW': 23
}

// 속성값 가져오기 (TB_ITEM_PROPERTY → TB_ITEM 고정 필드 fallback)
function getPropertyValue(propertyId: number): unknown {
  // 1. 먼저 TB_ITEM_PROPERTY에서 값 확인
  const propValue = props.item.propertyValues?.[propertyId]
  if (propValue !== undefined && propValue !== null) {
    return propValue
  }

  // 2. TB_ITEM 고정 필드에서 fallback (속성 정의 참조)
  const property = props.properties.find(p => p.propertyId === propertyId)
  if (!property) return null

  // 상태 속성 (propertyName === '상태' 또는 시스템 상태 속성)
  if (property.propertyName === '상태' && props.item.status) {
    return statusToOptionId[props.item.status] ?? null
  }

  // 우선순위 속성
  if (property.propertyName === '우선순위' && props.item.priority) {
    return priorityToOptionId[props.item.priority] ?? null
  }

  // 담당자 속성
  if (property.propertyType === 'USER' && props.item.assigneeUsername) {
    return props.item.assigneeUsername
  }

  // 요청일/마감일은 TB_ITEM 고정 컬럼으로 처리됨 (TB_PROPERTY_DEF에 없음)

  return null
}

// 속성 옵션 가져오기
function getPropertyOptions(property: PropertyDef): Array<{ value: string | number; label: string; color?: string }> {
  if (!property.options) return []
  return property.options
    .filter(opt => opt.useYn === 'Y')
    .map(opt => ({
      value: opt.optionId,
      label: opt.optionName,
      color: opt.color
    }))
}

// 날짜 포맷 (상세)
function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 날짜 포맷 (간략 - 테이블 셀용)
function formatDateShort(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).replace(/\. /g, '-').replace('.', '')
}

// 완료 처리
function handleComplete() {
  emit('complete', props.item.itemId)
}

// 삭제 처리
function handleDelete() {
  emit('delete', props.item.itemId)
}

// 복원 처리
function handleRestore() {
  emit('restore', props.item.itemId)
}

// 행 클릭 (상세 패널 열기)
function handleRowClick() {
  emit('click', props.item)
}

// 드래그 앤 드롭 이벤트 핸들러
function handleDragStart(event: DragEvent) {
  if (!props.draggable) return
  emit('dragstart', event)
}

function handleDragOver(event: DragEvent) {
  if (!props.draggable) return
  event.preventDefault()
  emit('dragover', event)
}

function handleDragLeave(event: DragEvent) {
  if (!props.draggable) return
  emit('dragleave', event)
}

function handleDragEnd(event: DragEvent) {
  if (!props.draggable) return
  emit('dragend', event)
}

function handleDrop(event: DragEvent) {
  if (!props.draggable) return
  event.preventDefault()
  emit('drop', event)
}

// 행 ref (외부 클릭 감지용)
const rowRef = ref<HTMLElement | null>(null)

// 외부 클릭 감지 (팝오버 내 요소 클릭은 무시)
function handleDocumentClick(event: MouseEvent) {
  if (!rowRef.value) return
  if (!editingField.value && !editingPropertyId.value) return

  const target = event.target as HTMLElement

  // 행 내부 클릭이면 무시
  if (rowRef.value.contains(target)) return

  // closest로 팝오버/드롭다운 요소 확인 (DOM에서 제거되기 전에 확인)
  try {
    if (target.closest('[data-item-popover], [data-popover]')) {
      return
    }
  } catch {
    // DOM에서 이미 제거된 경우 무시
  }

  // 현재 값 저장 (다른 핸들러에서 변경했는지 확인용)
  const currentField = editingField.value
  const currentPropertyId = editingPropertyId.value

  // 외부 클릭 - 편집 모드 종료 (다른 핸들러가 먼저 처리했으면 무시)
  setTimeout(() => {
    // handleSelectChange 등에서 이미 처리했으면 무시
    if (editingField.value !== currentField || editingPropertyId.value !== currentPropertyId) {
      return
    }
    editingField.value = null
    editingPropertyId.value = null
  }, 100)
}

onMounted(() => {
  // capture 사용 - 다른 행 클릭 시에도 현재 행의 팝오버를 닫기 위함
  // capture 단계에서 먼저 실행되어 .stop으로 막힌 이벤트도 감지 가능
  document.addEventListener('click', handleDocumentClick, true)
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick, true)
})
</script>

<template>
  <div
    ref="rowRef"
    :class="rowClasses"
    :draggable="draggable"
    @click="handleRowClick"
    @dragstart="handleDragStart"
    @dragover="handleDragOver"
    @dragleave="handleDragLeave"
    @dragend="handleDragEnd"
    @drop="handleDrop"
  >
    <!-- 드래그 핸들 (draggable일 때만 표시) -->
    <div
      v-if="draggable"
      class="w-6 h-full flex items-center justify-center flex-shrink-0 cursor-grab hover:bg-gray-100 text-gray-400 hover:text-gray-600"
    >
      <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
        <path d="M8 6a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM8 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM8 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM14 6a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM14 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM14 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0z" />
      </svg>
    </div>
    <!-- 제목 컬럼 -->
    <div
      class="group/title px-2 flex items-center gap-2 h-full border-r border-gray-200 relative"
      :style="{ width: `${columnWidths.title}px`, minWidth: '150px' }"
      @click.stop="handleCellClick($event, 'title')"
    >
      <!-- 인라인 편집 -->
      <template v-if="editingField === 'title'">
        <input
          v-model="editValue"
          type="text"
          class="w-full h-7 px-2 text-[13px] border border-primary-500 rounded focus:outline-none focus:ring-1 focus:ring-primary-500"
          @blur="finishEdit"
          @keydown.enter="finishEdit"
          @keydown.escape="cancelEdit"
          @click.stop
          autofocus
        />
      </template>
      <template v-else>
        <!-- 공유/이관 배지 -->
        <ItemBadges :item="item" size="sm" :show-owner-name="false" class="mr-1" />
        <span class="truncate text-[13px] text-gray-900 flex-1">{{ item.title }}</span>
        <!-- 지연 표시 -->
        <span
          v-if="isOverdue"
          class="flex-shrink-0 inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-medium text-red-700 bg-red-100 rounded"
          :title="`마감일 ${overdueDays}일 초과`"
        >
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          {{ overdueDays }}일 지연
        </span>
        <!-- Notion 스타일 '열기' 버튼 (hover 시 표시) -->
        <button
          class="flex-shrink-0 opacity-0 group-hover/title:opacity-100 transition-opacity px-1.5 py-0.5 text-[11px] text-gray-500 hover:text-primary-600 hover:bg-primary-50 rounded"
          title="상세 패널 열기"
          @click.stop="handleRowClick"
        >
          열기
        </button>
      </template>
    </div>

    <!-- 상태 컬럼 - 텍스트 라벨 + 클릭 시 팝오버 -->
    <div
      class="px-2 flex items-center h-full border-r border-gray-200 relative"
      :style="{ width: `${columnWidths.status}px`, minWidth: '50px' }"
      @click.stop="handleCellClick($event, 'status')"
    >
      <!-- 텍스트 라벨 (기본 표시) -->
      <span
        v-if="editingField !== 'status'"
        class="text-[13px] cursor-pointer hover:text-primary-600 truncate"
        :style="{ color: statusOptions.find(o => o.value === item.status)?.color }"
        :title="statusOptions.find(o => o.value === item.status)?.label"
      >
        {{ statusOptions.find(o => o.value === item.status)?.label || '-' }}
      </span>
      <!-- 팝오버 에디터 -->
      <div v-else data-item-popover @click.stop class="absolute left-0 top-full mt-1 z-50 bg-white shadow-lg rounded border border-gray-200 p-1" style="min-width: 100px;">
        <Select
          :model-value="item.status"
          :options="statusOptions"
          size="sm"
          placeholder="-"
          class="w-full"
          @update:model-value="handleSelectChange('status', $event)"
        />
      </div>
    </div>

    <!-- 우선순위 컬럼 - 텍스트 라벨 + 클릭 시 팝오버 -->
    <div
      class="px-2 flex items-center h-full border-r border-gray-200 relative"
      :style="{ width: `${columnWidths.priority}px`, minWidth: '50px' }"
      @click.stop="handleCellClick($event, 'priority')"
    >
      <!-- 텍스트 라벨 (기본 표시) -->
      <span
        v-if="editingField !== 'priority'"
        class="text-[13px] cursor-pointer hover:text-primary-600 truncate"
        :style="{ color: priorityOptions.find(o => o.value === item.priority)?.color }"
        :title="priorityOptions.find(o => o.value === item.priority)?.label"
      >
        {{ priorityOptions.find(o => o.value === item.priority)?.label || '-' }}
      </span>
      <!-- 팝오버 에디터 -->
      <div v-else data-item-popover @click.stop class="absolute left-0 top-full mt-1 z-50 bg-white shadow-lg rounded border border-gray-200 p-1" style="min-width: 90px;">
        <Select
          :model-value="item.priority"
          :options="priorityOptions"
          size="sm"
          placeholder="-"
          class="w-28"
          @update:model-value="handleSelectChange('priority', $event)"
        />
      </div>
    </div>

    <!-- 담당자 컬럼 - 텍스트 라벨 + 클릭 시 팝오버 -->
    <div
      class="px-2 flex items-center h-full border-r border-gray-200 relative"
      :style="{ width: `${columnWidths.assignee}px`, minWidth: '50px' }"
      @click.stop="handleCellClick($event, 'assigneeUsername')"
    >
      <!-- 텍스트 라벨 (기본 표시) -->
      <span
        v-if="editingField !== 'assigneeUsername'"
        class="text-[13px] cursor-pointer hover:text-primary-600 truncate text-gray-700"
        :title="sharedUsers?.find(u => u.username === item.assigneeUsername)?.userName || item.assigneeName"
      >
        {{ sharedUsers?.find(u => u.username === item.assigneeUsername)?.userName || item.assigneeName || '-' }}
      </span>
      <!-- 팝오버 에디터 -->
      <div v-else data-item-popover @click.stop class="absolute left-0 top-full mt-1 z-50 bg-white shadow-lg rounded border border-gray-200 p-1" style="min-width: 180px;">
        <UserSelect
          :model-value="item.assigneeUsername"
          :users="sharedUsers || []"
          :departments="departments || []"
          size="sm"
          placeholder="-"
          clearable
          class="w-36"
          @update:model-value="handleSelectChange('assigneeUsername', $event)"
        />
      </div>
    </div>

    <!-- 요청일 컬럼 - 텍스트 라벨 + 클릭 시 팝오버 -->
    <div
      class="px-2 flex items-center h-full border-r border-gray-200 relative"
      :style="{ width: `${columnWidths.requestDate}px`, minWidth: '80px' }"
      @click.stop="handleCellClick($event, 'requestDate')"
    >
      <!-- 텍스트 라벨 (기본 표시) -->
      <span
        v-if="editingField !== 'requestDate'"
        class="text-[13px] cursor-pointer hover:text-primary-600 truncate text-gray-700"
      >
        {{ formatDateShort(item.requestDate) }}
      </span>
      <!-- 팝오버 에디터 -->
      <div v-else data-item-popover @click.stop class="absolute left-0 top-full mt-1 z-50 bg-white shadow-lg rounded border border-gray-200 p-1" style="min-width: 160px;">
        <DatePicker
          :model-value="item.requestDate"
          size="sm"
          placeholder="-"
          clearable
          class="w-36"
          @update:model-value="handleSelectChange('requestDate', $event)"
        />
      </div>
    </div>

    <!-- 마감일 컬럼 - 텍스트 라벨 + 클릭 시 팝오버 -->
    <div
      class="px-2 flex items-center h-full border-r border-gray-200 relative"
      :style="{ width: `${columnWidths.dueDate}px`, minWidth: '80px' }"
      @click.stop="handleCellClick($event, 'dueDate')"
    >
      <!-- 텍스트 라벨 (기본 표시) -->
      <span
        v-if="editingField !== 'dueDate'"
        class="text-[13px] cursor-pointer hover:text-primary-600 truncate text-gray-700"
      >
        {{ formatDateShort(item.dueDate) }}
      </span>
      <!-- 팝오버 에디터 -->
      <div v-else data-item-popover @click.stop class="absolute left-0 top-full mt-1 z-50 bg-white shadow-lg rounded border border-gray-200 p-1" style="min-width: 160px;">
        <DatePicker
          :model-value="item.dueDate"
          size="sm"
          placeholder="-"
          clearable
          class="w-36"
          @update:model-value="handleSelectChange('dueDate', $event)"
        />
      </div>
    </div>

    <!-- 카테고리 컬럼 - 텍스트 라벨 + 클릭 시 팝오버 -->
    <div
      class="px-2 flex items-center h-full border-r border-gray-200 relative"
      :style="{ width: `${columnWidths.category}px`, minWidth: '60px' }"
      @click.stop="handleCellClick($event, 'categoryId')"
    >
      <!-- 텍스트 라벨 (기본 표시) -->
      <span
        v-if="editingField !== 'categoryId'"
        class="text-[13px] cursor-pointer hover:text-primary-600 truncate"
        :style="{ color: categoryOptions.find(o => o.value === item.categoryId)?.color }"
        :title="categoryOptions.find(o => o.value === item.categoryId)?.label"
      >
        {{ categoryOptions.find(o => o.value === item.categoryId)?.label || '-' }}
      </span>
      <!-- 팝오버 에디터 -->
      <div v-else data-item-popover @click.stop class="absolute left-0 top-full mt-1 z-50 bg-white shadow-lg rounded border border-gray-200 p-1" style="min-width: 120px;">
        <Select
          :model-value="item.categoryId"
          :options="categoryOptions"
          size="sm"
          placeholder="-"
          clearable
          class="w-28"
          @update:model-value="handleCategoryChange($event)"
        />
      </div>
    </div>

    <!-- 동적 속성 컬럼들 -->
    <template v-for="property in properties" :key="property.propertyId">
      <div
        class="px-2 flex items-center h-full border-r border-gray-200"
        :style="{
          width: `${propertyWidths[property.propertyId] || 150}px`,
          minWidth: `${propertyWidths[property.propertyId] || 150}px`
        }"
        @click.stop="startPropertyEdit(property.propertyId)"
      >
        <!-- TEXT 타입 -->
        <template v-if="property.propertyType === 'TEXT'">
          <!-- 인라인 편집 모드 -->
          <template v-if="editingPropertyId === property.propertyId">
            <input
              v-model="editPropertyValue"
              type="text"
              class="w-full h-7 px-2 text-[13px] border border-primary-500 rounded focus:outline-none focus:ring-1 focus:ring-primary-500"
              @blur="finishPropertyEdit(property.propertyId)"
              @keydown.enter="finishPropertyEdit(property.propertyId)"
              @keydown.escape="cancelPropertyEdit"
              @click.stop
              autofocus
            />
          </template>
          <template v-else>
            <span class="truncate text-[13px] text-gray-700 cursor-pointer hover:text-primary-600">
              {{ getPropertyValue(property.propertyId) || '-' }}
            </span>
          </template>
        </template>

        <!-- NUMBER 타입 -->
        <template v-else-if="property.propertyType === 'NUMBER'">
          <!-- 인라인 편집 모드 -->
          <template v-if="editingPropertyId === property.propertyId">
            <input
              v-model.number="editPropertyValue"
              type="number"
              class="w-full h-7 px-2 text-[13px] border border-primary-500 rounded focus:outline-none focus:ring-1 focus:ring-primary-500"
              @blur="finishPropertyEdit(property.propertyId)"
              @keydown.enter="finishPropertyEdit(property.propertyId)"
              @keydown.escape="cancelPropertyEdit"
              @click.stop
              autofocus
            />
          </template>
          <template v-else>
            <span class="truncate text-[13px] text-gray-700 cursor-pointer hover:text-primary-600">
              {{ getPropertyValue(property.propertyId) ?? '-' }}
            </span>
          </template>
        </template>

        <!-- DATE 타입 -->
        <template v-else-if="property.propertyType === 'DATE'">
          <DatePicker
            :model-value="getPropertyValue(property.propertyId) as string"
            size="sm"
            placeholder="-"
            clearable
            class="w-full"
            @update:model-value="handlePropertyChange(property.propertyId, $event)"
            @click.stop
          />
        </template>

        <!-- SELECT 타입 -->
        <template v-else-if="property.propertyType === 'SELECT'">
          <Select
            :model-value="getPropertyValue(property.propertyId) as number"
            :options="getPropertyOptions(property)"
            size="sm"
            placeholder="-"
            clearable
            class="w-full"
            @update:model-value="handlePropertyChange(property.propertyId, $event)"
            @click.stop
          />
        </template>

        <!-- MULTI_SELECT 타입 -->
        <template v-else-if="property.propertyType === 'MULTI_SELECT'">
          <div class="flex flex-wrap gap-1">
            <Badge
              v-for="optId in (getPropertyValue(property.propertyId) as number[] || [])"
              :key="optId"
              size="sm"
              variant="gray"
            >
              {{ property.options?.find(o => o.optionId === optId)?.optionName }}
            </Badge>
          </div>
        </template>

        <!-- CHECKBOX 타입 -->
        <template v-else-if="property.propertyType === 'CHECKBOX'">
          <input
            type="checkbox"
            :checked="getPropertyValue(property.propertyId) as boolean"
            class="w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
            @change="handlePropertyChange(property.propertyId, ($event.target as HTMLInputElement).checked)"
            @click.stop
          />
        </template>

        <!-- USER 타입 -->
        <template v-else-if="property.propertyType === 'USER'">
          <UserSelect
            :model-value="getPropertyValue(property.propertyId) as string"
            :users="sharedUsers || []"
            :departments="departments || []"
            size="sm"
            placeholder="-"
            clearable
            class="w-full"
            @update:model-value="handlePropertyChange(property.propertyId, $event)"
            @click.stop
          />
        </template>
      </div>
    </template>

    <!-- 댓글 수 컬럼 -->
    <div
      class="px-2 flex items-center justify-center h-full border-r border-gray-200"
      :style="{ width: `${columnWidths.comments}px`, minWidth: '40px' }"
    >
      <div class="flex items-center gap-1 text-[13px] text-gray-500">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <span>{{ item.commentCount || 0 }}</span>
      </div>
    </div>

    <!-- 액션 버튼 컬럼 -->
    <div
      class="px-2 flex items-center justify-center gap-1 h-full"
      :style="{ width: `${columnWidths.actions}px`, minWidth: '60px' }"
    >
      <template v-if="isInactive">
        <!-- 복원 버튼 -->
        <button
          class="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded transition-colors"
          title="복원"
          @click.stop="handleRestore"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
        </button>
      </template>
      <template v-else>
        <!-- 완료 버튼 -->
        <button
          class="p-1.5 text-gray-400 hover:text-green-600 hover:bg-green-50 rounded transition-colors"
          title="완료"
          @click.stop="handleComplete"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
        </button>
        <!-- 삭제 버튼 -->
        <button
          class="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
          title="삭제"
          @click.stop="handleDelete"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </button>
      </template>
    </div>
  </div>
</template>
