/**
 * 성과점수 API
 */
import { get, post, put, del } from './client'
import type {
  ItemScore,
  ScoreCalculateRequest,
  ScoreSearchRequest,
  ScoreSearchResult,
  ScoreSummary,
  ScoreApprovalRequest,
  WeightUpdateRequest
} from '@/types/score'

export const scoreApi = {
  // =============================================
  // 점수 조회
  // =============================================

  /**
   * 점수 상세 조회
   * @param scoreId 점수 ID
   */
  getScore(scoreId: number) {
    return get<ItemScore>(`/scores/${scoreId}`)
  },

  /**
   * 업무별 점수 조회
   * @param itemId 업무 ID
   */
  getScoreByItemId(itemId: number) {
    return get<ItemScore | null>(`/scores/item/${itemId}`)
  },

  /**
   * 내 점수 목록 조회
   * @param startDate 시작일 (YYYY-MM-DD)
   * @param endDate 종료일 (YYYY-MM-DD)
   */
  getMyScores(startDate?: string, endDate?: string) {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return get<ItemScore[]>('/scores/my', params)
  },

  /**
   * 내 점수 요약 조회
   * @param startDate 시작일 (YYYY-MM-DD)
   * @param endDate 종료일 (YYYY-MM-DD)
   */
  getMySummary(startDate?: string, endDate?: string) {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return get<ScoreSummary>('/scores/my/summary', params)
  },

  /**
   * 점수 검색 (PM용)
   * @param request 검색 조건
   */
  searchScores(request: ScoreSearchRequest) {
    return get<ScoreSearchResult>('/scores/search', request as Record<string, unknown>)
  },

  /**
   * 부서별 점수 요약 목록 조회 (PM용)
   * @param deptCode 부서 코드
   * @param startDate 시작일
   * @param endDate 종료일
   */
  getDepartmentSummary(deptCode: string, startDate?: string, endDate?: string) {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return get<ScoreSummary[]>(`/scores/department/${deptCode}/summary`, params)
  },

  /**
   * 승인 대기 점수 목록 조회 (PM용)
   */
  getPendingApproval() {
    return get<ItemScore[]>('/scores/pending')
  },

  // =============================================
  // 점수 계산 및 등록
  // =============================================

  /**
   * 업무 완료 시 점수 계산 및 등록
   * @param request 점수 계산 요청
   */
  calculateScore(request: ScoreCalculateRequest) {
    return post<ItemScore>('/scores/calculate', request)
  },

  // =============================================
  // 점수 수정
  // =============================================

  /**
   * 가중치 요소 수정 (PM용)
   * @param scoreId 점수 ID
   * @param request 가중치 수정 요청
   */
  updateWeightFactors(scoreId: number, request: WeightUpdateRequest) {
    return put<void>(`/scores/${scoreId}/weight`, request)
  },

  // =============================================
  // 승인 처리
  // =============================================

  /**
   * 점수 승인/거부 처리 (PM용)
   * @param scoreId 점수 ID
   * @param request 승인 요청
   */
  processApproval(scoreId: number, request: ScoreApprovalRequest) {
    return post<void>(`/scores/${scoreId}/approval`, request)
  },

  // =============================================
  // 삭제
  // =============================================

  /**
   * 점수 삭제 (ADMIN 전용)
   * @param scoreId 점수 ID
   */
  deleteScore(scoreId: number) {
    return del<void>(`/scores/${scoreId}`)
  }
}
