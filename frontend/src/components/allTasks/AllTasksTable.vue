<script setup lang="ts">
/**
 * 전체 업무 테이블 컴포넌트
 * - 트리/평면/접기 표시 모드 지원
 * - 출처 배지 표시
 * - 인라인 편집 없음 (조회 전용)
 */
import { computed } from 'vue'
import { Spinner, EmptyState } from '@/components/common'
import SourceBadge from './SourceBadge.vue'
import ItemBadges from '@/components/item/ItemBadges.vue'
import type { AllItemResponse, ChildDisplayMode } from '@/types/allTasks'

interface Props {
  /** 아이템 목록 */
  items: AllItemResponse[]
  /** 로딩 상태 */
  loading?: boolean
  /** 하위 업무 표시 모드 */
  childDisplayMode?: ChildDisplayMode
  /** 확장된 아이템 ID Set */
  expandedIds?: Set<number>
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  childDisplayMode: 'tree',
  expandedIds: () => new Set()
})

const emit = defineEmits<{
  (e: 'item-click', item: AllItemResponse): void
  (e: 'toggle-expand', itemId: number): void
}>()

// 상태 라벨
const statusLabels: Record<string, string> = {
  NOT_STARTED: '시작전',
  IN_PROGRESS: '진행중',
  PENDING: '대기중',
  COMPLETED: '완료',
  DELETED: '삭제'
}

// 상태 색상
const statusColors: Record<string, string> = {
  NOT_STARTED: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
  IN_PROGRESS: 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400',
  PENDING: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/50 dark:text-yellow-400',
  COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400',
  DELETED: 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-400'
}

// 우선순위 라벨
const priorityLabels: Record<string, string> = {
  URGENT: '긴급',
  HIGH: '높음',
  MEDIUM: '보통',
  LOW: '낮음'
}

// 우선순위 색상
const priorityColors: Record<string, string> = {
  URGENT: 'text-red-600 dark:text-red-400',
  HIGH: 'text-orange-600 dark:text-orange-400',
  MEDIUM: 'text-blue-600 dark:text-blue-400',
  LOW: 'text-gray-600 dark:text-gray-400'
}

// 날짜 포맷
function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 아이템이 마감 지연인지 확인
function isOverdue(item: AllItemResponse): boolean {
  if (!item.dueDate || item.status === 'COMPLETED' || item.status === 'DELETED') return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const dueDate = new Date(item.dueDate)
  dueDate.setHours(0, 0, 0, 0)
  return dueDate < today
}

// 아이템 클릭 - 슬라이드 패널만 열기 (페이지 이동 제거)
function onItemClick(item: AllItemResponse): void {
  emit('item-click', item)
}

// 확장/축소 토글
function onToggleExpand(itemId: number, event: Event): void {
  event.stopPropagation()
  emit('toggle-expand', itemId)
}

// 트리 모드에서 들여쓰기 계산
function getIndentStyle(depth: number): Record<string, string> {
  if (props.childDisplayMode === 'flat') return {}
  return {
    paddingLeft: `${depth * 24 + 8}px`
  }
}

// 아이템 확장 여부
function isExpanded(itemId: number): boolean {
  return props.expandedIds.has(itemId)
}

// 하위 업무 있는지 확인
function hasChildren(item: AllItemResponse): boolean {
  return (item.children && item.children.length > 0) || (item.childCount ?? 0) > 0
}

// 트리를 완전히 평탄화 (flat 모드용 - 모든 아이템을 depth 0으로)
function flattenAllItems(items: AllItemResponse[]): Array<{ item: AllItemResponse; depth: number }> {
  const result: Array<{ item: AllItemResponse; depth: number }> = []

  function collectAll(itemList: AllItemResponse[]) {
    for (const item of itemList) {
      result.push({ item, depth: 0 })
      if (item.children && item.children.length > 0) {
        collectAll(item.children)
      }
    }
  }

  collectAll(items)
  return result
}

