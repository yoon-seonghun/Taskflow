<script setup lang="ts">
/**
 * 캘린더 필터 패널
 * - 캘린더별 필터
 * - 타입별 필터 (Todo, 업무, 이벤트)
 * - 우선순위 필터
 */
import { ref, computed, watch, onMounted } from 'vue'
import { calendarApi } from '@/api/calendar'

interface Props {
  includeTodo: boolean
  includeItem: boolean
  includeEvent: boolean
  selectedCalendars: number[]
  selectedPriorities: string[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:includeTodo', value: boolean): void
  (e: 'update:includeItem', value: boolean): void
  (e: 'update:includeEvent', value: boolean): void
  (e: 'update:selectedCalendars', value: number[]): void
  (e: 'update:selectedPriorities', value: string[]): void
  (e: 'filterChange'): void
}>()

// 캘린더 항목 타입
interface CalendarItem {
  calendarId: number
  name: string
  color: string
  ownerName: string
  isOwner: boolean
}

// 캘린더 목록
const calendars = ref<CalendarItem[]>([])
const sharedCalendars = ref<CalendarItem[]>([])
const loadingCalendars = ref(false)

// 패널 접기/펼치기
const isExpanded = ref(true)

// 우선순위 옵션
const priorityOptions = [
  { value: 'URGENT', label: '긴급', color: 'bg-red-500' },
  { value: 'HIGH', label: '높음', color: 'bg-orange-500' },
  { value: 'NORMAL', label: '보통', color: 'bg-blue-500' },
  { value: 'LOW', label: '낮음', color: 'bg-gray-400' }
]

// 캘린더 목록 로드
async function loadCalendars() {
  loadingCalendars.value = true
  try {
    const response = await calendarApi.getMyCalendars()
    const allCalendars = (response.data || []).map(c => ({
      calendarId: c.calendarId,
      name: c.calendarName,
      color: c.color,
      ownerName: c.ownerName,
      isOwner: !c.isShared  // 공유받은 캘린더가 아니면 내 캘린더
    }))

    calendars.value = allCalendars.filter(c => c.isOwner)
    sharedCalendars.value = allCalendars.filter(c => !c.isOwner)
  } catch (err) {
    console.error('Failed to load calendars:', err)
  } finally {
    loadingCalendars.value = false
  }
}

// 타입 필터 토글
function toggleTodo() {
  emit('update:includeTodo', !props.includeTodo)
  emit('filterChange')
}

function toggleItem() {
  emit('update:includeItem', !props.includeItem)
  emit('filterChange')
}

function toggleEvent() {
  emit('update:includeEvent', !props.includeEvent)
  emit('filterChange')
}

// 캘린더 필터 토글
function toggleCalendar(calendarId: number) {
  const current = [...props.selectedCalendars]
  const index = current.indexOf(calendarId)

  if (index === -1) {
    current.push(calendarId)
  } else {
    current.splice(index, 1)
  }

  emit('update:selectedCalendars', current)
  emit('filterChange')
}

// 모든 캘린더 선택/해제
function toggleAllCalendars(select: boolean) {
  if (select) {
    const allIds = [...calendars.value, ...sharedCalendars.value].map(c => c.calendarId)
    emit('update:selectedCalendars', allIds)
  } else {
    emit('update:selectedCalendars', [])
  }
  emit('filterChange')
}

// 우선순위 필터 토글
function togglePriority(priority: string) {
  const current = [...props.selectedPriorities]
  const index = current.indexOf(priority)

  if (index === -1) {
    current.push(priority)
  } else {
    current.splice(index, 1)
  }

  emit('update:selectedPriorities', current)
  emit('filterChange')
}

// 모든 우선순위 선택/해제
function toggleAllPriorities(select: boolean) {
  if (select) {
    emit('update:selectedPriorities', priorityOptions.map(p => p.value))
  } else {
    emit('update:selectedPriorities', [])
  }
  emit('filterChange')
}

// 필터 초기화
function resetFilters() {
  emit('update:includeTodo', true)
  emit('update:includeItem', true)
  emit('update:includeEvent', true)

  const allCalendarIds = [...calendars.value, ...sharedCalendars.value].map(c => c.calendarId)
  emit('update:selectedCalendars', allCalendarIds)

  emit('update:selectedPriorities', priorityOptions.map(p => p.value))
  emit('filterChange')
}

// 캘린더 선택 여부
function isCalendarSelected(calendarId: number): boolean {
  return props.selectedCalendars.includes(calendarId)
}

// 우선순위 선택 여부
function isPrioritySelected(priority: string): boolean {
  return props.selectedPriorities.includes(priority)
}

// 활성 필터 수 계산
const activeFilterCount = computed(() => {
  let count = 0

  if (!props.includeTodo) count++
  if (!props.includeItem) count++
  if (!props.includeEvent) count++

  const totalCalendars = calendars.value.length + sharedCalendars.value.length
  if (props.selectedCalendars.length < totalCalendars) {
    count += totalCalendars - props.selectedCalendars.length
  }

  if (props.selectedPriorities.length < priorityOptions.length) {
    count += priorityOptions.length - props.selectedPriorities.length
  }

  return count
})

onMounted(() => {
  loadCalendars()
})
</script>

<template>
  <div class="calendar-filter-panel bg-white rounded-lg border border-gray-200 dark:bg-gray-800 dark:border-gray-700">
    <!-- 헤더 -->
    <div
      class="flex items-center justify-between p-3 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50"
      @click="isExpanded = !isExpanded"
    >
      <div class="flex items-center gap-2">
        <svg
          class="w-4 h-4 text-gray-500 transition-transform dark:text-gray-400"
          :class="{ 'rotate-180': !isExpanded }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
        <span class="text-sm font-medium text-gray-700 dark:text-gray-300">필터</span>
        <span
          v-if="activeFilterCount > 0"
          class="px-1.5 py-0.5 text-[10px] bg-primary-100 text-primary-700 rounded-full dark:bg-primary-900/50 dark:text-primary-300"
        >
          {{ activeFilterCount }}
        </span>
      </div>

