import { get, post, put, del } from './client'
import type {
  Board,
  BoardCreateRequest,
  BoardUpdateRequest,
  BoardShare,
  BoardShareRequest,
  BoardShareUpdateRequest,
  BoardListResponse,
  BoardDeleteRequest,
  BoardOrderRequest,
  TransferPreviewResponse,
  TransferResultResponse
} from '@/types/board'

export const boardApi = {
  // =============================================
  // 보드 CRUD
  // =============================================

  getBoards(params?: { useYn?: string; owned?: boolean }) {
    return get<Board[]>('/boards', params)
  },

  getBoardList() {
    return get<BoardListResponse>('/boards/list')
  },

  getBoard(boardId: number) {
    return get<Board>(`/boards/${boardId}`)
  },

  createBoard(data: BoardCreateRequest) {
    return post<Board>('/boards', data)
  },

  updateBoard(boardId: number, data: BoardUpdateRequest) {
    return put<Board>(`/boards/${boardId}`, data)
  },

  deleteBoard(boardId: number) {
    return del<void>(`/boards/${boardId}`)
  },

  // =============================================
  // 보드 관리 (신규 기능)
  // =============================================

  updateBoardOrder(boardId: number, data: BoardOrderRequest) {
    return put<void>(`/boards/${boardId}/order`, data)
  },

  deleteBoardWithTransfer(boardId: number, data: BoardDeleteRequest) {
    return del<TransferResultResponse>(`/boards/${boardId}/with-transfer`, data)
  },

  getTransferPreview(boardId: number) {
    return get<TransferPreviewResponse>(`/boards/${boardId}/transfer-preview`)
  },

  // =============================================
  // 공유 관리
  // =============================================

  getBoardShares(boardId: number) {
    return get<BoardShare[]>(`/boards/${boardId}/shares`)
  },

  addBoardShare(boardId: number, data: BoardShareRequest) {
    return post<BoardShare>(`/boards/${boardId}/shares`, data)
  },

  updateBoardSharePermission(boardId: number, userId: number, data: BoardShareUpdateRequest) {
    return put<void>(`/boards/${boardId}/shares/${userId}`, data)
  },

  removeBoardShare(boardId: number, userId: number) {
    return del<void>(`/boards/${boardId}/shares/${userId}`)
  }
}
