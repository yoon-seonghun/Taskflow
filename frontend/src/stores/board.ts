import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { boardApi } from '@/api/board'
import type {
  Board,
  BoardCreateRequest,
  BoardUpdateRequest,
  BoardShare,
  BoardShareRequest,
  BoardShareUpdateRequest,
  BoardListResponse,
  BoardDeleteRequest,
  TransferPreviewResponse,
  TransferResultResponse
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
    boards.value.filter(b => b.isOwner === true)
  )

  // 공유받은 보드
  const sharedBoards = computed(() =>
    boards.value.filter(b => b.isOwner !== true)
  )

  // 현재 보드 ID
  const currentBoardId = computed(() => currentBoard.value?.boardId || null)

  // 접근 가능한 모든 보드 (소유 + 공유)
  const accessibleBoards = computed(() =>
    boards.value.filter(b => b.useYn === 'Y')
  )

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

  /**
   * 공유 권한 변경
   */
  async function updateBoardSharePermission(
    boardId: number,
    userId: number,
    data: BoardShareUpdateRequest
  ): Promise<boolean> {
    // 1. 원본 데이터 백업
    const originalShares = [...boardShares.value]
    const shareIndex = boardShares.value.findIndex(s => s.userId === userId)

    // 2. Store 먼저 갱신 (Optimistic Update)
    if (shareIndex !== -1) {
      boardShares.value[shareIndex] = {
        ...boardShares.value[shareIndex],
        permission: data.permission
      }
    }

    try {
      const response = await boardApi.updateBoardSharePermission(boardId, userId, data)
      if (response.success) {
        return true
      }

      // 실패 시 롤백
      boardShares.value = originalShares
      error.value = response.message || '권한 변경에 실패했습니다.'
      return false
    } catch (e) {
      // 실패 시 롤백
      boardShares.value = originalShares
      error.value = '권한 변경에 실패했습니다.'
      return false
    }
  }

  // ==================== 보드 관리 (신규 기능) ====================

  /**
   * 보드 목록 조회 (소유/공유 분리)
   */
  async function fetchBoardList(): Promise<BoardListResponse | null> {
    loading.value = true
    error.value = null

    try {
      const response = await boardApi.getBoardList()
      if (response.success && response.data) {
        // 모든 보드를 합쳐서 저장
        const allBoards = [
          ...response.data.ownedBoards,
          ...response.data.sharedBoards
        ]
        _setBoards(allBoards)
        return response.data
      }
      error.value = response.message || '보드 목록을 불러오는데 실패했습니다.'
      return null
    } catch (e) {
      error.value = '보드 목록을 불러오는데 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 보드 순서 변경
   */
  async function updateBoardOrder(boardId: number, sortOrder: number): Promise<boolean> {
    // 1. 원본 데이터 백업
    const originalBoard = boards.value.find(b => b.boardId === boardId)
    if (!originalBoard) return false

    const originalSortOrder = originalBoard.sortOrder

    // 2. Store 먼저 갱신 (Optimistic Update)
    _updateBoard(boardId, { sortOrder })

    try {
      const response = await boardApi.updateBoardOrder(boardId, { sortOrder })
      if (response.success) {
        return true
      }

      // 실패 시 롤백
      _updateBoard(boardId, { sortOrder: originalSortOrder })
      error.value = response.message || '순서 변경에 실패했습니다.'
      return false
    } catch (e) {
      // 실패 시 롤백
      _updateBoard(boardId, { sortOrder: originalSortOrder })
      error.value = '순서 변경에 실패했습니다.'
      return false
    }
  }

  /**
   * 보드 삭제 (이관 포함)
   */
  async function deleteBoardWithTransfer(
    boardId: number,
    data: BoardDeleteRequest
  ): Promise<TransferResultResponse | null> {
    loading.value = true
    error.value = null

    // 1. 원본 데이터 백업
    const originalBoards = [...boards.value]
    const originalCurrentBoard = currentBoard.value

    try {
      const response = await boardApi.deleteBoardWithTransfer(boardId, data)
      if (response.success) {
        // 성공 시 보드 제거
        _removeBoard(boardId)
        return response.data || null
      }

      error.value = response.message || '보드 삭제에 실패했습니다.'
      return null
    } catch (e) {
      // 실패 시 롤백
      boards.value = originalBoards
      currentBoard.value = originalCurrentBoard
      error.value = '보드 삭제에 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 이관 대상 업무 미리보기
   */
  async function getTransferPreview(boardId: number): Promise<TransferPreviewResponse | null> {
    try {
      const response = await boardApi.getTransferPreview(boardId)
      if (response.success && response.data) {
        return response.data
      }
      error.value = response.message || '미리보기를 불러오는데 실패했습니다.'
      return null
    } catch (e) {
      error.value = '미리보기를 불러오는데 실패했습니다.'
      return null
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
    accessibleBoards,
    // Actions
    fetchBoards,
    fetchBoard,
    fetchBoardList,
    createBoard,
    updateBoard,
    updateBoardOrder,
    deleteBoard,
    deleteBoardWithTransfer,
    getTransferPreview,
    fetchBoardShares,
    addBoardShare,
    updateBoardSharePermission,
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
