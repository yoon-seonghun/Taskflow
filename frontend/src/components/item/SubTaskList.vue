<script setup lang="ts">
/**
 * 하위 업무 목록 컴포넌트 (v2.2)
 * - 접기/펼치기 지원
 * - 하위 업무 추가/완료/삭제
 * - 드래그 앤 드롭 순서 변경
 */
import { ref, computed, watch } from 'vue'
import { useItemStore } from '@/stores/item'
import type { Item, SubTaskCreateRequest, ItemStatus } from '@/types/item'

interface Props {
  parentItem: Item
  boardId: number
  expanded?: boolean
  maxDepth?: number
  includeCompleted?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  expanded: false,
  maxDepth: 2,
  includeCompleted: false
})

const emit = defineEmits<{
  (e: 'select', item: Item): void
  (e: 'update'): void
}>()

const itemStore = useItemStore()

// State
const isExpanded = ref(props.expanded)
const children = ref<Item[]>([])
const loading = ref(false)
const showAddForm = ref(false)
const newTaskTitle = ref('')

// 하위 업무 생성 가능 여부
const canCreateChild = computed(() => {
  return (props.parentItem.itemDepth ?? 0) < props.maxDepth
})

// 하위 업무 존재 여부
const hasChildren = computed(() => {
  return props.parentItem.hasChildren || (props.parentItem.childCount ?? 0) > 0
})

// 하위 업무 카운트 텍스트
const childCountText = computed(() => {
  const total = props.parentItem.childCount ?? 0
  const completed = props.parentItem.completedChildCount ?? 0
  if (total === 0) return ''
  return `${completed}/${total}`
})

// 진행률
const progressPercent = computed(() => {
  const total = props.parentItem.childCount ?? 0
  const completed = props.parentItem.completedChildCount ?? 0
  if (total === 0) return 0
  return Math.round((completed / total) * 100)
})

// 하위 업무 로드
async function loadChildren() {
  if (!hasChildren.value) return

  loading.value = true
  try {
    const result = await itemStore.fetchChildren(props.boardId, props.parentItem.itemId, {
      includeCompleted: props.includeCompleted,
      includeDeleted: false
    })
    children.value = result
  } finally {
    loading.value = false
  }
}

// 펼치기/접기 토글
async function toggleExpand() {
  isExpanded.value = !isExpanded.value
  if (isExpanded.value && children.value.length === 0) {
    await loadChildren()
  }
}

// 하위 업무 추가
async function addSubTask() {
  if (!newTaskTitle.value.trim()) return

  const request: SubTaskCreateRequest = {
    title: newTaskTitle.value.trim()
  }

  const result = await itemStore.createSubTask(props.boardId, props.parentItem.itemId, request)
  if (result) {
    newTaskTitle.value = ''
    showAddForm.value = false
    await loadChildren()
    emit('update')
  }
}

// 하위 업무 완료
async function completeChild(childId: number) {
  const success = await itemStore.completeItem(props.boardId, childId)
  if (success) {
    await loadChildren()
    emit('update')
  }
}

// 하위 업무 삭제
async function deleteChild(childId: number) {
  const success = await itemStore.deleteItem(props.boardId, childId)
  if (success) {
    await loadChildren()
    emit('update')
  }
}

// 하위 업무 선택 (상세 패널 열기)
function selectChild(child: Item) {
  emit('select', child)
}

// 상태 색상
const statusColors: Record<ItemStatus, string> = {
  NOT_STARTED: 'text-gray-500',
  IN_PROGRESS: 'text-blue-500',
  PENDING: 'text-yellow-500',
  COMPLETED: 'text-green-500',
  DELETED: 'text-red-500'
}

// 취소
function cancelAdd() {
  showAddForm.value = false
  newTaskTitle.value = ''
}

// props.expanded 변경 감지
watch(() => props.expanded, (val) => {
  isExpanded.value = val
  if (val && children.value.length === 0 && hasChildren.value) {
    loadChildren()
  }
})

// 초기 로드 (expanded가 true인 경우)
if (props.expanded && hasChildren.value) {
  loadChildren()
}
</script>