// 트리 모드에서 표시할 아이템 목록 (재귀)
function flattenItems(items: AllItemResponse[], depth: number = 0): Array<{ item: AllItemResponse; depth: number }> {
  const result: Array<{ item: AllItemResponse; depth: number }> = []

  for (const item of items) {
    result.push({ item, depth })

    // tree 모드: 항상 확장된 아이템의 하위 업무 표시
    // collapse 모드: 확장된 것만 하위 표시
    if ((props.childDisplayMode === 'tree' || props.childDisplayMode === 'collapse')
        && item.children && item.children.length > 0
        && isExpanded(item.itemId)) {
      result.push(...flattenItems(item.children, depth + 1))
    }
  }

  return result
}

// 표시할 아이템 목록
const displayItems = computed(() => {
  if (props.childDisplayMode === 'flat') {
    // 평면 모드: 모든 아이템을 depth 0으로 평탄화
    return flattenAllItems(props.items)
  }
  // tree/collapse 모드: 확장 상태에 따라 표시
  return flattenItems(props.items, 0)
})

// 소유자 이름 가져오기 (공유/배당 출처용)
function getOwnerName(item: AllItemResponse): string | undefined {
  if (item.sourceType === 'BOARD_SHARED' || item.sourceType === 'ITEM_SHARED') {
    return item.sharedByUserName || item.sharedByUsername
  }
  if (item.sourceType === 'ASSIGNED') {
    return item.assignedByName
  }
  return undefined
}
</script>

