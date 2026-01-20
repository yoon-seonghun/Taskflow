/**
 * 카테고리 타입 정의 (v2.0 - 소유자 기반 카테고리)
 */

/**
 * 카테고리 기본 정보
 */
export interface Category {
  categoryId: number
  categoryCode: string
  categoryName: string
  description?: string
  categoryColor?: string
  sortOrder: number
  useYn: string
  ownerUsername: string
  ownerName?: string
  createdAt?: string
  createdBy?: string
  updatedAt?: string
  updatedBy?: string
}

/**
 * 카테고리 상세 정보 (속성 및 공유 포함)
 */
export interface CategoryDetail extends Category {
  properties: CategoryProperty[]
  shares: CategoryShare[]
  boardCount?: number
}

/**
 * 카테고리에 연결된 속성 (API 응답 형식)
 */
export interface CategoryProperty {
  categoryPropertyId: number
  categoryId: number
  propertyId: number
  propertyName: string
  propertyType: string
  ownerType?: string  // GLOBAL, MANAGER, USER
  requiredYn?: string
  sortOrder: number
  defaultValue?: string
  createdAt?: string
  createdBy?: string
}

/**
 * 카테고리 공유 정보 (API 응답 형식)
 */
export interface CategoryShare {
  shareId: number
  categoryId?: number
  shareType: 'USER' | 'DEPARTMENT'
  shareTarget: string  // USERNAME 또는 DEPARTMENT_CODE
  shareTargetName?: string  // 사용자명 또는 부서명
  permission: 'READ' | 'EDIT'
  sharedAt?: string
  sharedBy?: string
}

/**
 * 카테고리 생성 요청
 */
export interface CategoryCreateRequest {
  categoryCode: string
  categoryName: string
  description?: string
  categoryColor?: string
  sortOrder?: number
  propertyIds?: number[]
}

/**
 * 카테고리 수정 요청
 */
export interface CategoryUpdateRequest {
  categoryCode?: string
  categoryName?: string
  description?: string
  categoryColor?: string
  sortOrder?: number
  useYn?: string
}

/**
 * 카테고리 공유 추가 요청
 */
export interface CategoryShareRequest {
  shareType: 'USER' | 'DEPARTMENT'
  shareTarget: string  // USERNAME 또는 DEPARTMENT_CODE
  permission: 'READ' | 'EDIT'
}

/**
 * 카테고리 속성 추가 요청
 */
export interface CategoryPropertyRequest {
  propertyId: number
  sortOrder?: number
  defaultValue?: string
}

/**
 * 카테고리 속성 순서 변경 요청
 */
export interface CategoryPropertyOrderRequest {
  propertyId: number
  sortOrder: number
}

/**
 * 카테고리 속성 기본값 변경 요청
 */
export interface CategoryPropertyDefaultValueRequest {
  propertyId: number
  defaultValue: string
}
