<script setup lang="ts">
/**
 * 속성 관리 컴포넌트 (설정 페이지 내 탭용)
 * - v2.0: 글로벌/매니저/사용자 속성 관리
 * - 속성 추가/수정/삭제 (논리 삭제)
 * - 사용 중인 속성의 타입 변경 불가
 */
import { ref, computed, onMounted } from 'vue'
import { propertyApi } from '@/api/property'
import { boardApi } from '@/api/board'
import type { PropertyDef, PropertyCreateRequest, PropertyUpdateRequest, PropertyType } from '@/types/property'
import { useUiStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'

const uiStore = useUiStore()
const authStore = useAuthStore()

// 관리자 여부
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

// 상태
const loading = ref(false)
const saving = ref(false)
const properties = ref<PropertyDef[]>([])
const defaultBoardId = ref<number | null>(null)

// 소유 유형 (v2.0)
type OwnerType = 'GLOBAL' | 'MANAGER' | 'USER'
const selectedOwnerType = ref<OwnerType>('USER')

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

// 편집 상태
const editingProperty = ref<PropertyDef | null>(null)
const showAddForm = ref(false)
const newProperty = ref<PropertyCreateRequest>({
  propertyName: '',
  propertyType: 'TEXT',
  ownerType: 'USER',
  visibleYn: 'Y'
})

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

// 소유 유형 라벨 가져오기
function getOwnerTypeLabel(ownerType?: string): string {
  switch (ownerType) {
    case 'GLOBAL':
      return '글로벌'
    case 'MANAGER':
      return '매니저'
    case 'USER':
    default:
      return '개인'
  }
}

// 소유 유형별 배지 색상
function getOwnerTypeBadgeClass(ownerType?: string): string {
  switch (ownerType) {
    case 'GLOBAL':
      return 'owner-global'
    case 'MANAGER':
      return 'owner-manager'
    case 'USER':
    default:
      return 'owner-user'
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
        주의: 사용 중인 속성의 타입은 변경할 수 없습니다.
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
    <div v-else class="settings-card">
      <div class="settings-card-title flex items-center justify-between">
        <span>추가 속성 목록</span>
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

      <div class="settings-card-content">
        <!-- 추가 폼 -->
        <div v-if="showAddForm" class="mb-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
          <h4 class="text-sm font-medium text-gray-900 mb-3">새 속성 추가</h4>
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

        <!-- 속성 목록 테이블 -->
        <table v-if="properties.length > 0" class="property-table">
          <thead>
            <tr>
              <th>속성명</th>
              <th class="w-[80px]">소유</th>
              <th class="w-[120px]">타입</th>
              <th class="w-[80px]">필수</th>
              <th class="w-[80px]">표시</th>
              <th class="w-[80px]">상태</th>
              <th class="w-[150px]">관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="property in properties" :key="property.propertyId">
              <!-- 편집 모드 -->
              <template v-if="editingProperty?.propertyId === property.propertyId">
                <td>
                  <input
                    v-model="editingProperty.propertyName"
                    type="text"
                    class="form-input"
                  />
                </td>
                <td>
                  <!-- 소유 유형은 변경 불가 -->
                  <span class="owner-badge" :class="getOwnerTypeBadgeClass(property.ownerType)">
                    {{ getOwnerTypeLabel(property.ownerType) }}
                  </span>
                </td>
                <td>
                  <select
                    v-model="editingProperty.propertyType"
                    class="form-input"
                    :disabled="isPropertyInUse(property)"
                    :title="isPropertyInUse(property) ? '사용 중인 속성의 타입은 변경할 수 없습니다.' : ''"
                  >
                    <option v-for="opt in propertyTypeOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                </td>
                <td>
                  <select v-model="editingProperty.requiredYn" class="form-input">
                    <option value="N">선택</option>
                    <option value="Y">필수</option>
                  </select>
                </td>
                <td>
                  <select v-model="editingProperty.visibleYn" class="form-input">
                    <option value="Y">표시</option>
                    <option value="N">숨김</option>
                  </select>
                </td>
                <td>-</td>
                <td>
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
                </td>
              </template>

              <!-- 조회 모드 -->
              <template v-else>
                <td class="font-medium text-gray-900">
                  {{ property.propertyName }}
                </td>
                <td>
                  <span class="owner-badge" :class="getOwnerTypeBadgeClass(property.ownerType)">
                    {{ getOwnerTypeLabel(property.ownerType) }}
                  </span>
                </td>
                <td>
                  <span class="type-badge">
                    {{ getPropertyTypeLabel(property.propertyType) }}
                  </span>
                </td>
                <td class="text-center">
                  <span v-if="property.requiredYn === 'Y'" class="text-red-500">필수</span>
                  <span v-else class="text-gray-400">-</span>
                </td>
                <td class="text-center">
                  <span v-if="property.visibleYn === 'Y'" class="text-green-600">O</span>
                  <span v-else class="text-gray-400">X</span>
                </td>
                <td>
                  <span
                    class="status-badge"
                    :class="property.useYn === 'Y' || property.useYn === undefined ? 'active' : 'inactive'"
                  >
                    {{ property.useYn === 'Y' || property.useYn === undefined ? '활성' : '비활성' }}
                  </span>
                </td>
                <td>
                  <div class="flex items-center gap-1">
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
                </td>
              </template>
            </tr>
          </tbody>
        </table>

        <!-- 빈 상태 -->
        <div v-else class="py-8 text-center">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
              d="M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z" />
          </svg>
          <p class="mt-2 text-sm text-gray-500">추가 속성이 없습니다.</p>
          <button
            type="button"
            class="mt-3 text-sm text-primary-600 hover:text-primary-700"
            @click="handleShowAddForm"
          >
            + 첫 번째 속성 추가하기
          </button>
        </div>
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

.property-table {
  @apply w-full text-sm;
}

.property-table thead {
  @apply bg-gray-50 border-y border-gray-200;
}

.property-table th {
  @apply px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider;
}

.property-table tbody {
  @apply divide-y divide-gray-100;
}

.property-table td {
  @apply px-4 py-3 whitespace-nowrap;
}

.property-table tbody tr {
  @apply hover:bg-gray-50 transition-colors;
}

.type-badge {
  @apply inline-flex items-center px-2 py-0.5 text-xs font-medium rounded bg-blue-50 text-blue-700;
}

.status-badge {
  @apply inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full;
}

.status-badge.active {
  @apply bg-green-100 text-green-700;
}

.status-badge.inactive {
  @apply bg-gray-100 text-gray-600;
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

/* 소유 유형 배지 */
.owner-badge {
  @apply inline-flex items-center px-2 py-0.5 text-xs font-medium rounded;
}

.owner-badge.owner-global {
  @apply bg-purple-100 text-purple-700;
}

.owner-badge.owner-manager {
  @apply bg-amber-100 text-amber-700;
}

.owner-badge.owner-user {
  @apply bg-gray-100 text-gray-600;
}
</style>
