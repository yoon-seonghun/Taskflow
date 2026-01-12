<script setup lang="ts">
/**
 * 업무 속성 편집 모달 (v2.0 - cap_7.jpg 스타일)
 * - 보드 편집 모달과 동일한 통합 스타일
 * - 업무명, 설명, 카테고리, 속성값 편집
 * - 속성 드래그앤드롭 순서 변경 지원
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { boardApi } from '@/api/board'
import { categoryApi } from '@/api/category'
import { propertyApi } from '@/api/property'
import { itemApi } from '@/api/item'
import { userApi } from '@/api/user'
import { useToast } from '@/composables/useToast'
import type { BoardProperty, BoardCategory } from '@/types/board'
import type { CategoryProperty } from '@/types/category'
import type { PropertyOption } from '@/types/property'
import type { ItemPropertySortRequest } from '@/types/item'
import type { User } from '@/types/user'

interface Props {
  modelValue: boolean  // 모달 열림/닫힘
  boardId: number
  itemId?: number | null  // 기존 업무 수정 시
  initialTitle?: string
  initialDescription?: string
  initialCategoryId?: number | null
  initialProperties?: Record<number, unknown>
  mode?: 'create' | 'edit'  // 생성 모드 또는 편집 모드
}

const props = withDefaults(defineProps<Props>(), {
  itemId: null,
  initialTitle: '',
  initialDescription: '',
  initialCategoryId: null,
  initialProperties: () => ({}),
  mode: 'create'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'apply', data: {
    title: string
    description: string
    categoryId: number | null
    properties: Record<number, unknown>
    propertySortOrders?: Record<number, number>  // propertyId -> sortOrder
  }): void
  (e: 'cancel'): void
}>()

const toast = useToast()

// 상태
const loading = ref(false)
const boardCategories = ref<BoardCategory[]>([])
const boardProperties = ref<BoardProperty[]>([])
const categoryProperties = ref<CategoryProperty[]>([])
const categoryLoading = ref(false)
const propertyOptions = ref<Map<number, PropertyOption[]>>(new Map())
const users = ref<User[]>([])  // v2.0.5: USER 타입 속성용 사용자 목록

// 드래그앤드롭 상태
const draggedPropertyIndex = ref<number | null>(null)
const dropTargetIndex = ref<number | null>(null)
const draggedGroupKey = ref<string | null>(null)

// 속성 순서 (propertyId -> sortOrder)
const propertySortOrders = ref<Map<number, number>>(new Map())

let isComponentMounted = false

// 폼 데이터
const formData = ref({
  title: props.initialTitle,
  description: props.initialDescription,
  categoryId: props.initialCategoryId as number | null,
  propertyValues: { ...props.initialProperties } as Record<number, unknown>
})

// 기본 카테고리
const defaultCategory = computed(() => {
  return boardCategories.value.find(c => c.isDefault)
})

// 모든 속성 (보드 속성 + 카테고리 속성)
// v2.0.3: 동일 propertyId뿐만 아니라 동일 이름의 속성도 중복 제외
const allProperties = computed(() => {
  const boardPropIds = new Set(boardProperties.value.map(p => p.propertyId))
  // 보드 속성의 이름 목록 (동일명 중복 방지용)
  const boardPropNames = new Set(boardProperties.value.map(p => p.propertyName))

  // 카테고리 속성: propertyId 중복 및 이름 중복 모두 제외
  const catProps = categoryProperties.value.filter(p => {
    // propertyId 중복 제외
    if (boardPropIds.has(p.propertyId)) {
      console.log('[allProperties] propertyId 중복 제외:', p.propertyId, p.propertyName)
      return false
    }
    // 동일 이름 제외 (v2.0.3)
    if (boardPropNames.has(p.propertyName)) {
      console.log('[allProperties] 동일 이름 카테고리 속성 제외:', p.propertyId, p.propertyName)
      return false
    }
    return true
  })

  // 속성을 ownerType별로 그룹화하여 반환
  return {
    category: catProps,
    global: boardProperties.value.filter(p => p.ownerType === 'GLOBAL'),
    manager: boardProperties.value.filter(p => p.ownerType === 'MANAGER'),
    user: boardProperties.value.filter(p => p.ownerType === 'USER')
  }
})

// 데이터 로드
async function loadData() {
  if (!props.boardId) return

  loading.value = true
  try {
    const [categoriesRes, propertiesRes] = await Promise.all([
      boardApi.getBoardCategories(props.boardId),
      boardApi.getBoardProperties(props.boardId)
    ])

    if (!isComponentMounted) return

    if (categoriesRes.success && categoriesRes.data) {
      boardCategories.value = categoriesRes.data

      // 초기 카테고리 설정
      if (props.initialCategoryId) {
        formData.value.categoryId = props.initialCategoryId
      } else {
        const defaultCat = categoriesRes.data.find(c => c.isDefault)
        if (defaultCat) {
          formData.value.categoryId = defaultCat.categoryId
        }
      }

      // 카테고리 속성 로드
      if (formData.value.categoryId) {
        await loadCategoryProperties(formData.value.categoryId)
      }
    }

    if (propertiesRes.success && propertiesRes.data) {
      boardProperties.value = propertiesRes.data

      // 옵션 로드
      const optionPromises: Promise<void>[] = []
      let hasUserType = false  // v2.0.5: USER 타입 존재 여부
      for (const prop of propertiesRes.data) {
        if (prop.propertyType === 'SELECT' || prop.propertyType === 'MULTI_SELECT') {
          optionPromises.push(loadPropertyOptions(prop.propertyId))
        }
        // v2.0.5: USER 타입 체크
        if (prop.propertyType === 'USER') {
          hasUserType = true
        }

        // 새 업무 생성 시 기본값 적용
        if (props.mode === 'create' && prop.defaultValue && !formData.value.propertyValues[prop.propertyId]) {
          formData.value.propertyValues[prop.propertyId] = prop.defaultValue
        }
      }
      await Promise.allSettled(optionPromises)

      // v2.0.5: USER 타입 속성이 있으면 사용자 목록 로드
      if (hasUserType) {
        await loadUsers()
      }
    }
  } catch (error) {
    console.error('Failed to load board data:', error)
    toast.error('보드 정보를 불러오는데 실패했습니다.')
  } finally {
    if (isComponentMounted) {
      loading.value = false
    }
  }
}

// 카테고리 속성 로드
async function loadCategoryProperties(categoryId: number | null) {
  if (!categoryId) {
    categoryProperties.value = []
    return
  }

  categoryLoading.value = true
  try {
    const response = await categoryApi.getProperties(categoryId)
    if (!isComponentMounted) return

    if (response.success && response.data) {
      categoryProperties.value = response.data

      // 옵션 로드 및 기본값 적용
      const optionPromises: Promise<void>[] = []
      for (const prop of response.data) {
        if (prop.propertyType === 'SELECT' || prop.propertyType === 'MULTI_SELECT') {
          optionPromises.push(loadPropertyOptions(prop.propertyId))
        }

        // 새 업무 생성 시 기본값 적용
        if (props.mode === 'create' && prop.defaultValue && !formData.value.propertyValues[prop.propertyId]) {
          formData.value.propertyValues[prop.propertyId] = prop.defaultValue
        }
      }
      await Promise.allSettled(optionPromises)
    }
  } catch (error) {
    console.error('Failed to load category properties:', error)
  } finally {
    if (isComponentMounted) {
      categoryLoading.value = false
    }
  }
}

// 속성 옵션 로드
async function loadPropertyOptions(propertyId: number) {
  if (propertyOptions.value.has(propertyId)) return

  try {
    const response = await propertyApi.getOptions(propertyId)
    if (!isComponentMounted) return

    if (response.success && response.data) {
      propertyOptions.value.set(propertyId, response.data)
    }
  } catch (error) {
    console.error('Failed to load property options:', error)
  }
}

// v2.0.5: 사용자 목록 로드 (USER 타입 속성용)
async function loadUsers() {
  if (users.value.length > 0) return

  try {
    const response = await userApi.getUsers({ useYn: 'Y', size: 1000 })
    if (!isComponentMounted) return

    if (response.success && response.data) {
      users.value = response.data.content || []
    }
  } catch (error) {
    console.error('Failed to load users:', error)
  }
}

// v2.0.5: MULTI_SELECT 옵션 토글
function toggleMultiSelectOption(propertyId: number, optionId: number) {
  const currentValue = formData.value.propertyValues[propertyId]
  let selectedOptions: number[] = []

  if (Array.isArray(currentValue)) {
    selectedOptions = [...currentValue]
  } else if (typeof currentValue === 'string' && currentValue) {
    selectedOptions = currentValue.split(',').map(Number).filter(n => !isNaN(n))
  }

  const index = selectedOptions.indexOf(optionId)
  if (index > -1) {
    selectedOptions.splice(index, 1)
  } else {
    selectedOptions.push(optionId)
  }

  formData.value.propertyValues[propertyId] = selectedOptions
}

// v2.0.5: MULTI_SELECT 옵션이 선택되었는지 확인
function isMultiSelectOptionSelected(propertyId: number, optionId: number): boolean {
  const currentValue = formData.value.propertyValues[propertyId]
  if (Array.isArray(currentValue)) {
    return currentValue.includes(optionId)
  } else if (typeof currentValue === 'string' && currentValue) {
    const selectedOptions = currentValue.split(',').map(Number).filter(n => !isNaN(n))
    return selectedOptions.includes(optionId)
  }
  return false
}

// 카테고리 변경
function handleCategoryChange(categoryId: number | null) {
  formData.value.categoryId = categoryId
  loadCategoryProperties(categoryId)
}

// 속성값 변경
function updatePropertyValue(propertyId: number, value: unknown) {
  formData.value.propertyValues[propertyId] = value
}

// 적용
function handleApply() {
  if (props.mode === 'create' && !formData.value.title.trim()) {
    toast.error('업무명을 입력해주세요.')
    return
  }

  // 순서 정보를 Record로 변환
  const sortOrders: Record<number, number> = {}
  propertySortOrders.value.forEach((sortOrder, propertyId) => {
    sortOrders[propertyId] = sortOrder
  })

  emit('apply', {
    title: formData.value.title,
    description: formData.value.description,
    categoryId: formData.value.categoryId,
    properties: formData.value.propertyValues,
    propertySortOrders: Object.keys(sortOrders).length > 0 ? sortOrders : undefined
  })
  emit('update:modelValue', false)
}

// 취소
function handleCancel() {
  emit('cancel')
  emit('update:modelValue', false)
}

// 닫기
function handleClose() {
  emit('update:modelValue', false)
}

// 모달 열림 감지
watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    // 초기값 복원
    formData.value = {
      title: props.initialTitle,
      description: props.initialDescription,
      categoryId: props.initialCategoryId,
      propertyValues: { ...props.initialProperties }
    }
    // 순서 정보 초기화
    propertySortOrders.value.clear()
    draggedPropertyIndex.value = null
    dropTargetIndex.value = null
    draggedGroupKey.value = null
    loadData()
  }
})

onMounted(() => {
  isComponentMounted = true
  if (props.modelValue) {
    loadData()
  }
})

onUnmounted(() => {
  isComponentMounted = false
})

// 속성 타입별 색상
const propertyColors = {
  category: { bg: 'bg-orange-50', border: 'border-orange-200', text: 'text-orange-700', ring: 'focus:ring-orange-500' },
  global: { bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-700', ring: 'focus:ring-blue-500' },
  manager: { bg: 'bg-purple-50', border: 'border-purple-200', text: 'text-purple-700', ring: 'focus:ring-purple-500' },
  user: { bg: 'bg-green-50', border: 'border-green-200', text: 'text-green-700', ring: 'focus:ring-green-500' }
}

// ==================== 속성 드래그앤드롭 ====================

// 그룹별 속성 배열 (드래그 가능하게 변환)
interface DraggableProperty {
  propertyId: number
  propertyName: string
  propertyType: string
  requiredYn?: string
  defaultValue?: string
  sortOrder?: number
  groupKey: string  // category, global, manager, user
}

// 모든 속성을 하나의 배열로 펼침 (드래그앤드롭용)
const flattenedProperties = computed(() => {
  const result: DraggableProperty[] = []

  // edit 모드일 때: initialProperties에 있는 속성만 표시
  // create 모드일 때: 모든 속성 표시
  const registeredPropertyIds = props.mode === 'edit'
    ? new Set(Object.keys(props.initialProperties).map(Number))
    : null

  // 카테고리 속성
  for (const prop of allProperties.value.category) {
    // edit 모드에서는 등록된 속성만 표시
    if (registeredPropertyIds && !registeredPropertyIds.has(prop.propertyId)) continue

    result.push({
      propertyId: prop.propertyId,
      propertyName: prop.propertyName,
      propertyType: prop.propertyType,
      requiredYn: prop.requiredYn,
      defaultValue: prop.defaultValue,
      sortOrder: propertySortOrders.value.get(prop.propertyId) || prop.sortOrder || 0,
      groupKey: 'category'
    })
  }

  // 글로벌 속성
  for (const prop of allProperties.value.global) {
    if (registeredPropertyIds && !registeredPropertyIds.has(prop.propertyId)) continue

    result.push({
      propertyId: prop.propertyId,
      propertyName: prop.propertyName,
      propertyType: prop.propertyType,
      requiredYn: prop.requiredYn,
      defaultValue: prop.defaultValue,
      sortOrder: propertySortOrders.value.get(prop.propertyId) || prop.sortOrder || 0,
      groupKey: 'global'
    })
  }

  // 매니저 속성
  for (const prop of allProperties.value.manager) {
    if (registeredPropertyIds && !registeredPropertyIds.has(prop.propertyId)) continue

    result.push({
      propertyId: prop.propertyId,
      propertyName: prop.propertyName,
      propertyType: prop.propertyType,
      requiredYn: prop.requiredYn,
      defaultValue: prop.defaultValue,
      sortOrder: propertySortOrders.value.get(prop.propertyId) || prop.sortOrder || 0,
      groupKey: 'manager'
    })
  }

  // 사용자 속성
  for (const prop of allProperties.value.user) {
    if (registeredPropertyIds && !registeredPropertyIds.has(prop.propertyId)) continue

    result.push({
      propertyId: prop.propertyId,
      propertyName: prop.propertyName,
      propertyType: prop.propertyType,
      requiredYn: prop.requiredYn,
      defaultValue: prop.defaultValue,
      sortOrder: propertySortOrders.value.get(prop.propertyId) || prop.sortOrder || 0,
      groupKey: 'user'
    })
  }

  // sortOrder로 정렬 (0이면 원래 순서 유지)
  const hasSortOrder = result.some(p => (p.sortOrder ?? 0) > 0)
  if (hasSortOrder) {
    result.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  }

  return result
})

// 드래그 시작
function handlePropertyDragStart(index: number, groupKey: string, event: DragEvent) {
  draggedPropertyIndex.value = index
  draggedGroupKey.value = groupKey
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(index))
  }
}

// 드래그 오버
function handlePropertyDragOver(index: number, event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  dropTargetIndex.value = index
}

// 드래그 리브
function handlePropertyDragLeave() {
  dropTargetIndex.value = null
}

// 드래그 엔드
function handlePropertyDragEnd() {
  draggedPropertyIndex.value = null
  dropTargetIndex.value = null
  draggedGroupKey.value = null
}

// 드롭
async function handlePropertyDrop(toIndex: number, event: DragEvent) {
  event.preventDefault()

  const fromIndex = draggedPropertyIndex.value
  if (fromIndex === null || fromIndex === toIndex) {
    handlePropertyDragEnd()
    return
  }

  const properties = [...flattenedProperties.value]
  if (properties.length === 0) {
    handlePropertyDragEnd()
    return
  }

  // 순서 변경
  const [movedItem] = properties.splice(fromIndex, 1)
  properties.splice(toIndex, 0, movedItem)

  // 새로운 순서 계산 및 저장
  properties.forEach((prop, idx) => {
    propertySortOrders.value.set(prop.propertyId, idx + 1)
  })

  handlePropertyDragEnd()

  // edit 모드이고 itemId가 있으면 API 호출
  if (props.mode === 'edit' && props.itemId) {
    await savePropertySortOrders()
  } else {
    toast.success('속성 순서가 변경되었습니다.')
  }
}

// 속성 순서 저장 (API 호출)
async function savePropertySortOrders() {
  if (!props.itemId || propertySortOrders.value.size === 0) return

  try {
    // itemPropertyId를 알 수 없으므로 프론트엔드에서는 propertyId 기반으로 관리
    // 실제 저장은 부모 컴포넌트에서 item 수정 시 처리
    toast.success('속성 순서가 변경되었습니다.')
  } catch (error) {
    console.error('Failed to save property sort orders:', error)
    toast.error('속성 순서 저장에 실패했습니다.')
  }
}

// 그룹 라벨 반환
function getGroupLabel(groupKey: string): string {
  const labels: Record<string, string> = {
    category: '카테고리',
    global: '글로벌',
    manager: '매니저',
    user: '사용자'
  }
  return labels[groupKey] || groupKey
}

// 그룹별 색상 반환
function getGroupColor(groupKey: string) {
  return propertyColors[groupKey as keyof typeof propertyColors] || propertyColors.global
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center">
      <!-- 배경 오버레이 -->
      <div class="fixed inset-0 bg-black/50" @click="handleClose" />

      <!-- 모달 컨테이너 -->
      <div class="relative bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-hidden flex flex-col">
        <!-- 헤더 -->
        <div class="px-6 py-4 border-b">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ mode === 'create' ? '새 업무 등록' : '업무 속성 수정' }}
          </h3>
        </div>

        <!-- 본문 -->
        <div class="flex-1 overflow-y-auto px-6 py-4">
          <!-- 로딩 -->
          <div v-if="loading" class="flex items-center justify-center py-8">
            <svg class="animate-spin h-8 w-8 text-blue-600" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </div>

          <div v-else class="space-y-4">
            <!-- 업무명 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">업무명 *</label>
              <input
                v-model="formData.title"
                type="text"
                placeholder="업무 제목을 입력하세요"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <!-- 설명 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
              <textarea
                v-model="formData.description"
                rows="3"
                placeholder="업무 설명을 입력하세요 (선택)"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <!-- 카테고리 선택 -->
            <div v-if="boardCategories.length > 0">
              <label class="block text-sm font-medium text-gray-700 mb-2">카테고리</label>
              <div class="max-h-32 overflow-y-auto border border-gray-200 rounded-lg p-2 space-y-1">
                <!-- 선택 안함 옵션 -->
                <label
                  class="flex items-center gap-2 p-2 rounded hover:bg-gray-50 cursor-pointer"
                  :class="{ 'bg-blue-50': formData.categoryId === null }"
                >
                  <input
                    type="radio"
                    name="category"
                    :checked="formData.categoryId === null"
                    class="w-4 h-4 text-blue-600 border-gray-300 focus:ring-blue-500"
                    @change="handleCategoryChange(null)"
                  />
                  <span class="text-sm text-gray-500">선택 안함</span>
                </label>
                <!-- 카테고리 목록 -->
                <label
                  v-for="cat in boardCategories"
                  :key="cat.categoryId"
                  class="flex items-center gap-2 p-2 rounded hover:bg-gray-50 cursor-pointer"
                  :class="{ 'bg-blue-50': formData.categoryId === cat.categoryId }"
                >
                  <input
                    type="radio"
                    name="category"
                    :checked="formData.categoryId === cat.categoryId"
                    class="w-4 h-4 text-blue-600 border-gray-300 focus:ring-blue-500"
                    @change="handleCategoryChange(cat.categoryId)"
                  />
                  <div
                    class="w-3 h-3 rounded flex-shrink-0"
                    :style="{ backgroundColor: cat.categoryColor || '#6B7280' }"
                  />
                  <span class="text-sm text-gray-700">
                    {{ cat.categoryName }}
                    <span v-if="cat.isDefault" class="text-xs text-gray-400">(기본)</span>
                  </span>
                </label>
              </div>
              <p class="mt-1 text-xs text-gray-500">업무에 적용할 카테고리를 선택하세요</p>
            </div>

            <!-- 속성 편집 영역 (드래그앤드롭 지원) -->
            <div class="space-y-4">
              <!-- 카테고리 로딩 -->
              <div v-if="categoryLoading" class="flex items-center gap-2 text-sm text-gray-500 py-2">
                <svg class="animate-spin h-4 w-4 text-orange-500" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                카테고리 속성 로딩 중...
              </div>

              <!-- 드래그앤드롭 가능한 속성 목록 -->
              <div v-else-if="flattenedProperties.length > 0">
                <div class="flex items-center justify-between mb-2">
                  <label class="block text-sm font-medium text-gray-700">
                    속성 ({{ flattenedProperties.length }})
                  </label>
                  <span class="text-xs text-gray-400">드래그하여 순서 변경</span>
                </div>
                <div class="space-y-2">
                  <div
                    v-for="(prop, index) in flattenedProperties"
                    :key="prop.propertyId"
                    draggable="true"
                    class="flex items-center gap-3 p-3 rounded-lg border transition-all duration-150 sortable-item"
                    :class="[
                      getGroupColor(prop.groupKey).bg,
                      getGroupColor(prop.groupKey).border,
                      {
                        'sortable-dragging scale-95': draggedPropertyIndex === index,
                        'sortable-drag-over': dropTargetIndex === index && draggedPropertyIndex !== null && draggedPropertyIndex !== index
                      }
                    ]"
                    @dragstart="handlePropertyDragStart(index, prop.groupKey, $event)"
                    @dragover="handlePropertyDragOver(index, $event)"
                    @dragleave="handlePropertyDragLeave"
                    @dragend="handlePropertyDragEnd"
                    @drop="handlePropertyDrop(index, $event)"
                  >
                    <!-- 드래그 핸들 -->
                    <div class="sortable-handle text-gray-400 hover:text-gray-600 flex-shrink-0">
                      <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M8 6a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM8 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM8 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM14 6a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM14 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM14 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0z"/>
                      </svg>
                    </div>

                    <!-- 그룹 뱃지 -->
                    <span
                      class="text-[10px] font-medium px-1.5 py-0.5 rounded flex-shrink-0"
                      :class="[getGroupColor(prop.groupKey).bg, getGroupColor(prop.groupKey).text]"
                    >
                      {{ getGroupLabel(prop.groupKey) }}
                    </span>

                    <!-- 속성명 -->
                    <span
                      class="text-sm font-medium min-w-[80px] flex-shrink-0"
                      :class="getGroupColor(prop.groupKey).text"
                    >
                      {{ prop.propertyName }}
                      <span v-if="prop.requiredYn === 'Y'" class="text-red-500">*</span>
                    </span>

                    <!-- 값 입력 -->
                    <div class="flex-1">
                      <!-- SELECT 타입 -->
                      <select
                        v-if="prop.propertyType === 'SELECT'"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border rounded focus:ring-2"
                        :class="[getGroupColor(prop.groupKey).border, getGroupColor(prop.groupKey).ring]"
                        @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLSelectElement).value || null)"
                      >
                        <option value="">선택</option>
                        <option
                          v-for="opt in propertyOptions.get(prop.propertyId) || []"
                          :key="opt.optionId"
                          :value="opt.optionId"
                        >
                          {{ opt.optionName }}
                        </option>
                      </select>
                      <!-- TEXT 타입 -->
                      <input
                        v-else-if="prop.propertyType === 'TEXT'"
                        type="text"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border rounded focus:ring-2"
                        :class="[getGroupColor(prop.groupKey).border, getGroupColor(prop.groupKey).ring]"
                        placeholder="텍스트 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <!-- NUMBER 타입 -->
                      <input
                        v-else-if="prop.propertyType === 'NUMBER'"
                        type="number"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border rounded focus:ring-2"
                        :class="[getGroupColor(prop.groupKey).border, getGroupColor(prop.groupKey).ring]"
                        placeholder="숫자 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <!-- DATE 타입 -->
                      <input
                        v-else-if="prop.propertyType === 'DATE'"
                        type="date"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border rounded focus:ring-2"
                        :class="[getGroupColor(prop.groupKey).border, getGroupColor(prop.groupKey).ring]"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <!-- v2.0.5: MULTI_SELECT 타입 -->
                      <div
                        v-else-if="prop.propertyType === 'MULTI_SELECT'"
                        class="flex flex-wrap gap-1.5"
                      >
                        <label
                          v-for="opt in propertyOptions.get(prop.propertyId) || []"
                          :key="opt.optionId"
                          class="inline-flex items-center gap-1.5 px-2 py-1 text-sm rounded cursor-pointer transition-colors"
                          :class="isMultiSelectOptionSelected(prop.propertyId, opt.optionId)
                            ? 'bg-blue-100 text-blue-700 border border-blue-200'
                            : 'bg-gray-100 text-gray-600 border border-gray-200 hover:bg-gray-200'"
                        >
                          <input
                            type="checkbox"
                            :checked="isMultiSelectOptionSelected(prop.propertyId, opt.optionId)"
                            class="w-3.5 h-3.5 text-blue-600 border-gray-300 rounded"
                            @change="toggleMultiSelectOption(prop.propertyId, opt.optionId)"
                          />
                          {{ opt.optionName }}
                        </label>
                        <span v-if="!(propertyOptions.get(prop.propertyId) || []).length" class="text-xs text-gray-400">
                          옵션 없음
                        </span>
                      </div>
                      <!-- CHECKBOX 타입 -->
                      <input
                        v-else-if="prop.propertyType === 'CHECKBOX'"
                        type="checkbox"
                        :checked="formData.propertyValues[prop.propertyId] === 'Y' || formData.propertyValues[prop.propertyId] === true"
                        class="w-5 h-5 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                        @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).checked ? 'Y' : 'N')"
                      />
                      <!-- v2.0.5: USER 타입 -->
                      <select
                        v-else-if="prop.propertyType === 'USER'"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border rounded focus:ring-2"
                        :class="[getGroupColor(prop.groupKey).border, getGroupColor(prop.groupKey).ring]"
                        @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLSelectElement).value || null)"
                      >
                        <option value="">사용자 선택</option>
                        <option
                          v-for="user in users"
                          :key="user.username"
                          :value="user.username"
                        >
                          {{ user.name }} ({{ user.username }})
                        </option>
                      </select>
                      <!-- 기타 타입 (텍스트로 처리) -->
                      <input
                        v-else
                        type="text"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border rounded focus:ring-2"
                        :class="[getGroupColor(prop.groupKey).border, getGroupColor(prop.groupKey).ring]"
                        placeholder="값 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <!-- 빈 상태 -->
              <div
                v-if="!loading && !categoryLoading && flattenedProperties.length === 0"
                class="text-center py-8 text-gray-400"
              >
                <p>이 보드에 설정된 카테고리나 속성이 없습니다.</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="flex justify-end gap-3 px-6 py-4 border-t bg-gray-50">
          <button
            type="button"
            class="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-lg"
            @click="handleCancel"
          >
            취소
          </button>
          <button
            type="button"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg"
            @click="handleApply"
          >
            {{ mode === 'create' ? '등록' : '저장' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
