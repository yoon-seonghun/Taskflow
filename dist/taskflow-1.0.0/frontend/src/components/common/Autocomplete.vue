<script setup lang="ts">
/**
 * 자동완성 컴포넌트
 * - 디바운스 적용 (300ms)
 * - 2글자 이상 입력 시 검색
 * - 키보드 네비게이션 지원
 * Compact UI: height 32px, font 13px
 */
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'

export interface AutocompleteOption {
  value: string | number
  label: string
  description?: string
  [key: string]: unknown
}

export type AutocompleteSize = 'sm' | 'md' | 'lg'

interface Props {
  modelValue: string
  options?: AutocompleteOption[]
  size?: AutocompleteSize
  placeholder?: string
  disabled?: boolean
  loading?: boolean
  error?: boolean
  errorMessage?: string
  label?: string
  required?: boolean
  minChars?: number
  debounceMs?: number
  maxResults?: number
  allowCreate?: boolean
  createLabel?: string
  emptyMessage?: string
}

const props = withDefaults(defineProps<Props>(), {
  options: () => [],
  size: 'md',
  placeholder: '입력하세요...',
  disabled: false,
  loading: false,
  error: false,
  required: false,
  minChars: 2,
  debounceMs: 300,
  maxResults: 10,
  allowCreate: false,
  createLabel: '새로 등록',
  emptyMessage: '검색 결과가 없습니다'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'search', query: string): void
  (e: 'select', option: AutocompleteOption): void
  (e: 'create', value: string): void
  (e: 'focus'): void
  (e: 'blur'): void
  (e: 'enter'): void
}>()

// 상태
const isOpen = ref(false)
const highlightedIndex = ref(-1)
const containerRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)
const dropdownRef = ref<HTMLElement | null>(null)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// 입력값
const inputValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 표시할 옵션 목록
const displayOptions = computed(() => {
  return props.options.slice(0, props.maxResults)
})

// 검색 가능 여부
const canSearch = computed(() => {
  return inputValue.value.length >= props.minChars
})

// 생성 가능 여부
const canCreate = computed(() => {
  if (!props.allowCreate || !inputValue.value.trim()) return false
  // 이미 동일한 항목이 있으면 생성 불가
  const query = inputValue.value.toLowerCase().trim()
  return !props.options.some(opt => opt.label.toLowerCase() === query)
})

// 드롭다운 표시 여부
const showDropdown = computed(() => {
  return isOpen.value && canSearch.value && (displayOptions.value.length > 0 || props.loading || canCreate.value)
})

// 입력 필드 스타일
const inputClasses = computed(() => {
  const base = [
    'w-full rounded border transition-all duration-150',
    'focus:outline-none focus:ring-2 focus:border-transparent',
    'placeholder:text-gray-400'
  ]

  const sizes: Record<AutocompleteSize, string> = {
    sm: 'px-2 py-1 text-xs h-7',
    md: 'px-3 py-1.5 text-[13px] h-8',
    lg: 'px-3 py-2 text-sm h-9'
  }

  const states = props.error
    ? 'border-red-500 focus:ring-red-500'
    : isOpen.value
      ? 'border-primary-500 ring-2 ring-primary-500'
      : 'border-gray-300 focus:ring-primary-500'

  const disabled = props.disabled
    ? 'bg-gray-100 cursor-not-allowed text-gray-500'
    : 'bg-white'

  return [...base, sizes[props.size], states, disabled].join(' ')
})

// 입력 핸들러 (디바운스 적용)
function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  inputValue.value = target.value

  // 디바운스 타이머 초기화
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }

  // 최소 글자수 미만이면 드롭다운 닫기
  if (target.value.length < props.minChars) {
    isOpen.value = false
    highlightedIndex.value = -1
    return
  }

  // 디바운스 후 검색
  debounceTimer = setTimeout(() => {
    emit('search', target.value)
    isOpen.value = true
    highlightedIndex.value = -1
  }, props.debounceMs)
}

// 포커스 핸들러
function handleFocus() {
  emit('focus')
  if (canSearch.value && displayOptions.value.length > 0) {
    isOpen.value = true
  }
}

// 블러 핸들러
function handleBlur() {
  // 드롭다운 클릭 시 블러 방지를 위해 딜레이
  setTimeout(() => {
    isOpen.value = false
    highlightedIndex.value = -1
    emit('blur')
  }, 200)
}

// 옵션 선택
function selectOption(option: AutocompleteOption) {
  inputValue.value = option.label
  emit('select', option)
  isOpen.value = false
  highlightedIndex.value = -1
}

// 새로 생성
function createNew() {
  const value = inputValue.value.trim()
  if (value) {
    emit('create', value)
    isOpen.value = false
    highlightedIndex.value = -1
  }
}

