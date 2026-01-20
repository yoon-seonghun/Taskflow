<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { calendarApi, type EventDetailResponse, type UserCalendarResponse } from '@/api/calendar'
import { useUiStore } from '@/stores/ui'
import { useGroupStore } from '@/stores/group'
import type { CalendarResponse, CalendarEvent, CalendarViewType, CalendarDateInfo } from '@/types/calendar'
import Spinner from '@/components/common/Spinner.vue'
import CalendarCellItem from '@/components/calendar/CalendarCellItem.vue'
import CalendarTooltip from '@/components/calendar/CalendarTooltip.vue'
import EventFormModal from '@/components/calendar/EventFormModal.vue'
import EventShareModal from '@/components/calendar/EventShareModal.vue'
import EventTransferModal from '@/components/calendar/EventTransferModal.vue'
import WeekView from '@/components/calendar/WeekView.vue'
import DayView from '@/components/calendar/DayView.vue'
import CalendarFilterPanel from '@/components/calendar/CalendarFilterPanel.vue'

const uiStore = useUiStore()
const groupStore = useGroupStore()

// 상태
const loading = ref(false)
const error = ref<string | null>(null)
const calendarData = ref<CalendarResponse | null>(null)

// 현재 연/월
const currentDate = ref(new Date())
const currentYear = computed(() => currentDate.value.getFullYear())
const currentMonth = computed(() => currentDate.value.getMonth() + 1)

// 뷰 타입
const viewType = ref<CalendarViewType>('month')

// 필터
const includeTodo = ref(true)
const includeItem = ref(true)
const includeEvent = ref(true)

// 전체보기 모드
const showAllEvents = ref(false)
const selectedCalendars = ref<number[]>([])
const selectedPriorities = ref<string[]>(['URGENT', 'HIGH', 'NORMAL', 'LOW'])

// 캘린더 목록 (헤더 드롭다운용)
const calendarList = ref<UserCalendarResponse[]>([])
const showCalendarDropdown = ref(false)

// 그룹 필터 (Todo/Item/Event 공통)
const selectedGroupFilter = ref<number | null>(null)
const showGroupDropdown = ref(false)

// 필터 패널 표시
const showFilterPanel = ref(false)

// 년/월 빠른 선택 상태
const showDatePicker = ref(false)
const pickerYear = ref(new Date().getFullYear())
const pickerMode = ref<'year' | 'month'>('month')

// 년도 범위 (현재 년도 기준 ±10년)
const yearRange = computed(() => {
  const years: number[] = []
  const baseYear = pickerYear.value
  const startYear = Math.floor(baseYear / 10) * 10 - 1
  for (let i = 0; i < 12; i++) {
    years.push(startYear + i)
  }
  return years
})

// 이벤트 모달 상태
const eventFormVisible = ref(false)
const eventShareVisible = ref(false)
const eventTransferVisible = ref(false)
const selectedEvent = ref<EventDetailResponse | null>(null)
const selectedDateForNewEvent = ref<string>('')

// 사용자 이벤트 데이터
const userEvents = ref<EventDetailResponse[]>([])

// 툴팁 상태
const tooltipVisible = ref(false)
const tooltipDate = ref('')
const tooltipAnchorRect = ref<DOMRect | null>(null)

// 오늘 날짜
const today = new Date()
const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

// 월 이름
const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']

// 요일 이름
const weekDays = ['일', '월', '화', '수', '목', '금', '토']

// 캘린더 날짜 배열 계산
const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value

  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)

  const startDay = firstDay.getDay()
  const daysInMonth = lastDay.getDate()

  const days: { date: string; day: number; isCurrentMonth: boolean; isToday: boolean }[] = []

  // 이전 달 날짜
  const prevMonthNum = month === 1 ? 12 : month - 1
  const prevYear = month === 1 ? year - 1 : year
  const prevMonthLastDay = new Date(prevYear, prevMonthNum, 0).getDate()

  for (let i = startDay - 1; i >= 0; i--) {
    const day = prevMonthLastDay - i
    const dateStr = `${prevYear}-${String(prevMonthNum).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    days.push({ date: dateStr, day, isCurrentMonth: false, isToday: dateStr === todayStr })
  }

  // 현재 달 날짜
  for (let i = 1; i <= daysInMonth; i++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    days.push({ date: dateStr, day: i, isCurrentMonth: true, isToday: dateStr === todayStr })
  }

  // 다음 달 날짜 (6주 채우기)
  const remaining = 42 - days.length
  const nextMonthNum = month === 12 ? 1 : month + 1
  const nextYear = month === 12 ? year + 1 : year

  for (let i = 1; i <= remaining; i++) {
    const dateStr = `${nextYear}-${String(nextMonthNum).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    days.push({ date: dateStr, day: i, isCurrentMonth: false, isToday: dateStr === todayStr })
  }

  return days
})

