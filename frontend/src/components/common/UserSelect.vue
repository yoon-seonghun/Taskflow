<script setup lang="ts">
/**
 * 사용자 선택 컴포넌트
 * - 부서별 필터링
 * - 사용자 검색
 * Compact UI: height 32px, font 13px
 */
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'

export interface UserOption {
  userId: number
  userName: string
  departmentId?: number
  departmentName?: string
}

export interface DepartmentOption {
  departmentId: number
  departmentName: string
}

export type SelectSize = 'sm' | 'md' | 'lg'

interface Props {
  modelValue?: number | null
  users: UserOption[]
  departments?: DepartmentOption[]
  size?: SelectSize
  placeholder?: string
  disabled?: boolean
  error?: boolean
  errorMessage?: string
  label?: string
  required?: boolean
  clearable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  users: () => [],
  departments: () => [],
  size: 'md',
  placeholder: '담당자 선택',
  disabled: false,
  error: false,
  required: false,
  clearable: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | null): void
  (e: 'change', value: number | null): void
}>()

const isOpen = ref(false)
const searchQuery = ref('')
const selectedDepartmentId = ref<number | null>(null)
const highlightedIndex = ref(-1)
const containerRef = ref<HTMLElement | null>(null)
const searchInputRef = ref<HTMLInputElement | null>(null)

// 선택된 사용자
const selectedUser = computed(() =>
  props.users.find(user => user.userId === props.modelValue)
)

// 부서별 필터링된 사용자
const departmentFilteredUsers = computed(() => {
  if (!selectedDepartmentId.value) {
    return props.users
  }
  return props.users.filter(user => user.departmentId === selectedDepartmentId.value)
})

// 검색 + 부서 필터링된 사용자
const filteredUsers = computed(() => {
  if (!searchQuery.value) {
    return departmentFilteredUsers.value
  }
  const query = searchQuery.value.toLowerCase()
  return departmentFilteredUsers.value.filter(user =>
    user.userName.toLowerCase().includes(query) ||
    (user.departmentName && user.departmentName.toLowerCase().includes(query))
  )
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

// 사용자 선택
function selectUser(user: UserOption) {
  emit('update:modelValue', user.userId)
  emit('change', user.userId)
  closeDropdown()
}

// 선택 해제
function clearSelection(event: Event) {
  event.stopPropagation()
  emit('update:modelValue', null)
  emit('change', null)
}

// 부서 필터 변경
function handleDepartmentChange(deptId: number | null) {
  selectedDepartmentId.value = deptId
  highlightedIndex.value = -1
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
        filteredUsers.value.length - 1
      )
      break
    case 'ArrowUp':
      event.preventDefault()
      highlightedIndex.value = Math.max(highlightedIndex.value - 1, 0)
      break
    case 'Enter':
      event.preventDefault()
      if (highlightedIndex.value >= 0 && filteredUsers.value[highlightedIndex.value]) {
        selectUser(filteredUsers.value[highlightedIndex.value])
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
      <span v-if="selectedUser" class="flex items-center gap-2 truncate">
        <span class="w-5 h-5 rounded-full bg-primary-100 text-primary-700 text-[10px] font-medium flex items-center justify-center flex-shrink-0">
          {{ selectedUser.userName.charAt(0) }}
        </span>
        <span class="truncate">{{ selectedUser.userName }}</span>
        <span v-if="selectedUser.departmentName" class="text-gray-400 text-[11px]">
          ({{ selectedUser.departmentName }})
        </span>
      </span>
      <span v-else class="text-gray-400 truncate">{{ placeholder }}</span>

      <!-- Actions -->
      <div class="flex items-center gap-1 flex-shrink-0">
        <!-- Clear Button -->
        <span
          v-if="clearable && selectedUser && !disabled"
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
        class="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden"
        style="min-width: 240px;"
      >
        <!-- Search + Department Filter -->
        <div class="p-2 border-b border-gray-100 space-y-2">
          <!-- Search Input -->
          <input
            ref="searchInputRef"
            v-model="searchQuery"
            type="text"
            class="w-full px-2 py-1.5 text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
            placeholder="이름 또는 부서로 검색..."
            @keydown="handleKeydown"
          />

          <!-- Department Filter -->
          <div v-if="departments.length > 0" class="flex flex-wrap gap-1">
            <button
              type="button"
              class="px-2 py-1 text-[11px] rounded-full transition-colors"
              :class="selectedDepartmentId === null ? 'bg-primary-100 text-primary-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
              @click="handleDepartmentChange(null)"
            >
              전체
            </button>
            <button
              v-for="dept in departments"
              :key="dept.departmentId"
              type="button"
              class="px-2 py-1 text-[11px] rounded-full transition-colors"
              :class="selectedDepartmentId === dept.departmentId ? 'bg-primary-100 text-primary-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
              @click="handleDepartmentChange(dept.departmentId)"
            >
              {{ dept.departmentName }}
            </button>
          </div>
        </div>

        <!-- Users List -->
        <div class="overflow-y-auto max-h-48">
          <div
            v-for="(user, index) in filteredUsers"
            :key="user.userId"
            class="px-3 py-2 text-[13px] cursor-pointer flex items-center gap-2"
            :class="[
              'hover:bg-gray-50',
              highlightedIndex === index ? 'bg-gray-100' : '',
              user.userId === modelValue ? 'bg-primary-50 text-primary-700' : 'text-gray-700'
            ]"
            @click="selectUser(user)"
            @mouseenter="highlightedIndex = index"
          >
            <!-- Avatar -->
            <span class="w-6 h-6 rounded-full bg-gray-200 text-gray-600 text-[11px] font-medium flex items-center justify-center flex-shrink-0">
              {{ user.userName.charAt(0) }}
            </span>

            <!-- User Info -->
            <div class="flex-1 min-w-0">
              <div class="truncate font-medium">{{ user.userName }}</div>
              <div v-if="user.departmentName" class="truncate text-[11px] text-gray-400">
                {{ user.departmentName }}
              </div>
            </div>

            <!-- Check Icon -->
            <svg
              v-if="user.userId === modelValue"
              class="w-4 h-4 text-primary-600 flex-shrink-0"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>

          <!-- Empty State -->
          <div v-if="filteredUsers.length === 0" class="px-3 py-4 text-center text-gray-500 text-[13px]">
            <template v-if="searchQuery">검색 결과가 없습니다</template>
            <template v-else>사용자가 없습니다</template>
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
