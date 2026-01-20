<script setup lang="ts">
/**
 * 일간 뷰 컴포넌트
 * - 단일 날짜의 시간대별 이벤트 표시
 * - 상세한 시간 슬롯
 */
import { computed, ref } from 'vue'
import type { CalendarEvent, CalendarDateInfo } from '@/types/calendar'
import type { EventDetailResponse } from '@/api/calendar'

interface Props {
  currentDate: Date
  events: Record<string, CalendarEvent[]>
  userEvents: EventDetailResponse[]
  dateInfo: Record<string, CalendarDateInfo>
  includeEvent: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'eventClick', event: CalendarEvent): void
  (e: 'dateClick', date: string, hour?: number): void
}>()

// 시간 배열 (0시 ~ 23시, 30분 단위)
const timeSlots = computed(() => {
  const slots: { hour: number; minute: number; label: string }[] = []
  for (let h = 0; h < 24; h++) {
    slots.push({ hour: h, minute: 0, label: formatTime(h, 0) })
    slots.push({ hour: h, minute: 30, label: '' })
  }
  return slots
})

// 오늘 날짜
const today = new Date()
const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

// 현재 시간
const currentHour = ref(today.getHours())
const currentMinute = ref(today.getMinutes())

// 현재 날짜 문자열
const currentDateStr = computed(() => {
  const d = props.currentDate
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

// 요일 이름
const weekDayNames = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일']

// 날짜 정보
const dayInfo = computed(() => {
  const d = props.currentDate
  return {
    year: d.getFullYear(),
    month: d.getMonth() + 1,
    day: d.getDate(),
    weekDay: d.getDay(),
    isToday: currentDateStr.value === todayStr
  }
})

// 캘린더 날짜 정보
const calendarDateInfo = computed(() => props.dateInfo?.[currentDateStr.value])

// 이벤트 가져오기
function getEventsForDate(): CalendarEvent[] {
  const dateStr = currentDateStr.value
  const events: CalendarEvent[] = []

  // 기존 Todo/Item 이벤트
  if (props.events && props.events[dateStr]) {
    events.push(...props.events[dateStr])
  }

  // 사용자 이벤트 추가
  if (props.includeEvent && props.userEvents.length > 0) {
    const userEventsForDate = props.userEvents.filter(e => {
      const start = e.startDate
      const end = e.endDate || e.startDate
      return dateStr >= start && dateStr <= end
    })

    for (const ue of userEventsForDate) {
      events.push({
        type: 'EVENT',  // 사용자 이벤트는 EVENT 타입
        id: ue.eventId,
        title: ue.title,
        priority: 'NORMAL',
        status: 'IN_PROGRESS',
        date: ue.startDate,
        isUserEvent: true,
        calendarColor: ue.calendarColor,
        startTime: ue.startTime,
        endTime: ue.endTime,
        isAllDay: ue.isAllDay,
        description: ue.description,
        location: ue.location,
        // 반복 이벤트 정보
        isRecurring: ue.isRecurring,
        recurrenceType: ue.recurrenceType || undefined
      } as CalendarEvent & { isUserEvent?: boolean; calendarColor?: string; startTime?: string; endTime?: string; isAllDay?: boolean; description?: string; location?: string })
    }
  }

  return events
}

// 종일 이벤트
const allDayEvents = computed(() => {
  return getEventsForDate().filter((e: any) => {
    if (e.isUserEvent && e.isAllDay) return true
    if (!e.isUserEvent) return true
    if (!e.startTime) return true
    return false
  })
})

// 시간대 이벤트
const timedEvents = computed(() => {
  return getEventsForDate().filter((e: any) => {
    return e.isUserEvent && !e.isAllDay && e.startTime
  })
})

// 특정 시간 슬롯의 이벤트
function getEventsForSlot(hour: number, minute: number): CalendarEvent[] {
  return timedEvents.value.filter((e: any) => {
    if (!e.startTime) return false
    const [h, m] = e.startTime.split(':').map(Number)
    // 30분 슬롯 단위로 체크
    if (minute === 0) {
      return h === hour && m < 30
    } else {
      return h === hour && m >= 30
    }
  })
}

// 시간 포맷
function formatTime(hour: number, minute: number): string {
  const period = hour < 12 ? '오전' : '오후'
  const h = hour === 0 ? 12 : hour > 12 ? hour - 12 : hour
  return `${period} ${h}:${String(minute).padStart(2, '0')}`
}

// 이벤트 색상
function getEventColor(event: CalendarEvent & { isUserEvent?: boolean; calendarColor?: string }): string {
  // 타입별 기본 색상
  if (event.type === 'TODO') {
    return '#14b8a6' // teal-500
  }
  if (event.type === 'EVENT') {
    // 캘린더 색상이 있고 업무 기본 색상(파란색)과 다르면 캘린더 색상 사용
    if (event.calendarColor && event.calendarColor !== '#3b82f6') {
      return event.calendarColor
    }
    return '#a855f7' // purple-500 (이벤트 전용 색상)
  }
  return '#3b82f6' // blue-500 (업무)
}

// 반복 이벤트 여부 확인
function isRecurringEvent(event: CalendarEvent): boolean {
  return event.isRecurring === true
}

// 음력 반복 여부 확인
function isLunarRecurrence(event: CalendarEvent): boolean {
  return event.recurrenceType?.endsWith('_LUNAR') === true
}

// 이벤트 표시 제목 (음력 반복인 경우 접미사 추가)
function getEventDisplayTitle(event: CalendarEvent): string {
  if (isLunarRecurrence(event)) {
    return `${event.title} (음력)`
  }
  return event.title
}

// 이벤트 클릭
function handleEventClick(event: CalendarEvent) {
  emit('eventClick', event)
}

// 셀 클릭
function handleCellClick(hour: number, minute: number) {
  emit('dateClick', currentDateStr.value, hour)
}

// 현재 시간 위치 계산 (%)
const currentTimePosition = computed(() => {
  if (!dayInfo.value.isToday) return -1
  const totalMinutes = currentHour.value * 60 + currentMinute.value
  return (totalMinutes / (24 * 60)) * 100
})
</script>

<template>
  <div class="day-view bg-white rounded-lg border border-gray-200 overflow-hidden dark:bg-gray-800 dark:border-gray-700">
    <!-- 날짜 헤더 -->
    <div class="sticky top-0 bg-white dark:bg-gray-800 z-10 border-b border-gray-200 dark:border-gray-700">
      <div class="p-4 text-center">
        <div
          class="text-2xl font-bold"
          :class="{
            'text-primary-600 dark:text-primary-400': dayInfo.isToday,
            'text-red-500': !dayInfo.isToday && (dayInfo.weekDay === 0 || calendarDateInfo?.isHoliday),
            'text-blue-500': !dayInfo.isToday && dayInfo.weekDay === 6 && !calendarDateInfo?.isHoliday,
            'text-gray-900 dark:text-gray-100': !dayInfo.isToday && dayInfo.weekDay !== 0 && dayInfo.weekDay !== 6 && !calendarDateInfo?.isHoliday
          }"
        >
          {{ dayInfo.month }}월 {{ dayInfo.day }}일
          <span class="text-lg font-normal">
            ({{ weekDayNames[dayInfo.weekDay] }})
          </span>
        </div>

        <!-- 음력 & 공휴일 -->
        <div class="mt-1 flex items-center justify-center gap-3 text-sm">
          <span v-if="calendarDateInfo?.lunarDisplay" class="text-gray-500 dark:text-gray-400">
            음력 {{ calendarDateInfo.lunarDisplay }}
          </span>
          <span v-if="calendarDateInfo?.holidayName" class="text-red-500 font-medium">
            {{ calendarDateInfo.holidayName }}
          </span>
        </div>

        <div v-if="dayInfo.isToday" class="mt-1 text-xs text-primary-500">
          오늘
        </div>
      </div>

      <!-- 종일 이벤트 -->
      <div v-if="allDayEvents.length > 0" class="px-4 pb-2">
        <div class="text-xs text-gray-500 dark:text-gray-400 mb-1">종일 이벤트</div>
        <div class="flex flex-wrap gap-1">
          <div
            v-for="event in allDayEvents"
            :key="`${event.type}-${event.id}`"
            class="px-2 py-1 rounded text-xs text-white cursor-pointer hover:opacity-80"
            :style="{ backgroundColor: getEventColor(event as any), ...(isRecurringEvent(event) ? { border: '1px dashed rgba(255,255,255,0.6)' } : {}) }"
            @click="handleEventClick(event)"
          >
            {{ getEventDisplayTitle(event) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 시간대별 그리드 -->
    <div class="relative max-h-[600px] overflow-y-auto">
      <!-- 현재 시간 라인 -->
      <div
        v-if="currentTimePosition >= 0"
        class="absolute left-0 right-0 z-10 pointer-events-none"
        :style="{ top: `${currentTimePosition}%` }"
      >
        <div class="flex items-center">
          <div class="w-12 text-right pr-1">
            <span class="text-[10px] text-red-500 font-medium">
              {{ formatTime(currentHour, currentMinute) }}
            </span>
          </div>
          <div class="flex-1 h-0.5 bg-red-500 relative">
            <div class="absolute -left-1 -top-1 w-2.5 h-2.5 bg-red-500 rounded-full" />
          </div>
        </div>
      </div>

      <!-- 시간 슬롯 -->
      <div
        v-for="slot in timeSlots"
        :key="`${slot.hour}-${slot.minute}`"
        class="flex border-b border-gray-100 dark:border-gray-700/50"
        :class="{
          'bg-yellow-50/30 dark:bg-yellow-900/5': slot.hour === currentHour && dayInfo.isToday,
          'border-gray-200 dark:border-gray-700': slot.minute === 0
        }"
      >
        <!-- 시간 레이블 -->
        <div class="w-16 flex-shrink-0 text-right pr-2 py-2 text-xs text-gray-400 dark:text-gray-500">
          {{ slot.label }}
        </div>

        <!-- 이벤트 영역 -->
        <div
          class="flex-1 min-h-[30px] p-1 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/30"
          @dblclick="handleCellClick(slot.hour, slot.minute)"
        >
          <div
            v-for="event in getEventsForSlot(slot.hour, slot.minute)"
            :key="`${event.type}-${event.id}`"
            class="mb-1 p-2 rounded text-sm text-white cursor-pointer hover:opacity-80"
            :style="{ backgroundColor: getEventColor(event as any), ...(isRecurringEvent(event) ? { border: '1px dashed rgba(255,255,255,0.6)' } : {}) }"
            @click.stop="handleEventClick(event)"
          >
            <div class="font-medium">{{ getEventDisplayTitle(event) }}</div>
            <div v-if="(event as any).startTime" class="text-xs opacity-80">
              {{ (event as any).startTime?.substring(0, 5) }}
              <span v-if="(event as any).endTime"> - {{ (event as any).endTime?.substring(0, 5) }}</span>
            </div>
            <div v-if="(event as any).location" class="text-xs opacity-80 mt-0.5">
              📍 {{ (event as any).location }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.day-view {
  font-variant-numeric: tabular-nums;
}
</style>
