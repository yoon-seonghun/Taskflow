import { get, post, put, del } from './client'
import type {
  ExternalDatasource,
  ExternalDatasourceCreateRequest,
  ExternalDatasourceUpdateRequest,
  ConnectionTestResponse
} from '@/types/externalDatasource'

export const externalDatasourceApi = {
  /**
   * 데이터소스 목록 조회
   */
  getDatasources(params?: { dbType?: string; keyword?: string }) {
    return get<ExternalDatasource[]>('/external-datasources', params)
  },

  /**
   * 활성 데이터소스 목록 조회
   */
  getActiveDatasources() {
    return get<ExternalDatasource[]>('/external-datasources/active')
  },

  /**
   * 데이터소스 상세 조회
   */
  getDatasource(id: number) {
    return get<ExternalDatasource>(`/external-datasources/${id}`)
  },

  /**
   * 데이터소스 생성
   */
  createDatasource(data: ExternalDatasourceCreateRequest) {
    return post<number>('/external-datasources', data)
  },

  /**
   * 데이터소스 수정
   */
  updateDatasource(id: number, data: ExternalDatasourceUpdateRequest) {
    return put<void>(`/external-datasources/${id}`, data)
  },

  /**
   * 데이터소스 삭제
   */
  deleteDatasource(id: number) {
    return del<void>(`/external-datasources/${id}`)
  },

  /**
   * 연결 테스트 (기존 데이터소스)
   */
  testConnection(id: number) {
    return post<ConnectionTestResponse>(`/external-datasources/${id}/test-connection`)
  },

  /**
   * 연결 테스트 (새 설정)
   */
  testNewConnection(data: ExternalDatasourceCreateRequest) {
    return post<ConnectionTestResponse>('/external-datasources/test-connection', data)
  },

  /**
   * DataSource 풀 재초기화
   */
  reinitializePool(id: number) {
    return post<void>(`/external-datasources/${id}/reinitialize`)
  }
}
