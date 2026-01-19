import { get, post, put, del } from './client'
import type {
  CalendarResponse,
  CalendarParams,
  GanttResponse,
  GanttParams
} from '@/types/calendar'

// 음력 관련 타입
export interface LunarDateResponse {
  year: number
  month: number
  day: number
  isLeapMonth: boolean
  displayText: string
}

export interface LunarConversionResponse {
  solar: string
  lunar: LunarDateResponse
}

export interface UserCalendarResponse {
  calendarId: number
  calendarName: string
  description: string | null
  color: string
  isDefault: boolean
  sortOrder: number
  ownerUsername: string
  ownerName: string
  // 공유 정보
  isShared: boolean
  sharedByUsername?: string
  sharedByName?: string
  shareType?: string  // VIEW, EDIT
  eventCount?: number
  createdAt: string
}

// CalendarFilterPanel용 타입 (캘린더 목록)
export interface CalendarListResponse {
  calendarId: number
  name: string
  description: string | null
  color: string
  isDefault: boolean
  sortOrder: number
  ownerUsername: string
  ownerName: string
  isOwner: boolean
  accessType?: string
}

export interface EventDetailResponse {
  eventId: number
  calendarId: number
  calendarName: string
  calendarColor: string
  ownerUsername: string
  ownerName: string
  title: string
  description: string | null
  location: string | null
  startDate: string
  endDate: string
  startTime: string | null
  endTime: string | null
  isAllDay: boolean
  isLunar: boolean
  lunarMonth: number | null
  lunarDay: number | null
  lunarLeapMonth: boolean | null
  isRecurring: boolean
  recurrenceType: string | null
  recurrenceInterval: number | null
  recurrenceEndDate: string | null
  recurrenceCount: number | null
  recurrenceDays: string | null
  accessType: string
  isShared: boolean
  sharedByUserName: string | null
  createdAt: string
  updatedAt: string | null
}

export interface EventRequest {
  calendarId: number
  title: string
  description?: string
  location?: string
  startDate: string
  endDate?: string
  startTime?: string
  endTime?: string
  isAllDay?: boolean
  isLunar?: boolean
  lunarMonth?: number
  lunarDay?: number
  lunarLeapMonth?: boolean
  isRecurring?: boolean
  recurrenceType?: string
  recurrenceInterval?: number
  recurrenceEndDate?: string
  recurrenceCount?: number
  recurrenceDays?: string
}

export const calendarApi = {
  /**
   * 캘린더 데이터 조회
   * @param params 조회 파라미터
   * @returns 캘린더 응답
   */
  getCalendarData(params?: CalendarParams) {
    return get<CalendarResponse>('/calendar', params)
  },

  /**
   * Gantt 차트 데이터 조회
   * @param params 조회 파라미터
   * @returns Gantt 응답
   */
  getGanttData(params?: GanttParams) {
    return get<GanttResponse>('/gantt', params)
  },

  // =============================================
  // 음력 변환 API
  // =============================================

  /**
   * 양력 → 음력 변환
   * @param solarDate 양력 날짜 (YYYY-MM-DD)
   */
  solarToLunar(solarDate: string) {
    return get<LunarConversionResponse>('/calendar/lunar', { solar: solarDate })
  },

  /**
   * 음력 → 양력 변환
   * @param year 음력 연도
   * @param month 음력 월
   * @param day 음력 일
   * @param leapMonth 윤달 여부
   */
  lunarToSolar(year: number, month: number, day: number, leapMonth: boolean = false) {
    return get<LunarConversionResponse>('/calendar/solar', {
      year,
      month,
      day,
      leapMonth
    })
  },

  /**
   * 월간 음력 데이터 조회 (캐싱용)
   * @param year 양력 연도
   * @param month 양력 월
   */
  getMonthLunarDates(year: number, month: number) {
    return get<Record<string, LunarDateResponse>>('/calendar/lunar/month', { year, month })
  },

  /**
   * 음력 날짜 유효성 검증
   */
  validateLunarDate(year: number, month: number, day: number, leapMonth: boolean = false) {
    return get<{
      isValid: boolean
      leapMonthOfYear: number | null
      monthDays: number
    }>('/calendar/lunar/valid', {
      year,
      month,
      day,
      leapMonth
    })
  },

  /**
   * 연도별 윤달 정보 조회
   */
  getLeapMonthInfo(year: number) {
    return get<{
      year: number
      hasLeapMonth: boolean
      leapMonth: number | null
      leapMonthDays: number | null
    }>('/calendar/lunar/leap', { year })
  },

  // =============================================
  // 사용자 캘린더 API
  // =============================================

  /**
   * 내 캘린더 목록 조회
   */
  getMyCalendars() {
    return get<UserCalendarResponse[]>('/calendars')
  },

  /**
   * 캘린더 목록 조회 (필터 패널용 - 내 캘린더 + 공유 캘린더)
   */
  getCalendars() {
    return get<CalendarListResponse[]>('/calendars')
  },

  /**
   * 캘린더 상세 조회
   */
  getCalendar(calendarId: number) {
    return get<UserCalendarResponse>(`/calendars/${calendarId}`)
  },

  /**
   * 캘린더 생성
   */
  createCalendar(data: {
    calendarName: string
    description?: string
    color?: string
    isDefault?: boolean
  }) {
    return post<UserCalendarResponse>('/calendars', data)
  },

  /**
   * 캘린더 수정
   */
  updateCalendar(calendarId: number, data: {
    calendarName?: string
    description?: string
    color?: string
    isDefault?: boolean
    sortOrder?: number
  }) {
    return put<UserCalendarResponse>(`/calendars/${calendarId}`, data)
  },

  /**
   * 캘린더 삭제
   */
  deleteCalendar(calendarId: number) {
    return del<void>(`/calendars/${calendarId}`)
  },

  // =============================================
  // 이벤트 API
  // =============================================

  /**
   * 이벤트 목록 조회 (기간별)
   */
  getEvents(params: {
    startDate: string
    endDate: string
    calendarId?: number
    includeShared?: boolean
  }) {
    return get<EventDetailResponse[]>('/events', params)
  },

  /**
   * 이벤트 상세 조회
   */
  getEvent(eventId: number) {
    return get<EventDetailResponse>(`/events/${eventId}`)
  },

  /**
   * 이벤트 생성
   */
  createEvent(data: EventRequest) {
    return post<EventDetailResponse>('/events', data)
  },

  /**
   * 이벤트 수정
   */
  updateEvent(eventId: number, data: EventRequest) {
    return put<EventDetailResponse>(`/events/${eventId}`, data)
  },

  /**
   * 이벤트 삭제
   */
  deleteEvent(eventId: number) {
    return del<void>(`/events/${eventId}`)
  },

  /**
   * 이벤트 공유 목록 조회
   */
  getEventShares(eventId: number) {
    return get<{
      username: string
      userName: string
      departmentName: string | null
      shareType: string
      sharedAt: string
    }[]>(`/events/${eventId}/shares`)
  },

  /**
   * 이벤트 공유 추가
   */
  addEventShare(eventId: number, data: { username: string; shareType: string }) {
    return post<void>(`/events/${eventId}/shares`, data)
  },

  /**
   * 이벤트 공유 해제
   */
  removeEventShare(eventId: number, username: string) {
    return del<void>(`/events/${eventId}/shares/${username}`)
  },

  /**
   * 이벤트 이관
   */
  transferEvent(eventId: number, targetUsername: string) {
    return post<EventDetailResponse>(`/events/${eventId}/transfer`, { targetUsername })
  }
}
