<script setup lang="ts">
/**
 * 날짜/시간 선택 컴포넌트
 * - Teleport를 사용하여 body에 오버레이 렌더링
 * - 모달/스크롤 컨테이너 overflow 문제 해결
 * Compact UI: height 32px, font 13px
 */
import { computed, ref, watch, onMounted, onUnmounted, nextTick } from 'vue'

export type DatePickerSize = 'sm' | 'md' | 'lg'
export type DatePickerMode = 'date' | 'datetime' | 'time'

interface Props {
  modelValue?: string | null
  mode?: DatePickerMode
  size?: DatePickerSize
  placeholder?: string
  disabled?: boolean
  error?: boolean
  errorMessage?: string
  label?: string
  required?: boolean
  clearable?: boolean
  minDate?: string
  maxDate?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  mode: 'date',
  size: 'md',
  placeholder: '날짜 선택',
  disabled: false,
  error: false,
  required: false,
  clearable: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
  (e: 'change', value: string | null): void
}>()

// 트리거 버튼 ref
const triggerRef = ref<HTMLElement | null>(null)

// 드롭다운 상태
const isOpen = ref(false)

// 드롭다운 위치 (Teleport용 절대 위치)
const dropdownStyle = ref({
  top: '0px',
  left: '0px',
  width: '288px'
})

// 현재 표시 중인 년/월
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth())

// 시간 값
const hours = ref('00')
const minutes = ref('00')

// 선택된 날짜 파싱
const selectedDate = computed(() => {
  if (!props.modelValue) return null
  return new Date(props.modelValue)
})

