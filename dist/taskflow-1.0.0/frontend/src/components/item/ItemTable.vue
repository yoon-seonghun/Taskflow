<script setup lang="ts">
/**
 * 아이템 테이블 뷰 컴포넌트
 * - 테이블 형식으로 아이템 목록 표시
 * - 컬럼 헤더 클릭 시 속성 관리
 * - 인라인 편집 지원
 * - 최소 15개 항목 표시 (Compact UI)
 */
import { ref, computed, watch } from 'vue'
import { useItemStore } from '@/stores/item'
import { usePropertyStore } from '@/stores/property'
import { useBoardStore } from '@/stores/board'
import { useDepartmentStore } from '@/stores/department'
import { useSlideOver } from '@/composables/useSlideOver'
import type { UserOption, DepartmentOption } from '@/components/common'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import { Spinner, EmptyState } from '@/components/common'
import PropertyHeader from '@/components/property/PropertyHeader.vue'
import ItemRow from './ItemRow.vue'
import { isItemOverdue } from '@/utils/item'
import type { Item } from '@/types/item'
import type { PropertyDef } from '@/types/property'

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
const boardStore = useBoardStore()
const departmentStore = useDepartmentStore()
const { openItemDetail } = useSlideOver()
const toast = useToast()
const confirm = useConfirm()

// 담당자 선택용 사용자 목록 (Board의 sharedUsers)
const sharedUsers = computed<UserOption[]>(() => {
  const users = boardStore.currentBoard?.sharedUsers || []
  return users.map(u => ({
    userId: u.userId,
    userName: u.userName,
    departmentId: undefined,
    departmentName: u.departmentName
  }))
})

// 부서 목록 (담당자 필터용)
const departments = computed<DepartmentOption[]>(() => {
  return departmentStore.activeFlatDepartments.map(d => ({
    departmentId: d.departmentId,
    departmentName: d.departmentName
  }))
})

// 상태
const selectedItemId = ref<number | null>(null)
const isLoading = ref(false)

// 컬럼 너비 (기본값)
const propertyWidths = ref<Record<number, number>>({})
const defaultColumnWidths = {
  title: 250,
  status: 96,
  priority: 96,
  startTime: 128,
  endTime: 128,
  assignee: 96,
  comments: 64,
  actions: 80
}

