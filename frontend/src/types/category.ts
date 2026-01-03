/**
 * 카테고리 타입 정의 (전역 카테고리)
 */

/**
 * 카테고리 응답 타입
 */
export interface Category {
  categoryId: number
  categoryName: string
  color: string
  sortOrder: number
  useYn: string
  createdAt?: string
  createdBy?: string
  updatedAt?: string
  updatedBy?: string
}

/**
 * 카테고리 생성 요청 타입
 */
export interface CategoryCreateRequest {
  categoryName: string
  color?: string
  sortOrder?: number
}

/**
 * 카테고리 수정 요청 타입
 */
export interface CategoryUpdateRequest {
  categoryName?: string
  color?: string
  sortOrder?: number
  useYn?: string
}
