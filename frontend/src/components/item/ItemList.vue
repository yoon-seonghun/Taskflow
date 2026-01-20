<script setup lang="ts">
/**
 * 리스트 뷰 컴포넌트
 * - 단순 목록 형태
 * - 빠른 스캔에 최적화
 * - 모바일 최적화
 * - 드래그 앤 드롭 정렬 지원
 */
import { ref, computed, watch } from 'vue'
import { useItemStore } from '@/stores/item'
import { useSlideOver } from '@/composables/useSlideOver'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import { useSortableDrag } from '@/composables/useSortableDrag'
import { Spinner, EmptyState } from '@/components/common'
import ItemListRow from './ItemListRow.vue'
import { isItemOverdue } from '@/utils/item'
import type { Item } from '@/types/item'

type StatusFilter = 'all' | 'not_started' | 'in_progress' | 'overdue' | 'pending'

interface Props {
  boardId: number
  filter?: StatusFilter
  groupId?: number | null
}

const props = withDefaults(defineProps<Props>(), {
  filter: 'all',
  groupId: null
})

const emit = defineEmits<{
  (e: 'itemClick', item: Item): void
}>()

const itemStore = useItemStore()
const { openItemDetail } = useSlideOver()
const toast = useToast()
const confirm = useConfirm()

// 상태
const isLoading = ref(false)
const selectedItemId = ref<number | null>(null)
const showCompleted = ref(false)

// 드래그 중 로컬 순서 (useSortableDrag에서 사용)
const localItems = ref<Item[] | null>(null)

// 활성 아이템 목록 (필터 적용, 정렬 포함)
const activeItems = computed({
  get() {
    // 드래그 중에는 로컬 순서 사용
    if (localItems.value) {
      return localItems.value
    }

    let items = itemStore.activeItems

    // 그룹 필터 적용
    if (props.groupId !== null && props.groupId !== undefined) {
      items = items.filter(i => i.groupId === props.groupId)
    }

    // 상태 필터 적용
    let filtered: Item[]
    switch (props.filter) {
      case 'not_started':
        filtered = items.filter(i => i.status === 'NOT_STARTED')
        break
      case 'in_progress':
        filtered = items.filter(i => i.status === 'IN_PROGRESS')
        break
      case 'overdue':
        filtered = items.filter(i => isItemOverdue(i))
        break
      case 'pending':
        filtered = items.filter(i => i.status === 'PENDING')
        break
      default:
        filtered = [...items]
    }

    // sortOrder 기준 정렬
    return [...filtered].sort((a, b) => (a.sortOrder ?? 999999) - (b.sortOrder ?? 999999))
  },
  set(newItems: Item[]) {
    // useSortableDrag에서 드래그 중 로컬 순서 업데이트
    localItems.value = newItems
  }
})

// 완료된 아이템 목록
const completedItems = computed(() => itemStore.completedItems)

// 로딩 상태
const loading = computed(() => itemStore.loading || isLoading.value)

