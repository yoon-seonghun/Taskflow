import { get, post, put, del } from './client'
import type {
  ExternalQuery,
  ExternalQueryCreateRequest,
  ExternalQueryUpdateRequest,
  QueryTestRequest,
  QueryTestResponse,
  QueryExecuteResponse
} from '@/types/externalQuery'

export const externalQueryApi = {
  // =============================================
  // 소유 유형별 조회
  // =============================================

  /**
   * 글로벌 쿼리 목록 조회
   */
  getGlobalQueries() {
    return get<ExternalQuery[]>('/external-queries/global')
  },

  /**
   * 매니저 쿼리 목록 조회
   */
  getManagerQueries() {
    return get<ExternalQuery[]>('/external-queries/manager')
  },

  /**
   * 사용자 쿼리 목록 조회
   */
  getUserQueries() {
    return get<ExternalQuery[]>('/external-queries/user')
  },

  /**
   * 접근 가능한 전체 쿼리 목록 조회
   */
  getAccessibleQueries() {
    return get<ExternalQuery[]>('/external-queries/accessible')
  },

  // =============================================
  // 소유 유형별 등록
  // =============================================

  /**
   * 글로벌 쿼리 생성
   */
  createGlobalQuery(data: ExternalQueryCreateRequest) {
    return post<number>('/external-queries/global', data)
  },

  /**
   * 매니저 쿼리 생성
   */
  createManagerQuery(data: ExternalQueryCreateRequest) {
    return post<number>('/external-queries/manager', data)
  },

  /**
   * 사용자 쿼리 생성
   */
  createUserQuery(data: ExternalQueryCreateRequest) {
    return post<number>('/external-queries/user', data)
  },

  // =============================================
  // 개별 쿼리 CRUD
  // =============================================

  /**
   * 쿼리 상세 조회
   */
  getQuery(id: number) {
    return get<ExternalQuery>(`/external-queries/${id}`)
  },

  /**
   * 쿼리 수정
   */
  updateQuery(id: number, data: ExternalQueryUpdateRequest) {
    return put<void>(`/external-queries/${id}`, data)
  },

  /**
   * 쿼리 삭제
   */
  deleteQuery(id: number) {
    return del<void>(`/external-queries/${id}`)
  },

  // =============================================
  // 쿼리 테스트/실행
  // =============================================

  /**
   * 쿼리 테스트 (저장 전)
   */
  testQuery(data: QueryTestRequest) {
    return post<QueryTestResponse>('/external-queries/test', data)
  },

  /**
   * 쿼리 실행 (옵션 조회)
   */
  executeQuery(id: number) {
    return get<QueryExecuteResponse>(`/external-queries/${id}/execute`)
  },

  /**
   * 쿼리 실행 (캐시 무시)
   */
  executeQueryNoCache(id: number) {
    return post<QueryExecuteResponse>(`/external-queries/${id}/execute`)
  },

  // =============================================
  // 캐시 관리
  // =============================================

  /**
   * 쿼리 캐시 무효화
   */
  evictCache(id: number) {
    return del<void>(`/external-queries/${id}/cache`)
  },

  // =============================================
  // 기타
  // =============================================

  /**
   * 데이터소스별 쿼리 목록 조회
   */
  getQueriesByDatasource(datasourceId: number) {
    return get<ExternalQuery[]>(`/external-queries/datasource/${datasourceId}`)
  },

  /**
   * 키워드 검색
   */
  searchQueries(keyword: string) {
    return get<ExternalQuery[]>('/external-queries/search', { keyword })
  }
}
