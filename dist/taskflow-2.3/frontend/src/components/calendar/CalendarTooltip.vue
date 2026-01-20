<script setup lang="ts">
/**
 * 캘린더 전체 이벤트 툴팁 컴포넌트
 * - 날짜 셀의 "+N개" 클릭 시 전체 이벤트 목록 표시
 * - 날짜 정보 (음력, 공휴일 등) 함께 표시
 */
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import type { CalendarEvent, CalendarDateInfo } from '@/types/calendar'

interface Props {
  visible: boolean
  date: string
  events: CalendarEvent[]
  dateInfo?: CalendarDateInfo
  anchorRect?: DOMRect | null  // 기준 요소 위치
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  events: () => [],
  dateInfo: undefined,
  anchorRect: null
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'select', event: CalendarEvent): void
}>()

const tooltipRef = ref<HTMLElement | null>(null)

// 우선순위 라벨
const priorityLabels: Record<string, string> = {
  URGENT: '긴급',
  HIGH: '높음',
  NORMAL: '보통',
  LOW: '낮음'
}

// 상태 라벨
const statusLabels: Record<string, string> = {
  NOT_STARTED: '시작전',
  IN_PROGRESS: '진행중',
  PENDING: '보류',
  COMPLETED: '완료',
  DELETED: '삭제됨'
}

// Todo 배경색 (teal/cyan 계열)
const todoColors: Record<string, string> = {
  URGENT: 'bg-rose-400',
  HIGH: 'bg-amber-400',
  NORMAL: 'bg-teal-500',
  LOW: 'bg-slate-400'
}

// Item 배경색 (blue/indigo 계열)
const itemColors: Record<string, string> = {
  URGENT: 'bg-red-500',
  HIGH: 'bg-orange-500',
  NORMAL: 'bg-blue-500',
  LOW: 'bg-gray-400'
}

// Event 배경색 (purple 계열)
const eventColors: Record<string, string> = {
  URGENT: 'bg-pink-500',
  HIGH: 'bg-fuchsia-500',
  NORMAL: 'bg-purple-500',
  LOW: 'bg-violet-400'
}

// 날짜 포맷
const formattedDate = computed(() => {
  if (!props.date) return ''
  const [year, month, day] = props.date.split('-')
  return `${year}년 ${parseInt(month)}월 ${parseInt(day)}일`
})

// 요일 계산
const dayOfWeekName = computed(() => {
  if (props.dateInfo?.dayOfWeekName) return props.dateInfo.dayOfWeekName
  if (!props.date) return ''
  const d = new Date(props.date)
  const weekDays = ['일', '월', '화', '수', '목', '금', '토']
  return weekDays[d.getDay()]
})

// Todo, Item, Event 분리
const todoEvents = computed(() => props.events.filter(e => e.type === 'TODO'))
const itemEvents = computed(() => props.events.filter(e => e.type === 'ITEM'))
const eventEvents = computed(() => props.events.filter(e => e.type === 'EVENT'))

// 위치 스타일
const positionStyle = computed(() => {
  if (!props.anchorRect) return {}

  const rect = props.anchorRect
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight

  // 기본 위치: 앵커 아래
  let top = rect.bottom + 8
  let left = rect.left

  // 화면 오른쪽 넘어가면 왼쪽으로 조정
  if (left + 320 > viewportWidth) {
    left = viewportWidth - 330
  }

  // 화면 아래로 넘어가면 위로 표시
  if (top + 400 > viewportHeight) {
    top = rect.top - 408
  }

  return {
    position: 'fixed',
    top: `${Math.max(8, top)}px`,
    left: `${Math.max(8, left)}px`
  }
})

// 외부 클릭 감지
function handleClickOutside(event: MouseEvent) {
  if (tooltipRef.value && !tooltipRef.value.contains(event.target as Node)) {
    emit('close')
  }
}

// ESC 키 처리
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    emit('close')
  }
}

function handleEventClick(event: CalendarEvent) {
  emit('select', event)
}

function getDotColor(event: CalendarEvent): string {
  if (event.type === 'TODO') {
    return todoColors[event.priority] || 'bg-teal-500'
  } else if (event.type === 'EVENT') {
    return eventColors[event.priority] || 'bg-purple-500'
  }
  return itemColors[event.priority] || 'bg-blue-500'
}