// 데이터 로드
async function fetchData() {
  loading.value = true
  error.value = null

  try {
    // 기존 캘린더 데이터 (Todo/Item)
    const response = await calendarApi.getCalendarData({
      year: currentYear.value,
      month: currentMonth.value,
      includeTodo: includeTodo.value,
      includeItem: includeItem.value
    })
    calendarData.value = response.data

    // 사용자 이벤트 로드
    if (includeEvent.value) {
      await fetchUserEvents()
    } else {
      userEvents.value = []
    }
  } catch (err: any) {
    error.value = err.message || '데이터를 불러오는 중 오류가 발생했습니다.'
    console.error('Calendar fetch error:', err)
  } finally {
    loading.value = false
  }
}

// 사용자 이벤트 로드
async function fetchUserEvents() {
  try {
    const year = currentYear.value
    const month = currentMonth.value
    const startDate = `${year}-${String(month).padStart(2, '0')}-01`
    const lastDay = new Date(year, month, 0).getDate()
    const endDate = `${year}-${String(month).padStart(2, '0')}-${lastDay}`

    const response = await calendarApi.getEvents({
      startDate: startDate,
      endDate: endDate,
      includeShared: true
    })
    userEvents.value = response.data || []
  } catch (err) {
    console.error('Failed to fetch user events:', err)
    userEvents.value = []
  }
}

// 캘린더 목록 로드 (헤더 드롭다운용)
async function fetchCalendarList() {
  try {
    const response = await calendarApi.getCalendars()
    calendarList.value = response.data || []

    // 처음 로드 시 모든 캘린더 선택
    if (selectedCalendars.value.length === 0 && calendarList.value.length > 0) {
      selectedCalendars.value = calendarList.value.map(c => c.calendarId)
    }
  } catch (err) {
    console.error('Failed to fetch calendar list:', err)
  }
}

// 캘린더 선택 토글 (이벤트 필터용)
function toggleCalendarSelection(calendarId: number) {
  const index = selectedCalendars.value.indexOf(calendarId)
  if (index === -1) {
    selectedCalendars.value.push(calendarId)
  } else {
    selectedCalendars.value.splice(index, 1)
  }
}

// 모든 캘린더 선택
function selectAllCalendars() {
  selectedCalendars.value = calendarList.value.map(c => c.calendarId)
}

// 모든 캘린더 해제
function deselectAllCalendars() {
  selectedCalendars.value = []
}

// 캘린더 드롭다운 외부 클릭 닫기
function closeCalendarDropdown() {
  showCalendarDropdown.value = false
}

// 이전 기간
function prevPeriod() {
  const date = new Date(currentDate.value)
  if (viewType.value === 'month') {
    date.setMonth(date.getMonth() - 1)
  } else if (viewType.value === 'week') {
    date.setDate(date.getDate() - 7)
  } else {
    date.setDate(date.getDate() - 1)
  }
  currentDate.value = date
}

// 다음 기간
function nextPeriod() {
  const date = new Date(currentDate.value)
  if (viewType.value === 'month') {
    date.setMonth(date.getMonth() + 1)
  } else if (viewType.value === 'week') {
    date.setDate(date.getDate() + 7)
  } else {
    date.setDate(date.getDate() + 1)
  }
  currentDate.value = date
}

// 오늘로 이동
function goToToday() {
  currentDate.value = new Date()
}

// 년/월 선택기 열기
function openDatePicker() {
  pickerYear.value = currentYear.value
  pickerMode.value = 'month'
  showDatePicker.value = true
}

// 년/월 선택기 닫기
function closeDatePicker() {
  showDatePicker.value = false
}

// 년도 선택
function selectYear(year: number) {
  pickerYear.value = year
  pickerMode.value = 'month'
}

// 월 선택
function selectMonth(month: number) {
  const newDate = new Date(currentDate.value)
  newDate.setFullYear(pickerYear.value)
  newDate.setMonth(month - 1)
  newDate.setDate(1)
  currentDate.value = newDate
  closeDatePicker()
}

// 년도 범위 이동
function prevYearRange() {
  pickerYear.value -= 10
}

function nextYearRange() {
  pickerYear.value += 10
}

// 네비게이션 제목
const navTitle = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value

  if (viewType.value === 'month') {
    return `${year}년 ${monthNames[month - 1]}`
  } else if (viewType.value === 'week') {
    const date = new Date(currentDate.value)
    const dayOfWeek = date.getDay()
    const startDate = new Date(date)
    startDate.setDate(date.getDate() - dayOfWeek)
    const endDate = new Date(startDate)
    endDate.setDate(startDate.getDate() + 6)

    const startMonth = startDate.getMonth() + 1
    const endMonth = endDate.getMonth() + 1

    if (startMonth === endMonth) {
      return `${year}년 ${startMonth}월 ${startDate.getDate()}일 - ${endDate.getDate()}일`
    } else {
      return `${startMonth}월 ${startDate.getDate()}일 - ${endMonth}월 ${endDate.getDate()}일`
    }
  } else {
    const date = currentDate.value
    const weekDayNames = ['일', '월', '화', '수', '목', '금', '토']
    return `${year}년 ${month}월 ${date.getDate()}일 (${weekDayNames[date.getDay()]})`
  }
})

// 이전/다음 버튼 레이블
const prevLabel = computed(() => {
  if (viewType.value === 'month') return '이전 달'
  if (viewType.value === 'week') return '이전 주'
  return '이전 날'
})