// 아이템 목록 (필터 적용)
const items = computed(() => {
  const activeItems = itemStore.activeItems

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

// 완료/삭제된 아이템 (당일)
const todayCompletedItems = computed(() => itemStore.todayCompletedItems)

// 표시할 속성 목록
const visibleProperties = computed(() => propertyStore.sortedProperties)

// 로딩 상태
const loading = computed(() => itemStore.loading || isLoading.value)

// 데이터 로드
async function loadData() {
  isLoading.value = true
  try {
    await Promise.all([
      itemStore.fetchItems(props.boardId),
      propertyStore.fetchProperties(props.boardId),
      departmentStore.fetchFlatDepartments({ useYn: 'Y' })
    ])

    // 속성 너비 초기화
    visibleProperties.value.forEach(prop => {
      if (!propertyWidths.value[prop.propertyId]) {
        propertyWidths.value[prop.propertyId] = 150
      }
    })
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

// 아이템 업데이트 핸들러
async function handleItemUpdate(itemId: number, field: string, value: unknown) {
  const success = await itemStore.updateItem(props.boardId, itemId, { [field]: value })
  if (!success) {
    toast.error('업데이트에 실패했습니다.')
  }
}

// 속성값 업데이트 핸들러
async function handlePropertyUpdate(itemId: number, propertyId: number, value: unknown) {
  const success = await itemStore.updateItemProperty(props.boardId, itemId, propertyId, value)
  if (!success) {
    toast.error('속성값 업데이트에 실패했습니다.')
  }
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

// 복원 처리 핸들러
async function handleRestore(itemId: number) {
  const success = await itemStore.restoreItem(props.boardId, itemId)
  if (success) {
    toast.success('복원되었습니다.')
  } else {
    toast.error('복원에 실패했습니다.')
  }
}

// 속성 이름 변경
// NOTE: 속성 관리 기능은 설정 > 코드 관리 메뉴에서 제공
function handlePropertyRename(propertyId: number) {
  toast.info('속성 이름 변경 기능 - 추후 구현')
}

// 속성 타입 변경
function handlePropertyTypeChange(propertyId: number) {
  toast.info('속성 타입 변경 기능 - 추후 구현')
}

// 속성 왼쪽 이동
async function handlePropertyMoveLeft(propertyId: number) {
  const properties = [...visibleProperties.value]
  const index = properties.findIndex(p => p.propertyId === propertyId)
  if (index > 0) {
    const newSortOrder = properties[index - 1].sortOrder ?? (index - 1)
    const currentSortOrder = properties[index].sortOrder ?? index
    await propertyStore.reorderProperty(propertyId, newSortOrder)
    await propertyStore.reorderProperty(properties[index - 1].propertyId, currentSortOrder)
  }
}

// 속성 오른쪽 이동
async function handlePropertyMoveRight(propertyId: number) {
  const properties = [...visibleProperties.value]
  const index = properties.findIndex(p => p.propertyId === propertyId)
  if (index < properties.length - 1) {
    const newSortOrder = properties[index + 1].sortOrder ?? (index + 1)
    const currentSortOrder = properties[index].sortOrder ?? index
    await propertyStore.reorderProperty(propertyId, newSortOrder)
    await propertyStore.reorderProperty(properties[index + 1].propertyId, currentSortOrder)
  }
}

// 속성 숨기기
async function handlePropertyHide(propertyId: number) {
  const success = await propertyStore.togglePropertyVisibility(propertyId)
  if (success) {
    toast.success('속성이 숨겨졌습니다.')
  }
}

// 속성 삭제
async function handlePropertyDelete(propertyId: number) {
  const confirmed = await confirm.show({
    title: '속성 삭제',
    message: '이 속성을 삭제하시겠습니까? 해당 속성의 모든 데이터가 삭제됩니다.',
    confirmText: '삭제',
    confirmType: 'danger'
  })

  if (confirmed) {
    const success = await propertyStore.deleteProperty(propertyId)
    if (success) {
      toast.success('속성이 삭제되었습니다.')
    } else {
      toast.error('속성 삭제에 실패했습니다.')
    }
  }
}

// 새 속성 추가
function handleAddProperty() {
  toast.info('새 속성 추가 기능 - 추후 구현')
}

// 정렬 핸들러
function handleSort(propertyId: number, direction: 'asc' | 'desc') {
  toast.info(`정렬: ${direction} - 추후 구현`)
}

// boardId 변경 시 데이터 재로드
watch(() => props.boardId, () => {
  loadData()
}, { immediate: true })
</script>

<template>
  <div class="flex flex-col h-full bg-white rounded-lg border border-gray-200 overflow-hidden">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <Spinner size="lg" />
    </div>

    <!-- 빈 상태 -->
    <template v-else-if="items.length === 0">
      <EmptyState
        title="업무가 없습니다"
        description="새 업무를 추가해보세요."
        icon="clipboard"
        class="flex-1"
      />
    </template>

    <!-- 테이블 -->
    <template v-else>
      <div class="flex-1 overflow-auto">
        <!-- 테이블 헤더 -->
        <div class="sticky top-0 z-10 flex bg-gray-50 border-b border-gray-200">
          <!-- 제목 헤더 (고정) -->
          <div
            class="flex-1 min-w-[200px] px-2 h-8 flex items-center text-[13px] font-medium text-gray-600 border-r border-gray-200"
          >
            <svg class="w-4 h-4 mr-1.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
            </svg>
            업무내용
          </div>

          <!-- 동적 속성 헤더 -->
          <PropertyHeader
            v-for="property in visibleProperties"
            :key="property.propertyId"
            :property="property"
            :width="propertyWidths[property.propertyId] || 150"
            @rename="handlePropertyRename"
            @change-type="handlePropertyTypeChange"
            @move-left="handlePropertyMoveLeft"
            @move-right="handlePropertyMoveRight"
            @hide="handlePropertyHide"
            @delete="handlePropertyDelete"
            @add-property="handleAddProperty"
            @sort="handleSort"
          />

          <!-- 댓글 헤더 -->
          <div class="w-16 min-w-[64px] px-2 h-8 flex items-center justify-center text-[13px] font-medium text-gray-600 border-r border-gray-200 bg-gray-50">
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
          </div>

          <!-- 액션 헤더 -->
          <div class="w-20 min-w-[80px] px-2 h-8 flex items-center justify-center text-[13px] font-medium text-gray-600 bg-gray-50">
            액션
          </div>
        </div>

        <!-- 테이블 바디 -->
        <div class="divide-y divide-gray-200">
          <ItemRow
            v-for="item in items"
            :key="item.itemId"
            :item="item"
            :properties="visibleProperties"
            :property-widths="propertyWidths"
            :shared-users="sharedUsers"
            :departments="departments"
            :selected="selectedItemId === item.itemId"
            @click="handleItemClick"
            @update="handleItemUpdate"
            @update-property="handlePropertyUpdate"
            @complete="handleComplete"
            @delete="handleDelete"
            @restore="handleRestore"
          />
        </div>

        <!-- 당일 완료/삭제된 아이템 (축소 가능) -->
        <div v-if="todayCompletedItems.length > 0" class="border-t border-gray-200">
          <details class="group">
            <summary class="px-4 py-2 text-[13px] text-gray-500 cursor-pointer hover:bg-gray-50 flex items-center gap-2">
              <svg
                class="w-4 h-4 transition-transform group-open:rotate-90"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
              오늘 처리된 업무 ({{ todayCompletedItems.length }}건)
            </summary>
            <div class="bg-gray-50">
              <ItemRow
                v-for="item in todayCompletedItems"
                :key="item.itemId"
                :item="item"
                :properties="visibleProperties"
                :property-widths="propertyWidths"
                :shared-users="sharedUsers"
                :departments="departments"
                :selected="selectedItemId === item.itemId"
                @click="handleItemClick"
                @update="handleItemUpdate"
                @update-property="handlePropertyUpdate"
                @complete="handleComplete"
                @delete="handleDelete"
                @restore="handleRestore"
              />
            </div>
          </details>
        </div>
      </div>
    </template>
  </div>
</template>
