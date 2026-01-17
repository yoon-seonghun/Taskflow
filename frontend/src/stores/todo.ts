import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { todoApi } from '@/api/todo'
import type {
  Todo,
  TodoShare,
  TodoCreateRequest,
  TodoUpdateRequest,
  TodoShareRequest,
  TodoTransferRequest,
  TodoBulkTransferRequest,
  TodoReorderRequest
} from '@/types/todo'

export const useTodoStore = defineStore('todo', () => {
  // ==================== State ====================
  const todos = ref<Todo[]>([])
  const todayTodos = ref<Todo[]>([])
  const overdueTodos = ref<Todo[]>([])
  const completedTodos = ref<Todo[]>([])
  const sharedTodos = ref<TodoShare[]>([])
  const transferredTodos = ref<Todo[]>([])
  const selectedTodo = ref<Todo | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // ==================== Getters ====================
  // 미완료 Todo 목록
  const activeTodos = computed(() => {
    return todos.value.filter(t => !t.isCompleted)
  })

  // 완료 Todo 수
  const completedCount = computed(() => {
    return todos.value.filter(t => t.isCompleted).length
  })

  // 지연 Todo 수
  const overdueCount = computed(() => {
    return overdueTodos.value.length
  })

  // 오늘 마감 수
  const todayCount = computed(() => {
    return todayTodos.value.length
  })

  // 공유받은 Todo 수
  const sharedCount = computed(() => {
    return sharedTodos.value.length
  })

  // ==================== Actions ====================

  /**
   * 내 Todo 목록 조회
   */
  async function fetchTodos(params?: {
    includeCompleted?: boolean
    dueDateFrom?: string
    dueDateTo?: string
  }): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await todoApi.getTodos(params)
      if (response.success && response.data) {
        todos.value = response.data
        return true
      }
      error.value = response.message || 'Todo 목록을 불러오는데 실패했습니다.'
      return false
    } catch (e) {
      error.value = 'Todo 목록을 불러오는데 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 오늘 마감 Todo 목록 조회
   */
  async function fetchTodayTodos(): Promise<boolean> {
    try {
      const response = await todoApi.getTodayTodos()
      if (response.success && response.data) {
        todayTodos.value = response.data
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 지연 Todo 목록 조회
   */
  async function fetchOverdueTodos(): Promise<boolean> {
    try {
      const response = await todoApi.getOverdueTodos()
      if (response.success && response.data) {
        overdueTodos.value = response.data
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 완료된 Todo 목록 조회
   */
  async function fetchCompletedTodos(params?: {
    completedDateFrom?: string
    completedDateTo?: string
  }): Promise<boolean> {
    try {
      const response = await todoApi.getCompletedTodos(params)
      if (response.success && response.data) {
        completedTodos.value = response.data
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 공유받은 Todo 목록 조회
   */
  async function fetchSharedTodos(): Promise<boolean> {
    try {
      const response = await todoApi.getSharedTodos()
      if (response.success && response.data) {
        sharedTodos.value = response.data
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 이관받은 Todo 목록 조회
   */
  async function fetchTransferredTodos(): Promise<boolean> {
    try {
      const response = await todoApi.getTransferredTodos()
      if (response.success && response.data) {
        transferredTodos.value = response.data
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * Todo 상세 조회
   */
  async function fetchTodo(todoId: number): Promise<Todo | null> {
    loading.value = true
    error.value = null

    try {
      const response = await todoApi.getTodo(todoId)
      if (response.success && response.data) {
        selectedTodo.value = response.data
        return response.data
      }
      error.value = response.message || 'Todo를 불러오는데 실패했습니다.'
      return null
    } catch (e) {
      error.value = 'Todo를 불러오는데 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Todo 생성
   */
  async function createTodo(data: TodoCreateRequest): Promise<Todo | null> {
    loading.value = true
    error.value = null

    try {
      const response = await todoApi.createTodo(data)
      if (response.success && response.data) {
        // 목록에 추가
        todos.value.unshift(response.data)
        return response.data
      }
      error.value = response.message || 'Todo 생성에 실패했습니다.'
      return null
    } catch (e) {
      error.value = 'Todo 생성에 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Todo 수정
   */
  async function updateTodo(todoId: number, data: TodoUpdateRequest): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await todoApi.updateTodo(todoId, data)
      if (response.success && response.data) {
        // 목록에서 업데이트
        const index = todos.value.findIndex(t => t.todoId === todoId)
        if (index !== -1) {
          todos.value[index] = response.data
        }
        if (selectedTodo.value?.todoId === todoId) {
          selectedTodo.value = response.data
        }
        return true
      }
      error.value = response.message || 'Todo 수정에 실패했습니다.'
      return false
    } catch (e) {
      error.value = 'Todo 수정에 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 완료 상태 토글
   */
  async function toggleComplete(todoId: number): Promise<boolean> {
    try {
      const response = await todoApi.toggleComplete(todoId)
      if (response.success && response.data) {
        // 목록에서 업데이트
        const index = todos.value.findIndex(t => t.todoId === todoId)
        if (index !== -1) {
          todos.value[index] = response.data
        }
        // 완료 목록 새로고침
        fetchCompletedTodos()
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * Todo 삭제
   */
  async function deleteTodo(todoId: number): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await todoApi.deleteTodo(todoId)
      if (response.success) {
        // 목록에서 제거
        todos.value = todos.value.filter(t => t.todoId !== todoId)
        if (selectedTodo.value?.todoId === todoId) {
          selectedTodo.value = null
        }
        return true
      }
      error.value = response.message || 'Todo 삭제에 실패했습니다.'
      return false
    } catch (e) {
      error.value = 'Todo 삭제에 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * Todo 이관
   */
  async function transferTodo(todoId: number, data: TodoTransferRequest): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await todoApi.transferTodo(todoId, data)
      if (response.success) {
        // 목록에서 제거
        todos.value = todos.value.filter(t => t.todoId !== todoId)
        return true
      }
      error.value = response.message || 'Todo 이관에 실패했습니다.'
      return false
    } catch (e) {
      error.value = 'Todo 이관에 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * Todo 순서 변경
   */
  async function reorderTodos(data: TodoReorderRequest): Promise<boolean> {
    try {
      const response = await todoApi.reorderTodos(data)
      if (response.success) {
        // 순서 반영
        const newOrder = data.todoIds
        todos.value.sort((a, b) => {
          return newOrder.indexOf(a.todoId) - newOrder.indexOf(b.todoId)
        })
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  // ==================== Todo 공유 ====================

  /**
   * Todo 공유 추가
   */
  async function addShare(todoId: number, data: TodoShareRequest): Promise<boolean> {
    try {
      const response = await todoApi.addShare(todoId, data)
      if (response.success) {
        // 상세 정보 새로고침
        if (selectedTodo.value?.todoId === todoId) {
          fetchTodo(todoId)
        }
        return true
      }
      error.value = response.message || '공유 추가에 실패했습니다.'
      return false
    } catch (e) {
      error.value = '공유 추가에 실패했습니다.'
      return false
    }
  }

  /**
   * Todo 공유 삭제
   */
  async function deleteShare(shareId: number): Promise<boolean> {
    try {
      const response = await todoApi.deleteShare(shareId)
      if (response.success) {
        // 상세 정보 새로고침
        if (selectedTodo.value) {
          fetchTodo(selectedTodo.value.todoId)
        }
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 일괄 이관 (사용자 삭제용)
   */
  async function transferAllTodos(data: TodoBulkTransferRequest): Promise<number> {
    try {
      const response = await todoApi.transferAllTodos(data)
      if (response.success && response.data) {
        return response.data.transferredCount
      }
      return 0
    } catch (e) {
      return 0
    }
  }

  /**
   * 사용자의 모든 Todo 삭제 (사용자 삭제용)
   */
  async function deleteAllTodos(userId: number): Promise<number> {
    try {
      const response = await todoApi.deleteAllTodos(userId)
      if (response.success && response.data) {
        return response.data.deletedCount
      }
      return 0
    } catch (e) {
      return 0
    }
  }

  /**
   * 선택된 Todo 설정
   */
  function setSelectedTodo(todo: Todo | null) {
    selectedTodo.value = todo
  }

  /**
   * 상태 초기화
   */
  function reset() {
    todos.value = []
    todayTodos.value = []
    overdueTodos.value = []
    completedTodos.value = []
    sharedTodos.value = []
    transferredTodos.value = []
    selectedTodo.value = null
    loading.value = false
    error.value = null
  }

  return {
    // State
    todos,
    todayTodos,
    overdueTodos,
    completedTodos,
    sharedTodos,
    transferredTodos,
    selectedTodo,
    loading,
    error,
    // Getters
    activeTodos,
    completedCount,
    overdueCount,
    todayCount,
    sharedCount,
    // Actions
    fetchTodos,
    fetchTodayTodos,
    fetchOverdueTodos,
    fetchCompletedTodos,
    fetchSharedTodos,
    fetchTransferredTodos,
    fetchTodo,
    createTodo,
    updateTodo,
    toggleComplete,
    deleteTodo,
    transferTodo,
    reorderTodos,
    addShare,
    deleteShare,
    transferAllTodos,
    deleteAllTodos,
    setSelectedTodo,
    reset
  }
})
