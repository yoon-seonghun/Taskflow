import { get, post, put, del } from './client'
import type {
  PropertyDef,
  PropertyCreateRequest,
  PropertyUpdateRequest,
  PropertyOption,
  OptionCreateRequest,
  OptionUpdateRequest
} from '@/types/property'

export interface GetPropertiesParams {
  useYn?: string     // Y: 활성만, N: 비활성만, 없음: 전체
  cached?: boolean   // 캐시 사용 여부
}

export interface GetOptionsParams {
  useYn?: string     // Y: 활성만, N: 비활성만, 없음: 전체
}

export const propertyApi = {
  // =============================================
  // 보드별 속성 정의 (레거시)
  // =============================================

  getProperties(boardId: number, params?: GetPropertiesParams) {
    const queryParams = new URLSearchParams()
    if (params?.useYn) queryParams.append('useYn', params.useYn)
    if (params?.cached) queryParams.append('cached', 'true')
    const queryString = queryParams.toString()
    return get<PropertyDef[]>(`/boards/${boardId}/properties${queryString ? `?${queryString}` : ''}`)
  },

  getProperty(propertyId: number) {
    return get<PropertyDef>(`/properties/${propertyId}`)
  },

  createProperty(boardId: number, data: PropertyCreateRequest) {
    return post<PropertyDef>(`/boards/${boardId}/properties`, data)
  },

  updateProperty(propertyId: number, data: PropertyUpdateRequest) {
    return put<PropertyDef>(`/properties/${propertyId}`, data)
  },

  deleteProperty(propertyId: number) {
    return del<void>(`/properties/${propertyId}`)
  },

  // =============================================
  // v2.0: 소유 유형별 속성 API
  // =============================================

  /**
   * 글로벌 속성 목록 조회
   */
  getGlobalProperties() {
    return get<PropertyDef[]>('/properties/global')
  },

  /**
   * 글로벌 속성 생성 (관리자 전용)
   */
  createGlobalProperty(data: PropertyCreateRequest) {
    return post<PropertyDef>('/properties/global', data)
  },

  /**
   * 매니저 속성 목록 조회 (본인 소유 + 상위 부서)
   */
  getManagerProperties() {
    return get<PropertyDef[]>('/properties/manager')
  },

  /**
   * 매니저 속성 생성
   */
  createManagerProperty(data: PropertyCreateRequest) {
    return post<PropertyDef>('/properties/manager', data)
  },

  /**
   * 사용자 속성 목록 조회 (본인이 생성한 속성)
   */
  getUserProperties() {
    return get<PropertyDef[]>('/properties/user')
  },

  /**
   * 사용자 속성 생성 (개인 속성)
   */
  createUserProperty(data: PropertyCreateRequest) {
    return post<PropertyDef>('/properties/user', data)
  },

  /**
   * 사용자가 접근 가능한 모든 속성 조회 (글로벌 + 매니저 + 본인)
   * 카테고리/보드에서 속성 선택 시 사용
   */
  getAccessibleProperties() {
    return get<PropertyDef[]>('/properties/accessible')
  },

  // =============================================
  // v2.0: 속성 이관 API
  // =============================================

  /**
   * 속성 소유권 이전
   */
  transferProperty(propertyId: number, newOwnerUsername: string) {
    return post<PropertyDef>(`/properties/${propertyId}/transfer?newOwner=${encodeURIComponent(newOwnerUsername)}`)
  },

  /**
   * 속성 복사 (다른 사용자에게)
   */
  copyPropertyToUser(propertyId: number, newOwnerUsername: string) {
    return post<PropertyDef>(`/properties/${propertyId}/copy?newOwner=${encodeURIComponent(newOwnerUsername)}`)
  },

  /**
   * 속성 일괄 이전
   */
  transferProperties(propertyIds: number[], newOwnerUsername: string) {
    return post<PropertyDef[]>(`/properties/transfer-batch?newOwner=${encodeURIComponent(newOwnerUsername)}`, propertyIds)
  },

  // =============================================
  // 속성 옵션
  // =============================================

  getOptions(propertyId: number, params?: GetOptionsParams) {
    const queryParams = new URLSearchParams()
    if (params?.useYn) queryParams.append('useYn', params.useYn)
    const queryString = queryParams.toString()
    return get<PropertyOption[]>(`/properties/${propertyId}/options${queryString ? `?${queryString}` : ''}`)
  },

  getOption(optionId: number) {
    return get<PropertyOption>(`/options/${optionId}`)
  },

  createOption(propertyId: number, data: OptionCreateRequest) {
    return post<PropertyOption>(`/properties/${propertyId}/options`, data)
  },

  updateOption(optionId: number, data: OptionUpdateRequest) {
    return put<PropertyOption>(`/options/${optionId}`, data)
  },

  deleteOption(optionId: number) {
    return del<void>(`/options/${optionId}`)
  }
}
