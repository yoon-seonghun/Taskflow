<script setup lang="ts">
/**
 * 보류 업무 메뉴
 * - 보류(PENDING) 상태 업무 목록 표시 (Cross-board)
 * - 서버 사이드 필터링/페이징
 * - 보류 기간 표시
 * - 재개/완료/삭제 처리
 */
import { ref, computed, onMounted, watch } from 'vue'
import { itemApi } from '@/api/item'
import { useBoardStore } from '@/stores/board'
import { useSlideOver } from '@/composables/useSlideOver'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import { Select, Spinner, EmptyState, Badge, Pagination } from '@/components/common'
import type { Item, Priority, CrossBoardSearchRequest } from '@/types/item'

const boardStore = useBoardStore()
const { openItemDetail } = useSlideOver()
const toast = useToast()
const confirm = useConfirm()

// 상태
const loading = ref(false)
const pendingItems = ref<Item[]>([])
const selectedBoardId = ref<number | null>(null)

// 페이징 상태
const currentPage = ref(0)
const pageSize = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)

// 통계
const stats = ref({
  total: 0,
  avgDays: 0,
  longPending: 0,
  urgentHigh: 0
})

// 보드 목록
const boardOptions = computed(() => [
  { value: null, label: '전체 보드' },
  ...boardStore.activeBoards.map(b => ({
    value: b.boardId,
    label: b.boardName
  }))
])

