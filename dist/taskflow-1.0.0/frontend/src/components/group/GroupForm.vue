<script setup lang="ts">
/**
 * 그룹 등록/수정 폼 컴포넌트
 * - 그룹 코드, 그룹명, 설명, 색상, 정렬 순서, 사용 여부
 */
import { ref, computed, watch, onUnmounted } from 'vue'
import Input from '@/components/common/Input.vue'
import Select from '@/components/common/Select.vue'
import type { SelectOption } from '@/components/common/Select.vue'
import type { Group, GroupCreateRequest, GroupUpdateRequest } from '@/types/group'

interface Props {
  group?: Group | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  group: null,
  loading: false
})

const emit = defineEmits<{
  (e: 'submit', data: GroupCreateRequest | GroupUpdateRequest): void
  (e: 'cancel'): void
}>()

// Form fields
const groupCode = ref('')
const groupName = ref('')
const groupDescription = ref('')
const groupColor = ref('#3B82F6')
const sortOrder = ref(0)
const useYn = ref('Y')

// 컬러 피커 표시 여부
const showColorPicker = ref(false)

// 프리셋 색상
const presetColors = [
  '#EF4444', // red
  '#F97316', // orange
  '#F59E0B', // amber
  '#EAB308', // yellow
  '#84CC16', // lime
  '#22C55E', // green
  '#10B981', // emerald
  '#14B8A6', // teal
  '#06B6D4', // cyan
  '#0EA5E9', // sky
  '#3B82F6', // blue
  '#6366F1', // indigo
  '#8B5CF6', // violet
  '#A855F7', // purple
  '#D946EF', // fuchsia
  '#EC4899', // pink
  '#F43F5E', // rose
  '#6B7280', // gray
]

// 편집 모드 여부
const isEditMode = computed(() => !!props.group)

// 버튼 텍스트
const submitButtonText = computed(() => isEditMode.value ? '수정' : '등록')

// 제목 텍스트
const formTitle = computed(() => isEditMode.value ? '그룹 수정' : '그룹 등록')

// 사용 여부 옵션
const useYnOptions: SelectOption[] = [
  { value: 'Y', label: '활성' },
  { value: 'N', label: '비활성' }
]

// 폼 유효성
const isValid = computed(() => {
  return groupCode.value.trim().length > 0 && groupName.value.trim().length > 0
})

// 그룹 코드 유효성 (영문+숫자+언더스코어, 2~20자)
const codeValidation = computed(() => {
  const code = groupCode.value
  if (!code) return { isValid: true, message: '' }

  const validLength = code.length >= 2 && code.length <= 20
  const validChars = /^[a-zA-Z0-9_]+$/.test(code)

  if (!validChars) {
    return { isValid: false, message: '영문, 숫자, 언더스코어만 사용 가능합니다.' }
  }
  if (!validLength) {
    return { isValid: false, message: '2~20자로 입력해주세요.' }
  }
  return { isValid: true, message: '' }
})

// 폼 초기화
function resetForm() {
  groupCode.value = ''
  groupName.value = ''
  groupDescription.value = ''
  groupColor.value = '#3B82F6'
  sortOrder.value = 0
  useYn.value = 'Y'
  showColorPicker.value = false
}

// 그룹 데이터로 폼 채우기
function fillForm(group: Group) {
  groupCode.value = group.groupCode
  groupName.value = group.groupName
  groupDescription.value = group.groupDescription || ''
  groupColor.value = group.groupColor || '#3B82F6'
  sortOrder.value = group.sortOrder
  useYn.value = group.useYn
}

// 색상 선택
function selectColor(color: string) {
  groupColor.value = color
  closeColorPicker()
}

// 컬러 피커 외부 클릭 핸들러
let colorPickerClickHandler: ((e: MouseEvent) => void) | null = null

function closeColorPicker() {
  showColorPicker.value = false
  if (colorPickerClickHandler) {
    document.removeEventListener('click', colorPickerClickHandler)
    colorPickerClickHandler = null
  }
}

// 컬러 피커 토글
function toggleColorPicker(event: Event) {
  event.stopPropagation()

  if (showColorPicker.value) {
    closeColorPicker()
  } else {
    showColorPicker.value = true
    // 외부 클릭 시 닫기
    colorPickerClickHandler = (e: MouseEvent) => {
      const target = e.target as HTMLElement
      if (!target.closest('.color-picker') && !target.closest('.color-preview')) {
        closeColorPicker()
      }
    }
    setTimeout(() => {
      if (colorPickerClickHandler) {
        document.addEventListener('click', colorPickerClickHandler)
      }
    }, 0)
  }
}

// 컴포넌트 언마운트 시 정리
onUnmounted(() => {
  closeColorPicker()
})