const nextLabel = computed(() => {
  if (viewType.value === 'month') return '다음 달'
  if (viewType.value === 'week') return '다음 주'
  return '다음 날'
})

// 날짜별 이벤트 가져오기 (Todo/Item + 사용자 이벤트)
function getEventsForDate(dateStr: string): CalendarEvent[] {
  let events: CalendarEvent[] = []

  // 기존 Todo/Item 이벤트 (그룹 필터 적용)
  if (calendarData.value?.events) {
    const existing = calendarData.value.events[dateStr] || []
    events.push(...filterByGroup(existing))
  }

  // 사용자 이벤트 추가 (캘린더 필터 + 그룹 필터 적용)
  if (includeEvent.value && userEvents.value.length > 0) {
    const userEventsForDate = userEvents.value.filter(e => {
      // 시작일~종료일 범위에 있는지 확인
      const start = e.startDate
      const end = e.endDate || e.startDate
      if (!(dateStr >= start && dateStr <= end)) {
        return false
      }
      // 캘린더 필터 적용 (선택된 캘린더에 속한 이벤트만)
      if (selectedCalendars.value.length > 0) {
        if (!selectedCalendars.value.includes(e.calendarId)) {
          return false
        }
      }
      // 그룹 필터 적용
      if (selectedGroupFilter.value !== null) {
        if (selectedGroupFilter.value === 0) {
          if (e.groupId) return false
        } else {
          if (e.groupId !== selectedGroupFilter.value) return false
        }
      }
      return true
    })

    for (const ue of userEventsForDate) {
      events.push({
        type: 'EVENT',  // 사용자 이벤트는 EVENT 타입
        id: ue.eventId,
        title: ue.title,
        priority: 'NORMAL',
        status: 'IN_PROGRESS',
        date: ue.startDate,
        // 이벤트 식별을 위한 추가 정보
        isUserEvent: true,
        calendarColor: ue.calendarColor,
        calendarName: ue.calendarName,
        calendarId: ue.calendarId,
        // 반복 이벤트 정보
        isRecurring: ue.isRecurring,
        recurrenceType: ue.recurrenceType || undefined,
        // 그룹 관련 정보
        groupId: ue.groupId || undefined,
        groupName: ue.groupName || undefined,
        groupColor: ue.groupColor || undefined,
        ownerUserName: ue.ownerName || undefined,
        isGroupShared: ue.isGroupShared || undefined
      } as CalendarEvent & { isUserEvent?: boolean; calendarColor?: string; calendarName?: string; calendarId?: number })
    }
  }

  return events
}

// 날짜별 기준 정보 가져오기 (TB_CALENDAR_DATE)
function getDateInfo(dateStr: string): CalendarDateInfo | undefined {
  if (!calendarData.value?.dateInfo) return undefined
  return calendarData.value.dateInfo[dateStr]
}

// Todo 배경색 (teal/cyan 계열)
function getTodoColor(priority: string): string {
  const colors: Record<string, string> = {
    URGENT: 'bg-rose-400',
    HIGH: 'bg-amber-400',
    NORMAL: 'bg-teal-500',
    LOW: 'bg-slate-400'
  }
  return colors[priority] || 'bg-teal-500'
}

// Item 배경색 (blue/indigo 계열)
function getItemColor(priority: string): string {
  const colors: Record<string, string> = {
    URGENT: 'bg-red-500',
    HIGH: 'bg-orange-500',
    NORMAL: 'bg-blue-500',
    LOW: 'bg-gray-400'
  }
  return colors[priority] || 'bg-blue-500'
}

// 이벤트 더블클릭 핸들러
function handleEventDoubleClick(event: CalendarEvent & { isUserEvent?: boolean }) {
  // 사용자 이벤트인 경우 편집 모달
  if (event.isUserEvent) {
    openEventForEdit(event.id)
    return
  }

  // ITEM 타입만 상세 패널 표시
  if (event.type === 'ITEM' && event.boardId) {
    uiStore.openSlideOver('ItemDetailPanel', {
      itemId: event.id,
      boardId: event.boardId
    }, () => {
      // 아이템 업데이트 시 리로드
      fetchData()
    })
  }
}

// 이벤트 편집 열기
async function openEventForEdit(eventId: number) {
  try {
    const response = await calendarApi.getEvent(eventId)
    selectedEvent.value = response.data
    eventFormVisible.value = true
  } catch (err) {
    console.error('Failed to load event:', err)
  }
}

// 새 이벤트 추가
function handleAddEvent(date?: string) {
  selectedEvent.value = null
  selectedDateForNewEvent.value = date || todayStr
  eventFormVisible.value = true
}

// 날짜 셀 클릭 (빈 영역)
function handleDateCellClick(dateStr: string) {
  handleAddEvent(dateStr)
}

// 이벤트 저장 완료
function handleEventSaved() {
  fetchData()
}

// 이벤트 삭제 완료
function handleEventDeleted() {
  fetchData()
}

// 이벤트 공유 열기
function openEventShare(event: EventDetailResponse) {
  selectedEvent.value = event
  eventShareVisible.value = true
}

// 이벤트 이관 열기
function openEventTransfer(event: EventDetailResponse) {
  selectedEvent.value = event
  eventTransferVisible.value = true
}

