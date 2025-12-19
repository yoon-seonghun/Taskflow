import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { boardApi } from '@/api/board'
import type {
  Board,
  BoardCreateRequest,
  BoardUpdateRequest,
  BoardShare,
  BoardShareRequest
} from '@/types/board'

export type ViewType = 'table' | 'kanban' | 'list'

export const useBoardStore = defineStore('board', () => {
  // ==================== State ====================
  const boards = ref<Board[]>([])
  const currentBoard = ref<Board | null>(null)
  const boardShares = ref<BoardShare[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const viewType = ref<ViewType>('table')

  // ==================== Getters ====================
  // 활성화된 보드만 필터링
  const activeBoards = computed(() =>
    boards.value.filter(b => b.useYn === 'Y')
  )

  // 내가 소유한 보드
  const ownedBoards = computed(() =>
    boards.value.filter(b => b.ownerYn === 'Y')
  )

  // 공유받은 보드
  const sharedBoards = computed(() =>
    boards.value.filter(b => b.ownerYn !== 'Y')
  )

  // 현재 보드 ID
  const currentBoardId = computed(() => currentBoard.value?.boardId || null)

  // ==================== Internal Mutations ====================
  function _setBoards(newBoards: Board[]) {
    boards.value = newBoards
  }

  function _addBoard(board: Board) {
    boards.value.push(board)
  }

  function _updateBoard(boardId: number, data: Partial<Board>) {
    const index = boards.value.findIndex(b => b.boardId === boardId)
    if (index !== -1) {
      boards.value[index] = { ...boards.value[index], ...data }
    }
    if (currentBoard.value?.boardId === boardId) {
      currentBoard.value = { ...currentBoard.value, ...data }
    }
  }

  function _removeBoard(boardId: number) {
    boards.value = boards.value.filter(b => b.boardId !== boardId)
    if (currentBoard.value?.boardId === boardId) {
      currentBoard.value = null
    }
  }

  function _addBoardShare(share: BoardShare) {
    boardShares.value.push(share)
  }

  function _removeBoardShare(userId: number) {
    boardShares.value = boardShares.value.filter(s => s.userId !== userId)
  }

  // ==================== Actions ====================

  /**
   * 보드 목록 조회
   */
  async function fetchBoards(params?: { useYn?: string }): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await boardApi.getBoards(params)
      if (response.success && response.data) {
        _setBoards(response.data)
        return true
      }
      error.value = response.message || '보드 목록을 불러오는데 실패했습니다.'
      return false
    } catch (e) {
      error.value = '보드 목록을 불러오는데 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 보드 상세 조회
   */
  async function fetchBoard(boardId: number): Promise<Board | null> {
    loading.value = true
    error.value = null

    try {
      const response = await boardApi.getBoard(boardId)
      if (response.success && response.data) {
        currentBoard.value = response.data
        // 목록에도 반영
        _updateBoard(boardId, response.data)
        return response.data
      }
      error.value = response.message || '보드를 불러오는데 실패했습니다.'
      return null
    } catch (e) {
      error.value = '보드를 불러오는데 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 보드 생성
   */
  async function createBoard(data: BoardCreateRequest): Promise<Board | null> {
    loading.value = true
    error.value = null

    try {
      const response = await boardApi.createBoard(data)
      if (response.success && response.data) {
        _addBoard(response.data)
        return response.data
      }
      error.value = response.message || '보드 생성에 실패했습니다.'
      return null
    } catch (e) {
      error.value = '보드 생성에 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 보드 수정 (Optimistic Update)
   */
  async function updateBoard(boardId: number, data: BoardUpdateRequest): Promise<boolean> {
    // 1. 원본 데이터 백업
    const originalBoard = boards.value.find(b => b.boardId === boardId)
    if (!originalBoard) return false

    const originalData = { ...originalBoard }

    // 2. Store 먼저 갱신 (Optimistic Update)
    _updateBoard(boardId, data)

    try {
      // 3. API 호출
      const response = await boardApi.updateBoard(boardId, data)
      if (response.success && response.data) {
        // 서버 응답으로 최종 업데이트
        _updateBoard(boardId, response.data)
        return true
      }

      // 4. 실패 시 롤백
      _updateBoard(boardId, originalData)
      error.value = response.message || '보드 수정에 실패했습니다.'
      return false
    } catch (e) {
      // 4. 실패 시 롤백
      _updateBoard(boardId, originalData)
      error.value = '보드 수정에 실패했습니다.'
      return false
    }
  }

  /**
   * 보드 삭제 (Optimistic Update)
   */
  async function deleteBoard(boardId: number): Promise<boolean> {
    // 1. 원본 데이터 백업
    const originalBoards = [...boards.value]
    const originalCurrentBoard = currentBoard.value

    // 2. Store 먼저 갱신 (Optimistic Update)
    _removeBoard(boardId)

    try {
      // 3. API 호출
      const response = await boardApi.deleteBoard(boardId)
      if (response.success) {
        return true
      }

      // 4. 실패 시 롤백
      boards.value = originalBoards
      currentBoard.value = originalCurrentBoard
      error.value = response.message || '보드 삭제에 실패했습니다.'
      return false
    } catch (e) {
      // 4. 실패 시 롤백
      boards.value = originalBoards
      currentBoard.value = originalCurrentBoard
      error.value = '보드 삭제에 실패했습니다.'
      return false
    }
  }

  /**
   * 공유 사용자 목록 조회
   */
  async function fetchBoardShares(boardId: number): Promise<boolean> {
    try {
      const response = await boardApi.getBoardShares(boardId)
      if (response.success && response.data) {
        boardShares.value = response.data
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 공유 사용자 추가 (Optimistic Update)
   */
  async function addBoardShare(boardId: number, data: BoardShareRequest): Promise<BoardShare | null> {
    try {
      const response = await boardApi.addBoardShare(boardId, data)
      if (response.success && response.data) {
        _addBoardShare(response.data)
        return response.data
      }
      error.value = response.message || '공유 사용자 추가에 실패했습니다.'
      return null
    } catch (e) {
      error.value = '공유 사용자 추가에 실패했습니다.'
      return null
    }
  }

  /**
   * 공유 사용자 제거 (Optimistic Update)
   */
  async function removeBoardShare(boardId: number, userId: number): Promise<boolean> {
    // 1. 원본 데이터 백업
    const originalShares = [...boardShares.value]

    // 2. Store 먼저 갱신 (Optimistic Update)
    _removeBoardShare(userId)

    try {
      // 3. API 호출
      const response = await boardApi.removeBoardShare(boardId, userId)
      if (response.success) {
        return true
      }

      // 4. 실패 시 롤백
      boardShares.value = originalShares
      error.value = response.message || '공유 사용자 제거에 실패했습니다.'
      return false
    } catch (e) {
      // 4. 실패 시 롤백
      boardShares.value = originalShares
      error.value = '공유 사용자 제거에 실패했습니다.'
      return false
    }
  }

  // ==================== Utility Functions ====================
  function setCurrentBoard(board: Board | null) {
    currentBoard.value = board
  }

  function setViewType(type: ViewType) {
    viewType.value = type
    // localStorage에 저장하여 새로고침 후에도 유지
    localStorage.setItem('taskflow_viewType', type)
  }

  function loadViewType() {
    const saved = localStorage.getItem('taskflow_viewType') as ViewType | null
    if (saved && ['table', 'kanban', 'list'].includes(saved)) {
      viewType.value = saved
    }
  }

  function getBoardById(boardId: number): Board | undefined {
    return boards.value.find(b => b.boardId === boardId)
  }

  function clearBoards() {
    boards.value = []
    currentBoard.value = null
    boardShares.value = []
    error.value = null
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    boards,
    currentBoard,
    boardShares,
    loading,
    error,
    viewType,
    // Getters
    activeBoards,
    ownedBoards,
    sharedBoards,
    currentBoardId,
    // Actions
    fetchBoards,
    fetchBoard,
    createBoard,
    updateBoard,
    deleteBoard,
    fetchBoardShares,
    addBoardShare,
    removeBoardShare,
    // Utility
    setCurrentBoard,
    setViewType,
    loadViewType,
    getBoardById,
    clearBoards,
    clearError
  }
})
