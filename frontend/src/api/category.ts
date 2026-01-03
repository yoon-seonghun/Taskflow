/**
 * 카테고리 API (전역 카테고리)
 */
import { get, post, put, del } from './client'
import type { Category, CategoryCreateRequest, CategoryUpdateRequest } from '@/types/category'

export const categoryApi = {
  /**
   * 카테고리 목록 조회
   * @param includeDeleted 삭제된 항목 포함 여부
   */
  getCategories(includeDeleted: boolean = false) {
    return get<Category[]>('/categories', { includeDeleted })
  },

  /**
   * 카테고리 단건 조회
   * @param categoryId 카테고리 ID
   */
  getCategory(categoryId: number) {
    return get<Category>(`/categories/${categoryId}`)
  },

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
  }
}
