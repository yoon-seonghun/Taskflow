<script setup lang="ts">
/**
 * 작업 템플릿 등록/수정 폼 컴포넌트
 * - 작업 내용, 기본 담당자, 기본 상태 설정
 * - 카테고리 및 속성 선택 (v2.0.5)
 * - 등록/변경 버튼 토글
 */
import { ref, computed, watch, onMounted } from 'vue'
import Input from '@/components/common/Input.vue'
import Select from '@/components/common/Select.vue'
import type { SelectOption } from '@/components/common/Select.vue'
import type { TaskTemplate, TaskTemplateCreateRequest, TaskTemplateUpdateRequest, DefaultItemStatus, TemplatePropertyRequest, TemplateOwnerType } from '@/types/template'
import type { User } from '@/types/user'
import type { Category } from '@/types/category'
import type { PropertyDef } from '@/types/property'
import { userApi } from '@/api/user'
import { categoryApi } from '@/api/category'
import { propertyApi } from '@/api/property'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

interface Props {
  template?: TaskTemplate | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  template: null,
  loading: false
})

const emit = defineEmits<{
  (e: 'submit', data: TaskTemplateCreateRequest | TaskTemplateUpdateRequest, selectedOwnerType: TemplateOwnerType): void
  (e: 'cancel'): void
}>()

const authStore = useAuthStore()
const uiStore = useUiStore()

// Form state
const content = ref('')
const defaultAssigneeUsername = ref<string | null>(null)
const defaultItemStatus = ref<DefaultItemStatus>('NOT_STARTED')
const categoryId = ref<number | null>(null)
const ownerType = ref<TemplateOwnerType>('USER')

// 선택된 속성 및 기본값
const selectedPropertyIds = ref<Set<number>>(new Set())
const propertyDefaults = ref<Record<number, string>>({})

// Users for assignee dropdown
const users = ref<User[]>([])
const loadingUsers = ref(false)

// Categories for dropdown
const categories = ref<Category[]>([])
const loadingCategories = ref(false)

// Available properties
const globalProperties = ref<PropertyDef[]>([])
const managerProperties = ref<PropertyDef[]>([])
const loadingProperties = ref(false)

// 상태 옵션 (완료/삭제 제외)
const statusOptions: SelectOption[] = [
  { value: 'NOT_STARTED', label: '시작전', color: '#6B7280' },
  { value: 'IN_PROGRESS', label: '진행중', color: '#3B82F6' },
  { value: 'PENDING', label: '보류', color: '#F59E0B' }
]

// 소유 유형 옵션
const ownerTypeOptions: SelectOption[] = [
  { value: 'GLOBAL', label: '글로벌', color: '#10B981' },
  { value: 'MANAGER', label: '매니저', color: '#8B5CF6' },
  { value: 'USER', label: '개인', color: '#3B82F6' }
]

// 사용자 권한에 따른 소유 유형 옵션 필터링
const availableOwnerTypeOptions = computed(() => {
  const userRole = authStore.user?.role
  if (userRole === 'ADMIN') {
    return ownerTypeOptions
  } else if (userRole === 'MANAGER') {
    return ownerTypeOptions.filter(opt => opt.value !== 'GLOBAL')
  }
  return ownerTypeOptions.filter(opt => opt.value === 'USER')
})

// 담당자 옵션 (username을 value로 사용)
const userOptions = computed<SelectOption[]>(() => {
  return users.value.map(user => ({
    value: user.username,
    label: user.userName
  }))
})

// 카테고리 옵션
const categoryOptions = computed<SelectOption[]>(() => {
  return categories.value.map(cat => ({
    value: cat.categoryId,
    label: cat.categoryName,
    color: cat.color
  }))
})

// 모든 속성 목록 (글로벌 + 매니저)
const allProperties = computed(() => {
  return [
    ...globalProperties.value.map(p => ({ ...p, ownerType: 'GLOBAL' as const })),
    ...managerProperties.value.map(p => ({ ...p, ownerType: 'MANAGER' as const }))
  ]
})

