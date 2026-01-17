<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useTodoStore } from '@/stores/todo'
import { useUiStore } from '@/stores/ui'
import { useSortableDrag } from '@/composables/useSortableDrag'
import type { Todo, TodoCreateRequest, TodoPriority, RepeatType, TodoShare } from '@/types/todo'
import Button from '@/components/common/Button.vue'
import Input from '@/components/common/Input.vue'
import Select from '@/components/common/Select.vue'
import DatePicker from '@/components/common/DatePicker.vue'
import Modal from '@/components/common/Modal.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import Badge from '@/components/common/Badge.vue'
import Spinner from '@/components/common/Spinner.vue'
import UserSelectModal from '@/components/common/UserSelectModal.vue'
import { PRIORITY_OPTIONS, REPEAT_TYPE_OPTIONS, getPriorityColor } from '@/types/todo'

const todoStore = useTodoStore()
const uiStore = useUiStore()

// State
const activeTab = ref<'today' | 'all' | 'shared' | 'completed'>('today')
const showCreateModal = ref(false)
const showShareModal = ref(false)
const showTransferModal = ref(false)
const showDeleteConfirm = ref(false)
const selectedTodo = ref<Todo | null>(null)
const editingTodo = ref<Todo | null>(null)

// 정렬 옵션
type SortOption = 'date' | 'custom'
const sortOption = ref<SortOption>('date')
const sharedSortOption = ref<SortOption>('date')

// 새 Todo 폼
const newTodo = ref<TodoCreateRequest>({
  title: '',
  priority: 'NORMAL',
  dueDate: undefined,
  dueTime: undefined,
  repeatType: undefined
})

// 드래그용 로컬 아이템 (사용자 지정 정렬 시)
const localTodos = ref<Todo[] | null>(null)
const localSharedTodos = ref<TodoShare[] | null>(null)

// 정렬 함수 - 날짜순 (날짜 없으면 제목순)
function sortByDate<T extends { dueDate?: string; title?: string; todoDueDate?: string; todoTitle?: string }>(items: T[]): T[] {
  return [...items].sort((a, b) => {
    const dateA = 'dueDate' in a ? a.dueDate : a.todoDueDate
    const dateB = 'dueDate' in b ? b.dueDate : b.todoDueDate
    const titleA = ('title' in a ? a.title : a.todoTitle) || ''
    const titleB = ('title' in b ? b.title : b.todoTitle) || ''

    // 둘 다 날짜가 없으면 제목순
    if (!dateA && !dateB) {
      return titleA.localeCompare(titleB, 'ko')
    }
    // 날짜가 없는 항목은 뒤로
    if (!dateA) return 1
    if (!dateB) return -1
    // 날짜순 정렬
    return dateA.localeCompare(dateB)
  })
}

// Computed
const displayTodos = computed({
  get() {
    // 드래그 중 로컬 순서 사용
    if (localTodos.value && activeTab.value === 'all') {
      return localTodos.value
    }

    let items: Todo[]
    switch (activeTab.value) {
      case 'today':
        return sortByDate(todoStore.todayTodos)
      case 'shared':
        return [] // sharedTodos는 다른 구조
      case 'completed':
        return todoStore.completedTodos
      default:
        items = todoStore.activeTodos
        // 정렬 적용
        if (sortOption.value === 'date') {
          return sortByDate(items)
        }
        // 사용자 지정 - sortOrder 기준
        return [...items].sort((a, b) => (a.sortOrder ?? 999999) - (b.sortOrder ?? 999999))
    }
  },
  set(newItems: Todo[]) {
    localTodos.value = newItems
  }
})

// 공유받은 Todo 정렬된 목록
const sortedSharedTodos = computed({
  get() {
    if (localSharedTodos.value) {
      return localSharedTodos.value
    }
    if (sharedSortOption.value === 'date') {
      return sortByDate(todoStore.sharedTodos)
    }
    return todoStore.sharedTodos
  },
  set(newItems: TodoShare[]) {
    localSharedTodos.value = newItems
  }
})

