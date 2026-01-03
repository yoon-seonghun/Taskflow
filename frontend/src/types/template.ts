/**
 * 작업 템플릿 타입 정의
 * 백엔드 TaskTemplateResponse/TaskTemplateSearchResponse와 일치
 */

export type TemplateStatus = 'ACTIVE' | 'INACTIVE'

export type DefaultItemStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'DELETED'

/**
 * 작업 템플릿 응답
 */
export interface TaskTemplate {
  templateId: number
  content: string
  defaultItemStatus?: DefaultItemStatus
  status: TemplateStatus
  sortOrder: number
  useCount: number
  createdAt: string
  createdBy: string          // 생성자 USERNAME
  createdByName?: string     // 생성자 표시명
  updatedAt?: string
  updatedBy?: string         // 수정자 USERNAME
  updatedByName?: string     // 수정자 표시명
}

/**
 * 작업 템플릿 생성 요청
 */
export interface TaskTemplateCreateRequest {
  content: string
  defaultAssigneeUsername?: string   // 기본 담당자 USERNAME
  defaultItemStatus?: DefaultItemStatus
  sortOrder?: number
}

/**
 * 작업 템플릿 수정 요청
 */
export interface TaskTemplateUpdateRequest {
  content?: string
  defaultAssigneeUsername?: string   // 기본 담당자 USERNAME
  defaultItemStatus?: DefaultItemStatus
  status?: TemplateStatus
  sortOrder?: number
}

/**
 * 작업 템플릿 검색 결과 (자동완성용)
 */
export interface TaskTemplateSearchResult {
  templateId: number
  content: string
  useCount: number
}