// 마운트 시 이벤트 리스너 등록
onMounted(() => {
  document.addEventListener('click', handleClickOutside, true)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside, true)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-all duration-200 ease-out"
      enter-from-class="opacity-0 translate-y-2"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition-all duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 translate-y-2"
    >
      <div
        v-if="visible"
        ref="tooltipRef"
        class="calendar-tooltip z-50 bg-white rounded-xl shadow-2xl border border-gray-200
               w-[320px] max-h-[400px] overflow-hidden
               dark:bg-gray-800 dark:border-gray-700"
        :style="positionStyle"
      >
        <!-- 헤더 -->
        <div class="sticky top-0 bg-white dark:bg-gray-800 px-4 py-3 border-b border-gray-100 dark:border-gray-700">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-sm font-semibold text-gray-900 dark:text-gray-100">
                {{ formattedDate }}
                <span
                  class="text-xs font-normal ml-1"
                  :class="{
                    'text-red-500': dateInfo?.isHoliday || dayOfWeekName === '일',
                    'text-blue-500': dayOfWeekName === '토' && !dateInfo?.isHoliday,
                    'text-gray-500 dark:text-gray-400': dayOfWeekName !== '일' && dayOfWeekName !== '토' && !dateInfo?.isHoliday
                  }"
                >
                  ({{ dayOfWeekName }})
                </span>
              </h3>
              <!-- 음력/공휴일 정보 -->
              <div v-if="dateInfo" class="flex items-center gap-2 mt-0.5">
                <span v-if="dateInfo.lunarDisplay" class="text-xs text-gray-400 dark:text-gray-500">
                  음력 {{ dateInfo.lunarDisplay }}
                </span>
                <span v-if="dateInfo.holidayName" class="text-xs text-red-500 font-medium">
                  {{ dateInfo.holidayName }}
                </span>
              </div>
            </div>
            <!-- 닫기 버튼 -->
            <button
              class="p-1 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              @click="$emit('close')"
            >
              <svg class="w-4 h-4 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 이벤트 목록 -->
        <div class="overflow-y-auto max-h-[320px] p-2">
          <!-- 이벤트 없음 -->
          <div v-if="events.length === 0" class="py-8 text-center text-gray-400 dark:text-gray-500">
            이 날짜에 등록된 일정이 없습니다.
          </div>

          <!-- Todo 섹션 -->
          <div v-if="todoEvents.length > 0" class="mb-3">
            <div class="flex items-center gap-1.5 px-2 py-1 text-xs text-teal-600 dark:text-teal-400 font-medium">
              <span class="w-2 h-2 rounded-full bg-teal-500" />
              <span>Todo ({{ todoEvents.length }})</span>
            </div>
            <div class="space-y-1">
              <button
                v-for="event in todoEvents"
                :key="`todo-${event.id}`"
                class="w-full flex items-start gap-2 px-2 py-1.5 rounded-lg text-left
                       hover:bg-teal-50 dark:hover:bg-teal-900/30 transition-colors group"
                @click="handleEventClick(event)"
              >
                <span
                  class="flex-shrink-0 mt-1 w-2 h-2 rounded-full"
                  :class="getDotColor(event)"
                />
                <div class="flex-1 min-w-0">
                  <p
                    class="text-sm text-gray-900 dark:text-gray-100 truncate"
                    :class="{ 'line-through opacity-60': event.isCompleted }"
                  >
                    {{ event.title }}
                  </p>
                  <div class="flex items-center gap-2 mt-0.5">
                    <span
                      class="text-[10px] px-1 py-0.5 rounded"
                      :class="{
                        'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300': event.priority === 'URGENT',
                        'bg-orange-100 text-orange-700 dark:bg-orange-900/50 dark:text-orange-300': event.priority === 'HIGH',
                        'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300': event.priority === 'NORMAL',
                        'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-400': event.priority === 'LOW'
                      }"
                    >
                      {{ priorityLabels[event.priority] || '보통' }}
                    </span>
                    <span v-if="event.isCompleted" class="text-[10px] text-green-600 dark:text-green-400">
                      완료됨
                    </span>
                  </div>
                </div>
              </button>
            </div>
          </div>

          <!-- Item 섹션 -->
          <div v-if="itemEvents.length > 0" class="mb-3">
            <div class="flex items-center gap-1.5 px-2 py-1 text-xs text-blue-600 dark:text-blue-400 font-medium">
              <span class="w-2 h-2 rounded-sm bg-blue-500" />
              <span>업무 ({{ itemEvents.length }})</span>
            </div>
            <div class="space-y-1">
              <button
                v-for="event in itemEvents"
                :key="`item-${event.id}`"
                class="w-full flex items-start gap-2 px-2 py-1.5 rounded-lg text-left
                       hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors group"
                @click="handleEventClick(event)"
              >
                <span
                  class="flex-shrink-0 mt-1 w-2 h-2 rounded-sm"
                  :class="getDotColor(event)"
                />
                <div class="flex-1 min-w-0">
                  <p
                    class="text-sm text-gray-900 dark:text-gray-100 truncate"
                    :class="{ 'line-through opacity-60': event.status === 'COMPLETED' }"
                  >
                    {{ event.title }}
                  </p>
                  <div class="flex items-center gap-2 mt-0.5 flex-wrap">
                    <!-- 우선순위 -->
                    <span
                      class="text-[10px] px-1 py-0.5 rounded"
                      :class="{
                        'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300': event.priority === 'URGENT',
                        'bg-orange-100 text-orange-700 dark:bg-orange-900/50 dark:text-orange-300': event.priority === 'HIGH',
                        'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300': event.priority === 'NORMAL',
                        'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-400': event.priority === 'LOW'
                      }"
                    >
                      {{ priorityLabels[event.priority] || '보통' }}
                    </span>
                    <!-- 상태 -->
                    <span
                      v-if="event.status"
                      class="text-[10px] px-1 py-0.5 rounded"
                      :class="{
                        'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300': event.status === 'NOT_STARTED',
                        'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400': event.status === 'IN_PROGRESS',
                        'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/50 dark:text-yellow-400': event.status === 'PENDING',
                        'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400': event.status === 'COMPLETED',
                        'bg-red-100 text-red-600 dark:bg-red-900/50 dark:text-red-400': event.status === 'DELETED'
                      }"
                    >
                      {{ statusLabels[event.status] || event.status }}
                    </span>
                    <!-- 보드명 -->
                    <span v-if="event.boardName" class="text-[10px] text-gray-400 dark:text-gray-500 truncate max-w-[100px]">
                      {{ event.boardName }}
                    </span>
                  </div>
                </div>
              </button>
            </div>
          </div>

          <!-- Event 섹션 -->
          <div v-if="eventEvents.length > 0">
            <div class="flex items-center gap-1.5 px-2 py-1 text-xs text-purple-600 dark:text-purple-400 font-medium">
              <span class="w-2 h-2 rotate-45 bg-purple-500" />
              <span>이벤트 ({{ eventEvents.length }})</span>
            </div>
            <div class="space-y-1">
              <button
                v-for="event in eventEvents"
                :key="`event-${event.id}`"
                class="w-full flex items-start gap-2 px-2 py-1.5 rounded-lg text-left
                       hover:bg-purple-50 dark:hover:bg-purple-900/30 transition-colors group"
                @click="handleEventClick(event)"
              >
                <span
                  class="flex-shrink-0 mt-1 w-2 h-2 rotate-45"
                  :class="getDotColor(event)"
                />
                <div class="flex-1 min-w-0">
                  <p class="text-sm text-gray-900 dark:text-gray-100 truncate">
                    {{ event.title }}
                  </p>
                  <div class="flex items-center gap-2 mt-0.5 flex-wrap">
                    <!-- 우선순위 -->
                    <span
                      class="text-[10px] px-1 py-0.5 rounded"
                      :class="{
                        'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300': event.priority === 'URGENT',
                        'bg-orange-100 text-orange-700 dark:bg-orange-900/50 dark:text-orange-300': event.priority === 'HIGH',
                        'bg-purple-100 text-purple-600 dark:bg-purple-900/50 dark:text-purple-300': event.priority === 'NORMAL',
                        'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-400': event.priority === 'LOW'
                      }"
                    >
                      {{ priorityLabels[event.priority] || '보통' }}
                    </span>
                    <!-- 반복 표시 -->
                    <span v-if="event.isRecurring" class="text-[10px] text-purple-500 dark:text-purple-400">
                      🔄 반복
                    </span>
                    <!-- 종일 표시 -->
                    <span v-if="event.isAllDay" class="text-[10px] text-gray-400 dark:text-gray-500">
                      종일
                    </span>
                  </div>
                </div>
              </button>
            </div>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="sticky bottom-0 bg-gray-50 dark:bg-gray-900/50 px-4 py-2 text-center border-t border-gray-100 dark:border-gray-700">
          <p class="text-[10px] text-gray-400 dark:text-gray-500">
            클릭하여 상세 보기 | ESC로 닫기
          </p>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
