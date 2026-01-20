/**
 * Calendar/Gantt 타입 정의
 */

/**
 * 캘린더 이벤트 타입
 */
export type CalendarEventType = 'TODO' | 'ITEM' | 'EVENT'

/**
 * 캘린더 이벤트
 */
export interface CalendarEvent {
  type: CalendarEventType
  id: number
  title: string
  priority: string
  isCompleted?: boolean
  status?: string
  boardId?: number
  boardName?: string
  date: string
  parentItemId?: number
  itemDepth?: number
  // 반복 이벤트 관련
  isRecurring?: boolean
  recurrenceType?: string
  // 그룹 관련
  groupId?: number
  groupName?: string
  groupColor?: string
  // 소유자/공유 관련
  ownerUserName?: string
  isGroupShared?: boolean  // 그룹 멤버로서 조회되는 항목 (본인 소유가 아님)
}

/**
 * 날짜 기준 정보 (TB_CALENDAR_DATE)
 */
export interface CalendarDateInfo {
  calDate: string              // "2026-01-18"
  solarYear: number
  solarMonth: number
  solarDay: number
  dayOfWeek: number            // 1=일, 2=월, ..., 7=토
  dayOfWeekName: string        // "일", "월", ...
  weekOfYear: number
  weekOfMonth: number
  quarter: number
  lunarYear: number
  lunarMonth: number
  lunarDay: number
  isLeapMonth: boolean
  lunarDisplay: string         // "12.5" 또는 "윤4.1"
  isHoliday: boolean
  holidayName?: string         // "설날", "추석" 등
  holidayType?: string         // "SOLAR_FIXED", "LUNAR_FIXED", "SUBSTITUTE", "TEMPORARY"
  isWorkday: boolean
  isWeekend: boolean
}

/**
 * 캘린더 응답
 */
export interface CalendarResponse {
  events: Record<string, CalendarEvent[]>
  dateInfo?: Record<string, CalendarDateInfo>  // 날짜별 기준 정보
  year: number
  month: number
}

/**
 * 캘린더 조회 파라미터
 */
export interface CalendarParams {
  year?: number
  month?: number
  includeTodo?: boolean
  includeItem?: boolean
  boardId?: number
  groupId?: number
}

/**
 * Gantt 날짜 범위
 */
export interface GanttDateRange {
  start: string
  end: string
}

/**
 * Gantt 속성 정보 (시작일/완료일)
 */
export interface GanttPropertyInfo {
  startDatePropertyId?: number
  completionDatePropertyId?: number
  hasStartDateProperty: boolean
  hasCompletionDateProperty: boolean
}

/**
 * Gantt 업무 항목
 */
export interface GanttItem {
  itemId: number
  title: string
  description?: string
  parentItemId?: number
  parentTitle?: string
  itemDepth: number
  requestDate?: string
  dueDate?: string
  startDate?: string
  completionDate?: string
  hasStartDateProperty: boolean
  hasCompletionDateProperty: boolean
  status: string
  priority: string
  assigneeUserName?: string
  boardId: number
  boardName: string
  boardOwnerName?: string
  createdAt: string
  // 공유/배당 정보
  isSharedToMe?: boolean
  sharedByUserName?: string
  isAssignedToMe?: boolean
  assignedByUserName?: string
  isBoardShared?: boolean
  isItemShared?: boolean
}

/**
 * Gantt 응답
 */
export interface GanttResponse {
  dateRange: GanttDateRange
  propertyInfo: GanttPropertyInfo
  items: GanttItem[]
}

/**
 * Gantt 범위 타입
 */
export type GanttRangeType = 'FULL' | 'WEEK' | 'MONTH' | 'CUSTOM'

/**
 * Gantt 날짜 단위
 */
export type GanttDateUnit = 'DAY' | 'WEEK' | 'MONTH'

/**
 * Gantt 조회 파라미터
 */
export interface GanttParams {
  boardId?: number
  range?: GanttRangeType
  startDate?: string
  endDate?: string
  includeCompleted?: boolean
}

/**
 * 캘린더 뷰 타입
 */
export type CalendarViewType = 'month' | 'week' | 'day'

/**
 * 우선순위 색상 매핑
 */
export const PRIORITY_COLORS: Record<string, string> = {
  URGENT: '#ef4444',  // red-500
  HIGH: '#f97316',    // orange-500
  NORMAL: '#3b82f6',  // blue-500
  LOW: '#9ca3af'      // gray-400
}

/**
 * 상태 색상 매핑
 */
export const STATUS_COLORS: Record<string, string> = {
  NOT_STARTED: '#9ca3af',  // gray-400
  IN_PROGRESS: '#3b82f6',  // blue-500
  COMPLETED: '#22c55e',    // green-500
  DELETED: '#ef4444',      // red-500
  PENDING: '#f59e0b'       // amber-500
}

/**
 * Gantt 범례 항목
 */
export const GANTT_LEGEND = {
  REQUEST_DATE: { symbol: '◆', label: '요청일', color: '#3b82f6' },
  DUE_DATE: { symbol: '◇', label: '마감일', color: '#3b82f6' },
  START_DATE: { symbol: '●', label: '시작일', color: '#22c55e' },
  COMPLETION_DATE: { symbol: '○', label: '완료일', color: '#22c55e' }
}