const todayCount = computed(() => todoStore.todayTodos.length)
const overdueCount = computed(() => todoStore.overdueCount)
const sharedCount = computed(() => todoStore.sharedCount)

// 우선순위 옵션
const priorityOptions = PRIORITY_OPTIONS.map(p => ({
  value: p.value,
  label: p.label
}))

// 반복 타입 옵션
const repeatTypeOptions = REPEAT_TYPE_OPTIONS.map(r => ({
  value: r.value,
  label: r.label
}))

// 정렬 옵션
const sortOptions = [
  { value: 'date', label: '날짜순' },
  { value: 'custom', label: '사용자 지정' }
]

// 드래그 앤 드롭 - 전체 탭
const {
  draggedIndex: todoDraggedIndex,
  dragOverIndex: todoDragOverIndex,
  getDragClass: getTodoDragClass,
  handleDragStart: handleTodoDragStart,
  handleDragOver: handleTodoDragOver,
  handleDragLeave: handleTodoDragLeave,
  handleDragEnd: baseTodoDragEnd,
  handleDrop: handleTodoDrop
} = useSortableDrag<Todo>({
  items: displayTodos,
  itemKey: 'todoId',
  onReorder: async (reorderedItems) => {
    const todoIds = reorderedItems.map(t => t.todoId)
    const success = await todoStore.reorderTodos({ todoIds })
    if (success) {
      uiStore.showSuccess('순서가 변경되었습니다')
      // 로컬 순서 초기화
      setTimeout(() => {
        localTodos.value = null
      }, 100)
    } else {
      uiStore.showWarning('순서 변경에 실패했습니다')
      localTodos.value = null
    }
  }
})

// 드래그 종료 래퍼
function handleTodoDragEnd(event: DragEvent) {
  baseTodoDragEnd(event)
  setTimeout(() => {
    localTodos.value = null
  }, 100)
}

// 드래그 앤 드롭 - 공유받은 탭 (클라이언트 측 정렬만)
const {
  draggedIndex: sharedDraggedIndex,
  dragOverIndex: sharedDragOverIndex,
  getDragClass: getSharedDragClass,
  handleDragStart: handleSharedDragStart,
  handleDragOver: handleSharedDragOver,
  handleDragLeave: handleSharedDragLeave,
  handleDragEnd: baseSharedDragEnd,
  handleDrop: handleSharedDrop
} = useSortableDrag<TodoShare>({
  items: sortedSharedTodos,
  itemKey: 'shareId',
  onReorder: async (reorderedItems) => {
    // 클라이언트 측에서만 순서 유지 (서버 저장 없음)
    localSharedTodos.value = reorderedItems
    uiStore.showSuccess('순서가 변경되었습니다 (새로고침 시 초기화)')
  }
})

// 공유 드래그 종료 래퍼
function handleSharedDragEnd(event: DragEvent) {
  baseSharedDragEnd(event)
}

// Methods
async function loadData() {
  await Promise.all([
    todoStore.fetchTodos(),
    todoStore.fetchTodayTodos(),
    todoStore.fetchOverdueTodos(),
    todoStore.fetchSharedTodos(),
    todoStore.fetchCompletedTodos()
  ])
}

function openCreateModal() {
  newTodo.value = {
    title: '',
    priority: 'NORMAL',
    dueDate: undefined,
    dueTime: undefined,
    repeatType: undefined
  }
  showCreateModal.value = true
}

async function handleCreate() {
  if (!newTodo.value.title.trim()) {
    uiStore.showWarning('제목을 입력해주세요')
    return
  }

  const result = await todoStore.createTodo(newTodo.value)
  if (result) {
    showCreateModal.value = false
    uiStore.showSuccess('Todo가 생성되었습니다')
    loadData()
  }
}

async function handleToggleComplete(todo: Todo) {
  await todoStore.toggleComplete(todo.todoId)
}

function handleEdit(todo: Todo) {
  editingTodo.value = { ...todo }
}