// 주간/일간 뷰에서 이벤트 클릭
function handleViewEventClick(event: CalendarEvent) {
  handleEventDoubleClick(event as any)
}

// 주간/일간 뷰에서 날짜/시간 클릭
function handleViewDateClick(dateStr: string, hour?: number) {
  selectedDateForNewEvent.value = dateStr
  selectedEvent.value = null
  eventFormVisible.value = true
}

// 필터 패널 토글
function toggleFilterPanel() {
  showFilterPanel.value = !showFilterPanel.value
}

// 필터 변경 핸들러
function handleFilterChange() {
  fetchData()
}

// events 객체 (주간/일간 뷰용)
const eventsMap = computed(() => {
  return calendarData.value?.events || {}
})

// dateInfo 객체 (주간/일간 뷰용)
const dateInfoMap = computed(() => {
  return calendarData.value?.dateInfo || {}
})

// 필터링된 사용자 이벤트 (주간/일간 뷰용)
const filteredUserEvents = computed(() => {
  let filtered = userEvents.value

  // 캘린더 필터 적용
  if (selectedCalendars.value.length === 0 && calendarList.value.length > 0) {
    // 캘린더가 있는데 아무것도 선택 안됐으면 빈 배열
    return []
  }
  if (selectedCalendars.value.length > 0) {
    filtered = filtered.filter(e => selectedCalendars.value.includes(e.calendarId))
  }

  // 그룹 필터 적용
  if (selectedGroupFilter.value !== null) {
    if (selectedGroupFilter.value === 0) {
      filtered = filtered.filter(e => !e.groupId)
    } else {
      filtered = filtered.filter(e => e.groupId === selectedGroupFilter.value)
    }
  }

  return filtered
})

// 더보기 클릭 핸들러 (전체 이벤트 툴팁 표시)
function handleShowMore(dateStr: string, event: MouseEvent) {
  const target = event.currentTarget as HTMLElement
  if (target) {
    tooltipAnchorRect.value = target.getBoundingClientRect()
  }
  tooltipDate.value = dateStr
  tooltipVisible.value = true
}

// 툴팁 닫기
function closeTooltip() {
  tooltipVisible.value = false
}

// 툴팁에서 이벤트 선택
function handleTooltipSelect(event: CalendarEvent) {
  closeTooltip()
  handleEventDoubleClick(event)
}

// 그룹 필터 옵션
const groupFilterOptions = computed(() => [
  { value: null, label: '전체', color: null },
  { value: 0, label: '그룹 없음', color: null },
  ...groupStore.groups.map(g => ({
    value: g.groupId,
    label: g.groupName,
    color: g.groupColor
  }))
])

// 그룹 드롭다운 닫기
function closeGroupDropdown() {
  showGroupDropdown.value = false
}

// 그룹 필터 적용 함수
function filterByGroup<T extends { groupId?: number | null }>(items: T[]): T[] {
  if (selectedGroupFilter.value === null) return items
  if (selectedGroupFilter.value === 0) {
    return items.filter(item => !item.groupId)
  }
  return items.filter(item => item.groupId === selectedGroupFilter.value)
}

// 초기 로드
onMounted(() => {
  fetchCalendarList()
  groupStore.fetchGroups()
  fetchData()
})

// 연/월 변경 시 재로드
watch([currentYear, currentMonth], () => {
  fetchData()
})

// Todo/Item/Event 필터 변경 시 재로드
watch([includeTodo, includeItem, includeEvent], () => {
  fetchData()
})
</script>

