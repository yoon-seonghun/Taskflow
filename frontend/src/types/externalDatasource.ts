/**
 * 외부 데이터소스 타입 정의
 */

export type DbType = 'MYSQL' | 'ORACLE' | 'MSSQL' | 'TIBERO6' | 'TIBERO7'

export type ConnectionType = 'SID' | 'SERVICE_NAME'

/**
 * 외부 데이터소스 응답
 */
export interface ExternalDatasource {
  datasourceId: number
  datasourceCode: string
  datasourceName: string
  dbType: DbType
  connectionType?: ConnectionType
  host: string
  port: number
  databaseName: string
  schemaName?: string
  username: string
  // password는 응답에 포함되지 않음
  connectionTimeout: number
  queryTimeout: number
  maxPoolSize: number
  useYn: string
  createdAt: string
  createdBy: string
  createdByName?: string
  updatedAt?: string
  updatedBy?: string
  updatedByName?: string
  queryCount?: number
}

/**
 * 외부 데이터소스 생성 요청
 */
export interface ExternalDatasourceCreateRequest {
  datasourceCode: string
  datasourceName: string
  dbType: DbType
  connectionType?: ConnectionType
  host: string
  port: number
  databaseName: string
  schemaName?: string
  username: string
  password: string
  connectionTimeout?: number
  queryTimeout?: number
  maxPoolSize?: number
}

/**
 * 외부 데이터소스 수정 요청
 */
export interface ExternalDatasourceUpdateRequest {
  datasourceName?: string
  connectionType?: ConnectionType
  host?: string
  port?: number
  databaseName?: string
  schemaName?: string
  username?: string
  password?: string  // 변경할 경우에만
  connectionTimeout?: number
  queryTimeout?: number
  maxPoolSize?: number
  useYn?: string
}

/**
 * 연결 테스트 응답
 */
export interface ConnectionTestResponse {
  success: boolean
  message: string
  connectionTimeMs?: number
  dbVersion?: string
  driverInfo?: string
}

/**
 * DB 타입 옵션
 */
export const DB_TYPE_OPTIONS: { value: DbType; label: string; defaultPort: number }[] = [
  { value: 'MYSQL', label: 'MySQL', defaultPort: 3306 },
  { value: 'ORACLE', label: 'Oracle', defaultPort: 1521 },
  { value: 'MSSQL', label: 'SQL Server', defaultPort: 1433 },
  { value: 'TIBERO6', label: 'Tibero 6', defaultPort: 8629 },
  { value: 'TIBERO7', label: 'Tibero 7', defaultPort: 8629 }
]

/**
 * 연결 타입 옵션 (Oracle용)
 */
export const CONNECTION_TYPE_OPTIONS: { value: ConnectionType; label: string }[] = [
  { value: 'SERVICE_NAME', label: 'Service Name' },
  { value: 'SID', label: 'SID' }
]
