<script setup lang="ts">
/**
 * 칸반 뷰 컴포넌트
 * - 상태/우선순위/담당자별 그룹핑
 * - 드래그 앤 드롭으로 상태 변경
 * - 카드 클릭 시 상세 패널
 */
import { ref, computed, watch } from 'vue'
import { useItemStore } from '@/stores/item'
import { usePropertyStore } from '@/stores/property'
import { useSlideOver } from '@/composables/useSlideOver'
import { useToast } from '@/composables/useToast'
import { Select, Spinner, EmptyState } from '@/components/common'
import KanbanColumn from './KanbanColumn.vue'
import { isItemOverdue } from '@/utils/item'
import type { Item, ItemStatus, Priority } from '@/types/item'

type GroupBy = 'status' | 'priority' | 'assignee' | 'group'
type StatusFilter = 'all' | 'not_started' | 'in_progress' | 'overdue' | 'pending'

interface Props {
  boardId: number
  filter?: StatusFilter
}

const props = withDefaults(defineProps<Props>(), {
  filter: 'all'
})

const emit = defineEmits<{
  (e: 'itemClick', item: Item): void
}>()

const itemStore = useItemStore()
const propertyStore = usePropertyStore()
const { openItemDetail, openItemCreate } = useSlideOver()
const toast = useToast()

// 상태
const isLoading = ref(false)
const groupBy = ref<GroupBy>('status')
const draggingItem = ref<Item | null>(null)

// 그룹 기준 옵션
const groupByOptions = [
  { value: 'status', label: '상태' },
  { value: 'priority', label: '우선순위' },
  { value: 'assignee', label: '담당자' },
  { value: 'group', label: '그룹' }
]

// 상태 정의
const statusConfig: Array<{ id: ItemStatus; title: string; color: string }> = [
  { id: 'NOT_STARTED', title: '시작전', color: '#6B7280' },
  { id: 'IN_PROGRESS', title: '진행중', color: '#3B82F6' },
  { id: 'PENDING', title: '대기', color: '#F59E0B' },
  { id: 'COMPLETED', title: '완료', color: '#10B981' },
  { id: 'DELETED', title: '삭제', color: '#EF4444' }
]

// 우선순위 정의
const priorityConfig: Array<{ id: Priority; title: string; color: string }> = [
  { id: 'URGENT', title: '긴급', color: '#EF4444' },
  { id: 'HIGH', title: '높음', color: '#F97316' },
  { id: 'NORMAL', title: '보통', color: '#3B82F6' },
  { id: 'LOW', title: '낮음', color: '#6B7280' }
]

// 아이템 목록 (필터 적용)
const items = computed(() => {
  const activeItems = itemStore.items.filter(i => i.status !== 'DELETED' && i.status !== 'COMPLETED')

  switch (props.filter) {
    case 'not_started':
      return activeItems.filter(i => i.status === 'NOT_STARTED')
    case 'in_progress':
      return activeItems.filter(i => i.status === 'IN_PROGRESS')
    case 'overdue':
      return activeItems.filter(i => isItemOverdue(i))
    case 'pending':
      return activeItems.filter(i => i.status === 'PENDING')
    default:
      return activeItems
  }
})

// 담당자 목록 (아이템에서 추출)
const assignees = computed(() => {
  const assigneeMap = new Map<number, { id: number; name: string }>()
  items.value.forEach(item => {
    if (item.assigneeId && item.assigneeName) {
      assigneeMap.set(item.assigneeId, { id: item.assigneeId, name: item.assigneeName })
    }
  })
  return Array.from(assigneeMap.values())
})

// 그룹 목록 (아이템에서 추출)
const groups = computed(() => {
  const groupMap = new Map<number, { id: number; name: string; color?: string }>()
  items.value.forEach(item => {
    if (item.groupId && item.groupName) {
      groupMap.set(item.groupId, { id: item.groupId, name: item.groupName, color: item.groupColor })
    }
  })
  return Array.from(groupMap.values())
})

// 컬럼 설정 (그룹 기준에 따라)
const columns = computed(() => {
  switch (groupBy.value) {
    case 'status':
      return statusConfig.map(s => ({
        id: s.id,
        title: s.title,
        color: s.color,
        items: items.value.filter(item => item.status === s.id)
      }))

    case 'priority':
      return priorityConfig.map(p => ({
        id: p.id,
        title: p.title,
        color: p.color,
        items: items.value.filter(item => item.priority === p.id)
      }))

    case 'assignee':
      const assigneeColumns = [
        {
          id: 'unassigned',
          title: '미지정',
          color: '#9CA3AF',
          items: items.value.filter(item => !item.assigneeId)
        },
        ...assignees.value.map(a => ({
          id: a.id,
          title: a.name,
          color: '#6366F1',
          items: items.value.filter(item => item.assigneeId === a.id)
        }))
      ]
      return assigneeColumns

    case 'group':
      const groupColumns = [
        {
          id: 'no-group',
          title: '미분류',
          color: '#9CA3AF',
          items: items.value.filter(item => !item.groupId)
        },
        ...groups.value.map(g => ({
          id: g.id,
          title: g.name,
          color: g.color || '#6366F1',
          items: items.value.filter(item => item.groupId === g.id)
        }))
      ]
      return groupColumns

    default:
      return []
  }
})

