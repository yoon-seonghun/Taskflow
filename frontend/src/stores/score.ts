/**
 * 성과점수 스토어
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { scoreApi } from '@/api/score'
import type {
  ItemScore,
  ScoreCalculateRequest,
  ScoreSearchRequest,
  ScoreSearchResult,
  ScoreSummary,
  ScoreApprovalRequest,
  WeightUpdateRequest
} from '@/types/score'

export const useScoreStore = defineStore('score', () => {
  // =============================================
  // State
  // =============================================

  // 내 점수 목록
  const myScores = ref<ItemScore[]>([])

  // 내 점수 요약
  const mySummary = ref<ScoreSummary | null>(null)

  // 검색 결과
  const searchResult = ref<ScoreSearchResult | null>(null)

  // 부서별 점수 요약
  const departmentSummaries = ref<ScoreSummary[]>([])

  // 승인 대기 목록
  const pendingScores = ref<ItemScore[]>([])

  // 현재 조회 중인 점수
  const currentScore = ref<ItemScore | null>(null)

  // 로딩 상태
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 검색 조건
  const searchRequest = ref<ScoreSearchRequest>({
    page: 0,
    size: 20,
    sortBy: 'completedAt',
    sortDirection: 'DESC'
  })

  // =============================================
  // Getters
  // =============================================

  // 승인 대기 건수
  const pendingCount = computed(() => pendingScores.value.length)

  // 등급별 점수 목록 필터
  const getScoresByGrade = computed(() => (grade: string) =>
    myScores.value.filter(s => s.baseGrade === grade)
  )

  // 평균 점수
  const averageScore = computed(() => {
    if (myScores.value.length === 0) return 0
    const sum = myScores.value.reduce((acc, s) => acc + s.finalScore, 0)
    return Math.round(sum / myScores.value.length * 100) / 100
  })

  // =============================================
  // Actions - 조회
  // =============================================

  /**
   * 점수 상세 조회
   */
  async function fetchScore(scoreId: number) {
    loading.value = true
    error.value = null
    try {
      const response = await scoreApi.getScore(scoreId)
      currentScore.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '점수를 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 업무별 점수 조회
   */
  async function fetchScoreByItemId(itemId: number) {
    loading.value = true
    error.value = null
    try {
      const response = await scoreApi.getScoreByItemId(itemId)
      currentScore.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '점수를 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 내 점수 목록 조회
   */
  async function fetchMyScores(startDate?: string, endDate?: string) {
    loading.value = true
    error.value = null
    try {
      const response = await scoreApi.getMyScores(startDate, endDate)
      myScores.value = response.data
    } catch (e: any) {
      error.value = e.message || '점수 목록을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 내 점수 요약 조회
   */
  async function fetchMySummary(startDate?: string, endDate?: string) {
    loading.value = true
    error.value = null
    try {
      const response = await scoreApi.getMySummary(startDate, endDate)
      mySummary.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '점수 요약을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 점수 검색 (PM용)
   */
  async function searchScores(request?: ScoreSearchRequest) {
    loading.value = true
    error.value = null
    try {
      if (request) {
        searchRequest.value = { ...searchRequest.value, ...request }
      }
      const response = await scoreApi.searchScores(searchRequest.value)
      searchResult.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '점수 검색에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 부서별 점수 요약 조회 (PM용)
   */
  async function fetchDepartmentSummary(deptCode: string, startDate?: string, endDate?: string) {
    loading.value = true
    error.value = null
    try {
      const response = await scoreApi.getDepartmentSummary(deptCode, startDate, endDate)
      departmentSummaries.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '부서 점수 요약을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 승인 대기 점수 목록 조회 (PM용)
   */
  async function fetchPendingApproval() {
    loading.value = true
    error.value = null
    try {
      const response = await scoreApi.getPendingApproval()
      pendingScores.value = response.data
    } catch (e: any) {
      error.value = e.message || '승인 대기 목록을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // =============================================
  // Actions - 점수 계산 및 등록
  // =============================================

  /**
   * 업무 완료 시 점수 계산 및 등록
   */
  async function calculateScore(request: ScoreCalculateRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await scoreApi.calculateScore(request)
      const newScore = response.data

      // 내 점수 목록에 추가
      myScores.value.unshift(newScore)

      // 승인 대기 상태면 대기 목록에도 추가
      if (newScore.approvalStatus === 'PENDING') {
        pendingScores.value.unshift(newScore)
      }

      return newScore
    } catch (e: any) {
      error.value = e.message || '점수 계산에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // =============================================
  // Actions - 수정
  // =============================================

  /**
   * 가중치 요소 수정 (PM용)
   */
  async function updateWeightFactors(scoreId: number, request: WeightUpdateRequest) {
    loading.value = true
    error.value = null
    try {
      await scoreApi.updateWeightFactors(scoreId, request)

      // 현재 점수 다시 로드
      await fetchScore(scoreId)

      // 목록에서 업데이트
      updateScoreInList(myScores.value, scoreId)
      updateScoreInList(pendingScores.value, scoreId)
      if (searchResult.value) {
        updateScoreInList(searchResult.value.items, scoreId)
      }
    } catch (e: any) {
      error.value = e.message || '가중치 수정에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // =============================================
  // Actions - 승인 처리
  // =============================================

  /**
   * 점수 승인/거부 처리 (PM용)
   */
  async function processApproval(scoreId: number, request: ScoreApprovalRequest) {
    loading.value = true
    error.value = null
    try {
      await scoreApi.processApproval(scoreId, request)

      // 승인 대기 목록에서 제거
      pendingScores.value = pendingScores.value.filter(s => s.scoreId !== scoreId)

      // 현재 점수 다시 로드
      await fetchScore(scoreId)

      // 목록에서 업데이트
      updateScoreInList(myScores.value, scoreId)
      if (searchResult.value) {
        updateScoreInList(searchResult.value.items, scoreId)
      }
    } catch (e: any) {
      error.value = e.message || '승인 처리에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // =============================================
  // Actions - 삭제
  // =============================================

  /**
   * 점수 삭제 (ADMIN 전용)
   */
  async function deleteScore(scoreId: number) {
    loading.value = true
    error.value = null
    try {
      await scoreApi.deleteScore(scoreId)

      // 목록에서 제거
      myScores.value = myScores.value.filter(s => s.scoreId !== scoreId)
      pendingScores.value = pendingScores.value.filter(s => s.scoreId !== scoreId)
      if (searchResult.value) {
        searchResult.value.items = searchResult.value.items.filter(s => s.scoreId !== scoreId)
        searchResult.value.totalCount--
      }

      // 현재 점수가 삭제된 경우 초기화
      if (currentScore.value?.scoreId === scoreId) {
        currentScore.value = null
      }
    } catch (e: any) {
      error.value = e.message || '점수 삭제에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // =============================================
  // Helper Functions
  // =============================================

  async function updateScoreInList(list: ItemScore[], scoreId: number) {
    const index = list.findIndex(s => s.scoreId === scoreId)
    if (index !== -1 && currentScore.value) {
      list[index] = { ...currentScore.value }
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentScore() {
    currentScore.value = null
  }

  function resetSearchRequest() {
    searchRequest.value = {
      page: 0,
      size: 20,
      sortBy: 'completedAt',
      sortDirection: 'DESC'
    }
  }

  // =============================================
  // Return
  // =============================================

  return {
    // State
    myScores,
    mySummary,
    searchResult,
    departmentSummaries,
    pendingScores,
    currentScore,
    loading,
    error,
    searchRequest,

    // Getters
    pendingCount,
    getScoresByGrade,
    averageScore,

    // Actions - 조회
    fetchScore,
    fetchScoreByItemId,
    fetchMyScores,
    fetchMySummary,
    searchScores,
    fetchDepartmentSummary,
    fetchPendingApproval,

    // Actions - 점수 계산
    calculateScore,

    // Actions - 수정
    updateWeightFactors,

    // Actions - 승인
    processApproval,

    // Actions - 삭제
    deleteScore,

    // Helpers
    clearError,
    clearCurrentScore,
    resetSearchRequest
  }
})
