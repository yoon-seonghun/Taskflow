<script setup lang="ts">
/**
 * 날짜/시간 선택 컴포넌트
 * Compact UI: height 32px, font 13px
 */
import { computed, ref, onMounted, onUnmounted } from 'vue'

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

const isOpen = ref(false)
const containerRef = ref<HTMLElement | null>(null)

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
      : 'border-gray-300 focus:ring-primary-500'

  const disabled = props.disabled
    ? 'bg-gray-100 cursor-not-allowed text-gray-500'
    : 'bg-white cursor-pointer hover:border-gray-400'

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

function toggleDropdown() {
  if (props.disabled) return
  isOpen.value = !isOpen.value
  if (isOpen.value && selectedDate.value) {
    currentYear.value = selectedDate.value.getFullYear()
    currentMonth.value = selectedDate.value.getMonth()
    hours.value = String(selectedDate.value.getHours()).padStart(2, '0')
    minutes.value = String(selectedDate.value.getMinutes()).padStart(2, '0')
  }
}

function closeDropdown() {
  isOpen.value = false
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

function selectDate(day: typeof calendarDays.value[0]) {
  if (day.isDisabled) return

  const date = new Date(day.date)

  if (props.mode === 'datetime') {
    date.setHours(parseInt(hours.value), parseInt(minutes.value))
  }

  const value = props.mode === 'datetime'
    ? date.toISOString()
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
  date.setHours(parseInt(hours.value), parseInt(minutes.value))

  emit('update:modelValue', date.toISOString())
  emit('change', date.toISOString())
}

function selectToday() {
  const today = new Date()
  if (props.mode === 'datetime') {
    emit('update:modelValue', today.toISOString())
  } else {
    const value = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
    emit('update:modelValue', value)
  }
  emit('change', props.modelValue)
  closeDropdown()
}

function clearValue(event: Event) {
  event.stopPropagation()
  emit('update:modelValue', null)
  emit('change', null)
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
        class="text-gray-400 hover:text-gray-600 p-0.5"
        @click="clearValue"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </span>
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
        class="absolute z-50 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg p-3"
        :class="mode === 'time' ? 'w-40' : 'w-72'"
      >
        <!-- Time Only Mode -->
        <div v-if="mode === 'time'" class="flex items-center gap-2">
          <input
            v-model="hours"
            type="text"
            maxlength="2"
            class="w-12 px-2 py-1 text-center text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500"
            @change="updateTime"
          />
          <span class="text-gray-500">:</span>
          <input
            v-model="minutes"
            type="text"
            maxlength="2"
            class="w-12 px-2 py-1 text-center text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500"
            @change="updateTime"
          />
        </div>

        <!-- Date/DateTime Mode -->
        <template v-else>
          <!-- Header -->
          <div class="flex items-center justify-between mb-2">
            <button
              type="button"
              class="p-1 hover:bg-gray-100 rounded"
              @click="prevMonth"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>

            <span class="text-[13px] font-medium">
              {{ currentYear }}년 {{ monthNames[currentMonth] }}
            </span>

            <button
              type="button"
              class="p-1 hover:bg-gray-100 rounded"
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
              class="text-center text-[11px] text-gray-500 py-1"
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
              class="w-8 h-8 text-[12px] rounded flex items-center justify-center transition-colors"
              :class="[
                day.isCurrentMonth ? '' : 'text-gray-300',
                day.isToday ? 'font-bold' : '',
                day.isSelected ? 'bg-primary-600 text-white' : 'hover:bg-gray-100',
                day.isDisabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'
              ]"
              :disabled="day.isDisabled"
              @click="selectDate(day)"
            >
              {{ day.date.getDate() }}
            </button>
          </div>

          <!-- Time Picker (DateTime mode) -->
          <div v-if="mode === 'datetime'" class="mt-3 pt-3 border-t border-gray-200">
            <div class="flex items-center justify-center gap-2">
              <span class="text-[13px] text-gray-600">시간:</span>
              <input
                v-model="hours"
                type="text"
                maxlength="2"
                class="w-10 px-1 py-0.5 text-center text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500"
                @change="updateTime"
              />
              <span class="text-gray-500">:</span>
              <input
                v-model="minutes"
                type="text"
                maxlength="2"
                class="w-10 px-1 py-0.5 text-center text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500"
                @change="updateTime"
              />
            </div>
          </div>

          <!-- Footer -->
          <div class="mt-3 pt-3 border-t border-gray-200 flex justify-between">
            <button
              type="button"
              class="text-[12px] text-primary-600 hover:text-primary-700"
              @click="selectToday"
            >
              오늘
            </button>
            <button
              type="button"
              class="text-[12px] text-gray-500 hover:text-gray-700"
              @click="closeDropdown"
            >
              닫기
            </button>
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