async function handleSaveEdit() {
  if (!editingTodo.value) return

  const result = await todoStore.updateTodo(editingTodo.value.todoId, {
    title: editingTodo.value.title,
    description: editingTodo.value.description,
    priority: editingTodo.value.priority,
    dueDate: editingTodo.value.dueDate,
    dueTime: editingTodo.value.dueTime,
    repeatType: editingTodo.value.repeatType
  })

  if (result) {
    editingTodo.value = null
    uiStore.showSuccess('수정되었습니다')
    loadData()
  }
}

function handleCancelEdit() {
  editingTodo.value = null
}

function handleDelete(todo: Todo) {
  selectedTodo.value = todo
  showDeleteConfirm.value = true
}

async function confirmDelete() {
  if (!selectedTodo.value) return

  const result = await todoStore.deleteTodo(selectedTodo.value.todoId)
  if (result) {
    showDeleteConfirm.value = false
    selectedTodo.value = null
    uiStore.showSuccess('삭제되었습니다')
    loadData()
  }
}

function handleShare(todo: Todo) {
  selectedTodo.value = todo
  showShareModal.value = true
}

async function handleShareConfirm(user: { username: string; name: string } | null) {
  if (!selectedTodo.value || !user) return

  const result = await todoStore.addShare(selectedTodo.value.todoId, {
    sharedUsername: user.username,
    permission: 'VIEW'
  })

  if (result) {
    showShareModal.value = false
    uiStore.showSuccess('공유되었습니다')
  }
}

function handleTransfer(todo: Todo) {
  selectedTodo.value = todo
  showTransferModal.value = true
}

