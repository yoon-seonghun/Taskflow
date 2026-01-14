<script setup lang="ts">
/**
 * 공유받은 업무 메뉴
 * - 다른 사용자가 공유해준 업무 목록 표시 (Cross-board)
 * - 서버 사이드 필터링/페이징
 * - 공유해준 사용자 표시
 * - 우선순위/상태별 필터
 */
import { ref, computed, onMounted, watch } from 'vue'
import { itemApi } from '@/api/item'
import { useBoardStore } from '@/stores/board'
import { useSlideOver } from '@/composables/useSlideOver'
import { useToast } from '@/composables/useToast'
import { Select, Spinner, EmptyState, Badge, Pagination } from '@/components/common'
import ItemBadges from '@/components/item/ItemBadges.vue'
import type { Item, Priority, ItemStatus, CrossBoardSearchRequest } from '@/types/item'

const boardStore = useBoardStore()
const { openItemDetail } = useSlideOver()
const toast = useToast()

// 상태
const loading = ref(false)
const sharedItems = ref<Item[]>([])
const selectedBoardId = ref<number | null>(null)
const priorityFilter = ref<Priority | 'all'>('all')
const statusFilter = ref<ItemStatus | 'all'>('all')

// 페이징 상태
const currentPage = ref(0)
const pageSize = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)

// 통계
const stats = ref({
  total: 0,
  inProgress: 0,
  pending: 0,
  notStarted: 0
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

// 상태 옵션
const statusOptions = [
  { value: 'all', label: '전체 상태' },
  { value: 'NOT_STARTED', label: '시작전' },
  { value: 'IN_PROGRESS', label: '진행중' },
  { value: 'PENDING', label: '보류' }
]

// 데이터 로드
async function loadData() {
  loading.value = true
  try {
    // 보드 목록 로드 (필터 옵션용)
    await boardStore.fetchBoards()

    // 공유받은 업무 조회
    const params: CrossBoardSearchRequest = {
      boardId: selectedBoardId.value || undefined,
      priority: priorityFilter.value !== 'all' ? priorityFilter.value : undefined,
      status: statusFilter.value !== 'all' ? statusFilter.value : undefined,
      page: currentPage.value,
      size: pageSize.value,
      sort: 'createdAt,desc'
    }

    const response = await itemApi.getSharedItems(params)
    if (response.success && response.data) {
      sharedItems.value = response.data.content
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
  const items = sharedItems.value

  stats.value = {
    total: totalElements.value,
    inProgress: items.filter(i => i.status === 'IN_PROGRESS').length,
    pending: items.filter(i => i.status === 'PENDING').length,
    notStarted: items.filter(i => i.status === 'NOT_STARTED').length
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
      const idx = sharedItems.value.findIndex(i => i.itemId === item.itemId)
      if (idx !== -1) {
        sharedItems.value[idx].priority = priority
      }
    } else {
      toast.error('우선순위 변경에 실패했습니다.')
    }
  } catch {
    toast.error('우선순위 변경에 실패했습니다.')
  }
}

// 상태 텍스트
function getStatusText(status: ItemStatus): string {
  const map: Record<ItemStatus, string> = {
    NOT_STARTED: '시작전',
    IN_PROGRESS: '진행중',
    PENDING: '보류',
    COMPLETED: '완료',
    DELETED: '삭제'
  }
  return map[status]
}

// 상태 색상
function getStatusColor(status: ItemStatus): string {
  const map: Record<ItemStatus, string> = {
    NOT_STARTED: 'bg-gray-100 text-gray-600',
    IN_PROGRESS: 'bg-blue-100 text-blue-700',
    PENDING: 'bg-yellow-100 text-yellow-700',
    COMPLETED: 'bg-green-100 text-green-700',
    DELETED: 'bg-red-100 text-red-700'
  }
  return map[status]
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
watch([selectedBoardId, priorityFilter, statusFilter], () => {
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
          공유받은 업무
          <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
          </svg>
        </h2>
        <p class="text-sm text-gray-500 mt-1">다른 사용자가 공유해준 업무를 관리합니다.</p>
      </div>
    </div>

    <!-- 통계 카드 -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4">
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-blue-600">{{ stats.total }}</div>
        <div class="text-sm text-gray-500">전체 공유</div>
      </div>
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-blue-500">{{ stats.inProgress }}</div>
        <div class="text-sm text-gray-500">진행중</div>
      </div>
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-yellow-500">{{ stats.pending }}</div>
        <div class="text-sm text-gray-500">보류</div>
      </div>
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="text-2xl font-bold text-gray-500">{{ stats.notStarted }}</div>
        <div class="text-sm text-gray-500">시작전</div>
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

        <!-- 상태 필터 -->
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-gray-700 whitespace-nowrap">상태:</span>
          <Select
            v-model="statusFilter"
            :options="statusOptions"
            size="sm"
            class="w-32"
          />
        </div>

        <div class="ml-auto text-sm text-gray-500">
          {{ sharedItems.length }}건 / 총 {{ totalElements }}건
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
      <template v-else-if="sharedItems.length === 0">
        <EmptyState
          title="공유받은 업무가 없습니다"
          description="다른 사용자가 업무를 공유하면 여기에 표시됩니다."
          icon="share"
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
                  공유자
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap min-w-[250px]">
                  작업 내용
                </th>
                <th class="px-4 h-10 text-left text-[13px] font-medium text-gray-600 whitespace-nowrap">
                  상태
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
                v-for="item in sharedItems"
                :key="item.itemId"
                class="group hover:bg-gray-50 transition-colors cursor-pointer"
                @click="handleItemClick(item)"
              >
                <!-- 공유자 -->
                <td class="px-4 h-12">
                  <div class="flex items-center gap-2">
                    <div class="w-6 h-6 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-[11px] font-medium">
                      {{ item.sharedByUserName?.charAt(0) || item.ownerName?.charAt(0) || '?' }}
                    </div>
                    <span class="text-[13px] text-gray-700">
                      {{ item.sharedByUserName || item.ownerName || '-' }}
                    </span>
                  </div>
                </td>

                <!-- 작업 내용 -->
                <td class="px-4 h-12">
                  <div class="flex items-center gap-2">
                    <!-- 공유/이관 배지 -->
                    <ItemBadges :item="item" size="sm" :show-owner-name="false" class="flex-shrink-0" />
                    <span class="text-[13px] text-gray-900 truncate max-w-[250px]" :title="item.title">
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

                <!-- 상태 -->
                <td class="px-4 h-12">
                  <span
                    class="inline-flex items-center px-2 py-0.5 text-[12px] font-medium rounded"
                    :class="getStatusColor(item.status)"
                  >
                    {{ getStatusText(item.status) }}
                  </span>
                </td>

                <!-- 마감일 -->
                <td class="px-4 h-12 text-[13px] text-gray-600 whitespace-nowrap">
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
