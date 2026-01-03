import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { positionApi } from '@/api/position'
import type {
  Position,
  PositionCreateRequest,
  PositionUpdateRequest,
  PositionOrderRequest
} from '@/types/position'

export const usePositionStore = defineStore('position', () => {
  // ==================== State ====================
  const positions = ref<Position[]>([])
  const selectedPosition = ref<Position | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // ==================== Getters ====================
  // 활성 직급만
  const activePositions = computed(() => {
    return positions.value.filter(p => p.useYn === 'Y')
  })

  // 선택된 직급 코드
  const selectedPositionCode = computed(() => selectedPosition.value?.positionCode || null)

  // 코드로 직급 찾기
  function getPositionByCode(code: string): Position | undefined {
    return positions.value.find(p => p.positionCode === code)
  }

  // ==================== Actions ====================

  /**
   * 직급 목록 조회
   */
  async function fetchPositions(params?: { useYn?: string }): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await positionApi.getPositions(params)
      if (response.success && response.data) {
        positions.value = response.data
        return true
      }
      error.value = response.message || '직급 목록을 불러오는데 실패했습니다.'
      return false
    } catch (e) {
      error.value = '직급 목록을 불러오는데 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 직급 생성
   */
  async function createPosition(data: PositionCreateRequest): Promise<Position | null> {
    loading.value = true
    error.value = null

    try {
      const response = await positionApi.createPosition(data)
      if (response.success && response.data) {
        // 목록 새로고침
        await fetchPositions()
        return response.data
      }
      error.value = response.message || '직급 생성에 실패했습니다.'
      return null
    } catch (e) {
      error.value = '직급 생성에 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 직급 수정
   */
  async function updatePosition(positionCode: string, data: PositionUpdateRequest): Promise<boolean> {
    error.value = null

    try {
      const response = await positionApi.updatePosition(positionCode, data)
      if (response.success && response.data) {
        // 목록에서 업데이트
        const index = positions.value.findIndex(p => p.positionCode === positionCode)
        if (index !== -1) {
          positions.value[index] = response.data
        }
        // 선택된 직급 업데이트
        if (selectedPosition.value?.positionCode === positionCode) {
          selectedPosition.value = response.data
        }
        return true
      }
      error.value = response.message || '직급 수정에 실패했습니다.'
      return false
    } catch (e) {
      error.value = '직급 수정에 실패했습니다.'
      return false
    }
  }

  /**
   * 직급 삭제
   */
  async function deletePosition(positionCode: string): Promise<boolean> {
    error.value = null

    try {
      const response = await positionApi.deletePosition(positionCode)
      if (response.success) {
        // 목록에서 제거
        positions.value = positions.value.filter(p => p.positionCode !== positionCode)
        // 선택 해제
        if (selectedPosition.value?.positionCode === positionCode) {
          selectedPosition.value = null
        }
        return true
      }
      error.value = response.message || '직급 삭제에 실패했습니다.'
      return false
    } catch (e) {
      error.value = '직급 삭제에 실패했습니다.'
      return false
    }
  }

  /**
   * 직급 순서 변경
   */
  async function updatePositionOrder(positionCode: string, sortOrder: number): Promise<boolean> {
    try {
      const response = await positionApi.updatePositionOrder(positionCode, sortOrder)
      if (response.success) {
        // 목록 새로고침
        await fetchPositions()
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 직급 순서 일괄 변경
   */
  async function updatePositionOrders(orders: PositionOrderRequest[]): Promise<boolean> {
    try {
      const response = await positionApi.updatePositionOrders(orders)
      if (response.success) {
        // 목록 새로고침
        await fetchPositions()
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  // ==================== Selection Functions ====================
  function selectPosition(position: Position | null) {
    selectedPosition.value = position
  }

  function clearSelection() {
    selectedPosition.value = null
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    positions,
    selectedPosition,
    loading,
    error,
    // Getters
    activePositions,
    selectedPositionCode,
    getPositionByCode,
    // Actions
    fetchPositions,
    createPosition,
    updatePosition,
    deletePosition,
    updatePositionOrder,
    updatePositionOrders,
    // Selection
    selectPosition,
    clearSelection,
    clearError
  }
})