<template>
  <div class="subtask-list">
    <!-- 헤더: 펼치기/접기 + 카운트 + 추가 버튼 -->
    <div class="flex items-center gap-2 py-1">
      <!-- 펼치기/접기 버튼 -->
      <button
        v-if="hasChildren"
        class="flex items-center gap-1 text-[12px] text-gray-500 hover:text-gray-700"
        @click="toggleExpand"
      >
        <svg
          class="w-4 h-4 transition-transform"
          :class="{ 'rotate-90': isExpanded }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
        <span>하위 업무</span>
        <span v-if="childCountText" class="text-gray-400">({{ childCountText }})</span>
      </button>

      <!-- 진행률 바 -->
      <div v-if="hasChildren && (parentItem.childCount ?? 0) > 0" class="flex-1 max-w-[100px] h-1.5 bg-gray-200 rounded-full overflow-hidden">
        <div
          class="h-full bg-green-500 transition-all duration-300"
          :style="{ width: `${progressPercent}%` }"
        />
      </div>

      <!-- 추가 버튼 -->
      <button
        v-if="canCreateChild && !showAddForm"
        class="flex items-center gap-1 px-2 py-0.5 text-[11px] text-gray-500 hover:text-primary-600 hover:bg-primary-50 rounded transition-colors"
        @click="showAddForm = true"
      >
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        <span>하위 업무 추가</span>
      </button>
    </div>

    <!-- 하위 업무 추가 폼 -->
    <div v-if="showAddForm" class="flex items-center gap-2 py-1 pl-5">
      <input
        v-model="newTaskTitle"
        type="text"
        class="flex-1 h-7 px-2 text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
        placeholder="하위 업무 제목 입력..."
        @keydown.enter="addSubTask"
        @keydown.escape="cancelAdd"
        autofocus
      />
      <button
        class="px-2 py-1 text-[12px] text-white bg-primary-600 hover:bg-primary-700 rounded transition-colors"
        @click="addSubTask"
      >
        추가
      </button>
      <button
        class="px-2 py-1 text-[12px] text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded transition-colors"
        @click="cancelAdd"
      >
        취소
      </button>
    </div>

    <!-- 하위 업무 목록 -->
    <div v-if="isExpanded" class="pl-5 space-y-0.5">
      <!-- 로딩 -->
      <div v-if="loading" class="py-2 text-[12px] text-gray-400">
        로딩 중...
      </div>

      <!-- 목록 -->
      <template v-else>
        <div
          v-for="child in children"
          :key="child.itemId"
          class="group flex items-center gap-2 py-1 px-2 hover:bg-gray-50 rounded cursor-pointer"
          @click="selectChild(child)"
        >
          <!-- 완료 체크박스 -->
          <button
            class="flex-shrink-0 w-4 h-4 border rounded transition-colors"
            :class="child.status === 'COMPLETED' ? 'bg-green-500 border-green-500 text-white' : 'border-gray-300 hover:border-green-500'"
            @click.stop="completeChild(child.itemId)"
          >
            <svg v-if="child.status === 'COMPLETED'" class="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
            </svg>
          </button>

          <!-- 제목 -->
          <span
            class="flex-1 text-[13px] truncate"
            :class="child.status === 'COMPLETED' ? 'line-through text-gray-400' : 'text-gray-700'"
          >
            {{ child.title }}
          </span>

          <!-- 상태 표시 -->
          <span
            v-if="child.status !== 'COMPLETED'"
            class="flex-shrink-0 text-[11px]"
            :class="statusColors[child.status]"
          >
            {{ child.status === 'IN_PROGRESS' ? '진행중' : child.status === 'PENDING' ? '보류' : '' }}
          </span>

          <!-- 하위 업무 카운트 -->
          <span v-if="(child.childCount ?? 0) > 0" class="flex-shrink-0 text-[11px] text-gray-400">
            {{ child.completedChildCount ?? 0 }}/{{ child.childCount }}
          </span>

          <!-- 삭제 버튼 (hover 시) -->
          <button
            class="flex-shrink-0 opacity-0 group-hover:opacity-100 p-1 text-gray-400 hover:text-red-500 transition-all"
            title="삭제"
            @click.stop="deleteChild(child.itemId)"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- 빈 상태 -->
        <div v-if="children.length === 0 && !loading" class="py-2 text-[12px] text-gray-400 text-center">
          하위 업무가 없습니다
        </div>
      </template>
    </div>
  </div>
</template>
