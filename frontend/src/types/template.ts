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
  defaultAssigneeId?: number
  defaultAssigneeName?: string
  defaultItemStatus?: DefaultItemStatus
  status: TemplateStatus
  sortOrder: number
  useCount: number
  createdAt: string
  createdBy: number
  createdByName?: string
  updatedAt?: string
  updatedBy?: number
  updatedByName?: string
}

/**
 * 작업 템플릿 생성 요청
 */
export interface TaskTemplateCreateRequest {
  content: string
  defaultAssigneeId?: number
  defaultItemStatus?: DefaultItemStatus
  sortOrder?: number
}

/**
 * 작업 템플릿 수정 요청
 */
export interface TaskTemplateUpdateRequest {
  content?: string
  defaultAssigneeId?: number
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
