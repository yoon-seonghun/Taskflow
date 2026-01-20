<script setup lang="ts">
/**
 * 카테고리 상세 모달 컴포넌트
 * - 카테고리 기본 정보 표시
 * - 공유 관리 (사용자/부서)
 * - 속성 관리
 */
import { ref, computed, watch } from 'vue'
import { useCategoryStore } from '@/stores/category'
import { useUiStore } from '@/stores/ui'
import { departmentApi } from '@/api/department'
import { propertyApi } from '@/api/property'
import type { CategoryDetail, CategoryShare, CategoryProperty, CategoryShareRequest, CategoryPropertyRequest } from '@/types/category'
import type { User } from '@/types/user'
import type { Department } from '@/types/department'
import { userApi } from '@/api/user'
import UserSearchSelector from '@/components/common/UserSearchSelector.vue'

interface Props {
  modelValue: boolean
  categoryId: number | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'updated'): void
}>()

const categoryStore = useCategoryStore()
const uiStore = useUiStore()

// 상태
const loading = ref(false)
const saving = ref(false)
const category = ref<CategoryDetail | null>(null)
const activeTab = ref<'shares' | 'properties'>('shares')

// 공유 추가 관련
const showAddShareForm = ref(false)
const shareType = ref<'USER' | 'DEPARTMENT'>('USER')
const selectedUser = ref<User | null>(null)
const selectedDepartment = ref<Department | null>(null)
const sharePermission = ref<'VIEW' | 'EDIT'>('VIEW')

// 속성 추가 관련
const showAddPropertyForm = ref(false)
const availableProperties = ref<{ propertyId: number; propertyName: string; propertyType: string }[]>([])
const selectedPropertyId = ref<number | null>(null)
const propertyDefaultValue = ref('')

// 속성 드래그앤드롭 관련
const draggedPropertyIndex = ref<number | null>(null)
const dropTargetIndex = ref<number | null>(null)

// 부서 목록
const departments = ref<Department[]>([])

// 이미 공유된 사용자 username 목록
const excludedUsernames = computed(() => {
  return category.value?.shares
    ?.filter(s => s.shareType === 'USER')
    .map(s => s.shareTarget) || []
})

// 이미 추가된 속성 ID 목록
const excludedPropertyIds = computed(() => {
  return category.value?.properties?.map(p => p.propertyId) || []
})

// 추가 가능한 속성 목록
const selectableProperties = computed(() => {
  return availableProperties.value.filter(p => !excludedPropertyIds.value.includes(p.propertyId))
})

