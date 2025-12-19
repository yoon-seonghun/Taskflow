import { get, post, put, del } from './client'
import type { Comment, CommentCreateRequest, CommentUpdateRequest } from '@/types/comment'

export const commentApi = {
  getComments(itemId: number) {
    return get<Comment[]>(`/items/${itemId}/comments`)
  },

  createComment(itemId: number, data: CommentCreateRequest) {
    return post<Comment>(`/items/${itemId}/comments`, data)
  },

  updateComment(commentId: number, data: CommentUpdateRequest) {
    return put<Comment>(`/comments/${commentId}`, data)
  },

  deleteComment(commentId: number) {
    return del<void>(`/comments/${commentId}`)
  }
}
