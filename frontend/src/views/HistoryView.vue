<script setup lang="ts">
/**
 * 이력관리 메뉴 (History View)
 * - 전체 / 작업 처리 이력 / 작업 등록 이력 전환
 * - 기간 필터, 검색, 페이징
 */
import { ref, computed, watch, onMounted } from 'vue'
import HistorySwitch from '@/components/history/HistorySwitch.vue'
import HistoryTable from '@/components/history/HistoryTable.vue'
import Pagination from '@/components/common/Pagination.vue'
import Input from '@/components/common/Input.vue'
import Select from '@/components/common/Select.vue'
import type { SelectOption } from '@/components/common/Select.vue'
import type { HistoryType } from '@/components/history/HistorySwitch.vue'
import type { ItemHistory, TemplateHistory, AuditLog, ItemHistorySearchRequest, TemplateHistorySearchRequest, AuditLogSearchRequest, AuditTargetType, AuditAction } from '@/types/history'
import { historyApi } from '@/api/history'
import { useUiStore } from '@/stores/ui'
import { useSlideOver } from '@/composables/useSlideOver'

const uiStore = useUiStore()
const { openItemDetail } = useSlideOver()

// 아이템 상세 패널 열기
function handleOpenItem(itemId: number, boardId: number) {
  openItemDetail(itemId, boardId)
}

// 이력 타입 (기본값: 전체)
const historyType = ref<HistoryType>('all')

// 로딩 상태
const loading = ref(false)
const itemLoading = ref(false)
const templateLoading = ref(false)
const managementLoading = ref(false)

// 작업 처리 이력 데이터
const itemHistories = ref<ItemHistory[]>([])
const itemTotalPages = ref(0)
const itemTotalElements = ref(0)

// 작업 등록 이력 데이터
const templateHistories = ref<TemplateHistory[]>([])
const templateTotalPages = ref(0)
const templateTotalElements = ref(0)

// 관리 이력 데이터
const managementHistories = ref<AuditLog[]>([])
const managementTotalPages = ref(0)
const managementTotalElements = ref(0)

// 공통 검색 키워드
const searchKeyword = ref('')

// 검색 조건 - 작업 처리 이력
const itemSearchParams = ref<ItemHistorySearchRequest>({
  page: 0,
  size: 20,
  keyword: '',
  result: undefined,
  startDate: '',
  endDate: ''
})

// 검색 조건 - 작업 등록 이력
const templateSearchParams = ref<TemplateHistorySearchRequest>({
  page: 0,
  size: 20,
  keyword: '',
  status: undefined,
  startDate: '',
  endDate: ''
})

// 검색 조건 - 관리 이력
const managementSearchParams = ref<AuditLogSearchRequest>({
  page: 0,
  size: 20,
  targetType: undefined,
  action: undefined,
  startDate: '',
  endDate: ''
})

// 작업 결과 옵션
const resultOptions: SelectOption[] = [
  { value: '', label: '전체' },
  { value: 'COMPLETED', label: '완료', color: '#10B981' },
  { value: 'DELETED', label: '삭제', color: '#EF4444' }
]

// 템플릿 상태 옵션
const statusOptions: SelectOption[] = [
  { value: '', label: '전체' },
  { value: 'ACTIVE', label: '활성', color: '#10B981' },
  { value: 'INACTIVE', label: '비활성', color: '#6B7280' }
]

// 관리 이력 - 대상 유형 옵션
const targetTypeOptions: SelectOption[] = [
  { value: '', label: '전체' },
  { value: 'BOARD', label: '보드', color: '#3B82F6' },
  { value: 'ITEM', label: '업무', color: '#10B981' },
  { value: 'BOARD_SHARE', label: '보드 공유', color: '#8B5CF6' },
  { value: 'ITEM_SHARE', label: '업무 공유', color: '#EC4899' }
]