// 편집 모드 여부
const isEditMode = computed(() => !!props.template)

// 버튼 텍스트
const submitButtonText = computed(() => isEditMode.value ? '변경' : '등록')

// 폼 유효성
const isValid = computed(() => {
  return content.value.trim().length > 0
})

// 사용자 목록 로드
async function loadUsers() {
  loadingUsers.value = true
  try {
    const response = await userApi.getUsers({ useYn: 'Y', size: 100 })
    users.value = response.data.content
  } catch (error) {
    console.error('Failed to load users:', error)
    uiStore.showError('담당자 목록을 불러오는데 실패했습니다.')
  } finally {
    loadingUsers.value = false
  }
}

// 카테고리 목록 로드
async function loadCategories() {
  loadingCategories.value = true
  try {
    const response = await categoryApi.getAccessibleCategories()
    if (response.success && response.data) {
      categories.value = response.data
    }
  } catch (error) {
    console.error('Failed to load categories:', error)
  } finally {
    loadingCategories.value = false
  }
}

// 속성 목록 로드
async function loadProperties() {
  loadingProperties.value = true
  try {
    const [globalRes, managerRes] = await Promise.all([
      propertyApi.getGlobalProperties(),
      propertyApi.getManagerProperties()
    ])
    if (globalRes.success && globalRes.data) {
      globalProperties.value = globalRes.data
    }
    if (managerRes.success && managerRes.data) {
      managerProperties.value = managerRes.data
    }
  } catch (error) {
    console.error('Failed to load properties:', error)
  } finally {
    loadingProperties.value = false
  }
}

// 속성 선택 토글
function toggleProperty(propertyId: number) {
  const newSet = new Set(selectedPropertyIds.value)
  if (newSet.has(propertyId)) {
    newSet.delete(propertyId)
    delete propertyDefaults.value[propertyId]
  } else {
    newSet.add(propertyId)
  }
  selectedPropertyIds.value = newSet
}

// 속성 기본값 변경
function updatePropertyDefault(propertyId: number, value: string) {
  propertyDefaults.value[propertyId] = value
}

// 속성이 선택되었는지 확인
function isPropertySelected(propertyId: number): boolean {
  return selectedPropertyIds.value.has(propertyId)
}

// ownerType 라벨
function getOwnerTypeLabel(ownerType: string): string {
  const labels: Record<string, string> = { GLOBAL: '글로벌', MANAGER: '매니저' }
  return labels[ownerType] || ownerType
}

// ownerType 색상
function getOwnerTypeColor(ownerType: string): string {
  const colors: Record<string, string> = {
    GLOBAL: 'bg-blue-100 text-blue-700',
    MANAGER: 'bg-purple-100 text-purple-700'
  }
  return colors[ownerType] || 'bg-gray-100 text-gray-700'
}

// 폼 초기화
function resetForm() {
  content.value = ''
  // 기본 담당자는 현재 사용자로 설정
  defaultAssigneeUsername.value = authStore.user?.username ?? null
  defaultItemStatus.value = 'NOT_STARTED'
  categoryId.value = null
  ownerType.value = 'USER'
  selectedPropertyIds.value = new Set()
  propertyDefaults.value = {}
}

// 템플릿 데이터로 폼 채우기
function fillForm(template: TaskTemplate) {
  content.value = template.content
  defaultAssigneeUsername.value = template.defaultAssigneeUsername ?? null
  defaultItemStatus.value = template.defaultItemStatus ?? 'NOT_STARTED'
  categoryId.value = template.categoryId ?? null
  ownerType.value = template.ownerType ?? 'USER'

  // 속성 복원
  selectedPropertyIds.value = new Set()
  propertyDefaults.value = {}
  if (template.properties) {
    template.properties.forEach(prop => {
      selectedPropertyIds.value.add(prop.propertyId)
      if (prop.defaultValue) {
        propertyDefaults.value[prop.propertyId] = prop.defaultValue
      }
    })
  }
}

