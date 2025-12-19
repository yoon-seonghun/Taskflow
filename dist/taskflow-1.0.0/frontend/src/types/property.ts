/**
 * 속성 정의 타입
 */

export type PropertyType =
  | 'TEXT'
  | 'NUMBER'
  | 'DATE'
  | 'SELECT'
  | 'MULTI_SELECT'
  | 'CHECKBOX'
  | 'USER'

export interface PropertyOption {
  optionId: number
  propertyId: number
  optionCode?: string     // DB에 없음 (null 반환)
  optionName: string      // DB: OPTION_LABEL
  color?: string
  sortOrder?: number      // DB에 없음 (null 반환)
  useYn?: string
}

export interface PropertyDef {
  propertyId: number
  boardId: number
  propertyName: string
  propertyCode?: string   // DB에 없음 (null 반환)
  propertyType: PropertyType
  requiredYn?: string
  defaultValue?: string
  sortOrder?: number      // DB에 없음 (null 반환)
  hiddenYn?: string       // deprecated, use visibleYn
  visibleYn?: string      // DB: VISIBLE_YN (hiddenYn 반전)
  systemYn?: string       // DB에 없음 (null 반환)
  useYn?: string          // 응답에서 미포함
  deleted?: boolean
  options?: PropertyOption[]
  createdAt?: string
  updatedAt?: string
}

export interface PropertyCreateRequest {
  propertyName: string
  propertyCode?: string   // DB에 없음 (무시됨)
  propertyType: PropertyType
  requiredYn?: string
  defaultValue?: string
  sortOrder?: number      // DB에 없음 (무시됨)
  hiddenYn?: string       // deprecated, visibleYn 사용 권장
  visibleYn?: string      // DB: VISIBLE_YN
}

export interface PropertyUpdateRequest {
  propertyName?: string
  propertyCode?: string   // DB에 없음 (무시됨)
  propertyType?: PropertyType
  requiredYn?: string
  defaultValue?: string
  sortOrder?: number      // DB에 없음 (무시됨)
  hiddenYn?: string       // deprecated, visibleYn 사용 권장
  visibleYn?: string      // DB: VISIBLE_YN
  useYn?: string
}

export interface OptionCreateRequest {
  optionCode?: string     // DB에 없음 (무시됨)
  optionName: string      // DB: OPTION_LABEL
  color?: string
  sortOrder?: number      // DB에 없음 (무시됨)
}

export interface OptionUpdateRequest {
  optionCode?: string     // DB에 없음 (무시됨)
  optionName?: string     // DB: OPTION_LABEL
  color?: string
  sortOrder?: number      // DB에 없음 (무시됨)
  useYn?: string
}
