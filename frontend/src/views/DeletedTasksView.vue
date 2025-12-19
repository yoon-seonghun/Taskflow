<script setup lang="ts">
/**
 * 삭제된 작업 메뉴
 * - 삭제된 작업 목록 표시 (논리 삭제)
 * - 기간 필터 (오늘, 이번주, 이번달, 사용자 지정)
 * - 정렬 (삭제시간 내림차순 기본)
 * - 삭제 전 상태 표시
 * - 복원 기능
 * 표시 컬럼: 등록시간, 삭제시간, 삭제 내용, 삭제 전 상태, 작업자, 처리
 */
import { ref, computed, onMounted, watch } from 'vue'
import { historyApi } from '@/api/history'
import { itemApi } from '@/api/item'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import { Spinner, EmptyState, Pagination, Badge } from '@/components/common'
import type { ItemHistory, ItemHistorySearchRequest, PreviousStatus } from '@/types/history'
import type { PageResponse } from '@/types/api'

const toast = useToast()
const confirm = useConfirm()

// 상태
const loading = ref(false)
const items = ref<ItemHistory[]>([])
const pagination = ref({
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0
})

// 기간 필터 타입
type PeriodFilter = 'today' | 'week' | 'month' | 'custom'

const periodFilter = ref<PeriodFilter>('today')
const customStartDate = ref('')
const customEndDate = ref('')
const sortDirection = ref<'asc' | 'desc'>('desc')

// 기간 필터 옵션
const periodOptions = [
  { value: 'today', label: '오늘' },
  { value: 'week', label: '이번주' },
  { value: 'month', label: '이번달' },
  { value: 'custom', label: '사용자 지정' }
]

// 날짜 계산 유틸
function getDateRange(period: PeriodFilter): { startDate: string; endDate: string } {
  const today = new Date()
  const endDate = formatDate(today)

  switch (period) {
    case 'today':
      return { startDate: endDate, endDate }

    case 'week': {
      const weekStart = new Date(today)
      const dayOfWeek = today.getDay()
      const diff = dayOfWeek === 0 ? 6 : dayOfWeek - 1
      weekStart.setDate(today.getDate() - diff)
      return { startDate: formatDate(weekStart), endDate }
    }

    case 'month': {
      const monthStart = new Date(today.getFullYear(), today.getMonth(), 1)
      return { startDate: formatDate(monthStart), endDate }
    }

    case 'custom':
      return {
        startDate: customStartDate.value || endDate,
        endDate: customEndDate.value || endDate
      }

    default:
      return { startDate: endDate, endDate }
  }
}

function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 삭제 전 상태 텍스트
function getPreviousStatusText(status?: PreviousStatus): string {
  if (!status) return '-'
  const map: Record<PreviousStatus, string> = {
    NOT_STARTED: '시작전',
    IN_PROGRESS: '진행중',
    PENDING: '보류'
  }
  return map[status] || status
}

// 삭제 전 상태 배지 variant
function getPreviousStatusVariant(status?: PreviousStatus): 'default' | 'primary' | 'warning' {
  if (!status) return 'default'
  const map: Record<PreviousStatus, 'default' | 'primary' | 'warning'> = {
    NOT_STARTED: 'default',
    IN_PROGRESS: 'primary',
    PENDING: 'warning'
  }
  return map[status] || 'default'
}

