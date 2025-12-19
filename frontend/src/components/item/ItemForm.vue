<script setup lang="ts">
/**
 * 아이템 편집 폼 컴포넌트
 * - 기본 속성 (제목, 상태, 우선순위, 담당자, 그룹, 마감일)
 * - 동적 속성 편집
 * - 모든 속성 인라인 편집
 * Compact UI 적용
 */
import { ref, computed, watch } from 'vue'
import { usePropertyStore } from '@/stores/property'
import { useBoardStore } from '@/stores/board'
import { Input, Select, DatePicker } from '@/components/common'
import { PropertyEditor } from '@/components/property'
import type { Item, ItemStatus, Priority, ItemUpdateRequest } from '@/types/item'
import type { SelectOption } from '@/components/common/Select.vue'

interface Props {
  item: Item
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emit = defineEmits<{
  (e: 'update', data: ItemUpdateRequest): void
  (e: 'change', field: string, value: unknown): void
}>()

const propertyStore = usePropertyStore()
const boardStore = useBoardStore()

// 폼 데이터
const formData = ref<ItemUpdateRequest>({
  title: props.item.title,
  content: props.item.content,
  status: props.item.status,
  priority: props.item.priority,
  groupId: props.item.groupId,
  assigneeId: props.item.assigneeId,
  startTime: props.item.startTime,
  endTime: props.item.endTime,
  dueDate: props.item.dueDate,
  properties: props.item.propertyValues || {}
})

// item 변경 시 폼 데이터 동기화
watch(() => props.item, (newItem) => {
  formData.value = {
    title: newItem.title,
    content: newItem.content,
    status: newItem.status,
    priority: newItem.priority,
    groupId: newItem.groupId,
    assigneeId: newItem.assigneeId,
    startTime: newItem.startTime,
    endTime: newItem.endTime,
    dueDate: newItem.dueDate,
    properties: newItem.propertyValues || {}
  }
}, { deep: true })

// 상태 옵션
const statusOptions: SelectOption[] = [
  { value: 'NOT_STARTED', label: '시작전', color: '#6B7280' },
  { value: 'IN_PROGRESS', label: '진행중', color: '#3B82F6' },
  { value: 'COMPLETED', label: '완료', color: '#10B981' },
  { value: 'DELETED', label: '삭제', color: '#EF4444' }
]

// 우선순위 옵션
const priorityOptions: SelectOption[] = [
  { value: 'URGENT', label: '긴급', color: '#EF4444' },
  { value: 'HIGH', label: '높음', color: '#F97316' },
  { value: 'NORMAL', label: '보통', color: '#3B82F6' },
  { value: 'LOW', label: '낮음', color: '#6B7280' }
]

// 그룹 옵션
const groupOptions = computed((): SelectOption[] => {
  const board = boardStore.currentBoard
  if (!board?.groups) return []
  return board.groups.map(g => ({
    value: g.groupId,
    label: g.groupName,
    color: g.color
  }))
})

// 담당자 옵션 (공유 사용자 포함)
const assigneeOptions = computed((): SelectOption[] => {
  const board = boardStore.currentBoard
  if (!board?.sharedUsers) return []
  return board.sharedUsers.map(u => ({
    value: u.userId,
    label: u.userName
  }))
})

// 동적 속성 목록 (시스템 속성 제외, 정렬순서대로)
const customProperties = computed(() => {
  return propertyStore.sortedProperties.filter(p => p.systemYn !== 'Y')
})

// 필드 변경 핸들러
function handleFieldChange<K extends keyof ItemUpdateRequest>(
  field: K,
  value: ItemUpdateRequest[K]
) {
  formData.value[field] = value
  emit('change', field, value)
  emit('update', formData.value)
}

// 상태 변경
function handleStatusChange(value: string | number | null) {
  handleFieldChange('status', value as ItemStatus)
}

// 우선순위 변경
function handlePriorityChange(value: string | number | null) {
  handleFieldChange('priority', value as Priority)
}

// 그룹 변경
function handleGroupChange(value: string | number | null) {
  handleFieldChange('groupId', value as number | undefined)
}

// 담당자 변경
function handleAssigneeChange(value: string | number | null) {
  handleFieldChange('assigneeId', value as number | undefined)
}

// 날짜 변경
function handleDateChange(field: 'startTime' | 'endTime' | 'dueDate', value: string | null) {
  handleFieldChange(field, value || undefined)
}

// 동적 속성 변경
function handlePropertyChange(propertyId: number, value: unknown) {
  const properties = { ...formData.value.properties, [propertyId]: value }
  formData.value.properties = properties
  emit('change', `property_${propertyId}`, value)
  emit('update', formData.value)
}

// 속성값 가져오기
function getPropertyValue(propertyId: number): unknown {
  return formData.value.properties?.[propertyId] ?? null
}
</script>

<template>
  <div class="item-form space-y-4">
    <!-- 제목 -->
    <div class="form-section">
      <label class="form-label">제목</label>
      <Input
        :model-value="formData.title"
        :disabled="disabled"
        placeholder="업무 제목을 입력하세요"
        @update:model-value="handleFieldChange('title', $event as string)"
      />
    </div>

