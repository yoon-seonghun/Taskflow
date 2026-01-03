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
  username: string      // 로그인 ID (백엔드 FK 참조용)
  userName: string      // 표시용 이름
  departmentName?: string
}

export interface Board {
  boardId: number
  boardName: string
  boardDescription?: string
  description?: string  // API 응답 호환
  ownerUsername?: string   // 소유자 USERNAME
  ownerName?: string       // 소유자 표시명
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
  updatedAt?: string
}

export interface BoardCreateRequest {
  boardName: string
  boardDescription?: string
  defaultView?: ViewType
  color?: string
}

export interface BoardUpdateRequest {
  boardName?: string
  boardDescription?: string
  defaultView?: ViewType
  color?: string
}

export type BoardPermission = 'VIEW' | 'EDIT' | 'FULL' | 'OWNER'

export interface BoardShare {
  boardShareId: number
  boardId: number
  boardName?: string
  username: string        // FK 참조용 USERNAME
  userName?: string       // 표시용 이름
  departmentName?: string
  permission: BoardPermission
  createdAt: string
}

export interface BoardShareRequest {
  username: string  // 백엔드 API는 username 필요
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
  targetUsername?: string   // 이관 대상 사용자 USERNAME
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

/**
 * 보드 소유권 이전 요청 DTO
 */
export interface BoardTransferRequest {
  targetUsername: string  // 이관받을 사용자 USERNAME
  reason?: string         // 이관 사유 (선택)
}