<template>
  <div class="p-4 md:p-6">
    <!-- 헤더 -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-bold text-gray-900 dark:text-gray-100">캘린더</h1>

      <!-- 뷰 타입 선택 & 필터 -->
      <div class="flex items-center gap-4">
        <!-- 이벤트 추가 버튼 -->
        <button
          class="flex items-center gap-1.5 px-3 py-1.5 bg-primary-600 hover:bg-primary-700 text-white text-sm rounded-lg transition-colors"
          @click="handleAddEvent()"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          이벤트
        </button>

        <!-- 필터 버튼 -->
        <button
          class="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-lg transition-colors"
          :class="showFilterPanel
            ? 'bg-primary-100 text-primary-700 dark:bg-primary-900/50 dark:text-primary-300'
            : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600'"
          @click="toggleFilterPanel"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
          </svg>
          필터
        </button>

        <!-- Todo/Item/Event 필터 (간략) -->
        <div class="hidden md:flex items-center gap-3 text-sm">
          <label class="flex items-center gap-1.5 cursor-pointer select-none">
            <input
              v-model="includeTodo"
              type="checkbox"
              class="w-4 h-4 rounded border-gray-300 text-teal-600 focus:ring-teal-500 dark:border-gray-600 dark:bg-gray-700 accent-teal-500"
            />
            <span class="flex items-center gap-1">
              <span class="w-2.5 h-2.5 rounded-full bg-teal-500"></span>
              <span class="text-gray-700 dark:text-gray-300">Todo</span>
            </span>
          </label>
          <label class="flex items-center gap-1.5 cursor-pointer select-none">
            <input
              v-model="includeItem"
              type="checkbox"
              class="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 accent-blue-500"
            />
            <span class="flex items-center gap-1">
              <span class="w-2.5 h-2.5 rounded-sm bg-blue-500"></span>
              <span class="text-gray-700 dark:text-gray-300">업무</span>
            </span>
          </label>
          <label class="flex items-center gap-1.5 cursor-pointer select-none">
            <input
              v-model="includeEvent"
              type="checkbox"
              class="w-4 h-4 rounded border-gray-300 text-purple-600 focus:ring-purple-500 dark:border-gray-600 dark:bg-gray-700 accent-purple-500"
            />
            <span class="flex items-center gap-1">
              <span class="w-2.5 h-2.5 rotate-45 bg-purple-500"></span>
              <span class="text-gray-700 dark:text-gray-300">이벤트</span>
            </span>
          </label>

          <!-- 구분선 -->
          <div class="w-px h-4 bg-gray-300 dark:bg-gray-600"></div>

          <!-- 캘린더 선택 드롭다운 -->
          <div class="relative">
            <button
              class="flex items-center gap-1.5 px-2 py-1 text-sm rounded-lg transition-colors"
              :class="showCalendarDropdown
                ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/50 dark:text-purple-300'
                : 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700'"
              @click="showCalendarDropdown = !showCalendarDropdown"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <span>캘린더</span>
              <span
                v-if="selectedCalendars.length < calendarList.length && calendarList.length > 0"
                class="px-1.5 py-0.5 text-[10px] bg-purple-500 text-white rounded-full"
              >
                {{ selectedCalendars.length }}
              </span>
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            <!-- 드롭다운 메뉴 -->
            <div
              v-if="showCalendarDropdown"
              class="absolute top-full right-0 mt-1 w-56 bg-white rounded-lg shadow-lg border border-gray-200 dark:bg-gray-800 dark:border-gray-700 z-50"
            >
              <!-- 헤더 -->
              <div class="flex items-center justify-between px-3 py-2 border-b border-gray-100 dark:border-gray-700">
                <span class="text-xs font-medium text-gray-500 dark:text-gray-400">이벤트 캘린더 필터</span>
                <button
                  class="text-[10px] text-primary-600 hover:text-primary-700 dark:text-primary-400"
                  @click="selectedCalendars.length < calendarList.length ? selectAllCalendars() : deselectAllCalendars()"
                >
                  {{ selectedCalendars.length < calendarList.length ? '전체 선택' : '전체 해제' }}
                </button>
              </div>

              <!-- 캘린더 목록 -->
              <div class="max-h-60 overflow-y-auto py-1">
                <label
                  v-for="cal in calendarList"
                  :key="cal.calendarId"
                  class="flex items-center gap-2 px-3 py-1.5 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50"
                >
                  <input
                    type="checkbox"
                    :checked="selectedCalendars.includes(cal.calendarId)"
                    class="w-3.5 h-3.5 rounded border-gray-300 dark:border-gray-600 dark:bg-gray-700"
                    :style="{ accentColor: cal.color || '#a855f7' }"
                    @change="toggleCalendarSelection(cal.calendarId)"
                  />
                  <span
                    class="w-2.5 h-2.5 rounded-sm flex-shrink-0"
                    :style="{ backgroundColor: cal.color || '#a855f7' }"
                  />
                  <span class="text-xs text-gray-700 dark:text-gray-300 truncate flex-1">
                    {{ cal.calendarName }}
                  </span>
                  <span
                    v-if="cal.isShared"
                    class="text-[10px] text-gray-400 dark:text-gray-500"
                  >
                    공유
                  </span>
                </label>

                <!-- 캘린더 없음 -->
                <div
                  v-if="calendarList.length === 0"
                  class="px-3 py-4 text-center text-xs text-gray-400 dark:text-gray-500"
                >
                  등록된 캘린더가 없습니다
                </div>
              </div>

              <!-- 푸터 안내 -->
              <div class="px-3 py-2 border-t border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50 rounded-b-lg">
                <p class="text-[10px] text-gray-400 dark:text-gray-500">
                  * 이벤트에만 적용 (Todo/업무 제외)
                </p>
              </div>
            </div>

            <!-- 배경 클릭 닫기 -->
            <div
              v-if="showCalendarDropdown"
              class="fixed inset-0 z-40"
              @click="closeCalendarDropdown"
            />
          </div>

          <!-- 그룹 필터 드롭다운 -->
          <div class="relative">
            <button
              class="flex items-center gap-1.5 px-2 py-1 text-sm rounded-lg transition-colors"
              :class="showGroupDropdown
                ? 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/50 dark:text-indigo-300'
                : 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700'"
              @click="showGroupDropdown = !showGroupDropdown"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              <span>그룹</span>
              <span
                v-if="selectedGroupFilter !== null"
                class="px-1.5 py-0.5 text-[10px] bg-indigo-500 text-white rounded-full"
              >
                {{ selectedGroupFilter === 0 ? '없음' : groupStore.groups.find(g => g.groupId === selectedGroupFilter)?.groupName?.slice(0, 4) || '선택' }}
              </span>
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            <!-- 그룹 드롭다운 메뉴 -->
            <div
              v-if="showGroupDropdown"
              class="absolute top-full right-0 mt-1 w-56 bg-white rounded-lg shadow-lg border border-gray-200 dark:bg-gray-800 dark:border-gray-700 z-50"
            >
              <!-- 헤더 -->
              <div class="flex items-center justify-between px-3 py-2 border-b border-gray-100 dark:border-gray-700">
                <span class="text-xs font-medium text-gray-500 dark:text-gray-400">그룹 필터</span>
                <button
                  v-if="selectedGroupFilter !== null"
                  class="text-[10px] text-primary-600 hover:text-primary-700 dark:text-primary-400"
                  @click="selectedGroupFilter = null; showGroupDropdown = false"
                >
                  필터 해제
                </button>
              </div>

              <!-- 그룹 목록 -->
              <div class="max-h-60 overflow-y-auto py-1">
                <button
                  v-for="option in groupFilterOptions"
                  :key="option.value ?? 'all'"
                  class="w-full flex items-center gap-2 px-3 py-1.5 text-left hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
                  :class="{ 'bg-indigo-50 dark:bg-indigo-900/30': selectedGroupFilter === option.value }"
                  @click="selectedGroupFilter = option.value; showGroupDropdown = false"
                >
                  <span
                    v-if="option.color"
                    class="w-2.5 h-2.5 rounded-full flex-shrink-0"
                    :style="{ backgroundColor: option.color }"
                  />
                  <span
                    v-else
                    class="w-2.5 h-2.5 rounded-full flex-shrink-0 bg-gray-300 dark:bg-gray-600"
                  />
                  <span class="text-xs text-gray-700 dark:text-gray-300 truncate flex-1">
                    {{ option.label }}
                  </span>
                  <svg
                    v-if="selectedGroupFilter === option.value"
                    class="w-4 h-4 text-indigo-600 dark:text-indigo-400"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                  </svg>
                </button>

                <!-- 그룹 없음 -->
                <div
                  v-if="groupStore.groups.length === 0"
                  class="px-3 py-4 text-center text-xs text-gray-400 dark:text-gray-500"
                >
                  등록된 그룹이 없습니다
                </div>
              </div>

              <!-- 푸터 안내 -->
              <div class="px-3 py-2 border-t border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50 rounded-b-lg">
                <p class="text-[10px] text-gray-400 dark:text-gray-500">
                  * Todo/업무/이벤트에 적용
                </p>
              </div>
            </div>

            <!-- 배경 클릭 닫기 -->
            <div
              v-if="showGroupDropdown"
              class="fixed inset-0 z-40"
              @click="closeGroupDropdown"
            />
          </div>

          <!-- 구분선 -->
          <div class="w-px h-4 bg-gray-300 dark:bg-gray-600"></div>

          <!-- 전체보기 토글 -->
          <label class="flex items-center gap-1.5 cursor-pointer select-none">
            <input
              v-model="showAllEvents"
              type="checkbox"
              class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500 dark:border-gray-600 dark:bg-gray-700 accent-primary-500"
            />
            <span class="text-gray-700 dark:text-gray-300">전체보기</span>
          </label>
        </div>

        <!-- 뷰 타입 -->
        <div class="flex border border-gray-200 rounded-lg overflow-hidden dark:border-gray-700">
          <button
            v-for="type in (['month', 'week', 'day'] as CalendarViewType[])"
            :key="type"
            class="px-3 py-1.5 text-sm transition-colors"
            :class="viewType === type
              ? 'bg-primary-600 text-white'
              : 'bg-white text-gray-700 hover:bg-gray-50 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700'"
            @click="viewType = type"
          >
            {{ type === 'month' ? '월간' : type === 'week' ? '주간' : '일간' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 네비게이션 -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <button
          class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
          :title="prevLabel"
          @click="prevPeriod"
        >
          <svg class="w-5 h-5 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>

        <!-- 년/월 선택 버튼 -->
        <div class="relative">
          <button
            class="flex items-center gap-1 px-3 py-1.5 text-lg font-semibold text-gray-900 dark:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors min-w-[180px] justify-center"
            @click="openDatePicker"
          >
            {{ navTitle }}
            <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          <!-- 년/월 선택 드롭다운 -->
          <div
            v-if="showDatePicker"
            class="absolute top-full left-1/2 -translate-x-1/2 mt-2 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 z-50 p-3 w-[280px]"
          >
            <!-- 년도 선택 모드 -->
            <template v-if="pickerMode === 'year'">
              <div class="flex items-center justify-between mb-3">
                <button
                  class="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                  @click="prevYearRange"
                >
                  <svg class="w-5 h-5 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                  </svg>
                </button>
                <span class="font-medium text-gray-900 dark:text-gray-100">
                  {{ yearRange[1] }} - {{ yearRange[10] }}
                </span>
                <button
                  class="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                  @click="nextYearRange"
                >
                  <svg class="w-5 h-5 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>
              <div class="grid grid-cols-3 gap-1">
                <button
                  v-for="year in yearRange"
                  :key="year"
                  class="py-2 text-sm rounded hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  :class="{
                    'bg-primary-100 text-primary-700 dark:bg-primary-900/50 dark:text-primary-400 font-medium': year === currentYear,
                    'text-gray-900 dark:text-gray-100': year !== currentYear
                  }"
                  @click="selectYear(year)"
                >
                  {{ year }}
                </button>
              </div>
            </template>

            <!-- 월 선택 모드 -->
            <template v-else>
              <div class="flex items-center justify-between mb-3">
                <button
                  class="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                  @click="pickerYear--"
                >
                  <svg class="w-5 h-5 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                  </svg>
                </button>
                <button
                  class="font-medium text-gray-900 dark:text-gray-100 hover:text-primary-600 dark:hover:text-primary-400"
                  @click="pickerMode = 'year'"
                >
                  {{ pickerYear }}년
                </button>
                <button
                  class="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                  @click="pickerYear++"
                >
                  <svg class="w-5 h-5 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>
              <div class="grid grid-cols-4 gap-1">
                <button
                  v-for="month in 12"
                  :key="month"
                  class="py-2 text-sm rounded hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  :class="{
                    'bg-primary-100 text-primary-700 dark:bg-primary-900/50 dark:text-primary-400 font-medium': pickerYear === currentYear && month === currentMonth,
                    'text-gray-900 dark:text-gray-100': !(pickerYear === currentYear && month === currentMonth)
                  }"
                  @click="selectMonth(month)"
                >
                  {{ month }}월
                </button>
              </div>
            </template>

            <!-- 닫기 버튼 -->
            <div class="mt-3 pt-2 border-t border-gray-200 dark:border-gray-700 flex justify-end">
              <button
                class="px-3 py-1 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100"
                @click="closeDatePicker"
              >
                닫기
              </button>
            </div>
          </div>

          <!-- 배경 클릭 시 닫기 -->
          <div
            v-if="showDatePicker"
            class="fixed inset-0 z-40"
            @click="closeDatePicker"
          />
        </div>

        <button
          class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
          :title="nextLabel"
          @click="nextPeriod"
        >
          <svg class="w-5 h-5 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </div>

      <button
        class="px-3 py-1.5 text-sm text-primary-600 hover:bg-primary-50 rounded-lg transition-colors dark:text-primary-400 dark:hover:bg-primary-900/30"
        @click="goToToday"
      >
        오늘
      </button>
    </div>

    <!-- 메인 레이아웃 (필터 패널 + 캘린더) -->
    <div class="flex gap-4">

      <!-- 필터 패널 (왼쪽) -->
      <div
        v-if="showFilterPanel"
        class="w-64 flex-shrink-0"
      >
        <CalendarFilterPanel
          v-model:includeTodo="includeTodo"
          v-model:includeItem="includeItem"
          v-model:includeEvent="includeEvent"
          v-model:selectedCalendars="selectedCalendars"
          v-model:selectedPriorities="selectedPriorities"
          @filterChange="handleFilterChange"
        />
      </div>

      <!-- 캘린더 영역 -->
      <div class="flex-1 min-w-0">
        <!-- 로딩 -->
        <div v-if="loading" class="flex items-center justify-center py-20">
          <Spinner size="lg" />
        </div>

        <!-- 에러 -->
        <div v-else-if="error" class="text-center py-20 text-red-500">
          {{ error }}
        </div>

        <!-- 월간 뷰 -->
        <div v-else-if="viewType === 'month'" class="bg-white rounded-lg border border-gray-200 overflow-hidden dark:bg-gray-800 dark:border-gray-700">
      <!-- 요일 헤더 -->
      <div class="grid grid-cols-7 border-b border-gray-200 dark:border-gray-700">
        <div
          v-for="(day, index) in weekDays"
          :key="day"
          class="p-2 text-center text-sm font-medium"
          :class="index === 0 ? 'text-red-500' : index === 6 ? 'text-blue-500' : 'text-gray-700 dark:text-gray-300'"
        >
          {{ day }}
        </div>
      </div>

      <!-- 날짜 그리드 -->
      <div class="grid grid-cols-7">
        <div
          v-for="(day, index) in calendarDays"
          :key="day.date"
          class="min-h-[100px] p-1 border-b border-r border-gray-100 dark:border-gray-700 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
          :class="{
            'bg-gray-50 dark:bg-gray-900/50': !day.isCurrentMonth,
            'bg-primary-50 dark:bg-primary-900/20 hover:bg-primary-100 dark:hover:bg-primary-900/30': day.isToday,
            'bg-red-50/50 dark:bg-red-900/10': day.isCurrentMonth && getDateInfo(day.date)?.isHoliday && !day.isToday
          }"
          @click="handleDateCellClick(day.date)"
        >
          <!-- 날짜 헤더 (날짜 + 음력) -->
          <div class="flex items-center gap-1 mb-1">
            <!-- 날짜 숫자 -->
            <span
              class="text-sm"
              :class="{
                'text-gray-400 dark:text-gray-600': !day.isCurrentMonth,
                'text-red-500 font-medium': day.isCurrentMonth && (index % 7 === 0 || getDateInfo(day.date)?.isHoliday),
                'text-blue-500': day.isCurrentMonth && index % 7 === 6 && !getDateInfo(day.date)?.isHoliday,
                'font-bold text-primary-600 dark:text-primary-400': day.isToday,
                'text-gray-900 dark:text-gray-100': day.isCurrentMonth && index % 7 !== 0 && index % 7 !== 6 && !day.isToday && !getDateInfo(day.date)?.isHoliday
              }"
            >
              {{ day.day }}
            </span>
            <!-- 음력 표시 -->
            <span
              v-if="getDateInfo(day.date)?.lunarDisplay && day.isCurrentMonth"
              class="text-[10px] text-gray-400 dark:text-gray-500"
            >
              ({{ getDateInfo(day.date)?.lunarDisplay }})
            </span>
          </div>
          <!-- 공휴일명 표시 -->
          <div
            v-if="getDateInfo(day.date)?.isHoliday && getDateInfo(day.date)?.holidayName && day.isCurrentMonth"
            class="text-[10px] text-red-500 font-medium truncate mb-0.5"
            :title="getDateInfo(day.date)?.holidayName"
          >
            {{ getDateInfo(day.date)?.holidayName }}
          </div>

          <!-- 이벤트 목록 -->
          <div class="space-y-0.5">
            <CalendarCellItem
              v-for="event in (showAllEvents ? getEventsForDate(day.date) : getEventsForDate(day.date).slice(0, 3))"
              :key="`${event.type}-${event.id}`"
              :event="event"
              :show-tooltip="true"
              @dblclick="handleEventDoubleClick(event)"
            />

            <!-- 더보기 표시 (전체보기 모드가 아닐 때만) -->
            <button
              v-if="!showAllEvents && getEventsForDate(day.date).length > 3"
              class="text-xs text-gray-500 px-1 hover:text-primary-600 hover:underline transition-colors dark:text-gray-400 dark:hover:text-primary-400"
              @click.stop="handleShowMore(day.date, $event)"
            >
              +{{ getEventsForDate(day.date).length - 3 }}개 더보기
            </button>
          </div>
        </div>
      </div>
        </div>

        <!-- 주간 뷰 -->
        <WeekView
          v-else-if="viewType === 'week'"
          :current-date="currentDate"
          :events="eventsMap"
          :user-events="filteredUserEvents"
          :date-info="dateInfoMap"
          :include-event="includeEvent"
          @event-click="handleViewEventClick"
          @date-click="handleViewDateClick"
        />

        <!-- 일간 뷰 -->
        <DayView
          v-else
          :current-date="currentDate"
          :events="eventsMap"
          :user-events="filteredUserEvents"
          :date-info="dateInfoMap"
          :include-event="includeEvent"
          @event-click="handleViewEventClick"
          @date-click="handleViewDateClick"
        />
      </div>
    </div>

    <!-- 범례 -->
    <div class="mt-4 flex flex-wrap items-center gap-4 md:gap-6 text-sm text-gray-600 dark:text-gray-400">
      <div class="flex items-center gap-2">
        <span class="w-3 h-3 rounded-full bg-teal-500" />
        <span>Todo</span>
      </div>
      <div class="flex items-center gap-2">
        <span class="w-3 h-3 rounded-sm bg-blue-500" />
        <span>업무</span>
      </div>
      <div class="flex items-center gap-2">
        <span class="w-3 h-3 rotate-45 bg-purple-500" />
        <span>이벤트</span>
      </div>
      <div class="flex items-center gap-2">
        <span class="text-red-500 font-medium text-xs">휴일</span>
        <span>공휴일</span>
      </div>
      <div class="flex items-center gap-2 text-xs text-gray-400 dark:text-gray-500">
        <span>(음력)</span>
        <span>음력 날짜</span>
      </div>
      <div class="flex items-center gap-2 text-xs text-amber-500 dark:text-amber-400">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
        <span>그룹 공유</span>
      </div>
      <div class="text-xs text-gray-400 dark:text-gray-500 ml-auto">
        * 빈 셀 더블클릭: 이벤트 추가 | 항목 더블클릭: 상세보기
      </div>
    </div>

    <!-- 전체 이벤트 툴팁 -->
    <CalendarTooltip
      :visible="tooltipVisible"
      :date="tooltipDate"
      :events="getEventsForDate(tooltipDate)"
      :date-info="getDateInfo(tooltipDate)"
      :anchor-rect="tooltipAnchorRect"
      @close="closeTooltip"
      @select="handleTooltipSelect"
    />

    <!-- 이벤트 폼 모달 -->
    <EventFormModal
      v-model:visible="eventFormVisible"
      :event="selectedEvent"
      :default-date="selectedDateForNewEvent"
      @saved="handleEventSaved"
      @deleted="handleEventDeleted"
      @share="openEventShare"
      @transfer="openEventTransfer"
    />

    <!-- 이벤트 공유 모달 -->
    <EventShareModal
      v-model:visible="eventShareVisible"
      :event="selectedEvent"
      @changed="fetchData"
    />

    <!-- 이벤트 이관 모달 -->
    <EventTransferModal
      v-model:visible="eventTransferVisible"
      :event="selectedEvent"
      @transferred="handleEventSaved"
    />

  </div>
</template>
