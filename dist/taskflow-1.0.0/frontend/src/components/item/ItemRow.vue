<script setup lang="ts">
/**
 * 아이템 테이블 행 컴포넌트
 * - 인라인 편집 지원
 * - 완료/삭제 버튼
 * - 행 클릭 시 상세 패널 오픈
 * Compact UI: height 36px, font 13px
 */
import { ref, computed, watch } from 'vue'
import { Select, DatePicker, Badge, UserSelect } from '@/components/common'
import type { UserOption, DepartmentOption } from '@/components/common'
import { isItemOverdue, getOverdueDays } from '@/utils/item'
import type { Item, ItemStatus, Priority } from '@/types/item'
import type { PropertyDef, PropertyOption } from '@/types/property'

interface Props {
  item: Item
  properties: PropertyDef[]
  propertyWidths: Record<number, number>
  sharedUsers?: UserOption[]
  departments?: DepartmentOption[]
  selected?: boolean
  isEditing?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selected: false,
  isEditing: false
})

const emit = defineEmits<{
  (e: 'click', item: Item): void
  (e: 'update', itemId: number, field: string, value: unknown): void
  (e: 'updateProperty', itemId: number, propertyId: number, value: unknown): void
  (e: 'complete', itemId: number): void
  (e: 'delete', itemId: number): void
  (e: 'restore', itemId: number): void
}>()

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
  { value: 'PENDING', label: '대기', color: '#F59E0B' },
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
  isInactive.value ? 'opacity-60' : ''
])

// 셀 클릭 핸들러
function handleCellClick(event: Event, field: string) {
  event.stopPropagation()
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
    case 'startTime':
      editValue.value = props.item.startTime
      break
    case 'endTime':
      editValue.value = props.item.endTime
      break
    case 'assigneeId':
      editValue.value = props.item.assigneeId
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
    case 'startTime': return props.item.startTime
    case 'endTime': return props.item.endTime
    case 'assigneeId': return props.item.assigneeId
    default: return null
  }
}

// Select 변경 핸들러
function handleSelectChange(field: string, value: string | number | null) {
  editValue.value = value
  emit('update', props.item.itemId, field, value)
  editingField.value = null
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
  if (property.propertyType === 'USER' && props.item.assigneeId) {
    return props.item.assigneeId
  }

  // 시작일 속성
  if (property.propertyName === '시작일' && props.item.startTime) {
    return props.item.startTime
  }

  // 마감일 속성
  if (property.propertyName === '마감일' && props.item.endTime) {
    return props.item.endTime
  }

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

// 날짜 포맷
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
</script>

<template>
  <div :class="rowClasses" @click="handleRowClick">
    <!-- 제목 컬럼 (고정) -->
    <div
      class="group/title flex-1 min-w-[200px] px-2 flex items-center gap-2 h-full border-r border-gray-200 relative"
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
            :model-value="getPropertyValue(property.propertyId) as number"
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
    <div class="w-16 min-w-[64px] px-2 flex items-center justify-center h-full border-r border-gray-200">
      <div class="flex items-center gap-1 text-[13px] text-gray-500">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <span>{{ item.commentCount || 0 }}</span>
      </div>
    </div>

    <!-- 액션 버튼 컬럼 -->
    <div class="w-20 min-w-[80px] px-2 flex items-center justify-center gap-1 h-full">
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
