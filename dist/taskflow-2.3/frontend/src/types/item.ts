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
  ownerUsername?: string     // 현재 소유자 USERNAME (v2.2.1)
  ownerName?: string         // 소유자 이름 (v2.2.1)
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
  shareCount?: number     // 공유 사용자 수 (v2.2.1)
  createdAt: string
  createdBy?: string          // 생성자 USERNAME
  createdByName?: string
  updatedAt?: string
  updatedByName?: string
  properties?: ItemProperty[]
  propertyValues?: Record<number, unknown>
  // 공유/이관 정보
  isSharedToMe?: boolean        // 공유받은 업무 여부
  sharedByUsername?: string     // 공유해준 사용자 USERNAME
  sharedByUserName?: string     // 공유해준 사용자 이름
  transferredFrom?: number      // 이관 원본 보드 ID
  transferredByUsername?: string // 이관해준 사용자 USERNAME
  transferredByUserName?: string // 이관해준 사용자 이름
  // 배당 정보 (v2.1)
  isAssignedToMe?: boolean       // 배당받은 업무 여부
  assignedByUsername?: string    // 배당해준 사용자 USERNAME
  assignedByUserName?: string    // 배당해준 사용자 이름
  assignedAt?: string            // 배당일시
  // 배당 대상 정보 (소유자 화면용 v2.1)
  assignedToUsername?: string    // 배당 대상 사용자 USERNAME
  assignedToUserName?: string    // 배당 대상 사용자 이름
  // 하위 업무 정보 (v2.2)
  parentItemId?: number          // 부모 업무 ID (NULL이면 기본 업무)
  itemDepth?: number             // 업무 깊이 (0=기본, 1~2=하위)
  childSortOrder?: number        // 하위 업무 내 정렬 순서
  childCount?: number            // 전체 하위 업무 수
  completedChildCount?: number   // 완료된 하위 업무 수
  hasChildren?: boolean          // 하위 업무 존재 여부
  canCreateChild?: boolean       // 하위 업무 생성 가능 여부 (depth < 2)
  parentInfo?: ParentInfo        // 부모 업무 정보
  rootInfo?: ParentInfo          // 최상위 부모 업무 정보
  children?: Item[]              // 하위 업무 목록 (트리 조회 시)
}

/**
 * 부모 업무 간략 정보 (v2.2)
 */
export interface ParentInfo {
  itemId: number
  title: string
  status: ItemStatus
}

export interface ItemProperty {
  itemPropertyId: number
  itemId: number
  propertyId: number
  propertyName?: string
  propertyCode?: string
  propertyType?: string
  ownerType?: 'GLOBAL' | 'MANAGER' | 'USER'  // 속성 소유자 타입
  dataSourceType?: 'INTERNAL' | 'EXTERNAL'   // 데이터 소스 타입
  externalQueryId?: number                    // 외부 쿼리 ID
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
  sortField?: string          // 정렬 필드 (sortOrder, createdAt, dueDate 등)
  sortDirection?: 'asc' | 'desc'  // 정렬 방향
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

/**
 * 아이템 순서 변경 요청 DTO
 */
export interface ItemReorderRequest {
  itemId: number
  newOrder: number
}

// =============================================
// v2.2: 하위 업무 (Sub-Task) 관련 타입
// =============================================

/**
 * 하위 업무 생성 요청 DTO (v2.2)
 */
export interface SubTaskCreateRequest {
  title: string
  content?: string
  description?: string
  status?: ItemStatus
  priority?: Priority
  assigneeUsername?: string
  requestDate?: string
  dueDate?: string
  properties?: Record<number, unknown>
}

/**
 * 하위 업무 순서 변경 요청 DTO (v2.2)
 */
export interface SubTaskReorderRequest {
  orders: SubTaskOrderItem[]
}

/**
 * 하위 업무 순서 항목 (v2.2)
 */
export interface SubTaskOrderItem {
  itemId: number
  sortOrder: number
}

/**
 * 상위 계층 응답 (Breadcrumb용, v2.2)
 */
export interface AncestorResponse {
  itemId: number
  itemDepth: number
  title: string
  status: ItemStatus
}

/**
 * 업무 완료 요청 DTO (v2.2)
 */
export interface ItemCompleteRequest {
  forceComplete?: boolean  // true: 미완료 하위 업무가 있어도 강제 완료
}

/**
 * 미완료 하위 업무 응답 DTO (v2.2)
 */
export interface IncompleteChildrenResponse {
  incompleteChildCount: number
  children: ChildSummary[]
}

/**
 * 하위 업무 간략 정보 (v2.2)
 */
export interface ChildSummary {
  itemId: number
  title: string
  status: ItemStatus
  itemDepth: number
}