// 속성 요청 배열 생성
function buildPropertiesRequest(): TemplatePropertyRequest[] {
  const result: TemplatePropertyRequest[] = []
  let sortOrder = 1
  selectedPropertyIds.value.forEach(propertyId => {
    result.push({
      propertyId,
      sortOrder: sortOrder++,
      defaultValue: propertyDefaults.value[propertyId] || undefined,
      requiredYn: 'N'
    })
  })
  return result
}

// 제출
function handleSubmit() {
  if (!isValid.value || props.loading) return

  const properties = buildPropertiesRequest()

  if (isEditMode.value) {
    const data: TaskTemplateUpdateRequest = {
      content: content.value.trim(),
      defaultAssigneeUsername: defaultAssigneeUsername.value ?? undefined,
      defaultItemStatus: defaultItemStatus.value,
      categoryId: categoryId.value,
      properties
    }
    emit('submit', data, ownerType.value)
  } else {
    const data: TaskTemplateCreateRequest = {
      content: content.value.trim(),
      defaultAssigneeUsername: defaultAssigneeUsername.value ?? undefined,
      defaultItemStatus: defaultItemStatus.value,
      categoryId: categoryId.value,
      properties
    }
    emit('submit', data, ownerType.value)
  }
}

// 취소
function handleCancel() {
  resetForm()
  emit('cancel')
}

// 템플릿 변경 감지
watch(() => props.template, (newTemplate) => {
  if (newTemplate) {
    fillForm(newTemplate)
  } else {
    resetForm()
  }
}, { immediate: true })

onMounted(() => {
  // 병렬로 데이터 로드
  loadUsers()
  loadCategories()
  loadProperties()

  if (!props.template) {
    // 새 템플릿 등록 시 기본 담당자를 현재 사용자로 설정
    defaultAssigneeUsername.value = authStore.user?.username ?? null
  }
})

// 외부에서 폼 초기화 가능하도록 expose
defineExpose({ resetForm })
</script>

