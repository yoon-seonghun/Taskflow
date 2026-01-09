<script setup lang="ts">
/**
 * 아이템 편집 폼 컴포넌트
 * - 기본 속성 (제목, 상태, 우선순위, 담당자, 그룹, 요청일, 마감일)
 * - 동적 속성 편집
 * - 모든 속성 인라인 편집
 * Compact UI 적용
 */
import { ref, computed, watch, onMounted } from 'vue'
import { usePropertyStore } from '@/stores/property'
import { useBoardStore } from '@/stores/board'
import { useCategoryStore } from '@/stores/category'
import { Input, Select, DatePicker } from '@/components/common'
import { PropertyEditor } from '@/components/property'
import { userApi } from '@/api/user'
import type { User } from '@/types/user'
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
const categoryStore = useCategoryStore()

// 모든 활성 사용자 목록
const allUsers = ref<User[]>([])

// 카테고리 목록 및 사용자 목록 로드
onMounted(async () => {
  if (categoryStore.categories.length === 0) {
    await categoryStore.fetchCategories()
  }

  // 모든 활성 사용자 로드
  try {
    const response = await userApi.getUsers({ useYn: 'Y', size: 1000 })
    if (response.success && response.data?.content) {
      allUsers.value = response.data.content
    }
  } catch (e) {
    console.error('Failed to load users:', e)
  }
})

// 폼 데이터
const formData = ref<ItemUpdateRequest>({
  title: props.item.title,
  content: props.item.content,
  status: props.item.status,
  priority: props.item.priority,
  groupId: props.item.groupId,
  categoryId: props.item.categoryId,
  assigneeUsername: props.item.assigneeUsername,
  requestDate: props.item.requestDate,
  dueDate: props.item.dueDate,
  properties: props.item.propertyValues || {},
  clear_fields: []
})

// item 변경 시 폼 데이터 동기화
watch(() => props.item, (newItem) => {
  formData.value = {
    title: newItem.title,
    content: newItem.content,
    status: newItem.status,
    priority: newItem.priority,
    groupId: newItem.groupId,
    categoryId: newItem.categoryId,
    assigneeUsername: newItem.assigneeUsername,
    requestDate: newItem.requestDate,
    dueDate: newItem.dueDate,
    properties: newItem.propertyValues || {},
    clear_fields: []
  }
}, { deep: true })

// 상태 옵션
const statusOptions: SelectOption[] = [
  { value: 'NOT_STARTED', label: '시작전', color: '#6B7280' },
  { value: 'IN_PROGRESS', label: '진행중', color: '#3B82F6' },
  { value: 'PENDING', label: '보류', color: '#F59E0B' },
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

// 카테고리 옵션
const categoryOptions = computed((): SelectOption[] => {
  return categoryStore.activeCategories.map(c => ({
    value: c.categoryId,
    label: c.categoryName,
    color: c.color
  }))
})

// 담당자 옵션 (모든 활성 사용자)
const assigneeOptions = computed((): SelectOption[] => {
  return allUsers.value.map(u => ({
    value: u.username,
    label: u.name || u.userName || u.username
  }))
})

// 동적 속성 목록 (해당 아이템에 선택된 속성만)
// v2.0 수정: item.properties에서 직접 속성 정보 사용 (propertyStore 의존성 제거)
// - 글로벌/매니저 속성은 boardId가 없어 propertyStore에 로드되지 않음
// - item.properties에는 백엔드에서 JOIN된 propertyName, propertyType 포함
const customProperties = computed(() => {
  if (!props.item.properties || props.item.properties.length === 0) {
    return []
  }

  // item.properties에서 직접 PropertyDef 형태로 변환
  // TB_ITEM_PROPERTY에 저장된 속성만 포함 (시스템 속성은 TB_ITEM 컬럼에 직접 저장되므로 미포함)
  return props.item.properties.map(p => ({
    propertyId: p.propertyId,
    propertyName: p.propertyName || `속성 ${p.propertyId}`,
    propertyType: p.propertyType || 'TEXT',
    requiredYn: 'N',  // item.properties에 미포함, 기본값 사용
    options: []  // 옵션은 PropertyEditor에서 별도 로드
  }))
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

// 카테고리 변경
function handleCategoryChange(value: string | number | null) {
  handleFieldChange('categoryId', value as number | undefined)
}

// 담당자 변경
function handleAssigneeChange(value: string | number | null) {
  handleFieldChange('assigneeUsername', value as string | undefined)
}

// 날짜 변경 (null로 클리어 지원)
function handleDateChange(field: 'requestDate' | 'dueDate', value: string | null) {
  if (value === null || value === undefined || value === '') {
    // null로 설정 요청 - clear_fields에 추가
    if (!formData.value.clear_fields) {
      formData.value.clear_fields = []
    }
    if (!formData.value.clear_fields.includes(field)) {
      formData.value.clear_fields.push(field)
    }
    formData.value[field] = undefined
    emit('change', field, null)
    emit('update', formData.value)
  } else {
    // 값이 있는 경우 - clear_fields에서 제거
    if (formData.value.clear_fields) {
      formData.value.clear_fields = formData.value.clear_fields.filter(f => f !== field)
    }
    handleFieldChange(field, value)
  }
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

      <!-- 카테고리 -->
      <div class="form-section">
        <label class="form-label">카테고리</label>
        <Select
          :model-value="formData.categoryId"
          :options="categoryOptions"
          :disabled="disabled"
          placeholder="카테고리 선택"
          clearable
          @update:model-value="handleCategoryChange"
        />
      </div>

      <!-- 담당자 -->
      <div class="form-section">
        <label class="form-label">담당자</label>
        <Select
          :model-value="formData.assigneeUsername"
          :options="assigneeOptions"
          :disabled="disabled"
          placeholder="담당자 선택"
          searchable
          clearable
          @update:model-value="handleAssigneeChange"
        />
      </div>

      <!-- 요청일 (기본속성 requestDate) -->
      <div class="form-section">
        <label class="form-label">요청일</label>
        <DatePicker
          :model-value="formData.requestDate"
          :disabled="disabled"
          mode="date"
          placeholder="요청일 선택"
          clearable
          @update:model-value="handleDateChange('requestDate', $event)"
        />
      </div>

      <!-- 마감일 (기본속성 dueDate) -->
      <div class="form-section">
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
            :users="assigneeOptions.map(o => ({ username: String(o.value), userName: o.label }))"
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
