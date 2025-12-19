<script setup lang="ts">
/**
 * 선택 드롭다운 컴포넌트
 * - 인라인 옵션 추가 지원
 * - 검색 기능
 * Compact UI: height 32px, font 13px
 */
import { computed, ref, watch, nextTick, onMounted, onUnmounted } from 'vue'

export interface SelectOption {
  value: string | number
  label: string
  color?: string
  disabled?: boolean
}

export type SelectSize = 'sm' | 'md' | 'lg'

interface Props {
  modelValue?: string | number | null
  options: SelectOption[]
  size?: SelectSize
  placeholder?: string
  disabled?: boolean
  error?: boolean
  errorMessage?: string
  label?: string
  required?: boolean
  searchable?: boolean
  clearable?: boolean
  allowCreate?: boolean
  createLabel?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  size: 'md',
  placeholder: '선택해주세요',
  disabled: false,
  error: false,
  required: false,
  searchable: false,
  clearable: false,
  allowCreate: false,
  createLabel: '새 옵션 추가'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number | null): void
  (e: 'create', value: string): void
  (e: 'change', value: string | number | null): void
}>()

const isOpen = ref(false)
const searchQuery = ref('')
const highlightedIndex = ref(-1)
const containerRef = ref<HTMLElement | null>(null)
const dropdownRef = ref<HTMLElement | null>(null)
const searchInputRef = ref<HTMLInputElement | null>(null)

// 선택된 옵션
const selectedOption = computed(() =>
  props.options.find(opt => opt.value === props.modelValue)
)

// 필터링된 옵션
const filteredOptions = computed(() => {
  if (!props.searchable || !searchQuery.value) {
    return props.options
  }
  const query = searchQuery.value.toLowerCase()
  return props.options.filter(opt =>
    opt.label.toLowerCase().includes(query)
  )
})

// 새 옵션 생성 가능 여부
const canCreate = computed(() => {
  if (!props.allowCreate || !searchQuery.value.trim()) return false
  const query = searchQuery.value.toLowerCase().trim()
  return !props.options.some(opt => opt.label.toLowerCase() === query)
})

// 트리거 버튼 스타일
const triggerClasses = computed(() => {
  const base = [
    'w-full rounded border transition-all duration-150',
    'flex items-center justify-between gap-2',
    'focus:outline-none focus:ring-2 focus:border-transparent',
    'text-left'
  ]

  const sizes: Record<SelectSize, string> = {
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
    : 'bg-white cursor-pointer hover:border-gray-400'

  return [...base, sizes[props.size], states, disabled].join(' ')
})

// 드롭다운 열기/닫기
function toggleDropdown() {
  if (props.disabled) return
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    searchQuery.value = ''
    highlightedIndex.value = -1
    nextTick(() => {
      searchInputRef.value?.focus()
    })
  }
}

function closeDropdown() {
  isOpen.value = false
  searchQuery.value = ''
}

// 옵션 선택
function selectOption(option: SelectOption) {
  if (option.disabled) return
  emit('update:modelValue', option.value)
  emit('change', option.value)
  closeDropdown()
}

// 선택 해제
function clearSelection(event: Event) {
  event.stopPropagation()
  emit('update:modelValue', null)
  emit('change', null)
}

// 새 옵션 생성
function createOption() {
  const value = searchQuery.value.trim()
  if (value) {
    emit('create', value)
    searchQuery.value = ''
  }
}

// 키보드 네비게이션
function handleKeydown(event: KeyboardEvent) {
  if (!isOpen.value) {
    if (event.key === 'Enter' || event.key === ' ' || event.key === 'ArrowDown') {
      event.preventDefault()
      toggleDropdown()
    }
    return
  }

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      highlightedIndex.value = Math.min(
        highlightedIndex.value + 1,
        filteredOptions.value.length - 1
      )
      break
    case 'ArrowUp':
      event.preventDefault()
      highlightedIndex.value = Math.max(highlightedIndex.value - 1, 0)
      break
    case 'Enter':
      event.preventDefault()
      if (highlightedIndex.value >= 0 && filteredOptions.value[highlightedIndex.value]) {
        selectOption(filteredOptions.value[highlightedIndex.value])
      } else if (canCreate.value) {
        createOption()
      }
      break
    case 'Escape':
      closeDropdown()
      break
  }
}