<template>
  <div class="h-full overflow-hidden flex flex-col">
    <!-- 로딩 -->
    <div v-if="loading && items.length === 0" class="flex-1 flex items-center justify-center">
      <Spinner size="lg" />
    </div>

    <!-- 빈 상태 -->
    <EmptyState
      v-else-if="items.length === 0"
      title="업무가 없습니다"
      description="조건에 맞는 업무가 없습니다."
      icon="clipboard"
    />

    <!-- 테이블 -->
    <div v-else class="flex-1 overflow-auto">
      <table class="w-full border-collapse">
        <!-- 헤더 -->
        <thead class="sticky top-0 z-10 bg-gray-50 dark:bg-gray-800">
          <tr class="text-xs text-gray-500 dark:text-gray-400">
            <th class="text-left py-2 px-3 font-medium w-10">
              <!-- 확장/축소 버튼 공간 -->
            </th>
            <th class="text-left py-2 px-3 font-medium min-w-[200px]">업무 내용</th>
            <th class="text-center py-2 px-2 font-medium w-10">출처</th>
            <th class="text-left py-2 px-3 font-medium w-16">상태</th>
            <th class="text-left py-2 px-3 font-medium w-16">우선순위</th>
            <th class="text-left py-2 px-3 font-medium w-20">담당자</th>
            <th class="text-left py-2 px-3 font-medium w-20">요청일</th>
            <th class="text-left py-2 px-3 font-medium w-20">마감일</th>
            <th class="text-left py-2 px-3 font-medium w-24">보드</th>
          </tr>
        </thead>

        <!-- 바디 -->
        <tbody class="text-sm">
          <tr
            v-for="{ item, depth } in displayItems"
            :key="item.itemId"
            class="border-t border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800/50 cursor-pointer transition-colors"
            :class="{
              'bg-green-50/30 dark:bg-green-900/10': item.status === 'COMPLETED',
              'bg-red-50/30 dark:bg-red-900/10': item.status === 'DELETED'
            }"
            @click="onItemClick(item)"
          >
            <!-- 확장/축소 버튼 -->
            <td class="py-2 px-1 text-center">
              <button
                v-if="hasChildren(item) && childDisplayMode !== 'flat'"
                type="button"
                class="p-1 rounded hover:bg-gray-200 dark:hover:bg-gray-700"
                @click="onToggleExpand(item.itemId, $event)"
              >
                <svg
                  class="w-4 h-4 text-gray-500 dark:text-gray-400 transition-transform"
                  :class="{ 'rotate-90': isExpanded(item.itemId) }"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </button>
              <!-- 하위 업무 표시 (depth > 0) -->
              <span
                v-else-if="depth > 0"
                class="inline-block w-4 h-4 text-gray-300 dark:text-gray-600"
              >
                └
              </span>
            </td>

            <!-- 업무 내용 -->
            <td
              class="py-2 px-3"
              :style="getIndentStyle(depth)"
            >
              <div class="flex items-center gap-2">
                <!-- 하위 업무 아이콘 -->
                <svg
                  v-if="depth > 0"
                  class="w-3.5 h-3.5 text-gray-400 dark:text-gray-500 flex-shrink-0"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
                </svg>

                <!-- 제목 -->
                <span
                  class="truncate font-medium text-gray-900 dark:text-gray-100"
                  :class="{ 'line-through opacity-60': item.status === 'COMPLETED' || item.status === 'DELETED' }"
                  :title="item.title"
                >
                  {{ item.title }}
                </span>

                <!-- 하위 업무 수 -->
                <span
                  v-if="hasChildren(item) && !isExpanded(item.itemId)"
                  class="text-xs text-gray-400 dark:text-gray-500"
                >
                  ({{ item.childCount || item.children?.length || 0 }})
                </span>

                <!-- 배지 (공유/배당/이관) -->
                <ItemBadges :item="item as any" size="sm" :show-owner-name="false" />
              </div>
            </td>

            <!-- 출처 -->
            <td class="py-2 px-2 text-center">
              <SourceBadge
                :source-type="item.sourceType"
                :board-color="item.boardColor"
                :owner-name="getOwnerName(item)"
                size="sm"
                :show-icon="true"
                :show-label="false"
              />
            </td>

            <!-- 상태 -->
            <td class="py-2 px-3">
              <span
                :class="['inline-block px-1.5 py-0.5 text-[10px] font-medium rounded', statusColors[item.status]]"
              >
                {{ statusLabels[item.status] }}
              </span>
            </td>

            <!-- 우선순위 -->
            <td class="py-2 px-3">
              <span
                v-if="item.priority"
                :class="['text-xs font-medium', priorityColors[item.priority]]"
              >
                {{ priorityLabels[item.priority] }}
              </span>
              <span v-else class="text-gray-400 dark:text-gray-500">-</span>
            </td>

            <!-- 담당자 -->
            <td class="py-2 px-3">
              <div v-if="item.assigneeUserName || item.assigneeUsername" class="flex items-center gap-1">
                <span class="w-5 h-5 rounded-full bg-gray-200 dark:bg-gray-600 flex items-center justify-center text-[10px] font-medium text-gray-600 dark:text-gray-300">
                  {{ (item.assigneeUserName || item.assigneeUsername || '?')[0] }}
                </span>
                <span class="text-xs text-gray-700 dark:text-gray-300 truncate max-w-[60px]">
                  {{ item.assigneeUserName || item.assigneeUsername }}
                </span>
              </div>
              <span v-else class="text-gray-400 dark:text-gray-500">-</span>
            </td>

            <!-- 요청일 -->
            <td class="py-2 px-3 text-xs text-gray-600 dark:text-gray-400">
              {{ formatDate(item.requestDate) }}
            </td>

            <!-- 마감일 -->
            <td class="py-2 px-3">
              <span
                :class="[
                  'text-xs',
                  isOverdue(item) ? 'text-red-600 dark:text-red-400 font-medium' : 'text-gray-600 dark:text-gray-400'
                ]"
              >
                {{ formatDate(item.dueDate) }}
                <span v-if="isOverdue(item)" class="ml-1">!</span>
              </span>
            </td>

            <!-- 보드 -->
            <td class="py-2 px-3">
              <div class="flex items-center gap-1">
                <span
                  v-if="item.boardColor"
                  class="w-2 h-2 rounded-full flex-shrink-0"
                  :style="{ backgroundColor: item.boardColor }"
                />
                <span class="text-xs text-gray-600 dark:text-gray-400 truncate max-w-[80px]" :title="item.boardName">
                  {{ item.boardName }}
                </span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 로딩 오버레이 (추가 로딩) -->
      <div v-if="loading && items.length > 0" class="py-4 text-center">
        <Spinner size="sm" />
      </div>
    </div>
  </div>
</template>
