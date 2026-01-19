<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { calendarApi } from '@/api/calendar'
import { boardApi } from '@/api/board'
import { useUiStore } from '@/stores/ui'
import type { GanttResponse, GanttItem, GanttRangeType, GanttDateUnit } from '@/types/calendar'
import type { Board } from '@/types/board'
import Spinner from '@/components/common/Spinner.vue'

const router = useRouter()
const uiStore = useUiStore()

// 상태
const loading = ref(false)
const error = ref<string | null>(null)
const ganttData = ref<GanttResponse | null>(null)
const boards = ref<Board[]>([])

// 필터
const selectedBoardId = ref<number | null>(null)
const rangeType = ref<GanttRangeType>('ALL')  // 기본값: 전체
const dateUnit = ref<GanttDateUnit>('DAY')
const includeCompleted = ref(false)
const customStartDate = ref('')
const customEndDate = ref('')


// 툴팁 상태
const tooltipItem = ref<GanttItem | null>(null)
const tooltipPosition = ref({ x: 0, y: 0 })
const tooltipVisible = ref(false)
let tooltipTimeout: ReturnType<typeof setTimeout> | null = null

// 오늘 날짜
const today = new Date()
const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

// 계층 구조로 정렬된 아이템 목록
const sortedItems = computed(() => {
  if (!ganttData.value?.items) return []

  const items = ganttData.value.items
  const itemMap = new Map<number, GanttItem>()
  const childrenMap = new Map<number, GanttItem[]>()
  const rootItems: GanttItem[] = []

  // 맵 구성
  items.forEach(item => {
    itemMap.set(item.itemId, item)
    if (item.parentItemId) {
      const children = childrenMap.get(item.parentItemId) || []
      children.push(item)
      childrenMap.set(item.parentItemId, children)
    } else {
      rootItems.push(item)
    }
  })

  // 재귀적으로 트리 순회하여 평탄화
  const result: GanttItem[] = []

  function traverse(item: GanttItem) {
    result.push(item)
    const children = childrenMap.get(item.itemId) || []
    children.forEach(child => traverse(child))
  }

  rootItems.forEach(item => traverse(item))

  return result
})

// 날짜 범위 계산
const dateRange = computed(() => {
  if (!ganttData.value?.dateRange) return { start: '', end: '' }
  return ganttData.value.dateRange
})

// 주차/월 계산 함수
function getWeekNumber(dateStr: string): number {
  const date = new Date(dateStr)
  const todayDate = new Date(todayStr)
  const diffTime = date.getTime() - todayDate.getTime()
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  return Math.floor(diffDays / 7)
}

function getMonthDiff(dateStr: string): number {
  const date = new Date(dateStr)
  const todayDate = new Date(todayStr)
  return (date.getFullYear() - todayDate.getFullYear()) * 12 + (date.getMonth() - todayDate.getMonth())
}