    <!-- 기본 속성 그리드 -->
    <div class="grid grid-cols-2 gap-3">
      <!-- 상태 -->
      <div class="form-section">
        <label class="form-label">상태</label>
        <Select
          :model-value="formData.status"
          :options="statusOptions"
          :disabled="disabled"
          placeholder="상태 선택"
          @update:model-value="handleStatusChange"
        />
      </div>

      <!-- 우선순위 -->
      <div class="form-section">
        <label class="form-label">우선순위</label>
        <Select
          :model-value="formData.priority"
          :options="priorityOptions"
          :disabled="disabled"
          placeholder="우선순위 선택"
          @update:model-value="handlePriorityChange"
        />
      </div>

      <!-- 그룹 -->
      <div class="form-section">
        <label class="form-label">그룹</label>
        <Select
          :model-value="formData.groupId"
          :options="groupOptions"
          :disabled="disabled"
          placeholder="그룹 선택"
          clearable
          @update:model-value="handleGroupChange"
        />
      </div>

      <!-- 담당자 -->
      <div class="form-section">
        <label class="form-label">담당자</label>
        <Select
          :model-value="formData.assigneeId"
          :options="assigneeOptions"
          :disabled="disabled"
          placeholder="담당자 선택"
          searchable
          clearable
          @update:model-value="handleAssigneeChange"
        />
      </div>

      <!-- 시작일 -->
      <div class="form-section">
        <label class="form-label">시작일</label>
        <DatePicker
          :model-value="formData.startTime"
          :disabled="disabled"
          mode="datetime"
          placeholder="시작일 선택"
          clearable
          @update:model-value="handleDateChange('startTime', $event)"
        />
      </div>

      <!-- 완료일 -->
      <div class="form-section">
        <label class="form-label">완료일</label>
        <DatePicker
          :model-value="formData.endTime"
          :disabled="disabled"
          mode="datetime"
          placeholder="완료일 선택"
          clearable
          @update:model-value="handleDateChange('endTime', $event)"
        />
      </div>

      <!-- 마감일 -->
      <div class="form-section col-span-2">
        <label class="form-label">마감일</label>
        <DatePicker
          :model-value="formData.dueDate"
          :disabled="disabled"
          mode="date"
          placeholder="마감일 선택"
          clearable
          @update:model-value="handleDateChange('dueDate', $event)"
        />
      </div>
    </div>

    <!-- 동적 속성 -->
    <div v-if="customProperties.length > 0" class="pt-3 border-t border-gray-200">
      <h4 class="text-[12px] font-medium text-gray-500 uppercase tracking-wide mb-3">
        추가 속성
      </h4>
      <div class="space-y-3">
        <div
          v-for="property in customProperties"
          :key="property.propertyId"
          class="form-section"
        >
          <label class="form-label">
            {{ property.propertyName }}
            <span v-if="property.requiredYn === 'Y'" class="text-red-500 ml-0.5">*</span>
          </label>
          <PropertyEditor
            :property="property"
            :model-value="getPropertyValue(property.propertyId)"
            :disabled="disabled"
            :users="assigneeOptions.map(o => ({ userId: Number(o.value), userName: o.label }))"
            @update:model-value="handlePropertyChange(property.propertyId, $event)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-section {
  @apply flex flex-col;
}

.form-label {
  @apply text-[12px] font-medium text-gray-600 mb-1;
}
</style>
