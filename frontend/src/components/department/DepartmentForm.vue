<script setup lang="ts">
/**
 * 부서 등록/수정 폼 컴포넌트
 * - 부서 코드, 부서명, 상위 부서, 정렬 순서, 사용 여부
 */
import { ref, computed, watch } from 'vue'
import Input from '@/components/common/Input.vue'
import Select from '@/components/common/Select.vue'
import type { SelectOption } from '@/components/common/Select.vue'
import type { Department, DepartmentCreateRequest, DepartmentUpdateRequest } from '@/types/department'
import { useDepartmentStore } from '@/stores/department'

interface Props {
  department?: Department | null
  parentDepartment?: Department | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  department: null,
  parentDepartment: null,
  loading: false
})

const emit = defineEmits<{
  (e: 'submit', data: DepartmentCreateRequest | DepartmentUpdateRequest): void
  (e: 'cancel'): void
}>()

const departmentStore = useDepartmentStore()

// Form fields
const departmentCode = ref('')
const departmentName = ref('')
const parentId = ref<number | null>(null)
const sortOrder = ref(0)
const useYn = ref('Y')

// 편집 모드 여부
const isEditMode = computed(() => !!props.department)

// 버튼 텍스트
const submitButtonText = computed(() => isEditMode.value ? '수정' : '등록')

// 제목 텍스트
const formTitle = computed(() => {
  if (isEditMode.value) {
    return '부서 수정'
  }
  if (props.parentDepartment) {
    return `하위 부서 추가 (${props.parentDepartment.departmentName})`
  }
  return '부서 등록'
})

// 상위 부서 옵션 (현재 부서 및 하위 부서 제외)
const parentOptions = computed<SelectOption[]>(() => {
  const options: SelectOption[] = [{ value: null as any, label: '(최상위)' }]

  const addOptions = (depts: Department[], prefix = '') => {
    for (const dept of depts) {
      // 편집 모드에서 자기 자신과 하위 부서는 제외
      if (isEditMode.value && props.department) {
        if (dept.departmentId === props.department.departmentId) continue
        if (isDescendantOf(dept, props.department.departmentId)) continue
      }

      options.push({
        value: dept.departmentId,
        label: prefix + dept.departmentName
      })

      if (dept.children?.length) {
        addOptions(dept.children, prefix + '  ')
      }
    }
  }

  // null 체크 추가
  if (departmentStore.departments?.length) {
    addOptions(departmentStore.departments)
  }
  return options
})

// 사용 여부 옵션
const useYnOptions: SelectOption[] = [
  { value: 'Y', label: '활성' },
  { value: 'N', label: '비활성' }
]

// 폼 유효성
const isValid = computed(() => {
  return departmentCode.value.trim().length > 0 && departmentName.value.trim().length > 0
})

// 부서 코드 유효성 (영문 대문자+숫자+언더스코어, 2~20자)
const codeValidation = computed(() => {
  const code = departmentCode.value
  if (!code) return { isValid: true, message: '' }

  const validLength = code.length >= 2 && code.length <= 20
  const validChars = /^[A-Z0-9_]+$/.test(code)

  if (!validChars) {
    return { isValid: false, message: '영문 대문자, 숫자, 언더스코어만 사용 가능합니다.' }
  }
  if (!validLength) {
    return { isValid: false, message: '2~20자로 입력해주세요.' }
  }
  return { isValid: true, message: '' }
})

// 부서 코드 자동 대문자 변환
watch(departmentCode, (newVal) => {
  if (newVal && newVal !== newVal.toUpperCase()) {
    departmentCode.value = newVal.toUpperCase()
  }
})

// 자손 여부 확인
function isDescendantOf(dept: Department, parentId: number): boolean {
  if (dept.parentId === parentId) return true
  if (!dept.children) return false
  return dept.children.some(child => isDescendantOf(child, parentId))
}

// 폼 초기화
function resetForm() {
  departmentCode.value = ''
  departmentName.value = ''
  parentId.value = props.parentDepartment?.departmentId ?? null
  sortOrder.value = 0
  useYn.value = 'Y'
}

// 부서 데이터로 폼 채우기
function fillForm(department: Department) {
  departmentCode.value = department.departmentCode
  departmentName.value = department.departmentName
  parentId.value = department.parentId ?? null
  sortOrder.value = department.sortOrder
  useYn.value = department.useYn
}

// 제출
function handleSubmit() {
  if (!isValid.value || !codeValidation.value.isValid || props.loading) return

  if (isEditMode.value) {
    const data: DepartmentUpdateRequest = {
      departmentCode: departmentCode.value.trim(),
      departmentName: departmentName.value.trim(),
      parentId: parentId.value ?? undefined,
      sortOrder: sortOrder.value,
      useYn: useYn.value
    }
    emit('submit', data)
  } else {
    const data: DepartmentCreateRequest = {
      departmentCode: departmentCode.value.trim(),
      departmentName: departmentName.value.trim(),
      parentId: parentId.value ?? undefined,
      sortOrder: sortOrder.value
    }
    emit('submit', data)
  }
}

// 취소
function handleCancel() {
  resetForm()
  emit('cancel')
}

// 부서 변경 감지
watch(() => props.department, (newDepartment) => {
  if (newDepartment) {
    fillForm(newDepartment)
  } else {
    resetForm()
  }
}, { immediate: true })

// 상위 부서 변경 감지 (하위 부서 추가 시)
watch(() => props.parentDepartment, (newParent) => {
  if (!props.department && newParent) {
    parentId.value = newParent.departmentId
  }
}, { immediate: true })

// 외부에서 폼 초기화 가능하도록 expose
defineExpose({ resetForm })
</script>

<template>
  <div class="department-form">
    <h3 class="form-title">{{ formTitle }}</h3>

    <div class="space-y-4">
      <!-- 부서 코드 -->
      <Input
        v-model="departmentCode"
        label="부서 코드"
        placeholder="영문 대문자, 숫자, 언더스코어 (예: DEV_TEAM)"
        :required="true"
        :maxlength="20"
        :disabled="loading"
        :error="!codeValidation.isValid && departmentCode.length > 0"
        :error-message="codeValidation.message"
      />

      <!-- 부서명 -->
      <Input
        v-model="departmentName"
        label="부서명"
        placeholder="부서명을 입력하세요"
        :required="true"
        :maxlength="100"
        :disabled="loading"
      />

      <!-- 상위 부서 -->
      <Select
        v-model="parentId"
        :options="parentOptions"
        label="상위 부서"
        placeholder="상위 부서 선택"
        :searchable="true"
        :disabled="loading"
      />

      <!-- 정렬 순서 -->
      <Input
        v-model.number="sortOrder"
        type="number"
        label="정렬 순서"
        placeholder="0"
        :disabled="loading"
      />

      <!-- 사용 여부 (편집 모드에서만) -->
      <Select
        v-if="isEditMode"
        v-model="useYn"
        :options="useYnOptions"
        label="사용 여부"
        :disabled="loading"
      />

      <!-- 버튼 영역 -->
      <div class="flex items-center gap-2 pt-2">
        <button
          type="button"
          class="btn-primary flex-1"
          :disabled="!isValid || !codeValidation.isValid || loading"
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
.department-form {
  @apply p-4 bg-white rounded-lg border border-gray-200;
}

.form-title {
  @apply text-base font-medium text-gray-900 mb-4;
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
</style>