// 날짜 레이블 생성
function getDateLabel(dateStr: string, isToday: boolean): string {
  const date = new Date(dateStr)

  if (dateUnit.value === 'DAY') {
    return `${date.getMonth() + 1}/${date.getDate()}`
  } else if (dateUnit.value === 'WEEK') {
    if (isToday) return 'W0'
    const weekNum = getWeekNumber(dateStr)
    return weekNum >= 0 ? `W+${weekNum}` : `W${weekNum}`
  } else if (dateUnit.value === 'MONTH') {
    if (isToday) return 'M0'
    const monthDiff = getMonthDiff(dateStr)
    return monthDiff >= 0 ? `M+${monthDiff}` : `M${monthDiff}`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 날짜 배열 생성 (단위에 따라 그룹화)
const dates = computed(() => {
  if (!dateRange.value.start || !dateRange.value.end) return []

  const result: { date: string; label: string; isToday: boolean; isWeekend: boolean; isFirst: boolean }[] = []
  const start = new Date(dateRange.value.start)
  const end = new Date(dateRange.value.end)

  const current = new Date(start)
  let lastWeek = -999
  let lastMonth = -999

  while (current <= end) {
    const dateStr = current.toISOString().split('T')[0]
    const dayOfWeek = current.getDay()
    const isToday = dateStr === todayStr

    let isFirst = false
    if (dateUnit.value === 'WEEK') {
      const weekNum = getWeekNumber(dateStr)
      if (weekNum !== lastWeek) {
        isFirst = true
        lastWeek = weekNum
      }
    } else if (dateUnit.value === 'MONTH') {
      const monthDiff = getMonthDiff(dateStr)
      if (monthDiff !== lastMonth) {
        isFirst = true
        lastMonth = monthDiff
      }
    } else {
      isFirst = true
    }

    result.push({
      date: dateStr,
      label: isFirst ? getDateLabel(dateStr, isToday) : '',
      isToday,
      isWeekend: dayOfWeek === 0 || dayOfWeek === 6,
      isFirst
    })

    current.setDate(current.getDate() + 1)
  }

  return result
})

// 타임라인 너비 (픽셀) - 일 단위 기준
const dayWidth = computed(() => {
  switch (dateUnit.value) {
    case 'DAY': return 40
    case 'WEEK': return 8  // 7일 = 56px
    case 'MONTH': return 3  // 30일 = 90px
    default: return 40
  }
})

// 주간/월간 모드에서 그룹화된 헤더 생성
const groupedHeaders = computed(() => {
  if (dateUnit.value === 'DAY') return []

  const groups: { label: string; width: number; isToday: boolean }[] = []
  let currentGroup: { label: string; count: number; isToday: boolean } | null = null
  let lastKey = ''

  dates.value.forEach(d => {
    const key = dateUnit.value === 'WEEK'
      ? `W${getWeekNumber(d.date)}`
      : `M${getMonthDiff(d.date)}`

    if (key !== lastKey) {
      if (currentGroup) {
        groups.push({
          label: currentGroup.label,
          width: currentGroup.count * dayWidth.value,
          isToday: currentGroup.isToday
        })
      }
      const weekNum = getWeekNumber(d.date)
      const monthDiff = getMonthDiff(d.date)
      let label = ''
      if (dateUnit.value === 'WEEK') {
        label = weekNum === 0 ? 'W0' : (weekNum > 0 ? `W+${weekNum}` : `W${weekNum}`)
      } else {
        label = monthDiff === 0 ? 'M0' : (monthDiff > 0 ? `M+${monthDiff}` : `M${monthDiff}`)
      }
      currentGroup = { label, count: 1, isToday: weekNum === 0 || monthDiff === 0 }
      lastKey = key
    } else if (currentGroup) {
      currentGroup.count++
      if (d.isToday) currentGroup.isToday = true
    }
  })

  if (currentGroup) {
    groups.push({
      label: currentGroup.label,
      width: currentGroup.count * dayWidth.value,
      isToday: currentGroup.isToday
    })
  }

  return groups
})

// 날짜를 픽셀 위치로 변환
function dateToPosition(dateStr: string | undefined): number {
  if (!dateStr || !dateRange.value.start) return 0

  const start = new Date(dateRange.value.start)
  const date = new Date(dateStr)
  const diffDays = Math.floor((date.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))

  return diffDays * dayWidth.value
}

// 바 너비 계산
function getBarWidth(startDate: string | undefined, endDate: string | undefined): number {
  if (!startDate) return dayWidth.value
  if (!endDate) return dayWidth.value

  const start = new Date(startDate)
  const end = new Date(endDate)
  const diffDays = Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1

  return Math.max(diffDays * dayWidth.value, dayWidth.value)
}

// 우선순위 색상
function getPriorityBorderColor(priority: string): string {
  const colors: Record<string, string> = {
    URGENT: 'border-red-500',
    HIGH: 'border-orange-500',
    NORMAL: 'border-blue-500',
    LOW: 'border-gray-400'
  }
  return colors[priority] || 'border-gray-400'
}

// 날짜 포맷
function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 데이터 로드
async function fetchData() {
  loading.value = true
  error.value = null

  try {
    const params: any = {
      range: rangeType.value,
      includeCompleted: includeCompleted.value
    }

    if (selectedBoardId.value) {
      params.boardId = selectedBoardId.value
    }

    if (rangeType.value === 'CUSTOM' && customStartDate.value && customEndDate.value) {
      params.startDate = customStartDate.value
      params.endDate = customEndDate.value
    }

    console.log('Gantt API params:', params)
    const response = await calendarApi.getGanttData(params)
    console.log('Gantt API response:', response)
    // API 응답에서 data 추출
    ganttData.value = response.data
  } catch (err: any) {
    error.value = err.message || '데이터를 불러오는 중 오류가 발생했습니다.'
    console.error('Gantt fetch error:', err)
  } finally {
    loading.value = false
  }
}

// 보드 목록 로드
async function fetchBoards() {
  try {
    const response = await boardApi.getBoards()
    boards.value = response.data || []
  } catch (err) {
    console.error('Boards fetch error:', err)
  }
}

// TODAY로 스크롤
function scrollToToday() {
  const todayLine = document.querySelector('.gantt-today-line')
  if (todayLine) {
    todayLine.scrollIntoView({ behavior: 'smooth', inline: 'center' })
  }
}

// 아이템 더블클릭 - 상세 패널 열기
function onItemDblClick(item: GanttItem) {
  uiStore.openSlideOver('ItemDetailPanel', {
    itemId: item.itemId,
    boardId: item.boardId
  }, () => {
    // 아이템 업데이트 후 리프레시
    fetchData()
  })
}

// 툴팁 표시
function showTooltip(item: GanttItem, event: MouseEvent) {
  if (tooltipTimeout) clearTimeout(tooltipTimeout)

  tooltipTimeout = setTimeout(() => {
    tooltipItem.value = item
    tooltipPosition.value = { x: event.clientX + 10, y: event.clientY + 10 }
    tooltipVisible.value = true
  }, 300)
}

// 툴팁 숨기기
function hideTooltip() {
  if (tooltipTimeout) {
    clearTimeout(tooltipTimeout)
    tooltipTimeout = null
  }
  tooltipVisible.value = false
  tooltipItem.value = null
}

// 초기 로드
onMounted(async () => {
  await fetchBoards()
  await fetchData()
})

// 필터 변경 시 재로드
watch([selectedBoardId, rangeType, includeCompleted], () => {
  console.log('Filter changed - boardId:', selectedBoardId.value, 'range:', rangeType.value)
  if (rangeType.value !== 'CUSTOM') {
    fetchData()
  }
})

// CUSTOM 범위 적용
function applyCustomRange() {
  if (customStartDate.value && customEndDate.value) {
    fetchData()
  }
}
</script>

<template>
  <div class="p-4 md:p-6 h-full flex flex-col">
    <!-- 헤더 -->
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-xl font-bold text-gray-900 dark:text-gray-100">Gantt 차트</h1>

      <!-- TODAY 버튼 -->
      <button
        class="px-3 py-1.5 text-sm text-primary-600 hover:bg-primary-50 rounded-lg transition-colors dark:text-primary-400 dark:hover:bg-primary-900/30"
        @click="scrollToToday"
      >
        TODAY
      </button>
    </div>

    <!-- 툴바 -->
    <div class="flex flex-wrap items-center gap-4 mb-4 p-3 bg-gray-50 rounded-lg dark:bg-gray-800">
      <!-- 보드 선택 -->
      <div class="flex items-center gap-2">
        <label class="text-sm text-gray-600 dark:text-gray-400">보드:</label>
        <select
          v-model="selectedBoardId"
          class="px-3 py-1.5 text-sm border border-gray-300 rounded-lg bg-white dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200"
        >
          <option :value="null">전체</option>
          <option v-for="board in boards" :key="board.boardId" :value="board.boardId">
            {{ board.boardName }}
          </option>
        </select>
      </div>

      <!-- 범위 선택 -->
      <div class="flex items-center gap-2">
        <label class="text-sm text-gray-600 dark:text-gray-400">범위:</label>
        <select
          v-model="rangeType"
          class="px-3 py-1.5 text-sm border border-gray-300 rounded-lg bg-white dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200"
        >
          <option value="FULL">전체</option>
          <option value="WEEK">주간</option>
          <option value="MONTH">월간</option>
          <option value="CUSTOM">사용자 지정</option>
        </select>
      </div>

      <!-- 사용자 지정 범위 -->
      <div v-if="rangeType === 'CUSTOM'" class="flex items-center gap-2">
        <input
          v-model="customStartDate"
          type="date"
          class="px-2 py-1.5 text-sm border border-gray-300 rounded-lg bg-white dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200"
        />
        <span class="text-gray-500">~</span>
        <input
          v-model="customEndDate"
          type="date"
          class="px-2 py-1.5 text-sm border border-gray-300 rounded-lg bg-white dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200"
        />
        <button
          class="px-3 py-1.5 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700"
          @click="applyCustomRange"
        >
          적용
        </button>
      </div>

      <!-- 단위 선택 -->
      <div class="flex items-center gap-2">
        <label class="text-sm text-gray-600 dark:text-gray-400">단위:</label>
        <div class="flex border border-gray-300 rounded-lg overflow-hidden dark:border-gray-600">
          <button
            v-for="unit in (['DAY', 'WEEK', 'MONTH'] as GanttDateUnit[])"
            :key="unit"
            class="px-2 py-1 text-sm transition-colors"
            :class="dateUnit === unit
              ? 'bg-primary-600 text-white'
              : 'bg-white text-gray-700 hover:bg-gray-50 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600'"
            @click="dateUnit = unit"
          >
            {{ unit === 'DAY' ? '일' : unit === 'WEEK' ? '주' : '월' }}
          </button>
        </div>
      </div>

      <!-- 완료 포함 -->
      <label class="flex items-center gap-1.5 cursor-pointer">
        <input
          v-model="includeCompleted"
          type="checkbox"
          class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500 dark:border-gray-600 dark:bg-gray-700"
        />
        <span class="text-sm text-gray-700 dark:text-gray-300">완료 포함</span>
      </label>
    </div>

    <!-- 범례 -->
    <div class="flex flex-wrap items-center gap-4 mb-4 text-sm text-gray-600 dark:text-gray-400">
      <div class="flex items-center gap-1.5">
        <span class="text-blue-500 font-bold">◆━◇</span>
        <span>요청~마감</span>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="text-green-500 font-bold">●━○</span>
        <span>시작~완료</span>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="w-0.5 h-4 bg-red-500" />
        <span>TODAY</span>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="inline-flex items-center px-1.5 py-0.5 text-[10px] font-medium rounded bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400">
          <svg class="w-3 h-3 mr-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
          </svg>
          배당
        </span>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="inline-flex items-center px-1.5 py-0.5 text-[10px] font-medium rounded bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400">
          <svg class="w-3 h-3 mr-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
          </svg>
          공유
        </span>
      </div>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <Spinner size="lg" />
    </div>

    <!-- 에러 -->
    <div v-else-if="error" class="flex-1 flex items-center justify-center text-red-500">
      {{ error }}
    </div>

    <!-- Gantt 차트 -->
    <div v-else-if="ganttData && sortedItems.length > 0" class="flex-1 overflow-auto border border-gray-200 rounded-lg dark:border-gray-700">
      <div class="flex min-w-max">
        <!-- 업무 목록 (좌측 고정) -->
        <div class="w-80 flex-shrink-0 bg-white border-r border-gray-200 dark:bg-gray-800 dark:border-gray-700">
          <!-- 헤더 -->
          <div class="h-12 px-3 flex items-center border-b border-gray-200 bg-gray-50 font-medium text-sm text-gray-700 dark:bg-gray-900 dark:border-gray-700 dark:text-gray-300">
            업무명
          </div>

          <!-- 업무 행 -->
          <div
            v-for="item in sortedItems"
            :key="item.itemId"
            class="h-16 px-3 flex items-center border-b border-gray-100 hover:bg-gray-50 cursor-pointer dark:border-gray-700 dark:hover:bg-gray-700/50"
            @dblclick="onItemDblClick(item)"
          >
            <div
              class="flex-1 min-w-0 flex items-center gap-2"
              :class="{ 'pl-4': item.itemDepth === 1, 'pl-8': item.itemDepth === 2 }"
            >
              <span v-if="item.itemDepth > 0" class="text-gray-400 flex-shrink-0">└</span>

              <!-- 제목 (호버 시 툴팁) -->
              <span
                :class="getPriorityBorderColor(item.priority)"
                class="border-l-2 pl-2 truncate flex-1 cursor-help"
                @mouseenter="showTooltip(item, $event)"
                @mouseleave="hideTooltip"
              >
                {{ item.title }}
              </span>

              <!-- 배당/공유 배지 -->
              <div class="flex items-center gap-1 flex-shrink-0">
                <!-- 배당받은 업무 -->
                <span
                  v-if="item.isAssignedToMe"
                  class="inline-flex items-center px-1.5 py-0.5 text-[10px] font-medium rounded bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400"
                  :title="item.assignedByUserName ? `${item.assignedByUserName}님이 배당` : '배당받은 업무'"
                >
                  <svg class="w-3 h-3 mr-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                  </svg>
                  배당
                </span>

                <!-- 공유받은 업무 (보드 공유) -->
                <span
                  v-if="item.isSharedToMe && !item.isAssignedToMe"
                  class="inline-flex items-center px-1.5 py-0.5 text-[10px] font-medium rounded bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400"
                  :title="item.sharedByUserName ? `${item.sharedByUserName}님이 공유` : '공유받은 업무'"
                >
                  <svg class="w-3 h-3 mr-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                  </svg>
                  공유
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 타임라인 (스크롤 영역) -->
        <div class="flex-1 overflow-x-auto">
          <!-- 날짜 헤더 - 일간 -->
          <div v-if="dateUnit === 'DAY'" class="h-12 flex border-b border-gray-200 bg-gray-50 dark:bg-gray-900 dark:border-gray-700">
            <div
              v-for="date in dates"
              :key="date.date"
              class="flex-shrink-0 flex items-center justify-center border-r border-gray-100 text-xs dark:border-gray-700"
              :style="{ width: `${dayWidth}px` }"
              :class="{
                'bg-red-50 dark:bg-red-900/20': date.isToday,
                'bg-gray-100 dark:bg-gray-800': date.isWeekend && !date.isToday
              }"
            >
              <span
                v-if="date.label"
                :class="date.isToday ? 'text-red-600 font-bold dark:text-red-400' : 'text-gray-600 dark:text-gray-400'"
              >
                {{ date.label }}
              </span>
            </div>
          </div>
          <!-- 날짜 헤더 - 주간/월간 (그룹화) -->
          <div v-else class="h-12 flex border-b border-gray-200 bg-gray-50 dark:bg-gray-900 dark:border-gray-700">
            <div
              v-for="(group, idx) in groupedHeaders"
              :key="idx"
              class="flex-shrink-0 flex items-center justify-center border-r border-gray-200 text-xs dark:border-gray-600"
              :style="{ width: `${group.width}px` }"
              :class="{ 'bg-red-50 dark:bg-red-900/20': group.isToday }"
            >
              <span :class="group.isToday ? 'text-red-600 font-bold dark:text-red-400' : 'text-gray-600 dark:text-gray-400'">
                {{ group.label }}
              </span>
            </div>
          </div>

          <!-- Gantt 행 -->
          <div
            v-for="item in sortedItems"
            :key="item.itemId"
            class="h-16 relative border-b border-gray-100 dark:border-gray-700"
            :style="{ width: `${dates.length * dayWidth}px` }"
          >
            <!-- 배경 그리드 -->
            <div class="absolute inset-0 flex">
              <div
                v-for="date in dates"
                :key="date.date"
                class="flex-shrink-0 border-r border-gray-50 dark:border-gray-800"
                :style="{ width: `${dayWidth}px` }"
                :class="{
                  'bg-red-50/50 dark:bg-red-900/10': date.isToday,
                  'bg-gray-50/50 dark:bg-gray-800/50': date.isWeekend && !date.isToday
                }"
              />
            </div>

            <!-- 요청일~마감일 바 (상단) - 더블클릭으로 상세 열기 -->
            <div
              v-if="item.requestDate || item.dueDate"
              class="absolute top-2 h-5 bg-blue-100 border border-blue-400 rounded flex items-center cursor-pointer hover:bg-blue-200 dark:bg-blue-900/50 dark:border-blue-500 dark:hover:bg-blue-900/70"
              :style="{
                left: `${dateToPosition(item.requestDate || item.dueDate)}px`,
                width: `${getBarWidth(item.requestDate, item.dueDate)}px`
              }"
              :title="`요청: ${formatDate(item.requestDate)} / 마감: ${formatDate(item.dueDate)} (더블클릭으로 상세보기)`"
              @dblclick.stop="onItemDblClick(item)"
            >
              <span class="absolute -left-1 text-blue-500 text-xs">◆</span>
              <span v-if="item.dueDate" class="absolute -right-1 text-blue-500 text-xs">◇</span>
            </div>

            <!-- 시작일~완료일 바 (하단) - 더블클릭으로 상세 열기 -->
            <div
              v-if="item.startDate || item.completionDate"
              class="absolute top-9 h-5 bg-green-100 border border-green-400 rounded flex items-center cursor-pointer hover:bg-green-200 dark:bg-green-900/50 dark:border-green-500 dark:hover:bg-green-900/70"
              :style="{
                left: `${dateToPosition(item.startDate || item.completionDate)}px`,
                width: `${getBarWidth(item.startDate, item.completionDate)}px`
              }"
              :title="`시작: ${formatDate(item.startDate)} / 완료: ${formatDate(item.completionDate)} (더블클릭으로 상세보기)`"
              @dblclick.stop="onItemDblClick(item)"
            >
              <span class="absolute -left-1 text-green-500 text-xs">●</span>
              <span v-if="item.completionDate" class="absolute -right-1 text-green-500 text-xs">○</span>
            </div>

            <!-- TODAY 라인 -->
            <div
              v-if="dates.some(d => d.isToday)"
              class="gantt-today-line absolute top-0 bottom-0 w-0.5 bg-red-500 z-10"
              :style="{ left: `${dateToPosition(todayStr) + dayWidth / 2}px` }"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 데이터 없음 -->
    <div v-else class="flex-1 flex items-center justify-center text-gray-500 dark:text-gray-400">
      표시할 업무가 없습니다.
    </div>

    <!-- 툴팁 -->
    <Teleport to="body">
      <div
        v-if="tooltipVisible && tooltipItem"
        class="fixed z-[9999] bg-white rounded-lg shadow-lg border border-gray-200 p-3 min-w-[250px] max-w-[350px] dark:bg-gray-800 dark:border-gray-700"
        :style="{ left: `${tooltipPosition.x}px`, top: `${tooltipPosition.y}px` }"
      >
        <!-- 업무 제목 -->
        <div class="font-medium text-sm text-gray-900 dark:text-white mb-2 line-clamp-2">
          {{ tooltipItem.title }}
        </div>

        <!-- 업무 설명 -->
        <div v-if="tooltipItem.description" class="text-xs text-gray-600 dark:text-gray-400 mb-2 line-clamp-3">
          {{ tooltipItem.description }}
        </div>

        <div class="border-t border-gray-100 dark:border-gray-700 pt-2 space-y-1.5 text-xs">
          <!-- 보드명 -->
          <div class="flex items-center gap-2">
            <span class="text-gray-500 dark:text-gray-400 w-16">보드:</span>
            <span class="text-gray-700 dark:text-gray-300">{{ tooltipItem.boardName }}</span>
          </div>

          <!-- 상위 업무 (하위 업무인 경우) -->
          <div v-if="tooltipItem.parentItemId && tooltipItem.parentTitle" class="flex items-center gap-2">
            <span class="text-gray-500 dark:text-gray-400 w-16">상위 업무:</span>
            <span class="text-indigo-600 dark:text-indigo-400">{{ tooltipItem.parentTitle }}</span>
          </div>

          <!-- 공유/배당 여부 -->
          <div class="flex items-center gap-2">
            <span class="text-gray-500 dark:text-gray-400 w-16">유형:</span>
            <div class="flex items-center gap-1">
              <span
                v-if="tooltipItem.isAssignedToMe"
                class="inline-flex items-center px-1.5 py-0.5 text-[10px] font-medium rounded bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400"
              >
                배당받음
              </span>
              <span
                v-else-if="tooltipItem.isSharedToMe"
                class="inline-flex items-center px-1.5 py-0.5 text-[10px] font-medium rounded bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400"
              >
                공유받음 (보드)
              </span>
              <span
                v-else
                class="text-gray-700 dark:text-gray-300"
              >
                내 업무
              </span>
            </div>
          </div>

          <!-- 공유자/배당자 -->
          <div v-if="tooltipItem.isAssignedToMe && tooltipItem.assignedByUserName" class="flex items-center gap-2">
            <span class="text-gray-500 dark:text-gray-400 w-16">배당자:</span>
            <span class="text-green-600 dark:text-green-400">{{ tooltipItem.assignedByUserName }}</span>
          </div>
          <div v-else-if="tooltipItem.isSharedToMe && tooltipItem.sharedByUserName" class="flex items-center gap-2">
            <span class="text-gray-500 dark:text-gray-400 w-16">공유자:</span>
            <span class="text-blue-600 dark:text-blue-400">{{ tooltipItem.sharedByUserName }}</span>
          </div>

          <!-- 날짜 정보 -->
          <div v-if="tooltipItem.requestDate || tooltipItem.dueDate" class="flex items-center gap-2">
            <span class="text-gray-500 dark:text-gray-400 w-16">기간:</span>
            <span class="text-gray-700 dark:text-gray-300">
              {{ formatDate(tooltipItem.requestDate) }} ~ {{ formatDate(tooltipItem.dueDate) }}
            </span>
          </div>
        </div>

        <!-- 안내 문구 -->
        <div class="mt-2 pt-2 border-t border-gray-100 dark:border-gray-700 text-[10px] text-gray-400 dark:text-gray-500">
          더블클릭으로 상세보기
        </div>
      </div>
    </Teleport>
  </div>
</template>
