<script setup lang="ts">
/**
 * 지연 업무 메뉴
 * - 마감일이 지난 업무 목록 표시 (Cross-board)
 * - 서버 사이드 필터링/페이징
 * - 지연 일수 표시
 * - 우선순위/담당자별 필터
 */
import { ref, computed, onMounted, watch } from 'vue'
import { itemApi } from '@/api/item'
import { useBoardStore } from '@/stores/board'
import { useSlideOver } from '@/composables/useSlideOver'
import { useToast } from '@/composables/useToast'
import { Select, Spinner, EmptyState, Badge, Pagination } from '@/components/common'
import type { Item, Priority, CrossBoardSearchRequest } from '@/types/item'

const boardStore = useBoardStore()
const { openItemDetail } = useSlideOver()
const toast = useToast()

// 상태
const loading = ref(false)
const overdueItems = ref<Item[]>([])
const selectedBoardId = ref<number | null>(null)
const priorityFilter = ref<Priority | 'all'>('all')

// 페이징 상태
const currentPage = ref(0)
const pageSize = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)

// 통계
const stats = ref({
  total: 0,
  urgent: 0,
  high: 0,
  avgDays: 0
})

// 보드 목록
const boardOptions = computed(() => [
  { value: null, label: '전체 보드' },
  ...boardStore.activeBoards.map(b => ({
    value: b.boardId,
    label: b.boardName
  }))
])

// 우선순위 옵션
const priorityOptions = [
  { value: 'all', label: '전체 우선순위' },
  { value: 'URGENT', label: '긴급' },
  { value: 'HIGH', label: '높음' },
  { value: 'NORMAL', label: '보통' },
  { value: 'LOW', label: '낮음' }
]