// 관리 이력 - 액션 옵션
const actionOptions: SelectOption[] = [
  { value: '', label: '전체' },
  { value: 'CREATE', label: '생성', color: '#10B981' },
  { value: 'UPDATE', label: '수정', color: '#3B82F6' },
  { value: 'DELETE', label: '삭제', color: '#EF4444' },
  { value: 'TRANSFER', label: '이관', color: '#F59E0B' },
  { value: 'SHARE', label: '공유', color: '#8B5CF6' },
  { value: 'UNSHARE', label: '공유해제', color: '#6B7280' }
]

// 기간 필터 옵션
const periodOptions: SelectOption[] = [
  { value: '', label: '전체 기간' },
  { value: 'today', label: '오늘' },
  { value: 'week', label: '이번 주' },
  { value: 'month', label: '이번 달' },
  { value: 'custom', label: '직접 입력' }
]

// 기간 필터
const selectedPeriod = ref('')
const customStartDate = ref('')
const customEndDate = ref('')

// 전체 건수 (전체 보기용)
const totalAllElements = computed(() => itemTotalElements.value + templateTotalElements.value)

// 기간 필터 변경 처리
function handlePeriodChange(period: string | number | null) {
  // null 또는 숫자 타입 처리
  const periodStr = period?.toString() ?? ''
  selectedPeriod.value = periodStr

  const today = new Date()
  let startDate = ''
  let endDate = ''

  switch (periodStr) {
    case 'today':
      startDate = formatDate(today)
      endDate = formatDate(today)
      break
    case 'week': {
      const weekStart = new Date(today)
      // 월요일 시작 (한국 표준): 일요일(0)이면 6일 전, 그 외는 (요일-1)일 전
      const dayOfWeek = today.getDay()
      const daysToMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1
      weekStart.setDate(today.getDate() - daysToMonday)
      startDate = formatDate(weekStart)
      endDate = formatDate(today)
      break
    }
    case 'month': {
      const monthStart = new Date(today.getFullYear(), today.getMonth(), 1)
      startDate = formatDate(monthStart)
      endDate = formatDate(today)
      break
    }
    case 'custom':
      // 직접 입력 모드
      return
    default:
      startDate = ''
      endDate = ''
  }

  customStartDate.value = startDate
  customEndDate.value = endDate

  // 모든 검색 파라미터에 날짜 적용
  itemSearchParams.value.startDate = startDate
  itemSearchParams.value.endDate = endDate
  templateSearchParams.value.startDate = startDate
  templateSearchParams.value.endDate = endDate
  managementSearchParams.value.startDate = startDate
  managementSearchParams.value.endDate = endDate
}

// 날짜 포맷팅 (YYYY-MM-DD)
function formatDate(date: Date): string {
  return date.toISOString().split('T')[0]
}

// 사용자 지정 날짜 변경
function handleCustomDateChange() {
  // 날짜 유효성 검증
  if (customStartDate.value && customEndDate.value) {
    if (customStartDate.value > customEndDate.value) {
      uiStore.showWarning('시작일은 종료일보다 이전이어야 합니다.')
      return
    }
  }

  // 모든 검색 파라미터에 날짜 적용
  itemSearchParams.value.startDate = customStartDate.value
  itemSearchParams.value.endDate = customEndDate.value
  templateSearchParams.value.startDate = customStartDate.value
  templateSearchParams.value.endDate = customEndDate.value
  managementSearchParams.value.startDate = customStartDate.value
  managementSearchParams.value.endDate = customEndDate.value
}

// 작업 처리 이력 조회
async function loadItemHistory() {
  itemLoading.value = true
  try {
    const keyword = historyType.value === 'all' ? searchKeyword.value : itemSearchParams.value.keyword
    const params: ItemHistorySearchRequest = {
      ...itemSearchParams.value,
      keyword: keyword || undefined,
      result: itemSearchParams.value.result || undefined,
      startDate: itemSearchParams.value.startDate || undefined,
      endDate: itemSearchParams.value.endDate || undefined
    }

    const response = await historyApi.getItemHistory(params)
    itemHistories.value = response.data.content
    itemTotalPages.value = response.data.totalPages
    itemTotalElements.value = response.data.totalElements
  } catch (error) {
    console.error('Failed to load item history:', error)
    uiStore.showError('작업 처리 이력을 불러오는데 실패했습니다.')
  } finally {
    itemLoading.value = false
  }
}

