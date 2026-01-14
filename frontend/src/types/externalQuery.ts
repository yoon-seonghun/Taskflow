/**
 * 외부 쿼리 타입 정의
 */

export type QueryOwnerType = 'GLOBAL' | 'MANAGER' | 'USER'

/**
 * 외부 쿼리 응답
 */
export interface ExternalQuery {
  queryId: number
  datasourceId: number
  datasourceCode?: string
  datasourceName?: string
  datasourceDbType?: string
  queryCode: string
  queryName: string
  description?: string
  querySql: string
  ownerType: QueryOwnerType
  ownerUsername?: string
  ownerName?: string
  ownerDeptCode?: string
  valueColumn: string
  labelColumn: string
  colorColumn?: string
  cacheEnabledYn: string
  cacheTtlSeconds?: number
  sortOrder: number
  lastExecutedAt?: string
  lastResultCount?: number
  useYn: string
  createdAt: string
  createdBy: string
  createdByName?: string
  updatedAt?: string
  updatedBy?: string
  updatedByName?: string
}

/**
 * 외부 쿼리 생성 요청
 */
export interface ExternalQueryCreateRequest {
  datasourceId: number
  queryCode: string
  queryName: string
  description?: string
  querySql: string
  valueColumn?: string
  labelColumn?: string
  colorColumn?: string
  cacheEnabledYn?: string
  cacheTtlSeconds?: number
  sortOrder?: number
}

/**
 * 외부 쿼리 수정 요청
 */
export interface ExternalQueryUpdateRequest {
  datasourceId?: number
  queryName?: string
  description?: string
  querySql?: string
  valueColumn?: string
  labelColumn?: string
  colorColumn?: string
  cacheEnabledYn?: string
  cacheTtlSeconds?: number
  sortOrder?: number
  useYn?: string
}

/**
 * 쿼리 테스트 요청
 */
export interface QueryTestRequest {
  datasourceId: number
  querySql: string
  valueColumn?: string
  labelColumn?: string
  colorColumn?: string
  maxRows?: number
}

/**
 * 쿼리 테스트 응답
 */
export interface QueryTestResponse {
  success: boolean
  message: string
  executionTimeMs?: number
  rowCount?: number
  columns?: string[]
  data?: Record<string, unknown>[]
  options?: QueryOptionPreview[]
}

/**
 * 쿼리 옵션 미리보기
 */
export interface QueryOptionPreview {
  value: string
  label: string
  color?: string
}

/**
 * 쿼리 실행 응답
 */
export interface QueryExecuteResponse {
  success: boolean
  message: string
  queryId: number
  queryCode?: string
  queryName?: string
  options: QueryOption[]
  cached: boolean
  lastExecutedAt?: string
  resultCount?: number
}

/**
 * 쿼리 옵션
 */
export interface QueryOption {
  value: string
  label: string
  color?: string
}