// 제출
function handleSubmit() {
  if (!isValid.value || !codeValidation.value.isValid || props.loading) return

  if (isEditMode.value) {
    const data: GroupUpdateRequest = {
      groupCode: groupCode.value.trim(),
      groupName: groupName.value.trim(),
      groupDescription: groupDescription.value.trim() || undefined,
      groupColor: groupColor.value,
      sortOrder: sortOrder.value,
      useYn: useYn.value
    }
    emit('submit', data)
  } else {
    const data: GroupCreateRequest = {
      groupCode: groupCode.value.trim(),
      groupName: groupName.value.trim(),
      groupDescription: groupDescription.value.trim() || undefined,
      groupColor: groupColor.value,
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

// 그룹 변경 감지
watch(() => props.group, (newGroup) => {
  if (newGroup) {
    fillForm(newGroup)
  } else {
    resetForm()
  }
}, { immediate: true })

// 외부에서 폼 초기화 가능하도록 expose
defineExpose({ resetForm })
</script>

<template>
  <div class="group-form">
    <h3 class="form-title">{{ formTitle }}</h3>

    <div class="space-y-4">
      <!-- 그룹 코드 -->
      <Input
        v-model="groupCode"
        label="그룹 코드"
        placeholder="영문, 숫자, 언더스코어 (예: PROJECT_A)"
        :required="true"
        :maxlength="20"
        :disabled="loading"
        :error="!codeValidation.isValid && groupCode.length > 0"
        :error-message="codeValidation.message"
      />

      <!-- 그룹명 -->
      <Input
        v-model="groupName"
        label="그룹명"
        placeholder="그룹명을 입력하세요"
        :required="true"
        :maxlength="100"
        :disabled="loading"
      />

      <!-- 그룹 설명 -->
      <div class="form-field">
        <label class="field-label">그룹 설명</label>
        <textarea
          v-model="groupDescription"
          class="textarea"
          placeholder="그룹에 대한 설명을 입력하세요"
          rows="3"
          :maxlength="500"
          :disabled="loading"
        />
      </div>

      <!-- 그룹 색상 -->
      <div class="form-field">
        <label class="field-label">그룹 색상</label>
        <div class="color-field">
          <button
            type="button"
            class="color-preview"
            :style="{ backgroundColor: groupColor }"
            :disabled="loading"
            @click="toggleColorPicker($event)"
          >
            <span class="color-value">{{ groupColor }}</span>
          </button>

          <!-- 컬러 피커 팝업 -->
          <div v-if="showColorPicker" class="color-picker">
            <div class="preset-colors">
              <button
                v-for="color in presetColors"
                :key="color"
                type="button"
                class="color-swatch"
                :class="{ 'selected': groupColor === color }"
                :style="{ backgroundColor: color }"
                @click="selectColor(color)"
              />
            </div>
            <div class="custom-color">
              <input
                v-model="groupColor"
                type="color"
                class="color-input"
              />
              <input
                v-model="groupColor"
                type="text"
                class="color-text"
                placeholder="#3B82F6"
                maxlength="7"
              />
            </div>
          </div>
        </div>
      </div>

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
.group-form {
  @apply p-4 bg-white rounded-lg border border-gray-200;
}

.form-title {
  @apply text-base font-medium text-gray-900 mb-4;
}

.form-field {
  @apply space-y-1;
}

.field-label {
  @apply block text-sm font-medium text-gray-700;
}

.textarea {
  @apply w-full px-3 py-2 text-sm border border-gray-300 rounded-lg
         focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
         disabled:bg-gray-50 disabled:text-gray-500
         resize-none;
}

.color-field {
  @apply relative;
}

.color-preview {
  @apply w-full h-10 rounded-lg border border-gray-300 cursor-pointer
         flex items-center justify-center
         hover:border-gray-400 transition-colors
         disabled:opacity-50 disabled:cursor-not-allowed;
}

.color-value {
  @apply text-sm font-medium text-white drop-shadow-sm;
}

.color-picker {
  @apply absolute z-10 mt-2 p-3 bg-white rounded-lg shadow-lg border border-gray-200;
}

.preset-colors {
  @apply grid grid-cols-6 gap-2 mb-3;
}

.color-swatch {
  @apply w-6 h-6 rounded cursor-pointer
         hover:scale-110 transition-transform;
}

.color-swatch.selected {
  @apply ring-2 ring-offset-2 ring-gray-900;
}

.custom-color {
  @apply flex items-center gap-2 pt-2 border-t border-gray-100;
}

.color-input {
  @apply w-8 h-8 rounded cursor-pointer border-0;
}

.color-text {
  @apply flex-1 px-2 py-1 text-sm border border-gray-300 rounded
         focus:outline-none focus:ring-1 focus:ring-primary-500;
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
