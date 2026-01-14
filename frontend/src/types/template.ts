/**
 * 작업 템플릿 타입 정의
 * 백엔드 TaskTemplateResponse/TaskTemplateSearchResponse와 일치
 */

export type TemplateStatus = 'ACTIVE' | 'INACTIVE'

export type DefaultItemStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'DELETED'

export type TemplateOwnerType = 'GLOBAL' | 'MANAGER' | 'USER'

/**
 * 템플릿 속성 응답
 */
export interface TemplateProperty {
  id?: number
  propertyId: number
  propertyName?: string
  propertyType?: string
  ownerType?: string
  sortOrder?: number
  defaultValue?: string
  requiredYn?: string
}

/**
 * 템플릿 속성 요청
 */
export interface TemplatePropertyRequest {
  propertyId: number
  sortOrder?: number
  defaultValue?: string
  requiredYn?: string
}

/**
 * 작업 템플릿 응답
 */
export interface TaskTemplate {
  templateId: number
  content: string
  defaultAssigneeUsername?: string  // 기본 담당자 USERNAME
  defaultAssigneeName?: string      // 기본 담당자 표시명
  defaultItemStatus?: DefaultItemStatus
  categoryId?: number | null
  categoryName?: string
  categoryColor?: string
  ownerType?: TemplateOwnerType     // 소유 유형 (GLOBAL/MANAGER/USER)
  ownerUsername?: string            // 소유자 USERNAME
  ownerDeptCode?: string            // 소유자 부서코드
  status: TemplateStatus
  sortOrder: number
  useCount: number
  createdAt: string
  createdBy: string          // 생성자 USERNAME
  createdByName?: string     // 생성자 표시명
  updatedAt?: string
  updatedBy?: string         // 수정자 USERNAME
  updatedByName?: string     // 수정자 표시명
  properties?: TemplateProperty[]
}

/**
 * 작업 템플릿 생성 요청
 */
export interface TaskTemplateCreateRequest {
  content: string
  defaultAssigneeUsername?: string   // 기본 담당자 USERNAME
  defaultItemStatus?: DefaultItemStatus
  categoryId?: number | null
  sortOrder?: number
  properties?: TemplatePropertyRequest[]
}

/**
 * 작업 템플릿 수정 요청
 */
export interface TaskTemplateUpdateRequest {
  content?: string
  defaultAssigneeUsername?: string   // 기본 담당자 USERNAME
  defaultItemStatus?: DefaultItemStatus
  categoryId?: number | null
  status?: TemplateStatus
  sortOrder?: number
  properties?: TemplatePropertyRequest[]
}

/**
 * 작업 템플릿 검색 결과 (자동완성용)
 */
export interface TaskTemplateSearchResult {
  templateId: number
  content: string
  useCount: number
}