// 작업 등록 이력 조회
async function loadTemplateHistory() {
  templateLoading.value = true
  try {
    const keyword = historyType.value === 'all' ? searchKeyword.value : templateSearchParams.value.keyword
    const params: TemplateHistorySearchRequest = {
      ...templateSearchParams.value,
      keyword: keyword || undefined,
      status: templateSearchParams.value.status || undefined,
      startDate: templateSearchParams.value.startDate || undefined,
      endDate: templateSearchParams.value.endDate || undefined
    }

    const response = await historyApi.getTemplateHistory(params)
    templateHistories.value = response.data.content
    templateTotalPages.value = response.data.totalPages
    templateTotalElements.value = response.data.totalElements
  } catch (error) {
    console.error('Failed to load template history:', error)
    uiStore.showError('작업 등록 이력을 불러오는데 실패했습니다.')
  } finally {
    templateLoading.value = false
  }
}

// 관리 이력 조회
async function loadManagementHistory() {
  managementLoading.value = true
  try {
    const params: AuditLogSearchRequest = {
      ...managementSearchParams.value,
      targetType: managementSearchParams.value.targetType || undefined,
      action: managementSearchParams.value.action || undefined,
      startDate: managementSearchParams.value.startDate || undefined,
      endDate: managementSearchParams.value.endDate || undefined
    }

    const response = await historyApi.getManagementHistory(params)
    managementHistories.value = response.data.content
    managementTotalPages.value = response.data.totalPages
    managementTotalElements.value = response.data.totalElements
  } catch (error) {
    console.error('Failed to load management history:', error)
    uiStore.showError('관리 이력을 불러오는데 실패했습니다.')
  } finally {
    managementLoading.value = false
  }
}

// 전체 이력 조회 (병렬 로드)
async function loadAllHistory() {
  loading.value = true
  try {
    await Promise.all([loadItemHistory(), loadTemplateHistory()])
  } finally {
    loading.value = false
  }
}

// 검색
function handleSearch() {
  if (historyType.value === 'all') {
    // 전체: 공통 키워드로 양쪽 검색
    itemSearchParams.value.page = 0
    templateSearchParams.value.page = 0
    loadAllHistory()
  } else if (historyType.value === 'item') {
    itemSearchParams.value.page = 0
    loadItemHistory()
  } else if (historyType.value === 'template') {
    templateSearchParams.value.page = 0
    loadTemplateHistory()
  } else if (historyType.value === 'management') {
    managementSearchParams.value.page = 0
    loadManagementHistory()
  }
}

// 페이지 변경 - 작업 처리 이력
function handleItemPageChange(page: number) {
  itemSearchParams.value.page = page
  loadItemHistory()
}

// 페이지 변경 - 작업 등록 이력
function handleTemplatePageChange(page: number) {
  templateSearchParams.value.page = page
  loadTemplateHistory()
}

// 페이지 변경 - 관리 이력
function handleManagementPageChange(page: number) {
  managementSearchParams.value.page = page
  loadManagementHistory()
}

// 페이지 변경 (단일 뷰용)
function handlePageChange(page: number) {
  if (historyType.value === 'item') {
    handleItemPageChange(page)
  } else if (historyType.value === 'template') {
    handleTemplatePageChange(page)
  } else if (historyType.value === 'management') {
    handleManagementPageChange(page)
  }
}

// 이력 타입 변경 시 데이터 로드
watch(historyType, (newType) => {
  // 기간 필터 초기화
  selectedPeriod.value = ''
  customStartDate.value = ''
  customEndDate.value = ''

  // 검색 조건 초기화
  itemSearchParams.value.page = 0
  itemSearchParams.value.startDate = ''
  itemSearchParams.value.endDate = ''
  templateSearchParams.value.page = 0
  templateSearchParams.value.startDate = ''
  templateSearchParams.value.endDate = ''
  managementSearchParams.value.page = 0
  managementSearchParams.value.startDate = ''
  managementSearchParams.value.endDate = ''
  searchKeyword.value = ''

  if (newType === 'all') {
    loadAllHistory()
  } else if (newType === 'item') {
    loadItemHistory()
  } else if (newType === 'template') {
    loadTemplateHistory()
  } else if (newType === 'management') {
    loadManagementHistory()
  }
})

