<script setup lang="ts">
/**
 * 속성 관리 컴포넌트 (설정 페이지 내 탭용)
 * - v2.0: 글로벌/매니저/사용자 속성 관리
 * - 속성 추가/수정/삭제 (논리 삭제)
 * - 그룹별 드래그앤드롭으로 순서 변경
 * - 사용 중인 속성의 타입 변경 불가
 *
 * v2.0.1: useGroupedSortableDrag composable 적용
 */
import { ref, computed, onMounted, watch } from 'vue'
import { propertyApi } from '@/api/property'
import { boardApi } from '@/api/board'
import type {
  PropertyDef,
  PropertyCreateRequest,
  PropertyUpdateRequest,
  PropertyType,
  PropertyOption,
  OptionCreateRequest,
  OptionUpdateRequest
} from '@/types/property'
import { useUiStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'
import { useGroupedSortableDrag } from '@/composables/useSortableDrag'

const uiStore = useUiStore()
const authStore = useAuthStore()

// 관리자 여부
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

// 상태
const loading = ref(false)
const saving = ref(false)
const reordering = ref(false)
const properties = ref<PropertyDef[]>([])
const defaultBoardId = ref<number | null>(null)

// 소유 유형 (v2.0)
type OwnerType = 'GLOBAL' | 'MANAGER' | 'USER'
const selectedOwnerType = ref<OwnerType>('USER')

// 드래그 앤 드롭 (useGroupedSortableDrag composable 사용)
const {
  draggedItem: draggedProperty,
  draggedGroup: draggedOwnerType,
  dragOverIndex,
  handleDragStart: baseDragStart,
  handleDragOver: baseDragOver,
  handleDragLeave,
  handleDragEnd,
  handleDrop: baseDrop,
  isDropTarget
} = useGroupedSortableDrag<PropertyDef, OwnerType>({
  itemKey: 'propertyId',
  sameGroupOnly: true
})

// 소유 유형 옵션
const ownerTypeOptions = computed(() => {
  const options: { value: OwnerType; label: string; description: string }[] = []

  // 관리자만 글로벌 속성 생성 가능
  if (isAdmin.value) {
    options.push({ value: 'GLOBAL', label: '글로벌', description: '모든 사용자가 사용 가능' })
    options.push({ value: 'MANAGER', label: '매니저', description: '본인 및 하위 부서에서 사용' })
  }

  // 모든 사용자가 개인 속성 생성 가능
  options.push({ value: 'USER', label: '개인', description: '본인만 사용 가능' })

  return options
})

// 그룹별 속성 분류
const globalProperties = computed(() =>
  properties.value.filter(p => p.ownerType === 'GLOBAL').sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
)

const managerProperties = computed(() =>
  properties.value.filter(p => p.ownerType === 'MANAGER').sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
)

const userProperties = computed(() =>
  properties.value.filter(p => p.ownerType === 'USER').sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
)

// 그룹 정보
const propertyGroups = computed(() => {
  const groups: { ownerType: OwnerType; label: string; color: string; items: PropertyDef[] }[] = []

  if (globalProperties.value.length > 0 || isAdmin.value) {
    groups.push({
      ownerType: 'GLOBAL',
      label: '글로벌 속성',
      color: 'purple',
      items: globalProperties.value
    })
  }

  if (managerProperties.value.length > 0 || isAdmin.value) {
    groups.push({
      ownerType: 'MANAGER',
      label: '매니저 속성',
      color: 'amber',
      items: managerProperties.value
    })
  }

  groups.push({
    ownerType: 'USER',
    label: '개인 속성',
    color: 'gray',
    items: userProperties.value
  })

  return groups
})

// 편집 상태
const editingProperty = ref<PropertyDef | null>(null)
const showAddForm = ref(false)
const newProperty = ref<PropertyCreateRequest>({
  propertyName: '',
  propertyType: 'TEXT',
  ownerType: 'USER',
  visibleYn: 'Y'
})

// v2.0.5: 옵션 관리 상태
const expandedPropertyId = ref<number | null>(null)  // 옵션 패널이 열린 속성 ID
const propertyOptions = ref<PropertyOption[]>([])     // 옵션 목록
const optionsLoading = ref(false)                     // 옵션 로딩 상태
const optionSaving = ref(false)                       // 옵션 저장 중
const newOptionName = ref('')                         // 새 옵션 이름
const newOptionColor = ref('#6B7280')                 // 새 옵션 색상
const editingOptionId = ref<number | null>(null)      // 편집 중인 옵션 ID
const editingOptionData = ref<{ name: string; color: string }>({ name: '', color: '' })

// 옵션 색상 팔레트
const optionColorPalette = [
  '#EF4444', '#F97316', '#F59E0B', '#EAB308',
  '#84CC16', '#22C55E', '#10B981', '#14B8A6',
  '#06B6D4', '#0EA5E9', '#3B82F6', '#6366F1',
  '#8B5CF6', '#A855F7', '#D946EF', '#EC4899',
  '#F43F5E', '#6B7280', '#374151', '#000000'
]

// 속성 타입 옵션
const propertyTypeOptions: { value: PropertyType; label: string; description: string }[] = [
  { value: 'TEXT', label: '텍스트', description: '자유 텍스트 입력' },
  { value: 'NUMBER', label: '숫자', description: '숫자만 입력 가능' },
  { value: 'DATE', label: '날짜', description: '날짜 선택' },
  { value: 'SELECT', label: '단일 선택', description: '옵션 중 하나 선택' },
  { value: 'MULTI_SELECT', label: '다중 선택', description: '옵션 중 여러 개 선택' },
  { value: 'CHECKBOX', label: '체크박스', description: '예/아니오' },
  { value: 'USER', label: '사용자', description: '사용자 선택' }
]

// 기본 속성 이름 (관리에서 제외)
// 담당자/요청일/마감일은 TB_ITEM 고정 컬럼으로 관리되므로 TB_PROPERTY_DEF에 없음
// 카테고리만 EAV로 관리 (CategoryContent에서 별도 관리)
const defaultPropertyNames = ['카테고리']

// 속성 사용 중 여부 체크 (간단한 휴리스틱)
function isPropertyInUse(property: PropertyDef): boolean {
  // SELECT/MULTI_SELECT 타입이고 옵션에 usageCount가 있으면 사용 중으로 판단
  if (property.options && property.options.length > 0) {
    return property.options.some(o => (o.usageCount || 0) > 0)
  }
  return false
}

// 속성 목록 조회 (v2.0: 접근 가능한 모든 속성)
async function loadProperties() {
  loading.value = true
  try {
    // v2.0: 사용자의 접근 가능한 속성 조회 (글로벌 + 매니저 + 본인 속성)
    const propsRes = await propertyApi.getAccessibleProperties()

    // 기본 속성 제외하고 필터링
    properties.value = propsRes.data.filter(
      p => !defaultPropertyNames.includes(p.propertyName)
    )

    // 기본 보드 ID 설정 (속성 추가 시 필요)
    const boardsRes = await boardApi.getBoards()
    if (boardsRes.data.length > 0) {
      defaultBoardId.value = boardsRes.data[0].boardId
    }
  } catch (error) {
    console.error('Failed to load properties:', error)
    uiStore.showError('속성 정보를 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

// 속성 추가 폼 표시
function handleShowAddForm() {
  showAddForm.value = true
  // 관리자가 아니면 기본값을 USER로
  selectedOwnerType.value = isAdmin.value ? 'GLOBAL' : 'USER'
  newProperty.value = {
    propertyName: '',
    propertyType: 'TEXT',
    requiredYn: 'N',
    visibleYn: 'Y'
  }
}

// 속성 추가 취소
function handleCancelAdd() {
  showAddForm.value = false
}

// 속성 추가 (v2.0: 소유 유형별 API 호출)
async function handleAddProperty() {
  if (!newProperty.value.propertyName.trim()) {
    uiStore.showWarning('속성명을 입력해주세요.')
    return
  }

  // 기본 속성 이름과 중복 확인
  if (defaultPropertyNames.includes(newProperty.value.propertyName.trim())) {
    uiStore.showWarning('기본 속성 이름은 사용할 수 없습니다.')
    return
  }

  saving.value = true
  try {
    // v2.0: 소유 유형에 따라 다른 API 호출
    switch (selectedOwnerType.value) {
      case 'GLOBAL':
        await propertyApi.createGlobalProperty(newProperty.value)
        break
      case 'MANAGER':
        await propertyApi.createManagerProperty(newProperty.value)
        break
      case 'USER':
      default:
        await propertyApi.createUserProperty(newProperty.value)
        break
    }

    const typeLabel = ownerTypeOptions.value.find(o => o.value === selectedOwnerType.value)?.label || '속성'
    uiStore.showSuccess(`${typeLabel} 속성이 추가되었습니다.`)
    handleCancelAdd()
    await loadProperties()
  } catch (error: any) {
    console.error('Failed to add property:', error)
    const message = error.response?.data?.message || '속성 추가에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    saving.value = false
  }
}

// 속성 편집 시작
function handleEditProperty(property: PropertyDef) {
  editingProperty.value = { ...property }
}

// 속성 편집 취소
function handleCancelEdit() {
  editingProperty.value = null
}

// 속성 저장
async function handleSaveProperty() {
  if (!editingProperty.value) return
  if (!editingProperty.value.propertyName.trim()) {
    uiStore.showWarning('속성명을 입력해주세요.')
    return
  }

  saving.value = true
  try {
    const updateData: PropertyUpdateRequest = {
      propertyName: editingProperty.value.propertyName.trim(),
      propertyType: editingProperty.value.propertyType,
      requiredYn: editingProperty.value.requiredYn,
      visibleYn: editingProperty.value.visibleYn
    }
    await propertyApi.updateProperty(editingProperty.value.propertyId, updateData)
    uiStore.showSuccess('속성이 수정되었습니다.')
    editingProperty.value = null
    await loadProperties()
  } catch (error: any) {
    console.error('Failed to update property:', error)
    const message = error.response?.data?.message || '속성 수정에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    saving.value = false
  }
}

// 속성 삭제 (논리 삭제)
async function handleDeleteProperty(property: PropertyDef) {
  const confirmed = await uiStore.confirm({
    title: '속성 삭제',
    message: `'${property.propertyName}' 속성을 삭제하시겠습니까?\n이 속성을 사용 중인 업무의 해당 값이 표시되지 않습니다.`,
    confirmText: '삭제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  saving.value = true
  try {
    await propertyApi.deleteProperty(property.propertyId)
    uiStore.showSuccess('속성이 삭제되었습니다.')
    await loadProperties()
  } catch (error: any) {
    console.error('Failed to delete property:', error)
    const message = error.response?.data?.message || '속성 삭제에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    saving.value = false
  }
}

// 속성 타입 라벨 가져오기
function getPropertyTypeLabel(type: PropertyType): string {
  const option = propertyTypeOptions.find(o => o.value === type)
  return option?.label || type
}

// =============================================
// v2.0.5: 옵션 관리 함수
// =============================================

// SELECT/MULTI_SELECT 타입인지 확인
function isSelectType(property: PropertyDef): boolean {
  return property.propertyType === 'SELECT' || property.propertyType === 'MULTI_SELECT'
}

// 옵션 패널 토글
async function toggleOptionsPanel(property: PropertyDef) {
  if (expandedPropertyId.value === property.propertyId) {
    // 이미 열려있으면 닫기
    expandedPropertyId.value = null
    propertyOptions.value = []
    editingOptionId.value = null
  } else {
    // 새로 열기
    expandedPropertyId.value = property.propertyId
    await loadOptions(property.propertyId)
  }
}

// 옵션 목록 조회
async function loadOptions(propertyId: number) {
  optionsLoading.value = true
  try {
    const res = await propertyApi.getOptions(propertyId)
    propertyOptions.value = res.data.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } catch (error) {
    console.error('Failed to load options:', error)
    uiStore.showError('옵션을 불러오는데 실패했습니다.')
    propertyOptions.value = []
  } finally {
    optionsLoading.value = false
  }
}

// 새 옵션 추가
async function handleAddOption() {
  if (!expandedPropertyId.value) return
  if (!newOptionName.value.trim()) {
    uiStore.showWarning('옵션명을 입력해주세요.')
    return
  }

  optionSaving.value = true
  try {
    const data: OptionCreateRequest = {
      optionName: newOptionName.value.trim(),
      color: newOptionColor.value
    }
    await propertyApi.createOption(expandedPropertyId.value, data)
    uiStore.showSuccess('옵션이 추가되었습니다.')
    newOptionName.value = ''
    newOptionColor.value = '#6B7280'
    await loadOptions(expandedPropertyId.value)
  } catch (error: any) {
    console.error('Failed to add option:', error)
    const message = error.response?.data?.message || '옵션 추가에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    optionSaving.value = false
  }
}

// 옵션 편집 시작
function startEditOption(option: PropertyOption) {
  editingOptionId.value = option.optionId
  editingOptionData.value = {
    name: option.optionName,
    color: option.color || '#6B7280'
  }
}

// 옵션 편집 취소
function cancelEditOption() {
  editingOptionId.value = null
  editingOptionData.value = { name: '', color: '' }
}

// 옵션 저장
async function handleSaveOption() {
  if (!editingOptionId.value || !expandedPropertyId.value) return
  if (!editingOptionData.value.name.trim()) {
    uiStore.showWarning('옵션명을 입력해주세요.')
    return
  }

  optionSaving.value = true
  try {
    const data: OptionUpdateRequest = {
      optionName: editingOptionData.value.name.trim(),
      color: editingOptionData.value.color
    }
    await propertyApi.updateOption(editingOptionId.value, data)
    uiStore.showSuccess('옵션이 수정되었습니다.')
    editingOptionId.value = null
    await loadOptions(expandedPropertyId.value)
  } catch (error: any) {
    console.error('Failed to update option:', error)
    const message = error.response?.data?.message || '옵션 수정에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    optionSaving.value = false
  }
}

// 옵션 삭제
async function handleDeleteOption(option: PropertyOption) {
  if (!expandedPropertyId.value) return

  const confirmed = await uiStore.confirm({
    title: '옵션 삭제',
    message: `'${option.optionName}' 옵션을 삭제하시겠습니까?`,
    confirmText: '삭제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  optionSaving.value = true
  try {
    await propertyApi.deleteOption(option.optionId)
    uiStore.showSuccess('옵션이 삭제되었습니다.')
    await loadOptions(expandedPropertyId.value)
  } catch (error: any) {
    console.error('Failed to delete option:', error)
    const message = error.response?.data?.message || '옵션 삭제에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    optionSaving.value = false
  }
}

// 옵션 순서 변경 (위/아래)
async function handleMoveOption(option: PropertyOption, direction: 'up' | 'down') {
  if (!expandedPropertyId.value) return

  const currentIndex = propertyOptions.value.findIndex(o => o.optionId === option.optionId)
  if (currentIndex === -1) return

  const newIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1
  if (newIndex < 0 || newIndex >= propertyOptions.value.length) return

  // 로컬에서 먼저 순서 변경
  const newOptions = [...propertyOptions.value]
  const [moved] = newOptions.splice(currentIndex, 1)
  newOptions.splice(newIndex, 0, moved)

  // sortOrder 업데이트
  newOptions.forEach((opt, idx) => {
    opt.sortOrder = idx + 1
  })

  propertyOptions.value = newOptions

  // API로 각 옵션의 sortOrder 업데이트 (간단 구현)
  optionSaving.value = true
  try {
    // 옮긴 두 옵션만 업데이트
    await Promise.all([
      propertyApi.updateOption(newOptions[currentIndex].optionId, { sortOrder: currentIndex + 1 }),
      propertyApi.updateOption(newOptions[newIndex].optionId, { sortOrder: newIndex + 1 })
    ])
  } catch (error: any) {
    console.error('Failed to reorder options:', error)
    uiStore.showError('순서 변경에 실패했습니다.')
    await loadOptions(expandedPropertyId.value)
  } finally {
    optionSaving.value = false
  }
}

// =============================================
// 드래그 앤 드롭 핸들러 (composable 래퍼)
// =============================================

function handleDragStart(event: DragEvent, property: PropertyDef, ownerType: OwnerType) {
  baseDragStart(property, ownerType, event)
}

function handleDragOver(event: DragEvent, index: number, ownerType: OwnerType) {
  baseDragOver(index, ownerType, event)
}

async function handleDrop(event: DragEvent, targetIndex: number, ownerType: OwnerType, groupItems: PropertyDef[]) {
  const reorderedItems = await baseDrop(targetIndex, ownerType, groupItems, event)

  if (!reorderedItems) return

  // 새 순서 생성
  const orders = reorderedItems.map((item, index) => ({
    propertyId: item.propertyId,
    sortOrder: index + 1
  }))

  // API 호출
  reordering.value = true
  try {
    await propertyApi.reorderProperties({
      ownerType,
      orders
    })

    // 로컬 상태 업데이트
    orders.forEach(order => {
      const prop = properties.value.find(p => p.propertyId === order.propertyId)
      if (prop) {
        prop.sortOrder = order.sortOrder
      }
    })

    uiStore.showSuccess('속성 순서가 변경되었습니다.')
  } catch (error: any) {
    console.error('Failed to reorder properties:', error)
    const message = error.response?.data?.message || '순서 변경에 실패했습니다.'
    uiStore.showError(message)
    // 실패 시 다시 로드
    await loadProperties()
  } finally {
    reordering.value = false
  }
}

// 초기 로드
onMounted(() => {
  loadProperties()
})
</script>

<template>
  <div class="properties-content">
    <!-- 설명 -->
    <div class="mb-4">
      <p class="text-sm text-gray-500">
        내가 생성한 속성을 관리합니다. 생성된 속성은 카테고리/보드에 연결하여 사용할 수 있습니다.
      </p>
      <p class="mt-1 text-xs text-amber-600">
        주의: 사용 중인 속성의 타입은 변경할 수 없습니다. 드래그하여 순서를 변경할 수 있습니다.
      </p>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="py-12 text-center">
      <svg class="animate-spin h-8 w-8 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">속성을 불러오는 중...</p>
    </div>

    <!-- 메인 콘텐츠 -->
    <div v-else class="space-y-4">
      <!-- 속성 추가 버튼 -->
      <div class="flex justify-end">
        <button
          v-if="!showAddForm"
          type="button"
          class="btn-add"
          @click="handleShowAddForm"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          속성 추가
        </button>
      </div>

      <!-- 추가 폼 -->
      <div v-if="showAddForm" class="settings-card">
        <div class="settings-card-title">새 속성 추가</div>
        <div class="settings-card-content">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- 소유 유형 선택 (첫 번째 필드) -->
            <div class="md:col-span-2">
              <label class="block text-xs font-medium text-gray-500 mb-2">소유 유형 *</label>
              <div class="flex flex-wrap gap-2">
                <label
                  v-for="opt in ownerTypeOptions"
                  :key="opt.value"
                  class="owner-type-option"
                  :class="{ 'selected': selectedOwnerType === opt.value }"
                >
                  <input
                    v-model="selectedOwnerType"
                    type="radio"
                    :value="opt.value"
                    class="sr-only"
                  />
                  <span class="owner-type-label">{{ opt.label }}</span>
                  <span class="owner-type-desc">{{ opt.description }}</span>
                </label>
              </div>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-500 mb-1">속성명 *</label>
              <input
                v-model="newProperty.propertyName"
                type="text"
                class="form-input"
                placeholder="예: 진행률"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-500 mb-1">타입 *</label>
              <select v-model="newProperty.propertyType" class="form-input">
                <option v-for="opt in propertyTypeOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }} - {{ opt.description }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-500 mb-1">필수 여부</label>
              <select v-model="newProperty.requiredYn" class="form-input">
                <option value="N">선택</option>
                <option value="Y">필수</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-500 mb-1">표시 여부</label>
              <select v-model="newProperty.visibleYn" class="form-input">
                <option value="Y">표시</option>
                <option value="N">숨김</option>
              </select>
            </div>
          </div>
          <div class="mt-4 flex justify-end gap-2">
            <button type="button" class="btn-cancel" @click="handleCancelAdd" :disabled="saving">
              취소
            </button>
            <button type="button" class="btn-primary" @click="handleAddProperty" :disabled="saving">
              {{ saving ? '저장 중...' : '추가' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 그룹별 속성 목록 -->
      <div
        v-for="group in propertyGroups"
        :key="group.ownerType"
        class="settings-card"
      >
        <div class="settings-card-title flex items-center gap-2">
          <span
            class="group-indicator"
            :class="`group-${group.color}`"
          />
          <span>{{ group.label }}</span>
          <span class="text-xs text-gray-400 font-normal ml-auto">
            {{ group.items.length }}개
          </span>
        </div>

        <div class="settings-card-content">
          <!-- 속성 목록 (드래그 가능) -->
          <div v-if="group.items.length > 0" class="property-list">
            <!-- v2.0.5: 속성 아이템과 옵션 패널을 감싸는 래퍼 -->
            <div
              v-for="(property, index) in group.items"
              :key="property.propertyId"
              class="property-wrapper"
            >
            <div
              class="property-item sortable-item"
              :class="{
                'sortable-drag-over': isDropTarget(index),
                'sortable-dragging': draggedProperty?.propertyId === property.propertyId
              }"
              draggable="true"
              @dragstart="handleDragStart($event, property, group.ownerType)"
              @dragend="handleDragEnd"
              @dragover="handleDragOver($event, index, group.ownerType)"
              @dragleave="handleDragLeave"
              @drop="handleDrop($event, index, group.ownerType, group.items)"
            >
              <!-- 드래그 핸들 -->
              <div class="drag-handle sortable-handle">
                <svg class="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 8h16M4 16h16" />
                </svg>
              </div>

              <!-- 편집 모드 -->
              <template v-if="editingProperty?.propertyId === property.propertyId">
                <div class="flex-1 grid grid-cols-1 md:grid-cols-4 gap-2 items-center">
                  <input
                    v-model="editingProperty.propertyName"
                    type="text"
                    class="form-input"
                    placeholder="속성명"
                  />
                  <select
                    v-model="editingProperty.propertyType"
                    class="form-input"
                    :disabled="isPropertyInUse(property)"
                  >
                    <option v-for="opt in propertyTypeOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                  <select v-model="editingProperty.requiredYn" class="form-input">
                    <option value="N">선택</option>
                    <option value="Y">필수</option>
                  </select>
                  <select v-model="editingProperty.visibleYn" class="form-input">
                    <option value="Y">표시</option>
                    <option value="N">숨김</option>
                  </select>
                </div>
                <div class="flex items-center gap-1">
                  <button type="button" class="icon-btn text-green-600" title="저장" @click="handleSaveProperty" :disabled="saving">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                    </svg>
                  </button>
                  <button type="button" class="icon-btn text-gray-400" title="취소" @click="handleCancelEdit" :disabled="saving">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              </template>

              <!-- 조회 모드 -->
              <template v-else>
                <div class="flex-1 flex items-center gap-3">
                  <span class="font-medium text-gray-900">{{ property.propertyName }}</span>
                  <span class="type-badge">{{ getPropertyTypeLabel(property.propertyType) }}</span>
                  <span v-if="property.requiredYn === 'Y'" class="text-xs text-red-500">필수</span>
                  <span v-if="property.visibleYn === 'N'" class="text-xs text-gray-400">숨김</span>
                  <!-- v2.0.5: SELECT/MULTI_SELECT 옵션 개수 표시 -->
                  <span
                    v-if="isSelectType(property)"
                    class="text-xs text-gray-400 cursor-pointer hover:text-blue-600"
                    @click.stop="toggleOptionsPanel(property)"
                  >
                    ({{ property.options?.length || 0 }}개 옵션)
                  </span>
                </div>
                <div class="flex items-center gap-1">
                  <!-- v2.0.5: 옵션 관리 버튼 (SELECT/MULTI_SELECT 전용) -->
                  <button
                    v-if="isSelectType(property)"
                    type="button"
                    class="icon-btn"
                    :class="{ 'text-blue-600 bg-blue-50': expandedPropertyId === property.propertyId }"
                    title="옵션 관리"
                    @click="toggleOptionsPanel(property)"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M4 6h16M4 12h16M4 18h7" />
                    </svg>
                  </button>
                  <button type="button" class="icon-btn" title="수정" @click="handleEditProperty(property)">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </button>
                  <button type="button" class="icon-btn text-red-500" title="삭제" @click="handleDeleteProperty(property)">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </template>
            </div>

            <!-- v2.0.5: 옵션 관리 패널 (SELECT/MULTI_SELECT 타입) -->
            <div
              v-if="isSelectType(property) && expandedPropertyId === property.propertyId"
              class="options-panel"
            >
              <div class="options-panel-header">
                <span class="text-sm font-medium text-gray-700">옵션 관리</span>
                <span class="text-xs text-gray-400">{{ propertyOptions.length }}개</span>
              </div>

              <!-- 옵션 로딩 -->
              <div v-if="optionsLoading" class="py-4 text-center">
                <svg class="animate-spin h-5 w-5 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
              </div>

              <!-- 옵션 목록 -->
              <div v-else class="options-list">
                <div
                  v-for="(option, optIndex) in propertyOptions"
                  :key="option.optionId"
                  class="option-item"
                >
                  <!-- 편집 모드 -->
                  <template v-if="editingOptionId === option.optionId">
                    <div class="flex-1 flex items-center gap-2">
                      <input
                        v-model="editingOptionData.name"
                        type="text"
                        class="option-input flex-1"
                        placeholder="옵션명"
                        @keyup.enter="handleSaveOption"
                        @keyup.escape="cancelEditOption"
                      />
                      <!-- 색상 선택 드롭다운 -->
                      <div class="relative color-picker-wrapper">
                        <button
                          type="button"
                          class="color-btn"
                          :style="{ backgroundColor: editingOptionData.color }"
                        />
                        <div class="color-palette">
                          <button
                            v-for="color in optionColorPalette"
                            :key="color"
                            type="button"
                            class="color-swatch"
                            :class="{ 'ring-2 ring-offset-1 ring-gray-900': editingOptionData.color === color }"
                            :style="{ backgroundColor: color }"
                            @click="editingOptionData.color = color"
                          />
                        </div>
                      </div>
                    </div>
                    <div class="flex items-center gap-1">
                      <button type="button" class="icon-btn-sm text-green-600" title="저장" @click="handleSaveOption" :disabled="optionSaving">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                      </button>
                      <button type="button" class="icon-btn-sm text-gray-400" title="취소" @click="cancelEditOption" :disabled="optionSaving">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  </template>

                  <!-- 조회 모드 -->
                  <template v-else>
                    <div class="flex-1 flex items-center gap-2">
                      <span class="option-color" :style="{ backgroundColor: option.color || '#6B7280' }" />
                      <span class="text-sm text-gray-700">{{ option.optionName }}</span>
                      <span v-if="option.usageCount" class="text-xs text-gray-400">({{ option.usageCount }}건 사용)</span>
                    </div>
                    <div class="flex items-center gap-0.5">
                      <!-- 위로 이동 -->
                      <button
                        type="button"
                        class="icon-btn-sm"
                        :class="{ 'opacity-30 cursor-not-allowed': optIndex === 0 }"
                        :disabled="optIndex === 0 || optionSaving"
                        title="위로"
                        @click="handleMoveOption(option, 'up')"
                      >
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7" />
                        </svg>
                      </button>
                      <!-- 아래로 이동 -->
                      <button
                        type="button"
                        class="icon-btn-sm"
                        :class="{ 'opacity-30 cursor-not-allowed': optIndex === propertyOptions.length - 1 }"
                        :disabled="optIndex === propertyOptions.length - 1 || optionSaving"
                        title="아래로"
                        @click="handleMoveOption(option, 'down')"
                      >
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                        </svg>
                      </button>
                      <!-- 수정 -->
                      <button type="button" class="icon-btn-sm" title="수정" @click="startEditOption(option)">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                      </button>
                      <!-- 삭제 -->
                      <button type="button" class="icon-btn-sm text-red-500" title="삭제" @click="handleDeleteOption(option)">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    </div>
                  </template>
                </div>

                <!-- 빈 상태 -->
                <div v-if="propertyOptions.length === 0 && !optionsLoading" class="py-3 text-center text-xs text-gray-400">
                  아직 등록된 옵션이 없습니다.
                </div>
              </div>

              <!-- 새 옵션 추가 -->
              <div class="add-option-form">
                <div class="flex items-center gap-2">
                  <input
                    v-model="newOptionName"
                    type="text"
                    class="option-input flex-1"
                    placeholder="새 옵션 이름"
                    @keyup.enter="handleAddOption"
                  />
                  <!-- 색상 선택 드롭다운 -->
                  <div class="relative color-picker-wrapper">
                    <button
                      type="button"
                      class="color-btn"
                      :style="{ backgroundColor: newOptionColor }"
                    />
                    <div class="color-palette">
                      <button
                        v-for="color in optionColorPalette"
                        :key="color"
                        type="button"
                        class="color-swatch"
                        :class="{ 'ring-2 ring-offset-1 ring-gray-900': newOptionColor === color }"
                        :style="{ backgroundColor: color }"
                        @click="newOptionColor = color"
                      />
                    </div>
                  </div>
                  <button
                    type="button"
                    class="add-option-btn"
                    :disabled="!newOptionName.trim() || optionSaving"
                    @click="handleAddOption"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                    </svg>
                    추가
                  </button>
                </div>
              </div>
            </div>
            </div><!-- property-wrapper 닫기 -->
          </div>

          <!-- 빈 상태 -->
          <div v-else class="py-6 text-center text-sm text-gray-400">
            {{ group.label }}이(가) 없습니다.
          </div>
        </div>
      </div>
    </div>

    <!-- 순서 변경 중 오버레이 -->
    <div v-if="reordering" class="fixed inset-0 bg-black/20 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg px-6 py-4 flex items-center gap-3 shadow-lg">
        <svg class="animate-spin h-5 w-5 text-primary-600" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
        <span class="text-sm text-gray-600">순서 변경 중...</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.properties-content {
  @apply w-full max-w-4xl;
}

.settings-card {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden;
}

.settings-card-title {
  @apply px-4 py-3 text-base font-medium text-gray-900 bg-gray-50 border-b border-gray-200;
}

.settings-card-content {
  @apply p-4;
}

.btn-add {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium
         text-primary-600 bg-primary-50 rounded-md
         hover:bg-primary-100 transition-colors;
}

.btn-primary {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150 disabled:opacity-50 disabled:cursor-not-allowed;
}

.btn-cancel {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150 disabled:opacity-50;
}

.form-input {
  @apply w-full px-3 py-1.5 text-sm border border-gray-300 rounded-md
         focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-100 disabled:cursor-not-allowed;
}

/* 그룹 인디케이터 */
.group-indicator {
  @apply w-3 h-3 rounded-full;
}

.group-indicator.group-purple {
  @apply bg-purple-500;
}

.group-indicator.group-amber {
  @apply bg-amber-500;
}

.group-indicator.group-gray {
  @apply bg-gray-400;
}

/* 속성 리스트 */
.property-list {
  @apply space-y-1;
}

/* v2.0.5: 속성 래퍼 (속성 + 옵션 패널) */
.property-wrapper {
  @apply space-y-1;
}

.property-item {
  @apply flex items-center gap-3 px-3 py-2 rounded-lg border border-transparent
         bg-gray-50 transition-all duration-150;
  cursor: grab;
}

.property-item:hover {
  @apply bg-gray-100;
}

.property-item.dragging {
  @apply opacity-50 border-dashed border-gray-300;
}

.property-item.drag-over {
  @apply border-primary-500 border-2 bg-primary-50;
}

.property-item:active {
  cursor: grabbing;
}

.drag-handle {
  @apply flex-shrink-0 cursor-grab;
}

.drag-handle:active {
  cursor: grabbing;
}

.type-badge {
  @apply inline-flex items-center px-2 py-0.5 text-xs font-medium rounded bg-blue-50 text-blue-700;
}

.icon-btn {
  @apply p-1.5 rounded hover:bg-gray-100 text-gray-500 transition-colors disabled:opacity-50;
}

/* 소유 유형 선택 옵션 */
.owner-type-option {
  @apply flex flex-col p-3 rounded-lg border-2 border-gray-200 cursor-pointer
         transition-all duration-150 hover:border-gray-300 hover:bg-gray-50;
}

.owner-type-option.selected {
  @apply border-primary-500 bg-primary-50;
}

.owner-type-label {
  @apply text-sm font-medium text-gray-900;
}

.owner-type-desc {
  @apply text-xs text-gray-500 mt-0.5;
}

.owner-type-option.selected .owner-type-label {
  @apply text-primary-700;
}

.owner-type-option.selected .owner-type-desc {
  @apply text-primary-600;
}

/* v2.0.5: 옵션 관리 패널 스타일 */
.options-panel {
  @apply ml-7 mt-1 mb-2 bg-white border border-gray-200 rounded-lg overflow-hidden;
}

.options-panel-header {
  @apply flex items-center justify-between px-3 py-2 bg-gray-50 border-b border-gray-100;
}

.options-list {
  @apply divide-y divide-gray-100;
}

.option-item {
  @apply flex items-center gap-2 px-3 py-2 hover:bg-gray-50 transition-colors;
}

.option-color {
  @apply w-3 h-3 rounded-full flex-shrink-0;
}

.option-input {
  @apply px-2 py-1 text-sm border border-gray-300 rounded
         focus:ring-1 focus:ring-primary-500 focus:border-primary-500;
}

.add-option-form {
  @apply px-3 py-2 bg-gray-50 border-t border-gray-100;
}

.add-option-btn {
  @apply inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium
         text-white bg-primary-600 rounded
         hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed
         transition-colors;
}

.icon-btn-sm {
  @apply p-1 rounded hover:bg-gray-100 text-gray-400 transition-colors
         disabled:opacity-50 disabled:cursor-not-allowed;
}

/* 색상 선택 드롭다운 */
.color-picker-wrapper {
  @apply relative;
}

.color-btn {
  @apply w-6 h-6 rounded border border-gray-300 cursor-pointer
         hover:ring-2 hover:ring-offset-1 hover:ring-gray-400;
}

.color-palette {
  @apply absolute z-10 top-full left-0 mt-1 p-1.5 bg-white border border-gray-200
         rounded-lg shadow-lg grid grid-cols-5 gap-1
         opacity-0 invisible transition-all duration-150;
}

.color-picker-wrapper:hover .color-palette,
.color-picker-wrapper:focus-within .color-palette {
  @apply opacity-100 visible;
}

.color-swatch {
  @apply w-5 h-5 rounded cursor-pointer
         hover:ring-2 hover:ring-offset-1 hover:ring-gray-400
         transition-all;
}
</style>