// 데이터 로드
async function loadData() {
  isLoading.value = true
  try {
    await itemStore.fetchItems(props.boardId)
  } catch (error) {
    toast.error('데이터를 불러오는데 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

// 아이템 클릭 핸들러
function handleItemClick(item: Item) {
  selectedItemId.value = item.itemId
  openItemDetail(item.itemId, props.boardId)
  emit('itemClick', item)
}

// 완료 처리 핸들러
async function handleComplete(itemId: number) {
  const success = await itemStore.completeItem(props.boardId, itemId)
  if (success) {
    toast.success('완료 처리되었습니다.')
  } else {
    toast.error('완료 처리에 실패했습니다.')
  }
}

// 삭제 처리 핸들러
async function handleDelete(itemId: number) {
  const confirmed = await confirm.show({
    title: '업무 삭제',
    message: '이 업무를 삭제하시겠습니까?',
    confirmText: '삭제',
    confirmType: 'danger'
  })

  if (confirmed) {
    const success = await itemStore.deleteItem(props.boardId, itemId)
    if (success) {
      toast.success('삭제되었습니다.')
    } else {
      toast.error('삭제에 실패했습니다.')
    }
  }
}

// 완료 목록 토글
function toggleCompleted() {
  showCompleted.value = !showCompleted.value
}

// 드래그 앤 드롭 정렬
const {
  draggedIndex,
  dragOverIndex,
  getDragClass,
  handleDragStart,
  handleDragOver,
  handleDragLeave,
  handleDragEnd: baseDragEnd,
  handleDrop
} = useSortableDrag<Item>({
  items: activeItems,
  itemKey: 'itemId',
  onReorder: async (reorderedItems, fromIndex, toIndex) => {
    if (!reorderedItems[toIndex]) return

    const itemId = reorderedItems[toIndex].itemId
    // 새 sortOrder 계산
    const newSortOrder = toIndex

    // API 호출
    const success = await itemStore.reorderItem(props.boardId, itemId, newSortOrder)
    if (success) {
      toast.success('순서가 변경되었습니다.')
    } else {
      toast.error('순서 변경에 실패했습니다.')
    }
  }
})

// 드래그 종료 시 localItems 초기화
function handleDragEndWrapper(event: DragEvent, index: number) {
  baseDragEnd(event)
  // 드래그 완료 후 로컬 순서 초기화 (store에서 가져온 순서 사용)
  setTimeout(() => {
    localItems.value = null
  }, 100)
}

// boardId 변경 시 데이터 재로드
watch(() => props.boardId, () => {
  loadData()
}, { immediate: true })
</script>

<template>
  <div class="h-full flex flex-col bg-white rounded-lg border border-gray-200 overflow-hidden dark:bg-gray-800 dark:border-gray-700">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <Spinner size="lg" />
    </div>

    <!-- 빈 상태 -->
    <EmptyState
      v-else-if="activeItems.length === 0 && completedItems.length === 0"
      title="업무가 없습니다"
      description="새 업무를 추가해보세요."
      icon="clipboard"
      class="flex-1"
    />

    <!-- 리스트 -->
    <template v-else>
      <div class="flex-1 overflow-y-auto">
        <!-- 활성 아이템 목록 -->
        <div v-if="activeItems.length > 0">
          <ItemListRow
            v-for="(item, index) in activeItems"
            :key="item.itemId"
            :item="item"
            :selected="selectedItemId === item.itemId"
            draggable
            :drag-class="getDragClass(index)"
            :is-drag-over="dragOverIndex === index"
            @click="handleItemClick"
            @complete="handleComplete"
            @delete="handleDelete"
            @dragstart="handleDragStart(index, $event)"
            @dragover="handleDragOver(index, $event)"
            @dragleave="handleDragLeave"
            @dragend="handleDragEndWrapper($event, index)"
            @drop="handleDrop(index, $event)"
          />
        </div>

        <!-- 활성 아이템 없음 -->
        <div v-else class="px-4 py-8 text-center text-gray-500 text-[13px] dark:text-gray-400">
          진행 중인 업무가 없습니다.
        </div>

        <!-- 완료된 아이템 섹션 -->
        <div v-if="completedItems.length > 0" class="border-t border-gray-200 dark:border-gray-700">
          <button
            class="w-full px-4 py-3 flex items-center justify-between text-[13px] text-gray-600 hover:bg-gray-50 transition-colors dark:text-gray-300 dark:hover:bg-gray-700"
            @click="toggleCompleted"
          >
            <span class="flex items-center gap-2">
              <svg
                class="w-4 h-4 transition-transform"
                :class="{ 'rotate-90': showCompleted }"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
              완료됨
            </span>
            <span class="text-gray-400 dark:text-gray-500">{{ completedItems.length }}</span>
          </button>

          <!-- 완료된 아이템 목록 -->
          <Transition
            enter-active-class="transition-all duration-200 ease-out"
            enter-from-class="opacity-0 max-h-0"
            enter-to-class="opacity-100 max-h-[2000px]"
            leave-active-class="transition-all duration-150 ease-in"
            leave-from-class="opacity-100 max-h-[2000px]"
            leave-to-class="opacity-0 max-h-0"
          >
            <div v-if="showCompleted" class="overflow-hidden bg-gray-50 dark:bg-gray-900/50">
              <ItemListRow
                v-for="item in completedItems"
                :key="item.itemId"
                :item="item"
                :selected="selectedItemId === item.itemId"
                @click="handleItemClick"
                @complete="handleComplete"
                @delete="handleDelete"
              />
            </div>
          </Transition>
        </div>
      </div>

      <!-- 하단 요약 -->
      <div class="flex-shrink-0 px-4 py-2 border-t border-gray-200 bg-gray-50 dark:border-gray-700 dark:bg-gray-900/50">
        <div class="flex items-center justify-between text-[12px] text-gray-500 dark:text-gray-400">
          <span>{{ activeItems.length }}개 진행 중</span>
          <span>{{ completedItems.length }}개 완료</span>
        </div>
      </div>
    </template>
  </div>
</template>
