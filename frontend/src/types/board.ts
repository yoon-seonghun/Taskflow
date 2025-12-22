/**
 * 보드(컬렉션) 타입 정의
 */

export type ViewType = 'TABLE' | 'KANBAN' | 'LIST'

export interface BoardGroup {
  groupId: number
  groupCode: string
  groupName: string
  description?: string
  color?: string
  sortOrder?: number
  useYn?: string
  memberCount?: number
}

export interface BoardSharedUser {
  userId: number
  userName: string
  departmentName?: string
}

export interface Board {
  boardId: number
  boardName: string
  boardDescription?: string
  description?: string  // API 응답 호환
  ownerId: number
  ownerName?: string
  defaultView: ViewType
  boardColor?: string
  color?: string  // API 응답 호환
  shareCount: number
  itemCount?: number
  pendingItemCount?: number
  sortOrder?: number
  useYn?: string
  ownerYn?: string
  currentUserPermission?: BoardPermission
  isOwner?: boolean
  groups?: BoardGroup[]
  sharedUsers?: BoardSharedUser[]
  createdAt: string
  createdBy?: number
  updatedAt?: string
  updatedBy?: number
}

export interface BoardCreateRequest {
  boardName: string
  boardDescription?: string
  defaultView?: ViewType
  boardColor?: string
}

export interface BoardUpdateRequest {
  boardName?: string
  boardDescription?: string
  defaultView?: ViewType
  boardColor?: string
}

export type BoardPermission = 'VIEW' | 'EDIT' | 'FULL' | 'OWNER'

export interface BoardShare {
  boardShareId: number
  boardId: number
  userId: number
  userName?: string
  loginId?: string
  departmentName?: string
  permission: BoardPermission
  createdAt: string
  createdBy: number
}

export interface BoardShareRequest {
  userId: number
  permission: BoardPermission
}

export interface BoardShareUpdateRequest {
  permission: BoardPermission
}

export interface BoardListResponse {
  ownedBoards: Board[]
  sharedBoards: Board[]
  totalOwnedCount: number
  totalSharedCount: number
}

export interface BoardDeleteRequest {
  targetUserId?: number
  forceDelete?: boolean
}

export interface BoardOrderRequest {
  sortOrder: number
}

export interface TransferPreviewResponse {
  boardId: number
  boardName: string
  pendingItems: PendingItem[]
  totalCount: number
}

export interface PendingItem {
  itemId: number
  title: string
  status: string
  priority?: string
  assigneeName?: string
}

export interface TransferResultResponse {
  transferredCount: number
  newBoardId: number
  newBoardName: string
  message: string
}
