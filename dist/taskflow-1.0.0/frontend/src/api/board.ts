import { get, post, put, del } from './client'
import type {
  Board,
  BoardCreateRequest,
  BoardUpdateRequest,
  BoardShare,
  BoardShareRequest
} from '@/types/board'

export const boardApi = {
  getBoards(params?: { useYn?: string }) {
    return get<Board[]>('/boards', params)
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

  // 공유 사용자 관련
  getBoardShares(boardId: number) {
    return get<BoardShare[]>(`/boards/${boardId}/shares`)
  },

  addBoardShare(boardId: number, data: BoardShareRequest) {
    return post<BoardShare>(`/boards/${boardId}/shares`, data)
  },

  removeBoardShare(boardId: number, userId: number) {
    return del<void>(`/boards/${boardId}/shares/${userId}`)
  }
}
