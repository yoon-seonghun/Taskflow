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
  categoryId?: number
  categoryName?: string
  categoryColor?: string
  title: string
  content?: string
  description?: string  // 상세 내용 (마크다운)
  status: ItemStatus
  priority: Priority
  assigneeUsername?: string   // 담당자 USERNAME (백엔드 FK 참조용)
  assigneeName?: string       // 담당자 표시용 이름
  requestDate?: string        // 요청일
  dueDate?: string            // 마감일 (지연 판정에 사용)
  sortOrder?: number
  completedAt?: string    // 완료일시
  deletedAt?: string      // 삭제일시
  commentCount?: number
  createdAt: string
  createdByName?: string
  updatedAt?: string
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
  ownerType?: 'GLOBAL' | 'MANAGER' | 'USER'  // 속성 소유자 타입
  value?: unknown           // 실제 값
  displayValue?: string     // 표시용 값 (옵션명, 사용자명 등)
  color?: string            // 옵션 색상
  sortOrder?: number        // 정렬 순서 (TB_ITEM_PROPERTY)
}

export interface ItemCreateRequest {
  title: string
  content?: string
  description?: string  // 상세 내용 (마크다운)
  status?: ItemStatus
  priority?: Priority
  groupId?: number
  categoryId?: number
  assigneeUsername?: string   // 담당자 USERNAME
  requestDate?: string        // 요청일
  dueDate?: string            // 마감일
  sortOrder?: number
  propertyIds?: number[]      // v2.0: 선택된 속성 ID 목록
  properties?: Record<number, unknown>
  propertySortOrders?: Record<number, number>  // 속성 정렬 순서 (propertyId -> sortOrder)
}

export interface ItemUpdateRequest {
  title?: string
  content?: string
  description?: string  // 상세 내용 (마크다운)
  status?: ItemStatus
  priority?: Priority
  groupId?: number
  categoryId?: number
  assigneeUsername?: string   // 담당자 USERNAME
  requestDate?: string        // 요청일
  dueDate?: string            // 마감일
  sortOrder?: number
  propertyIds?: number[]      // v2.0: 선택된 속성 ID 목록
  properties?: Record<number, unknown>
  propertySortOrders?: Record<number, number>  // 속성 정렬 순서 (propertyId -> sortOrder)
  /** null로 설정할 필드 목록 (예: ['requestDate', 'dueDate']) - snake_case로 전송됨 */
  clear_fields?: string[]
}

export interface ItemSearchRequest {
  keyword?: string
  status?: ItemStatus
  priority?: Priority
  assigneeUsername?: string   // 담당자 USERNAME
  groupId?: number
  departmentCode?: string     // 부서 CODE (외부 연동 키 정책)
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
  assigneeUsername?: string   // 담당자 USERNAME
  groupId?: number
  departmentCode?: string     // 부서 CODE (외부 연동 키 정책)
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
  targetUsername?: string   // 이관 대상 사용자 USERNAME
  reason?: string
}

/**
 * 업무 공유 정보 DTO
 */
export interface ItemShare {
  itemShareId: number
  itemId: number
  username: string          // 공유 사용자 USERNAME
  userName?: string         // 표시용 이름
  departmentName?: string
  permission: 'VIEW' | 'EDIT' | 'FULL'
  canView: boolean
  canEdit: boolean
  canDelete: boolean
  createdAt?: string
  updatedAt?: string
}

/**
 * 아이템 속성 순서 변경 요청 DTO
 */
export interface ItemPropertySortRequest {
  orders: PropertySortOrder[]
}

/**
 * 속성 순서 정보
 */
export interface PropertySortOrder {
  itemPropertyId: number
  sortOrder: number
}