// 데이터 로드
async function loadData() {
  loading.value = true
  try {
    // 보드 목록 로드 (필터 옵션용)
    await boardStore.fetchBoards()

    // 보류 업무 조회
    const params: CrossBoardSearchRequest = {
      boardId: selectedBoardId.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
      sort: 'updatedAt,desc'
    }

    const response = await itemApi.getPendingItems(params)
    if (response.success && response.data) {
      pendingItems.value = response.data.content
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
  const items = pendingItems.value
  const durations = items.map(item => getPendingDays(item))

  stats.value = {
    total: totalElements.value,
    avgDays: durations.length > 0
      ? Math.round(durations.reduce((sum, d) => sum + d, 0) / durations.length)
      : 0,
    longPending: durations.filter(d => d >= 7).length,
    urgentHigh: items.filter(i => i.priority === 'URGENT' || i.priority === 'HIGH').length
  }
}

// 아이템 클릭
function handleItemClick(item: Item) {
  openItemDetail(item.itemId, item.boardId)
}

// 재개 처리 (진행중으로 변경)
async function handleResume(item: Item) {
  try {
    const response = await itemApi.updateItem(item.boardId, item.itemId, {
      status: 'IN_PROGRESS'
    })
    if (response.success) {
      toast.success('업무가 재개되었습니다.')
      loadData() // 목록 새로고침
    } else {
      toast.error('상태 변경에 실패했습니다.')
    }
  } catch {
    toast.error('상태 변경에 실패했습니다.')
  }
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

// 삭제 처리
async function handleDelete(item: Item) {
  const confirmed = await confirm.show({
    title: '업무 삭제',
    message: '이 업무를 삭제하시겠습니까?',
    confirmText: '삭제',
    confirmType: 'danger'
  })

  if (confirmed) {
    try {
      const response = await itemApi.deleteItem(item.boardId, item.itemId)
      if (response.success) {
        toast.success('삭제되었습니다.')
        loadData() // 목록 새로고침
      } else {
        toast.error('삭제에 실패했습니다.')
      }
    } catch {
      toast.error('삭제에 실패했습니다.')
    }
  }
}

// 보류 기간 계산
function getPendingDays(item: Item): number {
  const updated = new Date(item.updatedAt || item.createdAt)
  const now = new Date()
  return Math.floor((now.getTime() - updated.getTime()) / (1000 * 60 * 60 * 24))
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

// 우선순위 배지 variant
function getPriorityVariant(priority: Priority): 'danger' | 'warning' | 'primary' | 'default' {
  const map: Record<Priority, 'danger' | 'warning' | 'primary' | 'default'> = {
    URGENT: 'danger',
    HIGH: 'warning',
    NORMAL: 'primary',
    LOW: 'default'
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
watch([selectedBoardId], () => {
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
          보류 업무
          <svg class="w-5 h-5 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </h2>
        <p class="text-sm text-gray-500 mt-1">보류 상태인 업무를 관리합니다.</p>
      </div>
    </div>

    <!-- 통계 카드 -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4">
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-yellow-600">{{ stats.total }}</div>
        <div class="text-sm text-gray-500">전체 보류</div>
      </div>
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-gray-700">{{ stats.avgDays }}일</div>
        <div class="text-sm text-gray-500">평균 보류 기간</div>
      </div>
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-orange-500">{{ stats.longPending }}</div>
        <div class="text-sm text-gray-500">7일 이상 보류</div>
      </div>
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-red-500">{{ stats.urgentHigh }}</div>
        <div class="text-sm text-gray-500">긴급/높음</div>
      </div>
    </div>

    <!-- 필터 영역 -->
    <div class="bg-white rounded-lg border border-gray-200 p-4 mb-4">
      <div class="flex flex-wrap items-center gap-4">
        <!-- 보드 필터 -->
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-gray-700">보드:</span>
          <Select
            v-model="selectedBoardId"
            :options="boardOptions"
            size="sm"
            class="w-40"
          />
        </div>

        <div class="ml-auto text-sm text-gray-500">
          {{ pendingItems.length }}건 / 총 {{ totalElements }}건
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
      <template v-else-if="pendingItems.length === 0">
        <EmptyState
          title="보류된 업무가 없습니다"
          description="현재 보류 상태인 업무가 없습니다."
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
                  보류 기간
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap min-w-[250px]">
                  작업 내용
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  우선순위
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  담당자
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  보류일
                </th>
                <th class="px-4 h-10 text-center text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  처리
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr
                v-for="item in pendingItems"
                :key="item.itemId"
                class="group hover:bg-gray-50 transition-colors cursor-pointer"
                @click="handleItemClick(item)"
              >
                <!-- 보류 기간 -->
                <td class="px-4 h-12">
                  <div class="flex items-center gap-1.5">
                    <span
                      class="inline-flex items-center justify-center min-w-[40px] px-2 py-1 text-[13px] font-bold rounded"
                      :class="[
                        getPendingDays(item) >= 14
                          ? 'bg-red-100 text-red-700'
                          : getPendingDays(item) >= 7
                            ? 'bg-orange-100 text-orange-700'
                            : 'bg-yellow-100 text-yellow-700'
                      ]"
                    >
                      {{ getPendingDays(item) }}일
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

                <!-- 우선순위 -->
                <td class="px-4 h-12">
                  <Badge :variant="getPriorityVariant(item.priority)" size="sm">
                    {{ getPriorityText(item.priority) }}
                  </Badge>
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

                <!-- 보류일 -->
                <td class="px-4 h-12 text-[13px] text-gray-600 whitespace-nowrap">
                  {{ formatDate(item.updatedAt || item.createdAt) }}
                </td>

                <!-- 처리 버튼 -->
                <td class="px-4 h-12 text-center" @click.stop>
                  <div class="flex items-center justify-center gap-1">
                    <button
                      class="px-2.5 py-1 text-[12px] font-medium text-white bg-blue-500 hover:bg-blue-600 rounded transition-colors"
                      title="재개"
                      @click="handleResume(item)"
                    >
                      재개
                    </button>
                    <button
                      class="px-2.5 py-1 text-[12px] font-medium text-white bg-green-500 hover:bg-green-600 rounded transition-colors"
                      title="완료"
                      @click="handleComplete(item)"
                    >
                      완료
                    </button>
                    <button
                      class="p-1 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                      title="삭제"
                      @click="handleDelete(item)"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
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