// 키보드 네비게이션
function handleKeydown(event: KeyboardEvent) {
  if (!isOpen.value) {
    if (event.key === 'ArrowDown' && canSearch.value) {
      isOpen.value = true
      highlightedIndex.value = 0
      event.preventDefault()
    } else if (event.key === 'Enter') {
      emit('enter')
    }
    return
  }

  const totalItems = displayOptions.value.length + (canCreate.value ? 1 : 0)

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      highlightedIndex.value = Math.min(highlightedIndex.value + 1, totalItems - 1)
      scrollToHighlighted()
      break

    case 'ArrowUp':
      event.preventDefault()
      highlightedIndex.value = Math.max(highlightedIndex.value - 1, 0)
      scrollToHighlighted()
      break

    case 'Enter':
      event.preventDefault()
      if (highlightedIndex.value >= 0 && highlightedIndex.value < displayOptions.value.length) {
        selectOption(displayOptions.value[highlightedIndex.value])
      } else if (canCreate.value && highlightedIndex.value === displayOptions.value.length) {
        createNew()
      } else {
        emit('enter')
      }
      break

    case 'Escape':
      isOpen.value = false
      highlightedIndex.value = -1
      break
  }
}

// 하이라이트 항목으로 스크롤
function scrollToHighlighted() {
  nextTick(() => {
    if (dropdownRef.value && highlightedIndex.value >= 0) {
      const items = dropdownRef.value.querySelectorAll('[data-option]')
      const item = items[highlightedIndex.value] as HTMLElement
      if (item) {
        item.scrollIntoView({ block: 'nearest' })
      }
    }
  })
}

// 외부 클릭 감지
function handleClickOutside(event: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(event.target as Node)) {
    isOpen.value = false
    highlightedIndex.value = -1
  }
}

// 포커스 메서드
function focus() {
  inputRef.value?.focus()
}

// 클리어 메서드
function clear() {
  inputValue.value = ''
  isOpen.value = false
  highlightedIndex.value = -1
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
})

defineExpose({ focus, clear })
</script>

<template>
  <div ref="containerRef" class="relative w-full">
    <!-- Label -->
    <label v-if="label" class="block text-[13px] font-medium text-gray-700 mb-1">
      {{ label }}
      <span v-if="required" class="text-red-500 ml-0.5">*</span>
    </label>

    <!-- Input -->
    <div class="relative">
      <!-- Prefix Slot -->
      <div
        v-if="$slots.prefix"
        class="absolute left-2.5 top-1/2 -translate-y-1/2 pointer-events-none"
      >
        <slot name="prefix" />
      </div>

      <input
        ref="inputRef"
        type="text"
        :value="inputValue"
        :class="[inputClasses, $slots.prefix ? 'pl-8' : '']"
        :placeholder="placeholder"
        :disabled="disabled"
        autocomplete="off"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
      />

      <!-- Loading Spinner -->
      <div
        v-if="loading"
        class="absolute right-2 top-1/2 -translate-y-1/2"
      >
        <svg
          class="w-4 h-4 text-gray-400 animate-spin"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            class="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            stroke-width="4"
          />
          <path
            class="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      </div>

      <!-- Search Icon (when not loading) -->
      <div
        v-else-if="!inputValue"
        class="absolute right-2 top-1/2 -translate-y-1/2"
      >
        <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </div>
    </div>

    <!-- Dropdown -->
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0 -translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-1"
    >
      <div
        v-if="showDropdown"
        ref="dropdownRef"
        class="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto"
      >
        <!-- Loading State -->
        <div v-if="loading" class="px-3 py-4 text-center text-[13px] text-gray-500">
          검색 중...
        </div>

        <!-- Options -->
        <template v-else>
          <!-- Option Items -->
          <div
            v-for="(option, index) in displayOptions"
            :key="option.value"
            data-option
            class="px-3 py-2 cursor-pointer transition-colors"
            :class="[
              highlightedIndex === index ? 'bg-primary-50 text-primary-700' : 'text-gray-700 hover:bg-gray-50'
            ]"
            @click="selectOption(option)"
            @mouseenter="highlightedIndex = index"
          >
            <div class="text-[13px] font-medium truncate">{{ option.label }}</div>
            <div v-if="option.description" class="text-[11px] text-gray-500 truncate mt-0.5">
              {{ option.description }}
            </div>
          </div>

          <!-- Empty State -->
          <div
            v-if="displayOptions.length === 0 && !canCreate"
            class="px-3 py-4 text-center text-[13px] text-gray-500"
          >
            {{ emptyMessage }}
          </div>

          <!-- Create New Option -->
          <div
            v-if="canCreate"
            data-option
            class="px-3 py-2 cursor-pointer border-t border-gray-100 transition-colors flex items-center gap-2"
            :class="[
              highlightedIndex === displayOptions.length ? 'bg-primary-50 text-primary-700' : 'text-primary-600 hover:bg-primary-50'
            ]"
            @click="createNew"
            @mouseenter="highlightedIndex = displayOptions.length"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            <span class="text-[13px]">"{{ inputValue }}" {{ createLabel }}</span>
          </div>
        </template>
      </div>
    </Transition>

    <!-- Error Message -->
    <p v-if="error && errorMessage" class="mt-1 text-xs text-red-500">
      {{ errorMessage }}
    </p>
  </div>
</template>
