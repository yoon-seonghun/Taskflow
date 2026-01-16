/**
 * 전체 업무 타입 정의 (v2.1)
 * - 소유 보드 + 공유 보드 + 개별 공유/배당 업무 통합 조회
 */

import type { PageResponse } from './api'
import type { Item, ItemStatus, Priority, ParentInfo, ItemProperty } from './item'

/**
 * 업무 출처 타입
 */
export type SourceType = 'OWNED' | 'BOARD_SHARED' | 'ITEM_SHARED' | 'ASSIGNED' | 'ALL'

/**
 * 하위 업무 표시 모드
 */
export type ChildDisplayMode = 'flat' | 'tree' | 'collapse'

/**
 * 전체 업무 응답 (Item 확장)
 */
export interface AllItemResponse extends Omit<Item, 'children'> {
  // 출처 관련 필드
  sourceType: SourceType         // 업무 출처 타입
  boardColor?: string            // 보드 색상
  sharedAt?: string              // 공유 일시

  // 배당 관련 필드
  shareType?: 'SHARE' | 'ASSIGN' // 공유 유형
  permission?: 'VIEW' | 'EDIT' | 'FULL' // 권한
  assignedByName?: string        // 배당자 이름 (sharedByName 대응)

  // 하위 업무 (재귀)
  children?: AllItemResponse[]
}

/**
 * 전체 업무 통계
 */
export interface AllItemsStats {
  totalCount: number
  ownedCount: number
  boardSharedCount: number
  itemSharedCount: number
  assignedCount: number
  rootTaskCount: number
  childTaskCount: number
  byStatus: Record<string, number>
  byPriority: Record<string, number>
}

/**
 * 전체 업무 페이지 응답
 */
export interface AllItemsPageResponse extends PageResponse<AllItemResponse> {
  stats: AllItemsStats
}

/**
 * 전체 업무 검색 요청
 */
export interface AllItemsSearchRequest {
  keyword?: string
  status?: ItemStatus
  priority?: Priority
  sourceType?: SourceType
  boardId?: number
  groupId?: number
  assigneeUsername?: string
  assignedByUsername?: string
  startDate?: string
  endDate?: string
  includeCompleted?: boolean
  includeDeleted?: boolean
  includeChildren?: boolean
  parentItemId?: number
  depthFilter?: number
  page?: number
  size?: number
  sortField?: string
  sortDirection?: 'asc' | 'desc'
}

/**
 * 전체 업무 필터 상태
 */
export interface AllItemsFilter {
  keyword: string
  status: ItemStatus | null
  priority: Priority | null
  sourceType: SourceType
  boardId: number | null
  groupId: number | null
  assigneeUsername: string | null
  assignedByUsername: string | null
  startDate: string | null
  endDate: string | null
  includeCompleted: boolean
  includeDeleted: boolean
  includeChildren: boolean
  depthFilter: number | null
  childDisplayMode: ChildDisplayMode
  sortField: string
  sortDirection: 'asc' | 'desc'
}

/**
 * 기본 필터 값
 */
export const defaultAllItemsFilter: AllItemsFilter = {
  keyword: '',
  status: null,
  priority: null,
  sourceType: 'ALL',
  boardId: null,
  groupId: null,
  assigneeUsername: null,
  assignedByUsername: null,
  startDate: null,
  endDate: null,
  includeCompleted: false,
  includeDeleted: false,
  includeChildren: true,
  depthFilter: null,
  childDisplayMode: 'tree',
  sortField: 'createdAt',
  sortDirection: 'desc'
}

/**
 * 출처 타입 라벨
 */
export const sourceTypeLabels: Record<SourceType, string> = {
  ALL: '전체',
  OWNED: '내 보드',
  BOARD_SHARED: '공유 보드',
  ITEM_SHARED: '공유 업무',
  ASSIGNED: '배당 업무'
}

/**
 * 출처 타입 아이콘 (Emoji)
 */
export const sourceTypeIcons: Record<SourceType, string> = {
  ALL: '📋',
  OWNED: '🏠',
  BOARD_SHARED: '📤',
  ITEM_SHARED: '📥',
  ASSIGNED: '🎯'
}

/**
 * 출처 타입 색상 (Tailwind CSS 클래스)
 */
export const sourceTypeColors: Record<SourceType, { bg: string; text: string; darkBg: string; darkText: string }> = {
  ALL: { bg: 'bg-gray-100', text: 'text-gray-600', darkBg: 'dark:bg-gray-700', darkText: 'dark:text-gray-300' },
  OWNED: { bg: 'bg-gray-100', text: 'text-gray-600', darkBg: 'dark:bg-gray-700', darkText: 'dark:text-gray-300' },
  BOARD_SHARED: { bg: 'bg-blue-100', text: 'text-blue-600', darkBg: 'dark:bg-blue-900/50', darkText: 'dark:text-blue-400' },
  ITEM_SHARED: { bg: 'bg-purple-100', text: 'text-purple-600', darkBg: 'dark:bg-purple-900/50', darkText: 'dark:text-purple-400' },
  ASSIGNED: { bg: 'bg-green-100', text: 'text-green-600', darkBg: 'dark:bg-green-900/50', darkText: 'dark:text-green-400' }
}
