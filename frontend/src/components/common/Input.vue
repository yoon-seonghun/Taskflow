<script setup lang="ts">
/**
 * 공통 입력 필드 컴포넌트
 * Compact UI: height 32px, font 13px
 */
import { computed, ref } from 'vue'

export type InputSize = 'sm' | 'md' | 'lg'
export type InputType = 'text' | 'password' | 'email' | 'number' | 'tel' | 'url' | 'search'

interface Props {
  modelValue?: string | number
  type?: InputType
  size?: InputSize
  placeholder?: string
  disabled?: boolean
  readonly?: boolean
  error?: boolean
  errorMessage?: string
  label?: string
  required?: boolean
  maxlength?: number
  autofocus?: boolean
  clearable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  type: 'text',
  size: 'md',
  placeholder: '',
  disabled: false,
  readonly: false,
  error: false,
  required: false,
  autofocus: false,
  clearable: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number): void
  (e: 'focus', event: FocusEvent): void
  (e: 'blur', event: FocusEvent): void
  (e: 'enter'): void
  (e: 'clear'): void
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const isFocused = ref(false)

const inputClasses = computed(() => {
  const base = [
    'w-full rounded border transition-all duration-150',
    'focus:outline-none focus:ring-2 focus:border-transparent',
    'placeholder:text-gray-400'
  ]

  // Size styles
  const sizes: Record<InputSize, string> = {
    sm: 'px-2 py-1 text-xs h-7',
    md: 'px-3 py-1.5 text-[13px] h-8',
    lg: 'px-3 py-2 text-sm h-9'
  }

  // State styles
  const states = props.error
    ? 'border-red-500 focus:ring-red-500'
    : 'border-gray-300 focus:ring-primary-500'

  const disabled = props.disabled ? 'bg-gray-100 cursor-not-allowed text-gray-500' : 'bg-white'

  return [
    ...base,
    sizes[props.size],
    states,
    disabled,
    props.clearable && props.modelValue ? 'pr-8' : ''
  ].filter(Boolean).join(' ')
})

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  const value = props.type === 'number' ? Number(target.value) : target.value
  emit('update:modelValue', value)
}

function handleFocus(event: FocusEvent) {
  isFocused.value = true
  emit('focus', event)
}

function handleBlur(event: FocusEvent) {
  isFocused.value = false
  emit('blur', event)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    emit('enter')
  }
}

function handleClear() {
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}

function focus() {
  inputRef.value?.focus()
}

function blur() {
  inputRef.value?.blur()
}

defineExpose({ focus, blur, inputRef })
</script>

<template>
  <div class="w-full">
    <!-- Label -->
    <label v-if="label" class="block text-[13px] font-medium text-gray-700 mb-1">
      {{ label }}
      <span v-if="required" class="text-red-500 ml-0.5">*</span>
    </label>

    <!-- Input Container -->
    <div class="relative">
      <input
        ref="inputRef"
        :type="type"
        :value="modelValue"
        :class="inputClasses"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :required="required"
        :maxlength="maxlength"
        :autofocus="autofocus"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
      />

      <!-- Clear Button -->
      <button
        v-if="clearable && modelValue && !disabled"
        type="button"
        class="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 p-0.5"
        @click="handleClear"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- Error Message -->
    <p v-if="error && errorMessage" class="mt-1 text-xs text-red-500">
      {{ errorMessage }}
    </p>
  </div>
</template>
