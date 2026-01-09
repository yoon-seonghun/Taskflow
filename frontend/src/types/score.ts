/**
 * 성과점수 타입 정의
 */

/**
 * 성과점수 등급
 */
export type ScoreGrade = 'PERFECT' | 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR'

/**
 * 승인 상태
 */
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/**
 * 성과점수 기본 정보
 */
export interface ItemScore {
  scoreId: number
  itemId: number
  itemTitle?: string
  assigneeUsername: string
  assigneeName?: string

  // 기본 점수
  baseGrade: ScoreGrade
  baseScore: number

  // 가중치 요소
  difficulty: number       // 1-5
  scopeChange: number      // 0-2
  riskHandling: number     // 0-2
  weightFactor: number     // 계산된 가중치

  // 최종 점수
  finalScore: number

  // 승인 정보
  approvalStatus: ApprovalStatus
  approvedBy?: string
  approvedAt?: string

  // 날짜 정보
  requestDate: string
  dueDate: string
  completedAt: string
  createdAt: string
  createdBy?: string
}

/**
 * 점수 계산 요청
 */
export interface ScoreCalculateRequest {
  itemId: number
  completedAt?: string   // 완료일 (null이면 오늘)
  difficulty?: number    // 1-5
  scopeChange?: number   // 0-2
  riskHandling?: number  // 0-2
}

/**
 * 점수 검색 요청
 */
export interface ScoreSearchRequest {
  assigneeUsername?: string
  deptCode?: string
  boardId?: number
  startDate?: string
  endDate?: string
  approvalStatus?: ApprovalStatus
  baseGrade?: ScoreGrade
  sortBy?: 'completedAt' | 'finalScore' | 'baseGrade'
  sortDirection?: 'ASC' | 'DESC'
  page?: number
  size?: number
}

/**
 * 점수 검색 결과
 */
export interface ScoreSearchResult {
  items: ItemScore[]
  totalCount: number
  page: number
  size: number
  totalPages: number
}

/**
 * 점수 요약 정보
 */
export interface ScoreSummary {
  assigneeUsername: string
  assigneeName?: string
  completedCount: number
  totalScore: number
  averageScore: number
  perfectCount: number
  excellentCount: number
  goodCount: number
  fairCount: number
  poorCount: number
  averageDifficulty?: number
  averageWeightFactor?: number
}

/**
 * 점수 승인 요청
 */
export interface ScoreApprovalRequest {
  status: 'APPROVED' | 'REJECTED'
  rejectReason?: string
  difficulty?: number    // 승인 시 수정 가능
  scopeChange?: number   // 승인 시 수정 가능
  riskHandling?: number  // 승인 시 수정 가능
}

/**
 * 가중치 수정 요청
 */
export interface WeightUpdateRequest {
  difficulty?: number
  scopeChange?: number
  riskHandling?: number
}

/**
 * 등급별 정보
 */
export const GRADE_INFO: Record<ScoreGrade, { label: string; score: number; color: string; description: string }> = {
  PERFECT: { label: '최상', score: 100, color: '#10b981', description: '50% 이내 완료' },
  EXCELLENT: { label: '우수', score: 90, color: '#3b82f6', description: '75% 이내 완료' },
  GOOD: { label: '양호', score: 80, color: '#6366f1', description: '기한 내 완료' },
  FAIR: { label: '보통', score: 60, color: '#f59e0b', description: '20% 초과' },
  POOR: { label: '미흡', score: 40, color: '#ef4444', description: '20% 이상 초과' }
}

/**
 * 난이도 레벨 정보
 */
export const DIFFICULTY_INFO: Record<number, { label: string; factor: number }> = {
  1: { label: '기본', factor: 1.00 },
  2: { label: '낮음', factor: 1.05 },
  3: { label: '보통', factor: 1.10 },
  4: { label: '높음', factor: 1.15 },
  5: { label: '최고', factor: 1.20 }
}

/**
 * 범위변경 레벨 정보
 */
export const SCOPE_CHANGE_INFO: Record<number, { label: string; factor: number }> = {
  0: { label: '없음', factor: 0 },
  1: { label: '소규모', factor: 0.05 },
  2: { label: '대규모', factor: 0.10 }
}

/**
 * 리스크대응 레벨 정보
 */
export const RISK_HANDLING_INFO: Record<number, { label: string; factor: number }> = {
  0: { label: '없음', factor: 0 },
  1: { label: '일반', factor: 0.05 },
  2: { label: '긴급', factor: 0.10 }
}
