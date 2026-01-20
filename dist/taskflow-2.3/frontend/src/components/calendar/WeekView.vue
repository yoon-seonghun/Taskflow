<script setup lang="ts">
/**
 * 주간 뷰 컴포넌트
 * - 시간대별 이벤트 표시
 * - 드래그 앤 드롭 지원 (향후)
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
  (e: 'eventEdit', eventId: number): void
}>()

// 시간 배열 (0시 ~ 23시)
const hours = Array.from({ length: 24 }, (_, i) => i)

// 요일 이름
const weekDayNames = ['일', '월', '화', '수', '목', '금', '토']

// 오늘 날짜
const today = new Date()
const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

// 현재 시간
const currentHour = ref(today.getHours())

// 주간 날짜 배열 계산
const weekDays = computed(() => {
  const date = new Date(props.currentDate)
  const dayOfWeek = date.getDay()
  const startDate = new Date(date)
  startDate.setDate(date.getDate() - dayOfWeek)

  const days: { date: string; day: number; weekDay: number; isToday: boolean; month: number }[] = []

  for (let i = 0; i < 7; i++) {
    const d = new Date(startDate)
    d.setDate(startDate.getDate() + i)
    const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push({
      date: dateStr,
      day: d.getDate(),
      weekDay: d.getDay(),
      isToday: dateStr === todayStr,
      month: d.getMonth() + 1
    })
  }

  return days
})

// 주간 제목
const weekTitle = computed(() => {
  if (weekDays.value.length === 0) return ''
  const start = weekDays.value[0]
  const end = weekDays.value[6]

  if (start.month === end.month) {
    return `${start.month}월 ${start.day}일 - ${end.day}일`
  } else {
    return `${start.month}월 ${start.day}일 - ${end.month}월 ${end.day}일`
  }
})

// 날짜별 이벤트 가져오기
function getEventsForDate(dateStr: string): CalendarEvent[] {
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
        // 반복 이벤트 정보
        isRecurring: ue.isRecurring,
        recurrenceType: ue.recurrenceType || undefined
      } as CalendarEvent & { isUserEvent?: boolean; calendarColor?: string; startTime?: string; endTime?: string; isAllDay?: boolean })
    }
  }

  return events
}

// 종일 이벤트 가져오기
function getAllDayEvents(dateStr: string): CalendarEvent[] {
  return getEventsForDate(dateStr).filter((e: any) => {
    // 사용자 이벤트가 종일 이벤트인 경우
    if (e.isUserEvent && e.isAllDay) return true
    // Todo/Item은 기본적으로 종일로 표시
    if (!e.isUserEvent) return true
    // 시간이 없는 이벤트도 종일로 표시
    if (!e.startTime) return true
    return false
  })
}

// 시간대별 이벤트 가져오기
function getHourEvents(dateStr: string, hour: number): CalendarEvent[] {
  return getEventsForDate(dateStr).filter((e: any) => {
    if (!e.isUserEvent || e.isAllDay || !e.startTime) return false
    const eventHour = parseInt(e.startTime.split(':')[0], 10)
    return eventHour === hour
  })
}

// 이벤트 색상 가져오기
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

// 시간 포맷
function formatHour(hour: number): string {
  if (hour === 0) return '오전 12시'
  if (hour < 12) return `오전 ${hour}시`
  if (hour === 12) return '오후 12시'
  return `오후 ${hour - 12}시`
}

// 날짜 정보 가져오기
function getDateInfoForDate(dateStr: string): CalendarDateInfo | undefined {
  return props.dateInfo?.[dateStr]
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

// 반복 이벤트 스타일 (점선 테두리)
function getRecurringStyle(event: CalendarEvent): string {
  if (!isRecurringEvent(event)) return ''
  return 'border: 1px dashed rgba(255,255,255,0.6);'
}

// 이벤트 클릭
function handleEventClick(event: CalendarEvent) {
  emit('eventClick', event)
}

// 날짜 셀 클릭
function handleCellClick(dateStr: string, hour?: number) {
  emit('dateClick', dateStr, hour)
}
</script>

<template>
  <div class="week-view bg-white rounded-lg border border-gray-200 overflow-hidden dark:bg-gray-800 dark:border-gray-700">
    <!-- 주간 헤더 -->
    <div class="sticky top-0 bg-white dark:bg-gray-800 z-10">
      <!-- 요일 헤더 -->
      <div class="grid grid-cols-8 border-b border-gray-200 dark:border-gray-700">
        <!-- 시간 레이블 영역 -->
        <div class="p-2 text-center text-xs text-gray-400 dark:text-gray-500 border-r border-gray-200 dark:border-gray-700">
          {{ weekTitle }}
        </div>

        <!-- 요일 -->
        <div
          v-for="day in weekDays"
          :key="day.date"
          class="p-2 text-center border-r border-gray-200 dark:border-gray-700 last:border-r-0"
          :class="{
            'bg-primary-50 dark:bg-primary-900/20': day.isToday
          }"
        >
          <div
            class="text-xs font-medium"
            :class="{
              'text-red-500': day.weekDay === 0 || getDateInfoForDate(day.date)?.isHoliday,
              'text-blue-500': day.weekDay === 6 && !getDateInfoForDate(day.date)?.isHoliday,
              'text-gray-600 dark:text-gray-400': day.weekDay !== 0 && day.weekDay !== 6 && !getDateInfoForDate(day.date)?.isHoliday
            }"
          >
            {{ weekDayNames[day.weekDay] }}
          </div>
          <div
            class="text-lg font-semibold"
            :class="{
              'text-primary-600 dark:text-primary-400': day.isToday,
              'text-red-500': !day.isToday && (day.weekDay === 0 || getDateInfoForDate(day.date)?.isHoliday),
              'text-blue-500': !day.isToday && day.weekDay === 6 && !getDateInfoForDate(day.date)?.isHoliday,
              'text-gray-900 dark:text-gray-100': !day.isToday && day.weekDay !== 0 && day.weekDay !== 6 && !getDateInfoForDate(day.date)?.isHoliday
            }"
          >
            {{ day.day }}
          </div>
          <!-- 음력 표시 -->
          <div
            v-if="getDateInfoForDate(day.date)?.lunarDisplay"
            class="text-[10px] text-gray-400 dark:text-gray-500"
          >
            {{ getDateInfoForDate(day.date)?.lunarDisplay }}
          </div>
          <!-- 공휴일 표시 -->
          <div
            v-if="getDateInfoForDate(day.date)?.holidayName"
            class="text-[10px] text-red-500 truncate"
          >
            {{ getDateInfoForDate(day.date)?.holidayName }}
          </div>
        </div>
      </div>

      <!-- 종일 이벤트 영역 -->
      <div class="grid grid-cols-8 border-b border-gray-200 dark:border-gray-700 min-h-[40px]">
        <div class="p-1 text-[10px] text-gray-400 border-r border-gray-200 dark:border-gray-700 flex items-center justify-center dark:text-gray-500">
          종일
        </div>
        <div
          v-for="day in weekDays"
          :key="`allday-${day.date}`"
          class="p-1 border-r border-gray-200 dark:border-gray-700 last:border-r-0 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50"
          :class="{
            'bg-primary-50/50 dark:bg-primary-900/10': day.isToday
          }"
          @dblclick="handleCellClick(day.date)"
        >
          <div class="space-y-0.5">
            <div
              v-for="event in getAllDayEvents(day.date).slice(0, 2)"
              :key="`${event.type}-${event.id}`"
              class="text-[10px] px-1 py-0.5 rounded truncate text-white cursor-pointer hover:opacity-80"
              :style="{ backgroundColor: getEventColor(event as any), ...(isRecurringEvent(event) ? { border: '1px dashed rgba(255,255,255,0.6)' } : {}) }"
              @click.stop="handleEventClick(event)"
            >
              {{ getEventDisplayTitle(event) }}
            </div>
            <div
              v-if="getAllDayEvents(day.date).length > 2"
              class="text-[10px] text-gray-500 dark:text-gray-400"
            >
              +{{ getAllDayEvents(day.date).length - 2 }}개
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 시간대별 그리드 -->
    <div class="max-h-[600px] overflow-y-auto">
      <div
        v-for="hour in hours"
        :key="hour"
        class="grid grid-cols-8 border-b border-gray-100 dark:border-gray-700/50"
        :class="{
          'bg-yellow-50/30 dark:bg-yellow-900/5': hour === currentHour
        }"
      >
        <!-- 시간 레이블 -->
        <div class="p-1 text-[10px] text-gray-400 border-r border-gray-200 dark:border-gray-700 text-right pr-2 dark:text-gray-500">
          {{ formatHour(hour) }}
        </div>

        <!-- 각 요일 셀 -->
        <div
          v-for="day in weekDays"
          :key="`${day.date}-${hour}`"
          class="min-h-[40px] p-0.5 border-r border-gray-100 dark:border-gray-700/50 last:border-r-0 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/30"
          :class="{
            'bg-primary-50/30 dark:bg-primary-900/10': day.isToday
          }"
          @dblclick="handleCellClick(day.date, hour)"
        >
          <div
            v-for="event in getHourEvents(day.date, hour)"
            :key="`${event.type}-${event.id}`"
            class="text-[10px] px-1 py-0.5 rounded truncate text-white cursor-pointer hover:opacity-80 mb-0.5"
            :style="{ backgroundColor: getEventColor(event as any), ...(isRecurringEvent(event) ? { border: '1px dashed rgba(255,255,255,0.6)' } : {}) }"
            @click.stop="handleEventClick(event)"
          >
            {{ (event as any).startTime?.substring(0, 5) }} {{ getEventDisplayTitle(event) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.week-view {
  font-variant-numeric: tabular-nums;
}
</style>
