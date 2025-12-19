/**
 * 댓글(리플) 타입 정의
 */

export interface Comment {
  commentId: number
  itemId: number
  itemTitle?: string
  content: string
  createdAt: string
  createdBy: number
  createdByName?: string
  updatedAt?: string
  updatedBy?: number
  updatedByName?: string
  edited: boolean
}

export interface CommentCreateRequest {
  content: string
}

export interface CommentUpdateRequest {
  content: string
}