// 외부 클릭 감지
function handleClickOutside(event: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(event.target as Node)) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 옵션 색상 스타일
function getOptionColorStyle(color?: string) {
  if (!color) return {}
  return {
    backgroundColor: `${color}20`,
    color: color
  }
}
</script>

<template>
  <div ref="containerRef" class="relative w-full">
    <!-- Label -->
    <label v-if="label" class="block text-[13px] font-medium text-gray-700 mb-1">
      {{ label }}
      <span v-if="required" class="text-red-500 ml-0.5">*</span>
    </label>

    <!-- Trigger -->
    <button
      type="button"
      :class="triggerClasses"
      :disabled="disabled"
      @click="toggleDropdown"
      @keydown="handleKeydown"
    >
      <!-- Selected Value -->
      <span v-if="selectedOption" class="flex items-center gap-2 truncate">
        <span
          v-if="selectedOption.color"
          class="w-2.5 h-2.5 rounded-full flex-shrink-0"
          :style="{ backgroundColor: selectedOption.color }"
        />
        {{ selectedOption.label }}
      </span>
      <span v-else class="text-gray-400 truncate">{{ placeholder }}</span>

      <!-- Actions -->
      <div class="flex items-center gap-1 flex-shrink-0">
        <!-- Clear Button -->
        <span
          v-if="clearable && selectedOption && !disabled"
          class="text-gray-400 hover:text-gray-600 p-0.5"
          @click="clearSelection"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </span>

        <!-- Arrow -->
        <svg
          class="w-4 h-4 text-gray-400 transition-transform"
          :class="{ 'rotate-180': isOpen }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
      </div>
    </button>

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
        v-if="isOpen"
        ref="dropdownRef"
        class="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-hidden"
      >
        <!-- Search Input -->
        <div v-if="searchable" class="p-2 border-b border-gray-100">
          <input
            ref="searchInputRef"
            v-model="searchQuery"
            type="text"
            class="w-full px-2 py-1.5 text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
            placeholder="검색..."
            @keydown="handleKeydown"
          />
        </div>

        <!-- Options List -->
        <div class="overflow-y-auto max-h-48">
          <div
            v-for="(option, index) in filteredOptions"
            :key="option.value"
            class="px-3 py-2 text-[13px] cursor-pointer flex items-center gap-2"
            :class="[
              option.disabled ? 'text-gray-400 cursor-not-allowed' : 'hover:bg-gray-50',
              highlightedIndex === index ? 'bg-gray-100' : '',
              option.value === modelValue ? 'bg-primary-50 text-primary-700' : 'text-gray-700'
            ]"
            @click="selectOption(option)"
            @mouseenter="highlightedIndex = index"
          >
            <!-- Color Indicator -->
            <span
              v-if="option.color"
              class="w-2.5 h-2.5 rounded-full flex-shrink-0"
              :style="{ backgroundColor: option.color }"
            />

            <!-- Label -->
            <span class="truncate flex-1">{{ option.label }}</span>

            <!-- Check Icon -->
            <svg
              v-if="option.value === modelValue"
              class="w-4 h-4 text-primary-600 flex-shrink-0"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>

          <!-- Empty State -->
          <div v-if="filteredOptions.length === 0 && !canCreate" class="px-3 py-4 text-center text-gray-500 text-[13px]">
            검색 결과가 없습니다
          </div>

          <!-- Create Option -->
          <div
            v-if="canCreate"
            class="px-3 py-2 text-[13px] text-primary-600 cursor-pointer hover:bg-primary-50 flex items-center gap-2 border-t border-gray-100"
            @click="createOption"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            <span>"{{ searchQuery }}" {{ createLabel }}</span>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Error Message -->
    <p v-if="error && errorMessage" class="mt-1 text-xs text-red-500">
      {{ errorMessage }}
    </p>
  </div>
</template>