// 데이터 로드
async function fetchDeletedItems(page = 0) {
  loading.value = true

  try {
    const { startDate, endDate } = getDateRange(periodFilter.value)

    const params: ItemHistorySearchRequest = {
      result: 'DELETED',
      startDate,
      endDate,
      page,
      size: pagination.value.size,
      sort: `deletedAt,${sortDirection.value}`
    }

    const response = await historyApi.getItemHistory(params)

    if (response.success && response.data) {
      const data = response.data as PageResponse<ItemHistory>
      items.value = data.content
      pagination.value = {
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages
      }
    } else {
      toast.error(response.message || '삭제된 작업을 불러오는데 실패했습니다.')
    }
  } catch (error) {
    toast.error('삭제된 작업을 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

// 복원 처리
async function handleRestore(item: ItemHistory) {
  const confirmed = await confirm.show({
    title: '업무 복원',
    message: `이 업무를 복원하시겠습니까?\n복원 후 상태: ${getPreviousStatusText(item.previousStatus)}`,
    confirmText: '복원',
    confirmType: 'primary'
  })

  if (confirmed) {
    try {
      const response = await itemApi.restoreItem(item.boardId, item.itemId)
      if (response.success) {
        toast.success('업무가 복원되었습니다.')
        fetchDeletedItems(pagination.value.page)
      } else {
        toast.error(response.message || '복원에 실패했습니다.')
      }
    } catch (error) {
      toast.error('복원에 실패했습니다.')
    }
  }
}

// 페이지 변경
function handlePageChange(page: number) {
  fetchDeletedItems(page)
}

// 필터 변경 시 재로드
watch([periodFilter, customStartDate, customEndDate, sortDirection], () => {
  fetchDeletedItems(0)
})

// 정렬 토글
function toggleSort() {
  sortDirection.value = sortDirection.value === 'desc' ? 'asc' : 'desc'
}

// 기간 필터 선택
function selectPeriod(period: PeriodFilter) {
  periodFilter.value = period
}

// 현재 필터 기간 텍스트
const currentPeriodText = computed(() => {
  const { startDate, endDate } = getDateRange(periodFilter.value)
  if (startDate === endDate) {
    return startDate
  }
  return `${startDate} ~ ${endDate}`
})

onMounted(() => {
  const today = formatDate(new Date())
  customStartDate.value = today
  customEndDate.value = today

  fetchDeletedItems()
})
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 헤더 -->
    <div class="flex items-center justify-between mb-4">
      <div>
        <h2 class="text-xl font-semibold text-gray-900 flex items-center gap-2">
          삭제된 작업
          <svg class="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </h2>
        <p class="text-sm text-gray-500 mt-1">{{ currentPeriodText }}</p>
      </div>

      <div class="flex items-center gap-2 text-sm text-gray-600">
        <span>총 {{ pagination.totalElements }}건</span>
      </div>
    </div>

    <!-- 필터 영역 -->
    <div class="bg-white rounded-lg border border-gray-200 p-4 mb-4">
      <div class="flex flex-wrap items-center gap-4">
        <!-- 기간 필터 버튼 -->
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-gray-700">기간:</span>
          <div class="flex rounded-lg border border-gray-200 overflow-hidden">
            <button
              v-for="option in periodOptions"
              :key="option.value"
              class="px-3 py-1.5 text-[13px] transition-colors"
              :class="[
                periodFilter === option.value
                  ? 'bg-primary-500 text-white'
                  : 'bg-white text-gray-700 hover:bg-gray-50'
              ]"
              @click="selectPeriod(option.value as PeriodFilter)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <!-- 사용자 지정 날짜 입력 -->
        <Transition
          enter-active-class="transition duration-200 ease-out"
          enter-from-class="opacity-0 -translate-x-2"
          enter-to-class="opacity-100 translate-x-0"
          leave-active-class="transition duration-150 ease-in"
          leave-from-class="opacity-100 translate-x-0"
          leave-to-class="opacity-0 -translate-x-2"
        >
          <div v-if="periodFilter === 'custom'" class="flex items-center gap-2">
            <input
              v-model="customStartDate"
              type="date"
              class="px-3 py-1.5 text-[13px] border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
            <span class="text-gray-400">~</span>
            <input
              v-model="customEndDate"
              type="date"
              class="px-3 py-1.5 text-[13px] border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>
        </Transition>

        <!-- 정렬 버튼 -->
        <div class="ml-auto flex items-center gap-2">
          <span class="text-sm text-gray-600">삭제시간순</span>
          <button
            class="p-1.5 rounded hover:bg-gray-100 transition-colors"
            :title="sortDirection === 'desc' ? '내림차순 (최신순)' : '오름차순 (오래된순)'"
            @click="toggleSort"
          >
            <svg
              class="w-4 h-4 text-gray-600 transition-transform"
              :class="{ 'rotate-180': sortDirection === 'asc' }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 테이블 -->
    <div class="flex-1 bg-white rounded-lg border border-gray-200 overflow-hidden flex flex-col">
      <!-- 로딩 상태 -->
      <div v-if="loading" class="flex-1 flex items-center justify-center">
        <Spinner size="lg" />
      </div>

      <!-- 빈 상태 -->
      <template v-else-if="items.length === 0">
        <EmptyState
          title="삭제된 작업이 없습니다"
          description="해당 기간에 삭제된 작업이 없습니다."
          icon="trash"
          class="flex-1"
        />
      </template>

      <!-- 테이블 -->
      <template v-else>
        <div class="flex-1 overflow-auto">
          <table class="w-full">
            <thead class="sticky top-0 bg-gray-50 z-10">
              <tr class="border-b border-gray-200">
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  <div class="flex items-center gap-1.5">
                    <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                    등록시간
                  </div>
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  <div class="flex items-center gap-1.5">
                    <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    삭제시간
                  </div>
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap min-w-[250px]">
                  <div class="flex items-center gap-1.5">
                    <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
                    </svg>
                    삭제 내용
                  </div>
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  <div class="flex items-center gap-1.5">
                    <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                    삭제 전 상태
                  </div>
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  <div class="flex items-center gap-1.5">
                    <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    작업자
                  </div>
                </th>
                <th class="px-4 h-10 text-center text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  처리
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr
                v-for="item in items"
                :key="item.itemId"
                class="hover:bg-gray-50 transition-colors"
              >
                <td class="px-4 h-10 text-[13px] text-gray-600 whitespace-nowrap">
                  {{ formatDateTime(item.createdAt) }}
                </td>
                <td class="px-4 h-10 text-[13px] text-red-600 font-medium whitespace-nowrap">
                  {{ formatDateTime(item.deletedAt) }}
                </td>
                <td class="px-4 h-10 text-[13px] text-gray-900">
                  <div class="flex items-center gap-2">
                    <span class="truncate max-w-[350px]" :title="item.title">
                      {{ item.title }}
                    </span>
                    <span v-if="item.boardName" class="px-1.5 py-0.5 text-[11px] bg-gray-100 text-gray-500 rounded flex-shrink-0">
                      {{ item.boardName }}
                    </span>
                  </div>
                </td>
                <td class="px-4 h-10">
                  <Badge
                    :variant="getPreviousStatusVariant(item.previousStatus)"
                    size="sm"
                  >
                    {{ getPreviousStatusText(item.previousStatus) }}
                  </Badge>
                </td>
                <td class="px-4 h-10 text-[13px] text-gray-600 whitespace-nowrap">
                  <div class="flex items-center gap-2">
                    <div class="w-6 h-6 rounded-full bg-red-100 text-red-600 flex items-center justify-center text-[11px] font-medium">
                      {{ item.workerName?.charAt(0) || '?' }}
                    </div>
                    <span>{{ item.workerName || '-' }}</span>
                  </div>
                </td>
                <td class="px-4 h-10 text-center">
                  <button
                    class="px-3 py-1.5 text-[12px] font-medium text-white bg-blue-500 hover:bg-blue-600 rounded transition-colors"
                    @click="handleRestore(item)"
                  >
                    복원
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 페이지네이션 -->
        <div v-if="pagination.totalPages > 1" class="border-t border-gray-200 px-4 py-3 flex items-center justify-between">
          <div class="text-sm text-gray-500">
            {{ pagination.page * pagination.size + 1 }} -
            {{ Math.min((pagination.page + 1) * pagination.size, pagination.totalElements) }} /
            {{ pagination.totalElements }}건
          </div>
          <Pagination
            :current-page="pagination.page"
            :total-pages="pagination.totalPages"
            @change="handlePageChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>
