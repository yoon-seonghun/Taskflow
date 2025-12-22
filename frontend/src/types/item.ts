/**
 * 아이템(업무) 타입 정의
 */

import type { PageResponse } from './api'

export type ItemStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'PENDING' | 'COMPLETED' | 'DELETED'
export type Priority = 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW'

export interface Item {
  itemId: number
  boardId: number
  boardName?: string
  groupId?: number
  groupName?: string
  groupColor?: string
  title: string
  content?: string
  description?: string  // 상세 내용 (마크다운)
  status: ItemStatus
  priority: Priority
  assigneeId?: number
  assigneeName?: string
  startTime?: string
  endTime?: string
  dueDate?: string        // DB에 없음, endTime으로 대체 (null 반환)
  sortOrder?: number      // DB에 없음 (null 반환)
  completedAt?: string
  completedBy?: number    // DB에 없음 (null 반환)
  deletedAt?: string
  deletedBy?: number      // DB에 없음 (null 반환)
  commentCount?: number
  createdAt: string
  createdBy?: number
  createdByName?: string
  updatedAt?: string
  updatedBy?: number
  updatedByName?: string
  properties?: ItemProperty[]
  propertyValues?: Record<number, unknown>
}

export interface ItemProperty {
  itemPropertyId: number
  itemId: number
  propertyId: number
  propertyName?: string
  propertyCode?: string
  propertyType?: string
  valueText?: string
  valueNumber?: number
  valueDate?: string
  valueUserId?: number
  valueUserName?: string
  optionName?: string
  optionColor?: string
}

export interface ItemCreateRequest {
  title: string
  content?: string
  description?: string  // 상세 내용 (마크다운)
  status?: ItemStatus
  priority?: Priority
  groupId?: number
  assigneeId?: number
  startTime?: string
  endTime?: string
  dueDate?: string
  sortOrder?: number
  properties?: Record<number, unknown>
}

export interface ItemUpdateRequest {
  title?: string
  content?: string
  description?: string  // 상세 내용 (마크다운)
  status?: ItemStatus
  priority?: Priority
  groupId?: number
  assigneeId?: number
  startTime?: string
  endTime?: string
  dueDate?: string
  sortOrder?: number
  properties?: Record<number, unknown>
}

export interface ItemSearchRequest {
  keyword?: string
  status?: ItemStatus
  priority?: Priority
  assigneeId?: number
  groupId?: number
  startDate?: string
  endDate?: string
  includeCompleted?: boolean
  includeDeleted?: boolean
  page?: number
  size?: number
  sort?: string
}

/**
 * Cross-board 검색 요청 DTO
 */
export interface CrossBoardSearchRequest {
  keyword?: string
  status?: ItemStatus
  priority?: Priority
  assigneeId?: number
  groupId?: number
  boardId?: number
  overdueOnly?: boolean
  startDate?: string
  endDate?: string
  page?: number
  size?: number
  sort?: string
}

/**
 * 페이징 응답 DTO (PageResponse<Item>의 타입 별칭)
 */
export type ItemPageResponse = PageResponse<Item>

/**
 * Cross-board 통계 DTO
 */
export interface CrossBoardStats {
  overdueCount: number
  pendingCount: number
  activeCount: number
  urgentOverdueCount: number
  highOverdueCount: number
}

/**
 * 업무 이관 요청 DTO
 */
export interface ItemTransferRequest {
  targetBoardId?: number
  targetUserId?: number
  reason?: string
}

/**
 * 업무 공유 정보 DTO
 */
export interface ItemShare {
  itemShareId: number
  itemId: number
  userId: number
  loginId?: string
  userName?: string
  departmentName?: string
  permission: 'VIEW' | 'EDIT' | 'FULL'
  canView: boolean
  canEdit: boolean
  canDelete: boolean
  createdAt?: string
  updatedAt?: string
}