// 로딩 상태
const loading = computed(() => itemStore.loading || isLoading.value)

// 데이터 로드
async function loadData() {
  isLoading.value = true
  try {
    await Promise.all([
      itemStore.fetchItems(props.boardId),
      propertyStore.fetchProperties(props.boardId)
    ])
  } catch (error) {
    toast.error('데이터를 불러오는데 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

// 아이템 클릭
function handleItemClick(item: Item) {
  openItemDetail(item.itemId, props.boardId)
  emit('itemClick', item)
}

// 드롭 핸들러 (상태/우선순위/담당자/그룹 변경)
async function handleDrop(columnId: string | number, item: Item) {
  if (!item.itemId) return

  let updateData: Partial<Item> = {}

  switch (groupBy.value) {
    case 'status':
      if (item.status === columnId) return
      updateData = { status: columnId as ItemStatus }
      break

    case 'priority':
      if (item.priority === columnId) return
      updateData = { priority: columnId as Priority }
      break

    case 'assignee':
      const newAssigneeId = columnId === 'unassigned' ? undefined : Number(columnId)
      if (item.assigneeId === newAssigneeId) return
      updateData = { assigneeId: newAssigneeId }
      break

    case 'group':
      const newGroupId = columnId === 'no-group' ? undefined : Number(columnId)
      if (item.groupId === newGroupId) return
      updateData = { groupId: newGroupId }
      break
  }

  // Optimistic Update 적용
  const success = await itemStore.updateItem(props.boardId, item.itemId, updateData)

  if (success) {
    const fieldNames: Record<GroupBy, string> = {
      status: '상태',
      priority: '우선순위',
      assignee: '담당자',
      group: '그룹'
    }
    toast.success(`${fieldNames[groupBy.value]}가 변경되었습니다.`)
  } else {
    toast.error('변경에 실패했습니다.')
  }
}

// 새 아이템 추가
function handleAddItem(columnId: string | number) {
  // 해당 컬럼의 기본값으로 아이템 생성 패널 열기
  const defaultValues: Partial<Item> = {}

  switch (groupBy.value) {
    case 'status':
      defaultValues.status = columnId as ItemStatus
      break
    case 'priority':
      defaultValues.priority = columnId as Priority
      break
    case 'assignee':
      if (columnId !== 'unassigned') {
        defaultValues.assigneeId = Number(columnId)
      }
      break
    case 'group':
      if (columnId !== 'no-group') {
        defaultValues.groupId = Number(columnId)
      }
      break
  }

  openItemCreate(props.boardId, defaultValues.groupId)
}

// 그룹 기준 변경
function handleGroupByChange(value: string | number | null) {
  if (value && typeof value === 'string') {
    groupBy.value = value as GroupBy
  }
}

// boardId 변경 시 데이터 재로드
watch(() => props.boardId, () => {
  loadData()
}, { immediate: true })
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 헤더: 그룹 기준 선택 -->
    <div class="flex-shrink-0 pb-3 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <span class="text-[13px] text-gray-500">그룹 기준:</span>
        <Select
          :model-value="groupBy"
          :options="groupByOptions"
          size="sm"
          class="w-32"
          @update:model-value="handleGroupByChange"
        />
      </div>

      <div class="text-[13px] text-gray-500">
        총 {{ items.length }}개 업무
      </div>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <Spinner size="lg" />
    </div>

    <!-- 빈 상태 -->
    <EmptyState
      v-else-if="items.length === 0"
      title="업무가 없습니다"
      description="새 업무를 추가해보세요."
      icon="clipboard"
      class="flex-1"
    />

    <!-- 칸반 보드 -->
    <div v-else class="flex-1 overflow-x-auto">
      <div class="flex gap-4 h-full pb-4 min-w-max">
        <KanbanColumn
          v-for="column in columns"
          :key="column.id"
          :column-id="column.id"
          :title="column.title"
          :items="column.items"
          :color="column.color"
          collapsible
          @item-click="handleItemClick"
          @drop="handleDrop"
          @add-item="handleAddItem"
        />
      </div>
    </div>
  </div>
</template>