// 카테고리 상세 로드
async function loadCategoryDetail() {
  if (!props.categoryId) return

  loading.value = true
  try {
    const detail = await categoryStore.fetchCategoryDetail(props.categoryId)
    category.value = detail
  } catch (error) {
    console.error('Failed to load category detail:', error)
    uiStore.showError('카테고리 상세를 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

// 부서 목록 로드
async function loadDepartments() {
  try {
    const response = await departmentApi.getDepartments({ useYn: 'Y' })
    departments.value = flattenDepartments(response.data || [])
  } catch (error) {
    console.error('Failed to load departments:', error)
  }
}

// 부서 트리를 평면 목록으로 변환
function flattenDepartments(depts: Department[], result: Department[] = []): Department[] {
  for (const dept of depts) {
    result.push(dept)
    if (dept.children && dept.children.length > 0) {
      flattenDepartments(dept.children, result)
    }
  }
  return result
}

// 속성 목록 로드 (카테고리 귀속 속성 = 현재 사용자가 생성한 USER 타입 속성만)
// - ADMIN/MANAGER/USER 역할과 관계없이 "사용자"로서 본인이 생성한 속성만 조회
async function loadAvailableProperties() {
  try {
    const response = await propertyApi.getUserProperties()
    availableProperties.value = response.data.map(p => ({
      propertyId: p.propertyId,
      propertyName: p.propertyName,
      propertyType: p.propertyType
    }))
  } catch (error) {
    console.error('Failed to load properties:', error)
  }
}

// 모달 닫기
function closeModal() {
  emit('update:modelValue', false)
  resetForms()
}

// 폼 초기화
function resetForms() {
  showAddShareForm.value = false
  showAddPropertyForm.value = false
  shareType.value = 'USER'
  selectedUser.value = null
  selectedDepartment.value = null
  sharePermission.value = 'VIEW'
  selectedPropertyId.value = null
  propertyDefaultValue.value = ''
}

// ==================== 공유 관리 ====================

// 공유 추가 폼 표시
function handleShowAddShareForm() {
  showAddShareForm.value = true
  selectedUser.value = null
  selectedDepartment.value = null
}

// 공유 추가 취소
function handleCancelAddShare() {
  showAddShareForm.value = false
  selectedUser.value = null
  selectedDepartment.value = null
}

// 사용자 선택 핸들러
function handleUserSelect(user: User | null) {
  selectedUser.value = user
}

// 공유 추가
async function handleAddShare() {
  if (!category.value) return

  let request: CategoryShareRequest

  // 프론트엔드 VIEW -> 백엔드 READ 변환
  const apiPermission = sharePermission.value === 'VIEW' ? 'READ' : 'EDIT'

  if (shareType.value === 'USER') {
    if (!selectedUser.value) {
      uiStore.showWarning('사용자를 선택해주세요.')
      return
    }
    request = {
      shareType: 'USER',
      shareTarget: selectedUser.value.username,
      permission: apiPermission
    }
  } else {
    if (!selectedDepartment.value) {
      uiStore.showWarning('부서를 선택해주세요.')
      return
    }
    request = {
      shareType: 'DEPARTMENT',
      shareTarget: selectedDepartment.value.departmentCode,
      permission: apiPermission
    }
  }

  saving.value = true
  try {
    await categoryStore.addShare(category.value.categoryId, request)
    uiStore.showSuccess('공유가 추가되었습니다.')
    handleCancelAddShare()
    await loadCategoryDetail()
    emit('updated')
  } catch (error: any) {
    console.error('Failed to add share:', error)
    const message = error.response?.data?.message || '공유 추가에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    saving.value = false
  }
}

// 공유 권한 변경
async function handleUpdateSharePermission(share: CategoryShare, newPermission: 'READ' | 'EDIT') {
  if (!category.value) return

  saving.value = true
  try {
    // 백엔드는 READ/EDIT 사용
    await categoryStore.updateSharePermission(category.value.categoryId, share.shareId, newPermission)
    uiStore.showSuccess('권한이 변경되었습니다.')
    await loadCategoryDetail()
    emit('updated')
  } catch (error: any) {
    console.error('Failed to update share permission:', error)
    uiStore.showError('권한 변경에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

// 공유 삭제
async function handleRemoveShare(share: CategoryShare) {
  if (!category.value) return

  const confirmed = await uiStore.confirm({
    title: '공유 해제',
    message: `'${share.shareTargetName || share.shareTarget}'의 공유를 해제하시겠습니까?`,
    confirmText: '해제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  saving.value = true
  try {
    await categoryStore.removeShare(category.value.categoryId, share.shareId)
    uiStore.showSuccess('공유가 해제되었습니다.')
    await loadCategoryDetail()
    emit('updated')
  } catch (error: any) {
    console.error('Failed to remove share:', error)
    uiStore.showError('공유 해제에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

// ==================== 속성 관리 ====================

// 속성 추가 폼 표시
function handleShowAddPropertyForm() {
  showAddPropertyForm.value = true
  selectedPropertyId.value = null
  propertyDefaultValue.value = ''
}

// 속성 추가 취소
function handleCancelAddProperty() {
  showAddPropertyForm.value = false
  selectedPropertyId.value = null
  propertyDefaultValue.value = ''
}

// 속성 추가
async function handleAddProperty() {
  if (!category.value || !selectedPropertyId.value) {
    uiStore.showWarning('속성을 선택해주세요.')
    return
  }

  const request: CategoryPropertyRequest = {
    propertyId: selectedPropertyId.value,
    defaultValue: propertyDefaultValue.value || undefined
  }

  saving.value = true
  try {
    await categoryStore.addProperty(category.value.categoryId, request)
    uiStore.showSuccess('속성이 추가되었습니다.')
    handleCancelAddProperty()
    await loadCategoryDetail()
    emit('updated')
  } catch (error: any) {
    console.error('Failed to add property:', error)
    // axios 에러의 경우 response.data.message 또는 response.data.error 사용
    // 일반 에러의 경우 error.message 사용
    let message = '속성 추가에 실패했습니다.'
    if (error.response?.data?.message) {
      message = error.response.data.message
    } else if (error.response?.data?.error) {
      message = error.response.data.error
    } else if (error.message) {
      message = error.message
    }
    uiStore.showError(message)
  } finally {
    saving.value = false
  }
}

// 속성 삭제
async function handleRemoveProperty(property: CategoryProperty) {
  if (!category.value) return

  const confirmed = await uiStore.confirm({
    title: '속성 제거',
    message: `'${property.propertyName}' 속성을 이 카테고리에서 제거하시겠습니까?`,
    confirmText: '제거',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  saving.value = true
  try {
    await categoryStore.removeProperty(category.value.categoryId, property.propertyId)
    uiStore.showSuccess('속성이 제거되었습니다.')
    await loadCategoryDetail()
    emit('updated')
  } catch (error: any) {
    console.error('Failed to remove property:', error)
    uiStore.showError('속성 제거에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

// 속성 기본값 변경
async function handleUpdatePropertyDefaultValue(property: CategoryProperty, newValue: string) {
  if (!category.value) return

  saving.value = true
  try {
    await categoryStore.updatePropertyDefaultValue(category.value.categoryId, property.propertyId, newValue)
    uiStore.showSuccess('기본값이 변경되었습니다.')
    await loadCategoryDetail()
    emit('updated')
  } catch (error: any) {
    console.error('Failed to update property default value:', error)
    uiStore.showError('기본값 변경에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

// 속성 타입 라벨
function getPropertyTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    TEXT: '텍스트',
    NUMBER: '숫자',
    DATE: '날짜',
    SELECT: '선택',
    MULTI_SELECT: '다중선택',
    CHECKBOX: '체크박스',
    USER: '사용자'
  }
  return labels[type] || type
}

// ==================== 속성 드래그앤드롭 ====================

// 드래그 시작
function handlePropertyDragStart(index: number, event: DragEvent) {
  draggedPropertyIndex.value = index
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
}

// 드롭
async function handlePropertyDrop(toIndex: number, event: DragEvent) {
  event.preventDefault()

  const fromIndex = draggedPropertyIndex.value
  if (fromIndex === null || fromIndex === toIndex || !category.value) {
    handlePropertyDragEnd()
    return
  }

  const properties = category.value.properties
  if (!properties || properties.length === 0) {
    handlePropertyDragEnd()
    return
  }

  // 로컬에서 먼저 순서 변경 (Optimistic Update)
  const [movedItem] = properties.splice(fromIndex, 1)
  properties.splice(toIndex, 0, movedItem)

  // 새로운 순서 계산
  const orders = properties.map((prop, idx) => ({
    propertyId: prop.propertyId,
    sortOrder: idx + 1
  }))

  // 로컬 상태 업데이트
  properties.forEach((prop, idx) => {
    prop.sortOrder = idx + 1
  })

  handlePropertyDragEnd()

  // API 호출
  try {
    await categoryStore.reorderProperties(category.value.categoryId, orders)
    uiStore.showSuccess('속성 순서가 변경되었습니다.')
  } catch (error) {
    console.error('Failed to reorder properties:', error)
    uiStore.showError('속성 순서 변경에 실패했습니다.')
    // 실패 시 원래대로 복원
    await loadCategoryDetail()
  }
}

// 권한 라벨
function getPermissionLabel(permission: string): string {
  return permission === 'READ' || permission === 'VIEW' ? '읽기' : '편집'
}

// 모달 열릴 때 데이터 로드
watch(() => props.modelValue, (isOpen) => {
  if (isOpen && props.categoryId) {
    loadCategoryDetail()
    loadDepartments()
    loadAvailableProperties()
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/50" @click="closeModal" />

        <!-- Modal -->
        <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-3xl max-h-[85vh] flex flex-col" @click.stop>
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <div class="flex items-center gap-3">
              <div
                v-if="category?.categoryColor"
                class="w-4 h-4 rounded"
                :style="{ backgroundColor: category.categoryColor }"
              ></div>
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
                {{ category?.categoryName || '카테고리 상세' }}
              </h2>
              <span v-if="category?.categoryCode" class="text-sm text-gray-500 dark:text-gray-400">
                ({{ category.categoryCode }})
              </span>
            </div>
            <button
              type="button"
              class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:text-gray-500 dark:hover:text-gray-300 dark:hover:bg-gray-700 rounded-lg transition-colors"
              @click="closeModal"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 로딩 -->
          <div v-if="loading" class="flex-1 flex items-center justify-center py-12">
            <svg class="animate-spin h-8 w-8 text-primary-600" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </div>

          <!-- Content -->
          <div v-else-if="category" class="flex-1 overflow-hidden flex flex-col">
            <!-- 카테고리 기본 정보 -->
            <div class="px-6 py-4 bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-700">
              <div class="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span class="text-gray-500 dark:text-gray-400">소유자:</span>
                  <span class="ml-2 font-medium text-gray-900 dark:text-white">{{ category.ownerName || category.ownerUsername }}</span>
                </div>
                <div v-if="category.description">
                  <span class="text-gray-500 dark:text-gray-400">설명:</span>
                  <span class="ml-2 text-gray-700 dark:text-gray-300">{{ category.description }}</span>
                </div>
                <div>
                  <span class="text-gray-500 dark:text-gray-400">사용 보드:</span>
                  <span class="ml-2 text-gray-700 dark:text-gray-300">{{ category.boardCount || 0 }}개</span>
                </div>
                <div>
                  <span class="text-gray-500 dark:text-gray-400">상태:</span>
                  <span
                    class="ml-2 px-2 py-0.5 text-xs font-medium rounded-full"
                    :class="category.useYn === 'Y' ? 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400' : 'bg-gray-100 text-gray-600 dark:bg-gray-600 dark:text-gray-300'"
                  >
                    {{ category.useYn === 'Y' ? '활성' : '비활성' }}
                  </span>
                </div>
              </div>
            </div>

            <!-- 탭 -->
            <div class="border-b border-gray-200 dark:border-gray-700">
              <div class="flex px-6">
                <button
                  type="button"
                  class="px-4 py-3 text-sm font-medium border-b-2 transition-colors -mb-px"
                  :class="activeTab === 'shares'
                    ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                    : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'"
                  @click="activeTab = 'shares'"
                >
                  공유 관리
                  <span class="ml-1.5 px-1.5 py-0.5 text-xs rounded-full bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300">
                    {{ category.shares?.length || 0 }}
                  </span>
                </button>
                <button
                  type="button"
                  class="px-4 py-3 text-sm font-medium border-b-2 transition-colors -mb-px"
                  :class="activeTab === 'properties'
                    ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                    : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'"
                  @click="activeTab = 'properties'"
                >
                  속성 관리
                  <span class="ml-1.5 px-1.5 py-0.5 text-xs rounded-full bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300">
                    {{ category.properties?.length || 0 }}
                  </span>
                </button>
              </div>
            </div>

            <!-- 탭 컨텐츠 -->
            <div class="flex-1 overflow-y-auto p-6">
              <!-- 공유 관리 탭 -->
              <div v-if="activeTab === 'shares'" class="space-y-4">
                <!-- 공유 추가 버튼 -->
                <div v-if="!showAddShareForm" class="flex justify-end">
                  <button
                    type="button"
                    class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-primary-600 bg-primary-50 rounded-lg hover:bg-primary-100 transition-colors"
                    @click="handleShowAddShareForm"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                    </svg>
                    공유 추가
                  </button>
                </div>

                <!-- 공유 추가 폼 -->
                <div v-if="showAddShareForm" class="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg border border-gray-200 dark:border-gray-600">
                  <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">새 공유 추가</h4>

                  <!-- 공유 타입 선택 -->
                  <div class="mb-4">
                    <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">공유 대상</label>
                    <div class="flex gap-4">
                      <label class="inline-flex items-center">
                        <input
                          v-model="shareType"
                          type="radio"
                          value="USER"
                          class="text-primary-600 focus:ring-primary-500"
                        />
                        <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">사용자</span>
                      </label>
                      <label class="inline-flex items-center">
                        <input
                          v-model="shareType"
                          type="radio"
                          value="DEPARTMENT"
                          class="text-primary-600 focus:ring-primary-500"
                        />
                        <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">부서</span>
                      </label>
                    </div>
                  </div>

                  <!-- 사용자 선택 -->
                  <div v-if="shareType === 'USER'" class="mb-4">
                    <UserSearchSelector
                      :exclude-usernames="excludedUsernames"
                      placeholder="공유할 사용자를 선택하세요"
                      @select="handleUserSelect"
                    />
                  </div>

                  <!-- 부서 선택 -->
                  <div v-else class="mb-4">
                    <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">부서 선택</label>
                    <select
                      v-model="selectedDepartment"
                      class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 rounded-lg focus:ring-primary-500 focus:border-primary-500"
                    >
                      <option :value="null">부서를 선택하세요</option>
                      <option
                        v-for="dept in departments"
                        :key="dept.departmentId"
                        :value="dept"
                        :disabled="category.shares?.some(s => s.shareType === 'DEPARTMENT' && s.shareTarget === dept.departmentCode)"
                      >
                        {{ dept.departmentName }}
                      </option>
                    </select>
                  </div>

                  <!-- 권한 선택 -->
                  <div class="mb-4">
                    <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">권한</label>
                    <select
                      v-model="sharePermission"
                      class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 rounded-lg focus:ring-primary-500 focus:border-primary-500"
                    >
                      <option value="VIEW">읽기</option>
                      <option value="EDIT">편집</option>
                    </select>
                  </div>

                  <!-- 버튼 -->
                  <div class="flex justify-end gap-2">
                    <button
                      type="button"
                      class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600"
                      @click="handleCancelAddShare"
                      :disabled="saving"
                    >
                      취소
                    </button>
                    <button
                      type="button"
                      class="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50"
                      @click="handleAddShare"
                      :disabled="saving"
                    >
                      {{ saving ? '추가 중...' : '추가' }}
                    </button>
                  </div>
                </div>

                <!-- 공유 목록 -->
                <div v-if="category.shares && category.shares.length > 0" class="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden">
                  <table class="w-full text-sm">
                    <thead class="bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600">
                      <tr>
                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">유형</th>
                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">대상</th>
                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">권한</th>
                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">공유일</th>
                        <th class="px-4 py-3 w-24"></th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
                      <tr v-for="share in category.shares" :key="share.shareId" class="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                        <td class="px-4 py-3">
                          <span
                            class="px-2 py-0.5 text-xs font-medium rounded-full"
                            :class="share.shareType === 'USER' ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400' : 'bg-purple-100 text-purple-700 dark:bg-purple-900/50 dark:text-purple-400'"
                          >
                            {{ share.shareType === 'USER' ? '사용자' : '부서' }}
                          </span>
                        </td>
                        <td class="px-4 py-3 font-medium text-gray-900 dark:text-white">
                          {{ share.shareTargetName || share.shareTarget }}
                        </td>
                        <td class="px-4 py-3">
                          <select
                            :value="share.permission"
                            class="px-2 py-1 text-xs border border-gray-300 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 rounded focus:ring-primary-500 focus:border-primary-500"
                            @change="handleUpdateSharePermission(share, ($event.target as HTMLSelectElement).value as 'READ' | 'EDIT')"
                            :disabled="saving"
                          >
                            <option value="READ">읽기</option>
                            <option value="EDIT">편집</option>
                          </select>
                        </td>
                        <td class="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs">
                          {{ share.sharedAt ? new Date(share.sharedAt).toLocaleDateString() : '-' }}
                        </td>
                        <td class="px-4 py-3">
                          <button
                            type="button"
                            class="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded transition-colors"
                            title="공유 해제"
                            @click="handleRemoveShare(share)"
                            :disabled="saving"
                          >
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                          </button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- 공유 없음 -->
                <div v-else-if="!showAddShareForm" class="py-8 text-center">
                  <svg class="mx-auto h-12 w-12 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                      d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                  <p class="mt-2 text-sm text-gray-500 dark:text-gray-400">아직 공유된 사용자/부서가 없습니다.</p>
                </div>
              </div>

              <!-- 속성 관리 탭 -->
              <div v-else-if="activeTab === 'properties'" class="space-y-4">
                <!-- 속성 추가 버튼 -->
                <div v-if="!showAddPropertyForm" class="flex justify-end">
                  <button
                    type="button"
                    class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-primary-600 bg-primary-50 rounded-lg hover:bg-primary-100 transition-colors"
                    @click="handleShowAddPropertyForm"
                    :disabled="selectableProperties.length === 0"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                    </svg>
                    속성 추가
                  </button>
                </div>

                <!-- 속성 추가 폼 -->
                <div v-if="showAddPropertyForm" class="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg border border-gray-200 dark:border-gray-600">
                  <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">속성 추가</h4>

                  <!-- 속성 선택 -->
                  <div class="mb-4">
                    <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">속성 선택</label>
                    <select
                      v-model="selectedPropertyId"
                      class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 rounded-lg focus:ring-primary-500 focus:border-primary-500"
                    >
                      <option :value="null">속성을 선택하세요</option>
                      <option v-for="prop in selectableProperties" :key="prop.propertyId" :value="prop.propertyId">
                        {{ prop.propertyName }} ({{ getPropertyTypeLabel(prop.propertyType) }})
                      </option>
                    </select>
                  </div>

                  <!-- 기본값 입력 -->
                  <div class="mb-4">
                    <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">기본값 (선택)</label>
                    <input
                      v-model="propertyDefaultValue"
                      type="text"
                      class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 rounded-lg focus:ring-primary-500 focus:border-primary-500"
                      placeholder="이 카테고리의 업무에 적용될 기본값"
                    />
                  </div>

                  <!-- 버튼 -->
                  <div class="flex justify-end gap-2">
                    <button
                      type="button"
                      class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600"
                      @click="handleCancelAddProperty"
                      :disabled="saving"
                    >
                      취소
                    </button>
                    <button
                      type="button"
                      class="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50"
                      @click="handleAddProperty"
                      :disabled="saving || !selectedPropertyId"
                    >
                      {{ saving ? '추가 중...' : '추가' }}
                    </button>
                  </div>
                </div>

                <!-- 속성 목록 -->
                <div v-if="category.properties && category.properties.length > 0" class="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden">
                  <table class="w-full text-sm">
                    <thead class="bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600">
                      <tr>
                        <th class="w-10 px-2 py-3"></th>
                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">순서</th>
                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">속성명</th>
                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">타입</th>
                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">기본값</th>
                        <th class="px-4 py-3 w-20"></th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
                      <tr
                        v-for="(property, index) in category.properties"
                        :key="property.propertyId"
                        draggable="true"
                        class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-all duration-150 sortable-item"
                        :class="{
                          'sortable-dragging': draggedPropertyIndex === index,
                          'sortable-drag-over': dropTargetIndex === index && draggedPropertyIndex !== null && draggedPropertyIndex !== index
                        }"
                        @dragstart="handlePropertyDragStart(index, $event)"
                        @dragover="handlePropertyDragOver(index, $event)"
                        @dragleave="handlePropertyDragLeave"
                        @dragend="handlePropertyDragEnd"
                        @drop="handlePropertyDrop(index, $event)"
                      >
                        <!-- 드래그 핸들 -->
                        <td class="w-10 px-2 py-3">
                          <div class="sortable-handle text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300">
                            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                              <path d="M8 6a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM8 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM8 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM14 6a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM14 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0zM14 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0z"/>
                            </svg>
                          </div>
                        </td>
                        <td class="px-4 py-3 text-gray-500 dark:text-gray-400">{{ property.sortOrder || index + 1 }}</td>
                        <td class="px-4 py-3 font-medium text-gray-900 dark:text-white">{{ property.propertyName }}</td>
                        <td class="px-4 py-3">
                          <span class="px-2 py-0.5 text-xs font-medium rounded-full bg-gray-100 text-gray-600 dark:bg-gray-600 dark:text-gray-300">
                            {{ getPropertyTypeLabel(property.propertyType) }}
                          </span>
                        </td>
                        <td class="px-4 py-3 text-gray-600 dark:text-gray-300">
                          {{ property.defaultValue || '-' }}
                        </td>
                        <td class="px-4 py-3">
                          <button
                            type="button"
                            class="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded transition-colors"
                            title="속성 제거"
                            @click="handleRemoveProperty(property)"
                            :disabled="saving"
                          >
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                          </button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- 속성 없음 -->
                <div v-else-if="!showAddPropertyForm" class="py-8 text-center">
                  <svg class="mx-auto h-12 w-12 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                      d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <p class="mt-2 text-sm text-gray-500 dark:text-gray-400">아직 연결된 속성이 없습니다.</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 rounded-b-xl">
            <div class="flex justify-end">
              <button
                type="button"
                class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600"
                @click="closeModal"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