      <button
        v-if="activeFilterCount > 0"
        class="text-xs text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
        @click.stop="resetFilters"
      >
        초기화
      </button>
    </div>

    <!-- 필터 내용 -->
    <div v-show="isExpanded" class="border-t border-gray-200 dark:border-gray-700">
      <!-- 타입 필터 -->
      <div class="p-3 border-b border-gray-100 dark:border-gray-700/50">
        <div class="text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">표시 항목</div>
        <div class="flex flex-wrap gap-2">
          <button
            class="flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-full transition-colors"
            :class="includeTodo
              ? 'bg-teal-100 text-teal-700 dark:bg-teal-900/50 dark:text-teal-300'
              : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'"
            @click="toggleTodo"
          >
            <span class="w-2 h-2 rounded-full bg-teal-500" />
            Todo
          </button>
          <button
            class="flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-full transition-colors"
            :class="includeItem
              ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300'
              : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'"
            @click="toggleItem"
          >
            <span class="w-2 h-2 rounded-sm bg-blue-500" />
            업무
          </button>
          <button
            class="flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-full transition-colors"
            :class="includeEvent
              ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/50 dark:text-purple-300'
              : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'"
            @click="toggleEvent"
          >
            <span class="w-2 h-2 rounded bg-purple-500" />
            이벤트
          </button>
        </div>
      </div>

      <!-- 우선순위 필터 -->
      <div class="p-3 border-b border-gray-100 dark:border-gray-700/50">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-gray-500 dark:text-gray-400">우선순위</span>
          <button
            class="text-[10px] text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
            @click="toggleAllPriorities(selectedPriorities.length < priorityOptions.length)"
          >
            {{ selectedPriorities.length < priorityOptions.length ? '전체 선택' : '전체 해제' }}
          </button>
        </div>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="priority in priorityOptions"
            :key="priority.value"
            class="flex items-center gap-1 px-2 py-0.5 text-[11px] rounded transition-colors"
            :class="isPrioritySelected(priority.value)
              ? 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-200'
              : 'bg-gray-50 text-gray-400 dark:bg-gray-800 dark:text-gray-500'"
            @click="togglePriority(priority.value)"
          >
            <span class="w-2 h-2 rounded-full" :class="priority.color" />
            {{ priority.label }}
          </button>
        </div>
      </div>

      <!-- 내 캘린더 -->
      <div v-if="calendars.length > 0" class="p-3 border-b border-gray-100 dark:border-gray-700/50">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-gray-500 dark:text-gray-400">내 캘린더</span>
          <button
            class="text-[10px] text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
            @click="toggleAllCalendars(selectedCalendars.length < calendars.length + sharedCalendars.length)"
          >
            {{ selectedCalendars.length < calendars.length + sharedCalendars.length ? '전체 선택' : '전체 해제' }}
          </button>
        </div>
        <div class="space-y-1">
          <label
            v-for="calendar in calendars"
            :key="calendar.calendarId"
            class="flex items-center gap-2 cursor-pointer group"
          >
            <input
              type="checkbox"
              :checked="isCalendarSelected(calendar.calendarId)"
              class="w-3.5 h-3.5 rounded border-gray-300 dark:border-gray-600 dark:bg-gray-700"
              :style="{ accentColor: calendar.color || '#3b82f6' }"
              @change="toggleCalendar(calendar.calendarId)"
            />
            <span
              class="w-2.5 h-2.5 rounded-sm flex-shrink-0"
              :style="{ backgroundColor: calendar.color || '#3b82f6' }"
            />
            <span class="text-xs text-gray-700 dark:text-gray-300 truncate flex-1">
              {{ calendar.name }}
            </span>
          </label>
        </div>
      </div>

      <!-- 공유 캘린더 -->
      <div v-if="sharedCalendars.length > 0" class="p-3">
        <div class="text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">공유 캘린더</div>
        <div class="space-y-1">
          <label
            v-for="calendar in sharedCalendars"
            :key="calendar.calendarId"
            class="flex items-center gap-2 cursor-pointer group"
          >
            <input
              type="checkbox"
              :checked="isCalendarSelected(calendar.calendarId)"
              class="w-3.5 h-3.5 rounded border-gray-300 dark:border-gray-600 dark:bg-gray-700"
              :style="{ accentColor: calendar.color || '#3b82f6' }"
              @change="toggleCalendar(calendar.calendarId)"
            />
            <span
              class="w-2.5 h-2.5 rounded-sm flex-shrink-0"
              :style="{ backgroundColor: calendar.color || '#3b82f6' }"
            />
            <span class="text-xs text-gray-700 dark:text-gray-300 truncate flex-1">
              {{ calendar.name }}
            </span>
            <span class="text-[10px] text-gray-400 dark:text-gray-500">
              {{ calendar.ownerName }}
            </span>
          </label>
        </div>
      </div>

      <!-- 캘린더 없음 -->
      <div v-if="!loadingCalendars && calendars.length === 0 && sharedCalendars.length === 0" class="p-3 text-center">
        <p class="text-xs text-gray-400 dark:text-gray-500">캘린더가 없습니다.</p>
      </div>

      <!-- 로딩 -->
      <div v-if="loadingCalendars" class="p-3 text-center">
        <div class="w-4 h-4 border-2 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto" />
      </div>
    </div>
  </div>
</template>
