/**
 * 카테고리 API (v2.0 - 소유자 기반 카테고리)
 */
import { get, post, put, patch, del } from './client'
import type {
  Category,
  CategoryDetail,
  CategoryCreateRequest,
  CategoryUpdateRequest,
  CategoryShareRequest,
  CategoryPropertyRequest,
  CategoryShare,
  CategoryProperty
} from '@/types/category'

export const categoryApi = {
  // =============================================
  // 카테고리 목록 조회
  // =============================================

  /**
   * 전체 카테고리 목록 조회 (관리자용)
   * @param includeDeleted 삭제된 항목 포함 여부
   */
  getCategories(includeDeleted: boolean = false) {
    return get<Category[]>('/categories', { includeDeleted })
  },

  /**
   * 접근 가능한 카테고리 목록 조회 (내 것 + 공유받은 것)
   */
  getAccessibleCategories() {
    return get<Category[]>('/categories/accessible')
  },

  /**
   * 내 카테고리 목록 조회
   */
  getMyCategories() {
    return get<Category[]>('/categories/my')
  },

  /**
   * 공유받은 카테고리 목록 조회
   */
  getSharedWithMeCategories() {
    return get<Category[]>('/categories/shared')
  },

  // =============================================
  // 카테고리 단건 조회
  // =============================================

  /**
   * 카테고리 기본 정보 조회
   * @param categoryId 카테고리 ID
   */
  getCategory(categoryId: number) {
    return get<Category>(`/categories/${categoryId}`)
  },

  /**
   * 카테고리 상세 조회 (속성 및 공유 포함)
   * @param categoryId 카테고리 ID
   */
  getCategoryDetail(categoryId: number) {
    return get<CategoryDetail>(`/categories/${categoryId}`, { withDetails: true })
  },

  // =============================================
  // 카테고리 CRUD
  // =============================================

  /**
   * 카테고리 생성
   * @param request 생성 요청
   */
  createCategory(request: CategoryCreateRequest) {
    return post<Category>('/categories', request)
  },

  /**
   * 카테고리 수정
   * @param categoryId 카테고리 ID
   * @param request 수정 요청
   */
  updateCategory(categoryId: number, request: CategoryUpdateRequest) {
    return put<Category>(`/categories/${categoryId}`, request)
  },

  /**
   * 카테고리 삭제 (논리 삭제)
   * @param categoryId 카테고리 ID
   */
  deleteCategory(categoryId: number) {
    return del<void>(`/categories/${categoryId}`)
  },

  // =============================================
  // 카테고리 공유 관리
  // =============================================

  /**
   * 카테고리 공유 목록 조회
   * @param categoryId 카테고리 ID
   */
  getShares(categoryId: number) {
    return get<CategoryShare[]>(`/categories/${categoryId}/shares`)
  },

  /**
   * 카테고리 공유 추가 (단건)
   * @param categoryId 카테고리 ID
   * @param request 공유 요청
   */
  addShare(categoryId: number, request: CategoryShareRequest) {
    return post<CategoryShare>(`/categories/${categoryId}/shares`, request)
  },

  /**
   * 카테고리 공유 추가 (다건)
   * @param categoryId 카테고리 ID
   * @param requests 공유 요청 목록
   */
  addShares(categoryId: number, requests: CategoryShareRequest[]) {
    return post<CategoryShare[]>(`/categories/${categoryId}/shares/bulk`, requests)
  },

  /**
   * 카테고리 공유 권한 변경
   * @param categoryId 카테고리 ID
   * @param shareId 공유 ID
   * @param permission 권한
   */
  updateSharePermission(categoryId: number, shareId: number, permission: 'READ' | 'EDIT') {
    return patch<void>(`/categories/shares/${shareId}/permission`, { permission })
  },

  /**
   * 카테고리 공유 해제
   * @param categoryId 카테고리 ID
   * @param shareId 공유 ID
   */
  removeShare(categoryId: number, shareId: number) {
    return del<void>(`/categories/${categoryId}/shares/${shareId}`)
  },

  // =============================================
  // 카테고리 속성 관리
  // =============================================

  /**
   * 카테고리별 속성 목록 조회
   * @param categoryId 카테고리 ID
   */
  getProperties(categoryId: number) {
    return get<CategoryProperty[]>(`/categories/${categoryId}/properties`)
  },

  /**
   * 카테고리에 속성 추가 (단건)
   * @param categoryId 카테고리 ID
   * @param request 속성 요청
   */
  addProperty(categoryId: number, request: CategoryPropertyRequest) {
    return post<void>(`/categories/${categoryId}/properties`, request)
  },

  /**
   * 카테고리에 속성 추가 (다건)
   * @param categoryId 카테고리 ID
   * @param requests 속성 요청 목록
   */
  addProperties(categoryId: number, requests: CategoryPropertyRequest[]) {
    return post<void>(`/categories/${categoryId}/properties/bulk`, requests)
  },

  /**
   * 카테고리에서 속성 제거
   * @param categoryId 카테고리 ID
   * @param propertyId 속성 ID
   */
  removeProperty(categoryId: number, propertyId: number) {
    return del<void>(`/categories/${categoryId}/properties/${propertyId}`)
  },

  /**
   * 카테고리 속성 순서 변경 (일괄)
   * @param categoryId 카테고리 ID
   * @param orders 속성 ID와 순서 목록
   */
  reorderProperties(categoryId: number, orders: { propertyId: number; sortOrder: number }[]) {
    return patch<void>(`/categories/${categoryId}/properties/order`, { orders })
  },

  /**
   * 카테고리 속성 기본값 변경
   * @param categoryId 카테고리 ID
   * @param propertyId 속성 ID
   * @param defaultValue 기본값
   */
  updatePropertyDefaultValue(categoryId: number, propertyId: number, defaultValue: string) {
    return patch<void>(`/categories/${categoryId}/properties/${propertyId}/default-value`, { defaultValue })
  },

  // =============================================
  // v2.0: 카테고리 이관 API
  // =============================================

  /**
   * 카테고리 소유권 이전
   * @param categoryId 카테고리 ID
   * @param newOwnerUsername 새 소유자 Username
   */
  transferCategory(categoryId: number, newOwnerUsername: string) {
    return post<Category>(`/categories/${categoryId}/transfer?newOwner=${encodeURIComponent(newOwnerUsername)}`)
  },

  /**
   * 카테고리 복사 (다른 사용자에게)
   * @param categoryId 원본 카테고리 ID
   * @param newOwnerUsername 새 소유자 Username
   */
  copyCategoryToUser(categoryId: number, newOwnerUsername: string) {
    return post<Category>(`/categories/${categoryId}/copy?newOwner=${encodeURIComponent(newOwnerUsername)}`)
  },

  /**
   * 카테고리 일괄 이전
   * @param categoryIds 카테고리 ID 목록
   * @param newOwnerUsername 새 소유자 Username
   */
  transferCategories(categoryIds: number[], newOwnerUsername: string) {
    return post<Category[]>(`/categories/transfer-batch?newOwner=${encodeURIComponent(newOwnerUsername)}`, categoryIds)
  }
}
