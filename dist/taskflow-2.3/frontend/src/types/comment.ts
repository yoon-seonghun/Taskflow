/**
 * 댓글(리플) 타입 정의
 */

export interface Comment {
  commentId: number
  itemId: number
  itemTitle?: string
  content: string
  createdAt: string
  createdBy: string          // 작성자 USERNAME
  createdByUserId?: number   // 작성자 ID (SSE 이벤트용)
  createdByName?: string     // 작성자 표시명
  updatedAt?: string
  updatedBy?: string         // 수정자 USERNAME
  updatedByUserId?: number   // 수정자 ID (SSE 이벤트용)
  updatedByName?: string     // 수정자 표시명
  edited: boolean
}

export interface CommentCreateRequest {
  content: string
}

export interface CommentUpdateRequest {
  content: string
}