// 표시용 포맷된 값
const displayValue = computed(() => {
  if (!props.modelValue) return ''
  const date = new Date(props.modelValue)

  if (props.mode === 'time') {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }

  const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

  if (props.mode === 'datetime') {
    return `${dateStr} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }

  return dateStr
})

// 달력 데이터
const calendarDays = computed(() => {
  const firstDay = new Date(currentYear.value, currentMonth.value, 1)
  const lastDay = new Date(currentYear.value, currentMonth.value + 1, 0)

  const days: Array<{ date: Date; isCurrentMonth: boolean; isToday: boolean; isSelected: boolean; isDisabled: boolean }> = []

  // 이전 달 날짜들
  const startDay = firstDay.getDay()
  for (let i = startDay - 1; i >= 0; i--) {
    const date = new Date(currentYear.value, currentMonth.value, -i)
    days.push({
      date,
      isCurrentMonth: false,
      isToday: false,
      isSelected: false,
      isDisabled: isDateDisabled(date)
    })
  }

  // 현재 달 날짜들
  const today = new Date()
  for (let i = 1; i <= lastDay.getDate(); i++) {
    const date = new Date(currentYear.value, currentMonth.value, i)
    days.push({
      date,
      isCurrentMonth: true,
      isToday: isSameDay(date, today),
      isSelected: selectedDate.value ? isSameDay(date, selectedDate.value) : false,
      isDisabled: isDateDisabled(date)
    })
  }

  // 다음 달 날짜들 (6주 채우기)
  const remainingDays = 42 - days.length
  for (let i = 1; i <= remainingDays; i++) {
    const date = new Date(currentYear.value, currentMonth.value + 1, i)
    days.push({
      date,
      isCurrentMonth: false,
      isToday: false,
      isSelected: false,
      isDisabled: isDateDisabled(date)
    })
  }

  return days
})

// 월 이름
const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']
const dayNames = ['일', '월', '화', '수', '목', '금', '토']

// 트리거 스타일
const triggerClasses = computed(() => {
  const base = [
    'w-full rounded border transition-all duration-150',
    'flex items-center justify-between gap-2',
    'focus:outline-none focus:ring-2 focus:border-transparent',
    'text-left'
  ]

  const sizes: Record<DatePickerSize, string> = {
    sm: 'px-2 py-1 text-xs h-7',
    md: 'px-3 py-1.5 text-[13px] h-8',
    lg: 'px-3 py-2 text-sm h-9'
  }

  const states = props.error
    ? 'border-red-500 focus:ring-red-500'
    : isOpen.value
      ? 'border-primary-500 ring-2 ring-primary-500'
      : 'border-gray-300 focus:ring-primary-500 dark:border-gray-600'

  const disabled = props.disabled
    ? 'bg-gray-100 cursor-not-allowed text-gray-500 dark:bg-gray-800 dark:text-gray-400'
    : 'bg-white cursor-pointer hover:border-gray-400 dark:bg-gray-800 dark:text-gray-100 dark:hover:border-gray-500'

  return [...base, sizes[props.size], states, disabled].join(' ')
})

function isSameDay(date1: Date, date2: Date): boolean {
  return (
    date1.getFullYear() === date2.getFullYear() &&
    date1.getMonth() === date2.getMonth() &&
    date1.getDate() === date2.getDate()
  )
}

function isDateDisabled(date: Date): boolean {
  if (props.minDate && date < new Date(props.minDate)) return true
  if (props.maxDate && date > new Date(props.maxDate)) return true
  return false
}

// 드롭다운 위치 계산
function updateDropdownPosition() {
  if (!triggerRef.value) return

  const rect = triggerRef.value.getBoundingClientRect()
  const dropdownHeight = props.mode === 'time' ? 60 : 340
  const dropdownWidth = props.mode === 'time' ? 160 : 288
  const viewportHeight = window.innerHeight
  const viewportWidth = window.innerWidth

  // 아래 공간 확인
  const spaceBelow = viewportHeight - rect.bottom
  const spaceAbove = rect.top

  // 세로 위치: 아래 공간 부족하면 위로
  let top: number
  if (spaceBelow < dropdownHeight && spaceAbove > spaceBelow) {
    top = rect.top - dropdownHeight - 4
  } else {
    top = rect.bottom + 4
  }

  // 가로 위치: 오른쪽 정렬 (넘치면 왼쪽으로)
  let left = rect.right - dropdownWidth
  if (left < 8) {
    left = rect.left
  }
  if (left + dropdownWidth > viewportWidth - 8) {
    left = viewportWidth - dropdownWidth - 8
  }

  dropdownStyle.value = {
    top: `${Math.max(8, top)}px`,
    left: `${Math.max(8, left)}px`,
    width: `${dropdownWidth}px`
  }
}

function openDropdown() {
  if (props.disabled) return

  isOpen.value = true

  if (selectedDate.value) {
    currentYear.value = selectedDate.value.getFullYear()
    currentMonth.value = selectedDate.value.getMonth()
    hours.value = String(selectedDate.value.getHours()).padStart(2, '0')
    minutes.value = String(selectedDate.value.getMinutes()).padStart(2, '0')
  }

  nextTick(() => {
    updateDropdownPosition()
  })
}

function closeDropdown() {
  isOpen.value = false
}

function toggleDropdown() {
  if (isOpen.value) {
    closeDropdown()
  } else {
    openDropdown()
  }
}

// 외부 클릭 감지
function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement

  // 트리거 버튼 클릭은 무시 (toggleDropdown에서 처리)
  if (triggerRef.value?.contains(target)) return

  // 드롭다운 내부 클릭은 무시
  if (target.closest('[data-datepicker-dropdown]')) return

  closeDropdown()
}

// 스크롤/리사이즈 시 위치 업데이트
function handleScrollOrResize() {
  if (isOpen.value) {
    updateDropdownPosition()
  }
}

function prevMonth() {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

function nextMonth() {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

// 로컬 시간대 유지하는 ISO 형식 변환 (UTC 변환 방지)
function toLocalISOString(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}

function selectDate(day: typeof calendarDays.value[0]) {
  if (day.isDisabled) return

  const date = new Date(day.date)

  if (props.mode === 'datetime') {
    date.setHours(parseInt(hours.value), parseInt(minutes.value), 0, 0)
  }

  const value = props.mode === 'datetime'
    ? toLocalISOString(date)
    : `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

  emit('update:modelValue', value)
  emit('change', value)

  if (props.mode === 'date') {
    closeDropdown()
  }
}

function updateTime() {
  if (!selectedDate.value) return

  const date = new Date(selectedDate.value)
  date.setHours(parseInt(hours.value), parseInt(minutes.value), 0, 0)

  const value = toLocalISOString(date)
  emit('update:modelValue', value)
  emit('change', value)
}

function selectToday() {
  const today = new Date()
  const value = props.mode === 'datetime'
    ? toLocalISOString(today)
    : `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  emit('update:modelValue', value)
  emit('change', value)
  closeDropdown()
}

function clearValue(event: Event) {
  event.stopPropagation()
  emit('update:modelValue', null)
  emit('change', null)
}

// ESC 키로 닫기
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && isOpen.value) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('scroll', handleScrollOrResize, true)
  window.addEventListener('resize', handleScrollOrResize)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('scroll', handleScrollOrResize, true)
  window.removeEventListener('resize', handleScrollOrResize)
})
</script>

<template>
  <div class="relative w-full">
    <!-- Label -->
    <label v-if="label" class="block text-[13px] font-medium text-gray-700 mb-1 dark:text-gray-300">
      {{ label }}
      <span v-if="required" class="text-red-500 ml-0.5">*</span>
    </label>

    <!-- Trigger -->
    <button
      ref="triggerRef"
      type="button"
      :class="triggerClasses"
      :disabled="disabled"
      @click="toggleDropdown"
    >
      <!-- Calendar Icon -->
      <div class="flex items-center gap-2 truncate">
        <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <span v-if="displayValue">{{ displayValue }}</span>
        <span v-else class="text-gray-400">{{ placeholder }}</span>
      </div>

      <!-- Clear Button -->
      <span
        v-if="clearable && modelValue && !disabled"
        class="text-gray-400 hover:text-gray-600 p-0.5 dark:hover:text-gray-300"
        @click="clearValue"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </span>
    </button>

    <!-- Error Message -->
    <p v-if="error && errorMessage" class="mt-1 text-xs text-red-500">
      {{ errorMessage }}
    </p>

    <!-- Teleport: 드롭다운을 body에 렌더링 -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-150 ease-out"
        enter-from-class="opacity-0 scale-95"
        enter-to-class="opacity-100 scale-100"
        leave-active-class="transition duration-100 ease-in"
        leave-from-class="opacity-100 scale-100"
        leave-to-class="opacity-0 scale-95"
      >
        <div
          v-if="isOpen"
          data-datepicker-dropdown
          class="fixed z-[9999] bg-white border border-gray-200 rounded-lg shadow-xl p-3 dark:bg-gray-800 dark:border-gray-700"
          :style="dropdownStyle"
        >
          <!-- Time Only Mode -->
          <div v-if="mode === 'time'" class="flex items-center gap-2">
            <input
              v-model="hours"
              type="text"
              maxlength="2"
              class="w-12 px-2 py-1 text-center text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
              @change="updateTime"
            />
            <span class="text-gray-500 dark:text-gray-400">:</span>
            <input
              v-model="minutes"
              type="text"
              maxlength="2"
              class="w-12 px-2 py-1 text-center text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
              @change="updateTime"
            />
          </div>

          <!-- Date/DateTime Mode -->
          <template v-else>
            <!-- Header -->
            <div class="flex items-center justify-between mb-2">
              <button
                type="button"
                class="p-1 hover:bg-gray-100 rounded dark:hover:bg-gray-700"
                @click="prevMonth"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                </svg>
              </button>

              <span class="text-[13px] font-medium dark:text-gray-200">
                {{ currentYear }}년 {{ monthNames[currentMonth] }}
              </span>

              <button
                type="button"
                class="p-1 hover:bg-gray-100 rounded dark:hover:bg-gray-700"
                @click="nextMonth"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </button>
            </div>

            <!-- Day Names -->
            <div class="grid grid-cols-7 gap-0.5 mb-1">
              <div
                v-for="day in dayNames"
                :key="day"
                class="text-center text-[11px] text-gray-500 py-1 dark:text-gray-400"
              >
                {{ day }}
              </div>
            </div>

            <!-- Calendar Grid -->
            <div class="grid grid-cols-7 gap-0.5">
              <button
                v-for="(day, index) in calendarDays"
                :key="index"
                type="button"
                class="w-8 h-8 text-[12px] rounded flex items-center justify-center transition-colors dark:text-gray-200"
                :class="[
                  day.isCurrentMonth ? '' : 'text-gray-300 dark:text-gray-600',
                  day.isToday ? 'font-bold' : '',
                  day.isSelected ? 'bg-primary-600 text-white' : 'hover:bg-gray-100 dark:hover:bg-gray-700',
                  day.isDisabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'
                ]"
                :disabled="day.isDisabled"
                @click="selectDate(day)"
              >
                {{ day.date.getDate() }}
              </button>
            </div>

            <!-- Time Picker (DateTime mode) -->
            <div v-if="mode === 'datetime'" class="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-center gap-2">
                <span class="text-[13px] text-gray-600 dark:text-gray-400">시간:</span>
                <input
                  v-model="hours"
                  type="text"
                  maxlength="2"
                  class="w-10 px-1 py-0.5 text-center text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
                  @change="updateTime"
                />
                <span class="text-gray-500 dark:text-gray-400">:</span>
                <input
                  v-model="minutes"
                  type="text"
                  maxlength="2"
                  class="w-10 px-1 py-0.5 text-center text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
                  @change="updateTime"
                />
              </div>
            </div>

            <!-- Footer -->
            <div class="mt-3 pt-3 border-t border-gray-200 flex justify-between dark:border-gray-700">
              <button
                type="button"
                class="text-[12px] text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
                @click="selectToday"
              >
                오늘
              </button>
              <button
                type="button"
                class="text-[12px] text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
                @click="closeDropdown"
              >
                닫기
              </button>
            </div>
          </template>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