// 현재 페이지 계산
function getCurrentPage(): number {
  switch (historyType.value) {
    case 'item':
      return itemSearchParams.value.page ?? 0
    case 'template':
      return templateSearchParams.value.page ?? 0
    case 'management':
      return managementSearchParams.value.page ?? 0
    default:
      return 0
  }
}

// 전체 페이지 수 계산
function getCurrentTotalPages(): number {
  switch (historyType.value) {
    case 'item':
      return itemTotalPages.value
    case 'template':
      return templateTotalPages.value
    case 'management':
      return managementTotalPages.value
    default:
      return 0
  }
}

// 초기 로드
onMounted(() => {
  loadAllHistory()
})
</script>

<template>
  <div class="history-view">
    <!-- 헤더 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">이력관리</h1>
      <p class="mt-1 text-sm text-gray-500">
        작업 처리 이력과 작업 등록 이력을 조회합니다.
      </p>
    </div>

    <!-- 이력 타입 스위치 -->
    <div class="mb-6">
      <HistorySwitch v-model="historyType" />
    </div>

    <!-- 검색 필터 -->
    <div class="filter-section">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- 키워드 검색 (전체 모드) -->
        <Input
          v-if="historyType === 'all'"
          v-model="searchKeyword"
          placeholder="작업 내용 검색..."
          :clearable="true"
          @enter="handleSearch"
        />

        <!-- 키워드 검색 (작업 처리 이력) -->
        <Input
          v-else-if="historyType === 'item'"
          v-model="itemSearchParams.keyword"
          placeholder="작업 내용 검색..."
          :clearable="true"
          @enter="handleSearch"
        />

        <!-- 키워드 검색 (작업 등록 이력) -->
        <Input
          v-else-if="historyType === 'template'"
          v-model="templateSearchParams.keyword"
          placeholder="작업 내용 검색..."
          :clearable="true"
          @enter="handleSearch"
        />

        <!-- 관리 이력 - 대상 유형 필터 -->
        <Select
          v-if="historyType === 'management'"
          v-model="managementSearchParams.targetType"
          :options="targetTypeOptions"
          placeholder="대상 유형"
          :clearable="true"
          @change="handleSearch"
        />

        <!-- 관리 이력 - 액션 필터 -->
        <Select
          v-if="historyType === 'management'"
          v-model="managementSearchParams.action"
          :options="actionOptions"
          placeholder="액션"
          :clearable="true"
          @change="handleSearch"
        />

        <!-- 작업 결과 필터 (전체 또는 작업 처리 이력) -->
        <Select
          v-if="historyType === 'all' || historyType === 'item'"
          v-model="itemSearchParams.result"
          :options="resultOptions"
          placeholder="작업 결과"
          :clearable="true"
          @change="handleSearch"
        />

        <!-- 상태 필터 (전체 또는 작업 등록 이력) -->
        <Select
          v-if="historyType === 'all' || historyType === 'template'"
          v-model="templateSearchParams.status"
          :options="statusOptions"
          placeholder="템플릿 상태"
          :clearable="true"
          @change="handleSearch"
        />

        <!-- 기간 필터 -->
        <Select
          v-model="selectedPeriod"
          :options="periodOptions"
          placeholder="기간 선택"
          @change="handlePeriodChange"
        />

        <!-- 검색 버튼 -->
        <button
          type="button"
          class="btn-primary h-8"
          @click="handleSearch"
        >
          <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          검색
        </button>
      </div>

      <!-- 사용자 지정 날짜 입력 -->
      <div v-if="selectedPeriod === 'custom'" class="mt-4 flex items-center gap-4">
        <div class="flex items-center gap-2">
          <span class="text-sm text-gray-600">시작일</span>
          <input
            v-model="customStartDate"
            type="date"
            class="date-input"
            @change="handleCustomDateChange"
          />
        </div>
        <span class="text-gray-400">~</span>
        <div class="flex items-center gap-2">
          <span class="text-sm text-gray-600">종료일</span>
          <input
            v-model="customEndDate"
            type="date"
            class="date-input"
            @change="handleCustomDateChange"
          />
        </div>
        <button
          type="button"
          class="btn-secondary h-8"
          @click="handleSearch"
        >
          적용
        </button>
      </div>
    </div>

    <!-- 전체 보기 모드 -->
    <template v-if="historyType === 'all'">
      <!-- 전체 결과 정보 -->
      <div class="flex items-center justify-between mb-4">
        <p class="text-sm text-gray-600">
          전체
          <span class="font-medium text-gray-900">{{ totalAllElements }}</span>
          건
          <span class="text-gray-400 mx-2">|</span>
          작업 처리
          <span class="font-medium text-blue-600">{{ itemTotalElements }}</span>
          건,
          작업 등록
          <span class="font-medium text-green-600">{{ templateTotalElements }}</span>
          건
        </p>
      </div>

      <!-- 작업 처리 이력 섹션 -->
      <div class="mb-8">
        <div class="section-header">
          <h2 class="section-title">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
            </svg>
            작업 처리 이력
            <span class="text-sm font-normal text-gray-500">({{ itemTotalElements }}건)</span>
          </h2>
        </div>
        <div class="table-container">
          <HistoryTable
            type="item"
            :item-histories="itemHistories"
            :loading="itemLoading"
            @open-item="handleOpenItem"
          />
        </div>
        <div v-if="itemTotalPages > 1" class="mt-4 flex justify-center">
          <Pagination
            :current-page="itemSearchParams.page ?? 0"
            :total-pages="itemTotalPages"
            @change="handleItemPageChange"
          />
        </div>
      </div>

      <!-- 작업 등록 이력 섹션 -->
      <div>
        <div class="section-header">
          <h2 class="section-title">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            작업 등록 이력
            <span class="text-sm font-normal text-gray-500">({{ templateTotalElements }}건)</span>
          </h2>
        </div>
        <div class="table-container">
          <HistoryTable
            type="template"
            :template-histories="templateHistories"
            :loading="templateLoading"
            @open-item="handleOpenItem"
          />
        </div>
        <div v-if="templateTotalPages > 1" class="mt-4 flex justify-center">
          <Pagination
            :current-page="templateSearchParams.page ?? 0"
            :total-pages="templateTotalPages"
            @change="handleTemplatePageChange"
          />
        </div>
      </div>
    </template>

    <!-- 단일 보기 모드 (작업 처리 이력, 작업 등록 이력, 관리 이력) -->
    <template v-else>
      <!-- 결과 정보 -->
      <div class="flex items-center justify-between mb-4">
        <p class="text-sm text-gray-600">
          총
          <span class="font-medium text-gray-900">
            {{ historyType === 'item' ? itemTotalElements : historyType === 'template' ? templateTotalElements : managementTotalElements }}
          </span>
          건
        </p>
      </div>

      <!-- 이력 테이블 -->
      <div class="table-container">
        <HistoryTable
          :type="historyType"
          :item-histories="itemHistories"
          :template-histories="templateHistories"
          :management-histories="managementHistories"
          :loading="historyType === 'item' ? itemLoading : historyType === 'template' ? templateLoading : managementLoading"
          @open-item="handleOpenItem"
        />
      </div>

      <!-- 페이지네이션 -->
      <div
        v-if="getCurrentTotalPages() > 1"
        class="mt-6 flex justify-center"
      >
        <Pagination
          :current-page="getCurrentPage()"
          :total-pages="getCurrentTotalPages()"
          @change="handlePageChange"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.history-view {
  @apply max-w-7xl mx-auto;
}

.filter-section {
  @apply p-4 bg-white rounded-lg border border-gray-200 mb-6;
}

.section-header {
  @apply mb-3;
}

.section-title {
  @apply flex items-center gap-2 text-base font-semibold text-gray-800;
}

.table-container {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden;
}

.btn-primary {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150;
}

.btn-secondary {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150;
}

.date-input {
  @apply px-3 py-1.5 text-sm border border-gray-300 rounded-lg
         focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500;
}
</style>
