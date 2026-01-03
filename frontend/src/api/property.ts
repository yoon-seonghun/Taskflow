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
  // 속성 정의
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

  // 속성 옵션
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
