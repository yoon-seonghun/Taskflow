import { get, post, put, del } from './client'
import type {
  PropertyDef,
  PropertyCreateRequest,
  PropertyUpdateRequest,
  PropertyOption,
  OptionCreateRequest,
  OptionUpdateRequest
} from '@/types/property'

export const propertyApi = {
  // 속성 정의
  getProperties(boardId: number) {
    return get<PropertyDef[]>(`/boards/${boardId}/properties`)
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
  getOptions(propertyId: number) {
    return get<PropertyOption[]>(`/properties/${propertyId}/options`)
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