async function handleTransferConfirm(user: { username: string; name: string } | null) {
  if (!selectedTodo.value || !user) return

  const result = await todoStore.transferTodo(selectedTodo.value.todoId, {
    toUsername: user.username
  })

  if (result) {
    showTransferModal.value = false
    selectedTodo.value = null
    uiStore.showSuccess('이관되었습니다')
    loadData()
  }
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

function formatTime(timeStr?: string): string {
  if (!timeStr) return ''
  return timeStr.substring(0, 5)
}

// Lifecycle
onMounted(() => {
  loadData()
})

// Tab 변경 시 데이터 로드
watch(activeTab, () => {
  loadData()
})
</script>

<template>
  <div class="todo-view p-4 md:p-6 dark:bg-gray-900 min-h-screen">
    <!-- 헤더 -->
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h1 class="text-xl font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
          </svg>
          Todo List
        </h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          개인 할 일 목록을 관리합니다
        </p>
      </div>
      <Button variant="primary" size="sm" @click="openCreateModal">
        <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        새 Todo
      </Button>
    </div>

    <!-- 탭 -->
    <div class="flex space-x-1 mb-4 bg-gray-100 dark:bg-gray-800 rounded-lg p-1 overflow-x-auto">
      <button
        v-for="tab in [
          { key: 'today', label: '오늘', count: todayCount },
          { key: 'all', label: '전체', count: todoStore.activeTodos.length },
          { key: 'shared', label: '공유받은', count: sharedCount },
          { key: 'completed', label: '완료', count: todoStore.completedCount }
        ]"
        :key="tab.key"
        @click="activeTab = tab.key as any"
        :class="[
          'px-4 py-2 text-sm font-medium rounded-md transition-colors whitespace-nowrap',
          activeTab === tab.key
            ? 'bg-white dark:bg-gray-700 text-blue-600 dark:text-blue-400 shadow-sm'
            : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
        ]"
      >
        {{ tab.label }}
        <span v-if="tab.count" class="ml-1 text-xs">
          ({{ tab.count }})
        </span>
      </button>
    </div>

    <!-- 정렬 옵션 (전체/공유받은 탭) -->
    <div v-if="activeTab === 'all' || activeTab === 'shared'" class="flex items-center justify-end gap-2 mb-4">
      <span class="text-xs text-gray-500 dark:text-gray-400">정렬:</span>
      <select
        :value="activeTab === 'all' ? sortOption : sharedSortOption"
        @change="activeTab === 'all' ? sortOption = ($event.target as HTMLSelectElement).value as SortOption : sharedSortOption = ($event.target as HTMLSelectElement).value as SortOption"
        class="text-xs border border-gray-300 dark:border-gray-600 rounded-md px-2 py-1 bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        <option v-for="opt in sortOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
      <span v-if="(activeTab === 'all' && sortOption === 'custom') || (activeTab === 'shared' && sharedSortOption === 'custom')" class="text-xs text-gray-400 dark:text-gray-500">
        (드래그하여 순서 변경)
      </span>
    </div>

    <!-- 지연 경고 -->
    <div v-if="overdueCount > 0" class="mb-4 p-3 bg-red-50 dark:bg-red-900/20 rounded-lg flex items-center gap-2">
      <svg class="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <span class="text-sm text-red-700 dark:text-red-300">
        {{ overdueCount }}개의 지연된 Todo가 있습니다
      </span>
    </div>

    <!-- 로딩 -->
    <div v-if="todoStore.loading" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <!-- Todo 목록 -->
    <div v-else-if="displayTodos.length > 0" class="space-y-2">
      <div
        v-for="(todo, index) in displayTodos"
        :key="todo.todoId"
        :draggable="activeTab === 'all' && sortOption === 'custom'"
        :class="[
          'group p-4 rounded-lg border transition-all',
          todo.isCompleted
            ? 'bg-gray-50 dark:bg-gray-800/50 border-gray-200 dark:border-gray-700'
            : 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700 hover:shadow-md',
          activeTab === 'all' && sortOption === 'custom' ? 'cursor-move' : '',
          getTodoDragClass(index)
        ]"
        @dragstart="activeTab === 'all' && sortOption === 'custom' && handleTodoDragStart(index, $event)"
        @dragover="activeTab === 'all' && sortOption === 'custom' && handleTodoDragOver(index, $event)"
        @dragleave="activeTab === 'all' && sortOption === 'custom' && handleTodoDragLeave()"
        @dragend="activeTab === 'all' && sortOption === 'custom' && handleTodoDragEnd($event)"
        @drop="activeTab === 'all' && sortOption === 'custom' && handleTodoDrop(index, $event)"
      >
        <!-- 편집 모드 -->
        <div v-if="editingTodo?.todoId === todo.todoId" class="space-y-3">
          <Input v-model="editingTodo.title" placeholder="Todo 제목" />
          <div class="flex flex-wrap gap-2">
            <Select
              v-model="editingTodo.priority"
              :options="priorityOptions"
              size="sm"
              class="w-28"
            />
            <DatePicker v-model="editingTodo.dueDate" size="sm" />
            <Input v-model="editingTodo.dueTime" type="time" size="sm" class="w-28" />
          </div>
          <div class="flex justify-end gap-2">
            <Button variant="ghost" size="sm" @click="handleCancelEdit">취소</Button>
            <Button variant="primary" size="sm" @click="handleSaveEdit">저장</Button>
          </div>
        </div>

        <!-- 일반 모드 -->
        <div v-else class="flex items-start gap-3">
          <!-- 드래그 핸들 (사용자 지정 정렬 시) -->
          <div
            v-if="activeTab === 'all' && sortOption === 'custom'"
            class="mt-0.5 text-gray-400 dark:text-gray-500 cursor-grab active:cursor-grabbing"
          >
            <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path d="M7 2a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 2zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 8zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 14zm6-8a2 2 0 1 0-.001-4.001A2 2 0 0 0 13 6zm0 2a2 2 0 1 0 .001 4.001A2 2 0 0 0 13 8zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 13 14z"/>
            </svg>
          </div>

          <!-- 체크박스 -->
          <button
            @click="handleToggleComplete(todo)"
            :class="[
              'mt-0.5 w-5 h-5 rounded border-2 flex items-center justify-center transition-colors',
              todo.isCompleted
                ? 'bg-green-500 border-green-500 text-white'
                : 'border-gray-300 dark:border-gray-600 hover:border-green-500'
            ]"
          >
            <svg v-if="todo.isCompleted" class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
            </svg>
          </button>

          <!-- 내용 -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <span
                :class="[
                  'text-sm font-medium',
                  todo.isCompleted
                    ? 'text-gray-400 dark:text-gray-500 line-through'
                    : 'text-gray-900 dark:text-white'
                ]"
              >
                {{ todo.title }}
              </span>
              <Badge
                :variant="todo.priority === 'URGENT' ? 'danger' : todo.priority === 'HIGH' ? 'warning' : 'secondary'"
                size="xs"
              >
                {{ PRIORITY_OPTIONS.find(p => p.value === todo.priority)?.label }}
              </Badge>
              <Badge v-if="todo.repeatType && todo.repeatType !== 'NONE'" variant="info" size="xs">
                🔄 {{ REPEAT_TYPE_OPTIONS.find(r => r.value === todo.repeatType)?.label }}
              </Badge>
              <Badge v-if="todo.isOverdue" variant="danger" size="xs">지연</Badge>
              <Badge v-if="todo.isDueToday && !todo.isOverdue" variant="warning" size="xs">오늘</Badge>
            </div>
            <div class="flex items-center gap-3 mt-1 text-xs text-gray-500 dark:text-gray-400">
              <span v-if="todo.dueDate">
                📅 {{ formatDate(todo.dueDate) }}
                <span v-if="todo.dueTime"> {{ formatTime(todo.dueTime) }}</span>
              </span>
              <span v-if="todo.shareCount && todo.shareCount > 0">
                👥 {{ todo.shareCount }}명과 공유
              </span>
              <span v-if="todo.transferredFromUserName">
                ↩️ {{ todo.transferredFromUserName }}님에게서 이관
              </span>
            </div>
          </div>

          <!-- 액션 버튼 -->
          <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
              @click="handleEdit(todo)"
              class="p-1.5 text-gray-400 hover:text-blue-500 rounded"
              title="수정"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
              </svg>
            </button>
            <div class="relative">
              <button
                @click="handleShare(todo)"
                class="p-1.5 text-gray-400 hover:text-green-500 rounded"
                title="공유"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                </svg>
              </button>
              <span
                v-if="todo.shareCount && todo.shareCount > 0"
                class="absolute -top-1 -right-1 min-w-[16px] h-4 px-1 flex items-center justify-center text-[10px] font-medium text-white bg-green-500 rounded-full"
              >
                {{ todo.shareCount > 9 ? '9+' : todo.shareCount }}
              </span>
            </div>
            <button
              @click="handleTransfer(todo)"
              class="p-1.5 text-gray-400 hover:text-purple-500 rounded"
              title="이관"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M17 8l4 4m0 0l-4 4m4-4H3" />
              </svg>
            </button>
            <button
              @click="handleDelete(todo)"
              class="p-1.5 text-gray-400 hover:text-red-500 rounded"
              title="삭제"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 공유받은 Todo 목록 -->
    <div v-else-if="activeTab === 'shared' && sortedSharedTodos.length > 0" class="space-y-2">
      <div
        v-for="(share, index) in sortedSharedTodos"
        :key="share.shareId"
        :draggable="sharedSortOption === 'custom'"
        :class="[
          'p-4 rounded-lg border bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700 hover:shadow-md transition-shadow',
          sharedSortOption === 'custom' ? 'cursor-move' : '',
          getSharedDragClass(index)
        ]"
        @dragstart="sharedSortOption === 'custom' && handleSharedDragStart(index, $event)"
        @dragover="sharedSortOption === 'custom' && handleSharedDragOver(index, $event)"
        @dragleave="sharedSortOption === 'custom' && handleSharedDragLeave()"
        @dragend="sharedSortOption === 'custom' && handleSharedDragEnd($event)"
        @drop="sharedSortOption === 'custom' && handleSharedDrop(index, $event)"
      >
        <div class="flex items-center gap-2 flex-wrap">
          <!-- 드래그 핸들 -->
          <div
            v-if="sharedSortOption === 'custom'"
            class="text-gray-400 dark:text-gray-500 cursor-grab active:cursor-grabbing"
          >
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path d="M7 2a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 2zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 8zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 14zm6-8a2 2 0 1 0-.001-4.001A2 2 0 0 0 13 6zm0 2a2 2 0 1 0 .001 4.001A2 2 0 0 0 13 8zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 13 14z"/>
            </svg>
          </div>
          <span class="text-sm font-medium text-gray-900 dark:text-white">
            {{ share.todoTitle }}
          </span>
          <Badge
            :variant="share.todoPriority === 'URGENT' ? 'danger' : share.todoPriority === 'HIGH' ? 'warning' : 'secondary'"
            size="xs"
          >
            {{ PRIORITY_OPTIONS.find(p => p.value === share.todoPriority)?.label || '보통' }}
          </Badge>
          <Badge :variant="share.permission === 'EDIT' ? 'success' : 'secondary'" size="xs">
            {{ share.permission === 'EDIT' ? '수정 가능' : '조회만' }}
          </Badge>
        </div>
        <div class="flex items-center gap-3 mt-2 text-xs text-gray-500 dark:text-gray-400">
          <span v-if="share.todoDueDate" class="flex items-center gap-1">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            {{ formatDate(share.todoDueDate) }}
            <span v-if="share.todoDueTime">{{ formatTime(share.todoDueTime) }}</span>
          </span>
          <span class="flex items-center gap-1">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            {{ share.todoOwnerUserName || share.sharedByUserName }}님의 Todo
          </span>
          <span class="flex items-center gap-1">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
            </svg>
            {{ share.sharedByUserName }}님이 공유
          </span>
        </div>
      </div>
    </div>

    <!-- 빈 상태 -->
    <div v-else class="text-center py-12">
      <svg class="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
      </svg>
      <p class="text-gray-500 dark:text-gray-400">
        {{ activeTab === 'shared' ? '공유받은 Todo가 없습니다' : 'Todo가 없습니다' }}
      </p>
      <Button v-if="activeTab !== 'shared'" variant="outline" size="sm" class="mt-4" @click="openCreateModal">
        새 Todo 만들기
      </Button>
    </div>

    <!-- 생성 모달 -->
    <Modal v-model="showCreateModal" title="새 Todo 만들기">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            제목 *
          </label>
          <Input v-model="newTodo.title" placeholder="할 일을 입력하세요" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              우선순위
            </label>
            <Select v-model="newTodo.priority" :options="priorityOptions" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              반복
            </label>
            <Select v-model="newTodo.repeatType" :options="repeatTypeOptions" />
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              마감일
            </label>
            <DatePicker v-model="newTodo.dueDate" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              마감 시간
            </label>
            <Input v-model="newTodo.dueTime" type="time" />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <Button variant="ghost" @click="showCreateModal = false">취소</Button>
          <Button variant="primary" @click="handleCreate">생성</Button>
        </div>
      </template>
    </Modal>

    <!-- 공유 모달 -->
    <UserSelectModal
      :show="showShareModal"
      title="Todo 공유"
      @close="showShareModal = false"
      @select="handleShareConfirm"
    />

    <!-- 이관 모달 -->
    <UserSelectModal
      :show="showTransferModal"
      title="Todo 이관"
      @close="showTransferModal = false"
      @select="handleTransferConfirm"
    />

    <!-- 삭제 확인 -->
    <ConfirmDialog
      v-model="showDeleteConfirm"
      title="Todo 삭제"
      :message="`'${selectedTodo?.title}' Todo를 삭제하시겠습니까?`"
      confirmText="삭제"
      confirmVariant="danger"
      @confirm="confirmDelete"
    />
  </div>
</template>

<style scoped>
.todo-view {
  max-width: 800px;
  margin: 0 auto;
}

/* 드래그 앤 드롭 스타일 */
.sortable-dragging {
  opacity: 0.5;
  background-color: #e0f2fe !important;
}

.sortable-drag-over {
  border-top: 2px solid #3B82F6 !important;
}

:deep(.sortable-dragging) {
  opacity: 0.5;
  background-color: #e0f2fe !important;
}

:deep(.sortable-drag-over) {
  border-top: 2px solid #3B82F6 !important;
}

.dark .sortable-dragging,
.dark :deep(.sortable-dragging) {
  background-color: #1e3a5f !important;
}
</style>