<template>
  <div class="template-form">
    <div class="space-y-4">
      <!-- 작업 내용 -->
      <Input
        v-model="content"
        label="작업 내용"
        placeholder="자주 사용하는 작업 내용을 입력하세요"
        :required="true"
        :maxlength="500"
        :disabled="loading"
      />

      <!-- 소유 유형 (신규 등록 시만 표시) -->
      <Select
        v-if="!isEditMode"
        v-model="ownerType"
        :options="availableOwnerTypeOptions"
        label="템플릿 유형"
        placeholder="유형 선택"
        :disabled="loading"
      />

      <!-- 편집 모드 시 소유 유형 표시 (읽기 전용) -->
      <div v-if="isEditMode && template?.ownerType" class="owner-type-badge">
        <label class="text-sm font-medium text-gray-700 mb-1 block">템플릿 유형</label>
        <span
          class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
          :class="{
            'bg-green-100 text-green-700': template.ownerType === 'GLOBAL',
            'bg-purple-100 text-purple-700': template.ownerType === 'MANAGER',
            'bg-blue-100 text-blue-700': template.ownerType === 'USER'
          }"
        >
          {{ template.ownerType === 'GLOBAL' ? '글로벌' : template.ownerType === 'MANAGER' ? '매니저' : '개인' }}
        </span>
      </div>

      <!-- 기본 담당자 -->
      <Select
        v-model="defaultAssigneeUsername"
        :options="userOptions"
        label="기본 담당자"
        placeholder="담당자 선택"
        :searchable="true"
        :clearable="true"
        :disabled="loading || loadingUsers"
      />

      <!-- 기본 상태 -->
      <Select
        v-model="defaultItemStatus"
        :options="statusOptions"
        label="기본 상태"
        placeholder="상태 선택"
        :disabled="loading"
      />

      <!-- 카테고리 선택 -->
      <Select
        v-model="categoryId"
        :options="categoryOptions"
        label="카테고리"
        placeholder="카테고리 선택 (선택사항)"
        :searchable="true"
        :clearable="true"
        :disabled="loading || loadingCategories"
      />

      <!-- 속성 선택 -->
      <div v-if="allProperties.length > 0" class="property-section">
        <div class="flex items-center justify-between mb-2">
          <label class="text-sm font-medium text-gray-700">템플릿 속성</label>
          <span class="text-xs text-gray-400">{{ selectedPropertyIds.size }}개 선택</span>
        </div>
        <p class="text-xs text-gray-500 mb-3">템플릿 적용 시 자동으로 지정할 속성을 선택하세요</p>

        <div class="property-list">
          <div
            v-for="prop in allProperties"
            :key="prop.propertyId"
            class="property-item"
            :class="{ 'selected': isPropertySelected(prop.propertyId) }"
          >
            <div class="flex items-center gap-2 flex-1">
              <input
                type="checkbox"
                :checked="isPropertySelected(prop.propertyId)"
                class="w-4 h-4 text-blue-600 border-gray-300 rounded cursor-pointer"
                @change="toggleProperty(prop.propertyId)"
              />
              <span
                class="w-2 h-2 rounded-full"
                :class="{
                  'bg-blue-500': prop.ownerType === 'GLOBAL',
                  'bg-purple-500': prop.ownerType === 'MANAGER'
                }"
              ></span>
              <span class="text-sm text-gray-700">{{ prop.propertyName }}</span>
              <span
                class="px-1.5 py-0.5 text-[10px] font-medium rounded"
                :class="getOwnerTypeColor(prop.ownerType)"
              >
                {{ getOwnerTypeLabel(prop.ownerType) }}
              </span>
            </div>

            <!-- 기본값 입력 (선택된 속성만) -->
            <div v-if="isPropertySelected(prop.propertyId)" class="mt-2 pl-6">
              <input
                type="text"
                class="w-full px-2 py-1 text-xs border border-gray-200 rounded focus:outline-none focus:border-blue-400"
                placeholder="기본값 (선택사항)"
                :value="propertyDefaults[prop.propertyId] || ''"
                @input="updatePropertyDefault(prop.propertyId, ($event.target as HTMLInputElement).value)"
              />
            </div>
          </div>
        </div>

        <div v-if="allProperties.length === 0" class="text-center py-4 text-gray-400 text-sm">
          선택 가능한 속성이 없습니다.
        </div>
      </div>

      <!-- 버튼 영역 -->
      <div class="flex items-center gap-2 pt-2">
        <button
          type="button"
          class="btn-primary flex-1"
          :disabled="!isValid || loading"
          @click="handleSubmit"
        >
          <span v-if="loading" class="flex items-center justify-center gap-2">
            <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            처리중...
          </span>
          <span v-else>{{ submitButtonText }}</span>
        </button>

        <button
          v-if="isEditMode"
          type="button"
          class="btn-secondary"
          :disabled="loading"
          @click="handleCancel"
        >
          취소
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.template-form {
  @apply p-4 bg-white rounded-lg border border-gray-200;
}

.btn-primary {
  @apply px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed
         transition-colors duration-150;
}

.btn-secondary {
  @apply px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed
         transition-colors duration-150;
}

.property-section {
  @apply bg-gray-50 rounded-lg p-4 border border-gray-100;
}

.property-list {
  @apply space-y-2 max-h-48 overflow-y-auto pr-1;
}

.property-item {
  @apply p-2.5 bg-white rounded-lg border border-gray-200 transition-colors;
}

.property-item.selected {
  @apply bg-blue-50 border-blue-200;
}

.property-item:hover:not(.selected) {
  @apply border-gray-300;
}
</style>
