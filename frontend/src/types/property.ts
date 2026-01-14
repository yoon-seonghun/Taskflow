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
  sortOrder?: number
  useYn?: string
  usageCount?: number     // 사용 횟수 (관리용)
}

/**
 * 속성 소유 유형
 * - GLOBAL: 관리자 생성, 전체 사용 가능
 * - MANAGER: 매니저 생성, 부서 내 사용 가능
 * - USER: 개인 생성, 카테고리에 그룹화하여 사용
 */
export type OwnerType = 'GLOBAL' | 'MANAGER' | 'USER'

/**
 * 데이터 소스 유형
 * - INTERNAL: 내부 옵션 테이블 사용 (기본값)
 * - EXTERNAL: 외부 쿼리 결과 사용
 */
export type DataSourceType = 'INTERNAL' | 'EXTERNAL'

export interface PropertyDef {
  propertyId: number
  boardId?: number | null   // v2.0: 사용자 속성은 null
  propertyName: string
  propertyCode?: string     // DB에 없음 (null 반환)
  propertyType: PropertyType
  ownerType?: OwnerType     // v2.0: 속성 소유 유형
  ownerUsername?: string    // v2.0: 소유자 사용자명 (USER/MANAGER 타입)
  ownerDeptCode?: string    // v2.0: 소유자 부서 코드 (MANAGER 타입)
  requiredYn?: string       // v2.0: CategoryProperty/BoardProperty로 이동됨
  defaultValue?: string
  sortOrder?: number
  hiddenYn?: string         // deprecated, use visibleYn
  visibleYn?: string        // DB: VISIBLE_YN (hiddenYn 반전)
  systemYn?: string         // DB에 없음 (null 반환)
  useYn?: string            // 응답에서 미포함
  deleted?: boolean
  options?: PropertyOption[]
  // 외부 쿼리 연동 (SELECT/MULTI_SELECT/CHECKBOX 타입용)
  externalQueryId?: number | null    // 연동된 외부 쿼리 ID
  dataSourceType?: DataSourceType    // 데이터 소스 유형 (INTERNAL/EXTERNAL)
  externalQueryCode?: string         // 외부 쿼리 코드 (표시용)
  externalQueryName?: string         // 외부 쿼리명 (표시용)
  createdAt?: string
  updatedAt?: string
}

export interface PropertyCreateRequest {
  propertyName: string
  propertyCode?: string     // DB에 없음 (무시됨)
  propertyType: PropertyType
  ownerDeptCode?: string    // v2.0: 매니저 속성 생성 시 부서 코드
  requiredYn?: string
  defaultValue?: string
  sortOrder?: number
  hiddenYn?: string         // deprecated, visibleYn 사용 권장
  visibleYn?: string        // DB: VISIBLE_YN
  // 외부 쿼리 연동
  externalQueryId?: number | null    // 외부 쿼리 ID (EXTERNAL 타입 시)
  dataSourceType?: DataSourceType    // 데이터 소스 유형 (기본값: INTERNAL)
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
  // 외부 쿼리 연동
  externalQueryId?: number | null    // 외부 쿼리 ID (null로 설정하면 연동 해제)
  dataSourceType?: DataSourceType    // 데이터 소스 유형
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