// 데이터 로드
async function loadData() {
  loading.value = true
  try {
    // 보드 목록 로드 (필터 옵션용)
    await boardStore.fetchBoards()

    // 지연 업무 조회
    const params: CrossBoardSearchRequest = {
      boardId: selectedBoardId.value || undefined,
      priority: priorityFilter.value !== 'all' ? priorityFilter.value : undefined,
      page: currentPage.value,
      size: pageSize.value,
      sort: 'dueDate,asc'
    }

    const response = await itemApi.getOverdueItems(params)
    if (response.success && response.data) {
      overdueItems.value = response.data.content
      totalElements.value = response.data.totalElements
      totalPages.value = response.data.totalPages

      // 통계 계산
      calculateStats()
    } else {
      toast.error('데이터를 불러오는데 실패했습니다.')
    }
  } catch (error) {
    toast.error('데이터를 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

// 통계 계산
function calculateStats() {
  const items = overdueItems.value
  const durations = items.map(item => getOverdueDays(item))

  stats.value = {
    total: totalElements.value,
    urgent: items.filter(i => i.priority === 'URGENT').length,
    high: items.filter(i => i.priority === 'HIGH').length,
    avgDays: durations.length > 0
      ? Math.round(durations.reduce((sum, d) => sum + d, 0) / durations.length)
      : 0
  }
}

// 아이템 클릭
function handleItemClick(item: Item) {
  openItemDetail(item.itemId, item.boardId)
}

// 완료 처리
async function handleComplete(item: Item) {
  try {
    const response = await itemApi.completeItem(item.boardId, item.itemId)
    if (response.success) {
      toast.success('완료 처리되었습니다.')
      loadData() // 목록 새로고침
    } else {
      toast.error('완료 처리에 실패했습니다.')
    }
  } catch {
    toast.error('완료 처리에 실패했습니다.')
  }
}

// 우선순위 변경
async function handlePriorityChange(item: Item, priority: Priority) {
  try {
    const response = await itemApi.updateItem(item.boardId, item.itemId, { priority })
    if (response.success) {
      toast.success('우선순위가 변경되었습니다.')
      // 로컬 업데이트
      const idx = overdueItems.value.findIndex(i => i.itemId === item.itemId)
      if (idx !== -1) {
        overdueItems.value[idx].priority = priority
      }
    } else {
      toast.error('우선순위 변경에 실패했습니다.')
    }
  } catch {
    toast.error('우선순위 변경에 실패했습니다.')
  }
}

// 지연 일수 계산
function getOverdueDays(item: Item): number {
  if (!item.dueDate) return 0
  const dueDate = new Date(item.dueDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  dueDate.setHours(0, 0, 0, 0)
  const diff = today.getTime() - dueDate.getTime()
  return Math.max(0, Math.floor(diff / (1000 * 60 * 60 * 24)))
}

// 우선순위 배지 색상
function getPriorityVariant(priority: Priority): 'danger' | 'warning' | 'primary' | 'default' {
  const map: Record<Priority, 'danger' | 'warning' | 'primary' | 'default'> = {
    URGENT: 'danger',
    HIGH: 'warning',
    NORMAL: 'primary',
    LOW: 'default'
  }
  return map[priority]
}

// 우선순위 텍스트
function getPriorityText(priority: Priority): string {
  const map: Record<Priority, string> = {
    URGENT: '긴급',
    HIGH: '높음',
    NORMAL: '보통',
    LOW: '낮음'
  }
  return map[priority]
}

// 날짜 포맷
function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR', {
    month: '2-digit',
    day: '2-digit'
  })
}

// 페이지 변경
function handlePageChange(page: number) {
  currentPage.value = page
  loadData()
}

// 필터 변경 시 재조회
watch([selectedBoardId, priorityFilter], () => {
  currentPage.value = 0 // 필터 변경 시 첫 페이지로
  loadData()
})

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 헤더 -->
    <div class="flex items-center justify-between mb-4">
      <div>
        <h2 class="text-xl font-semibold text-gray-900 flex items-center gap-2">
          지연 업무
          <svg class="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </h2>
        <p class="text-sm text-gray-500 mt-1">마감일이 지난 업무를 관리합니다.</p>
      </div>
    </div>

    <!-- 통계 카드 -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4">
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-red-600">{{ stats.total }}</div>
        <div class="text-sm text-gray-500">전체 지연</div>
      </div>
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-red-500">{{ stats.urgent }}</div>
        <div class="text-sm text-gray-500">긴급</div>
      </div>
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-orange-500">{{ stats.high }}</div>
        <div class="text-sm text-gray-500">높음</div>
      </div>
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-gray-700">{{ stats.avgDays }}일</div>
        <div class="text-sm text-gray-500">평균 지연</div>
      </div>
    </div>

    <!-- 필터 영역 -->
    <div class="bg-white rounded-lg border border-gray-200 p-4 mb-4">
      <div class="flex flex-wrap items-center gap-4">
        <!-- 보드 필터 -->
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-gray-700 whitespace-nowrap">보드:</span>
          <Select
            v-model="selectedBoardId"
            :options="boardOptions"
            size="sm"
            class="w-40"
          />
        </div>

        <!-- 우선순위 필터 -->
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-gray-700 whitespace-nowrap">우선순위:</span>
          <Select
            v-model="priorityFilter"
            :options="priorityOptions"
            size="sm"
            class="w-36"
          />
        </div>

        <div class="ml-auto text-sm text-gray-500">
          {{ overdueItems.length }}건 / 총 {{ totalElements }}건
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
      <template v-else-if="overdueItems.length === 0">
        <EmptyState
          title="지연된 업무가 없습니다"
          description="모든 업무가 마감일 내에 처리되고 있습니다."
          icon="check-circle"
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
                  지연 일수
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap min-w-[250px]">
                  작업 내용
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  마감일
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  우선순위
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  담당자
                </th>
                <th class="px-4 h-10 text-center text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  처리
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr
                v-for="item in overdueItems"
                :key="item.itemId"
                class="group hover:bg-gray-50 transition-colors cursor-pointer"
                @click="handleItemClick(item)"
              >
                <!-- 지연 일수 -->
                <td class="px-4 h-12">
                  <div class="flex items-center gap-1.5">
                    <span
                      class="inline-flex items-center justify-center min-w-[40px] px-2 py-1 text-[13px] font-bold rounded"
                      :class="[
                        getOverdueDays(item) >= 7
                          ? 'bg-red-100 text-red-700'
                          : getOverdueDays(item) >= 3
                            ? 'bg-orange-100 text-orange-700'
                            : 'bg-yellow-100 text-yellow-700'
                      ]"
                    >
                      {{ getOverdueDays(item) }}일
                    </span>
                  </div>
                </td>

                <!-- 작업 내용 -->
                <td class="px-4 h-12">
                  <div class="flex items-center gap-2">
                    <span class="text-[13px] text-gray-900 truncate max-w-[300px]" :title="item.title">
                      {{ item.title }}
                    </span>
                    <!-- Notion 스타일 '열기' 버튼 -->
                    <button
                      class="flex-shrink-0 opacity-0 group-hover:opacity-100 transition-opacity px-1.5 py-0.5 text-[11px] text-gray-500 hover:text-primary-600 hover:bg-primary-50 rounded"
                      title="상세 패널 열기"
                      @click.stop="handleItemClick(item)"
                    >
                      열기
                    </button>
                    <span v-if="item.boardName" class="px-1.5 py-0.5 text-[11px] bg-gray-100 text-gray-500 rounded flex-shrink-0">
                      {{ item.boardName }}
                    </span>
                  </div>
                </td>

                <!-- 마감일 -->
                <td class="px-4 h-12 text-[13px] text-red-600 font-medium whitespace-nowrap">
                  {{ formatDate(item.dueDate) }}
                </td>

                <!-- 우선순위 -->
                <td class="px-4 h-12" @click.stop>
                  <Select
                    :model-value="item.priority"
                    :options="[
                      { value: 'URGENT', label: '긴급' },
                      { value: 'HIGH', label: '높음' },
                      { value: 'NORMAL', label: '보통' },
                      { value: 'LOW', label: '낮음' }
                    ]"
                    size="sm"
                    class="w-24"
                    @update:model-value="handlePriorityChange(item, $event as Priority)"
                  />
                </td>

                <!-- 담당자 -->
                <td class="px-4 h-12 text-[13px] text-gray-600 whitespace-nowrap">
                  <div class="flex items-center gap-2">
                    <div class="w-6 h-6 rounded-full bg-primary-100 text-primary-600 flex items-center justify-center text-[11px] font-medium">
                      {{ item.assigneeName?.charAt(0) || '?' }}
                    </div>
                    <span>{{ item.assigneeName || '-' }}</span>
                  </div>
                </td>

                <!-- 처리 버튼 -->
                <td class="px-4 h-12 text-center" @click.stop>
                  <button
                    class="px-3 py-1.5 text-[12px] font-medium text-white bg-green-500 hover:bg-green-600 rounded transition-colors"
                    @click="handleComplete(item)"
                  >
                    완료
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 페이징 -->
        <div v-if="totalPages > 1" class="flex-shrink-0 border-t border-gray-200 px-4 py-3">
          <Pagination
            :current-page="currentPage"
            :total-pages="totalPages"
            :total-elements="totalElements"
            @page-change="handlePageChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>
