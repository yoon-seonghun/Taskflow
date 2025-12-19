<script setup lang="ts">
/**
 * 인라인 편집기 컴포넌트
 * 클릭하면 편집 모드로 전환
 * Compact UI: font 13px
 */
import { ref, computed, watch, nextTick } from 'vue'

export type EditorType = 'text' | 'textarea' | 'number'

interface Props {
  modelValue: string | number
  type?: EditorType
  placeholder?: string
  disabled?: boolean
  required?: boolean
  maxlength?: number
  rows?: number
  emptyText?: string
  selectOnFocus?: boolean
  submitOnBlur?: boolean
  submitOnEnter?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  placeholder: '',
  disabled: false,
  required: false,
  rows: 3,
  emptyText: '클릭하여 입력',
  selectOnFocus: true,
  submitOnBlur: true,
  submitOnEnter: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number): void
  (e: 'submit', value: string | number): void
  (e: 'cancel'): void
}>()

const isEditing = ref(false)
const localValue = ref<string | number>(props.modelValue)
const inputRef = ref<HTMLInputElement | HTMLTextAreaElement | null>(null)

// 원본 값 (취소 시 복원용)
const originalValue = ref<string | number>(props.modelValue)

// 표시 값
const displayValue = computed(() => {
  if (props.modelValue === '' || props.modelValue === null || props.modelValue === undefined) {
    return ''
  }
  return String(props.modelValue)
})

// props 변경 감지
watch(() => props.modelValue, (newVal) => {
  localValue.value = newVal
  if (!isEditing.value) {
    originalValue.value = newVal
  }
})

// 편집 모드 시작
function startEditing() {
  if (props.disabled) return

  isEditing.value = true
  originalValue.value = props.modelValue
  localValue.value = props.modelValue

  nextTick(() => {
    inputRef.value?.focus()
    if (props.selectOnFocus && inputRef.value) {
      inputRef.value.select()
    }
  })
}

// 편집 완료 (저장)
function submit() {
  const value = props.type === 'number' ? Number(localValue.value) : localValue.value

  // 필수 체크
  if (props.required && (value === '' || value === null)) {
    cancel()
    return
  }

  isEditing.value = false
  emit('update:modelValue', value)
  emit('submit', value)
}

// 편집 취소
function cancel() {
  isEditing.value = false
  localValue.value = originalValue.value
  emit('cancel')
}

// 입력 핸들러
function handleInput(event: Event) {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement
  localValue.value = props.type === 'number' ? Number(target.value) : target.value
}

// 키보드 핸들러
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    cancel()
  } else if (event.key === 'Enter') {
    // textarea는 Ctrl+Enter로 제출
    if (props.type === 'textarea') {
      if (event.ctrlKey || event.metaKey) {
        event.preventDefault()
        submit()
      }
    } else if (props.submitOnEnter) {
      event.preventDefault()
      submit()
    }
  }
}

// 블러 핸들러
function handleBlur() {
  if (props.submitOnBlur) {
    submit()
  }
}
</script>

<template>
  <div class="inline-block w-full">
    <!-- Edit Mode -->
    <div v-if="isEditing" class="w-full">
      <!-- Textarea -->
      <textarea
        v-if="type === 'textarea'"
        ref="inputRef"
        :value="localValue"
        :placeholder="placeholder"
        :maxlength="maxlength"
        :rows="rows"
        class="w-full px-2 py-1 text-[13px] border border-primary-500 rounded focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
        @input="handleInput"
        @keydown="handleKeydown"
        @blur="handleBlur"
      />

      <!-- Text/Number Input -->
      <input
        v-else
        ref="inputRef"
        :type="type"
        :value="localValue"
        :placeholder="placeholder"
        :maxlength="maxlength"
        class="w-full px-2 py-1 text-[13px] border border-primary-500 rounded focus:outline-none focus:ring-2 focus:ring-primary-500 h-8"
        @input="handleInput"
        @keydown="handleKeydown"
        @blur="handleBlur"
      />

      <!-- Help Text -->
      <p v-if="type === 'textarea'" class="mt-1 text-[11px] text-gray-400">
        Ctrl+Enter로 저장, ESC로 취소
      </p>
    </div>

    <!-- Display Mode -->
    <div
      v-else
      class="min-h-[32px] px-2 py-1 rounded cursor-text transition-colors"
      :class="[
        disabled ? 'cursor-default' : 'hover:bg-gray-100',
        !displayValue ? 'text-gray-400' : 'text-gray-900'
      ]"
      @click="startEditing"
    >
      <!-- Slot for custom display -->
      <slot name="display" :value="displayValue">
        <span
          v-if="type === 'textarea'"
          class="text-[13px] whitespace-pre-wrap break-words"
        >
          {{ displayValue || emptyText }}
        </span>
        <span v-else class="text-[13px] truncate block">
          {{ displayValue || emptyText }}
        </span>
      </slot>
    </div>
  </div>
</template>
