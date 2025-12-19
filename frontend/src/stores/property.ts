import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { propertyApi } from '@/api/property'
import type {
  PropertyDef,
  PropertyCreateRequest,
  PropertyUpdateRequest,
  PropertyOption,
  OptionCreateRequest,
  OptionUpdateRequest
} from '@/types/property'

export const usePropertyStore = defineStore('property', () => {
  // ==================== State ====================
  const propertyDefinitions = ref<PropertyDef[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const currentBoardId = ref<number | null>(null)

  // ==================== Getters ====================
  // 활성화된 속성만 필터링 (useYn이 없으면 활성으로 간주)
  const activeProperties = computed(() =>
    propertyDefinitions.value.filter(p => p.useYn !== 'N')
  )

  // 숨김 처리되지 않은 속성 (visibleYn = 'Y' 또는 hiddenYn != 'Y')
  const visibleProperties = computed(() =>
    activeProperties.value.filter(p => p.visibleYn === 'Y' || (p.visibleYn === undefined && p.hiddenYn !== 'Y'))
  )

  // 정렬 순서대로 정렬된 속성 (sortOrder가 null이면 0으로 처리)
  const sortedProperties = computed(() =>
    [...visibleProperties.value].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  )

  // 시스템 속성 (상태, 우선순위 등) - systemYn이 없으면 일반 속성으로 간주
  const systemProperties = computed(() =>
    propertyDefinitions.value.filter(p => p.systemYn === 'Y')
  )

  // 사용자 정의 속성
  const customProperties = computed(() =>
    activeProperties.value.filter(p => p.systemYn !== 'Y')
  )

  // 타입별 속성
  const selectProperties = computed(() =>
    activeProperties.value.filter(p => p.propertyType === 'SELECT' || p.propertyType === 'MULTI_SELECT')
  )

  // ==================== Internal Mutations ====================
  function _setProperties(properties: PropertyDef[]) {
    propertyDefinitions.value = properties
  }

  function _addProperty(property: PropertyDef) {
    propertyDefinitions.value.push(property)
  }

  function _updateProperty(propertyId: number, data: Partial<PropertyDef>) {
    const index = propertyDefinitions.value.findIndex(p => p.propertyId === propertyId)
    if (index !== -1) {
      propertyDefinitions.value[index] = { ...propertyDefinitions.value[index], ...data }
    }
  }

  function _removeProperty(propertyId: number) {
    propertyDefinitions.value = propertyDefinitions.value.filter(p => p.propertyId !== propertyId)
  }

  function _addOption(propertyId: number, option: PropertyOption) {
    const property = propertyDefinitions.value.find(p => p.propertyId === propertyId)
    if (property) {
      property.options = [...(property.options || []), option]
    }
  }

  function _updateOption(propertyId: number, optionId: number, data: Partial<PropertyOption>) {
    const property = propertyDefinitions.value.find(p => p.propertyId === propertyId)
    if (property?.options) {
      const optionIndex = property.options.findIndex(o => o.optionId === optionId)
      if (optionIndex !== -1) {
        property.options[optionIndex] = { ...property.options[optionIndex], ...data }
      }
    }
  }

  function _removeOption(propertyId: number, optionId: number) {
    const property = propertyDefinitions.value.find(p => p.propertyId === propertyId)
    if (property?.options) {
      property.options = property.options.filter(o => o.optionId !== optionId)
    }
  }

  // ==================== Actions ====================

  /**
   * 속성 정의 목록 조회
   */
  async function fetchProperties(boardId: number): Promise<boolean> {
    loading.value = true
    error.value = null
    currentBoardId.value = boardId

    try {
      const response = await propertyApi.getProperties(boardId)
      if (response.success && response.data) {
        _setProperties(response.data)
        return true
      }
      error.value = response.message || '속성 정의를 불러오는데 실패했습니다.'
      return false
    } catch (e) {
      error.value = '속성 정의를 불러오는데 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 속성 정의 생성
   */
  async function createProperty(boardId: number, data: PropertyCreateRequest): Promise<PropertyDef | null> {
    loading.value = true
    error.value = null

    try {
      const response = await propertyApi.createProperty(boardId, data)
      if (response.success && response.data) {
        _addProperty(response.data)
        return response.data
      }
      error.value = response.message || '속성 생성에 실패했습니다.'
      return null
    } catch (e) {
      error.value = '속성 생성에 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 속성 정의 수정 (Optimistic Update)
   */
  async function updateProperty(propertyId: number, data: PropertyUpdateRequest): Promise<boolean> {
    // 1. 원본 데이터 백업
    const originalProperty = propertyDefinitions.value.find(p => p.propertyId === propertyId)
    if (!originalProperty) return false

    const originalData = { ...originalProperty }

    // 2. Store 먼저 갱신 (Optimistic Update)
    _updateProperty(propertyId, data)

    try {
      // 3. API 호출
      const response = await propertyApi.updateProperty(propertyId, data)
      if (response.success && response.data) {
        // 서버 응답으로 최종 업데이트
        _updateProperty(propertyId, response.data)
        return true
      }

      // 4. 실패 시 롤백
      _updateProperty(propertyId, originalData)
      error.value = response.message || '속성 수정에 실패했습니다.'
      return false
    } catch (e) {
      // 4. 실패 시 롤백
      _updateProperty(propertyId, originalData)
      error.value = '속성 수정에 실패했습니다.'
      return false
    }
  }

  /**
   * 속성 정의 삭제 (Optimistic Update)
   */
  async function deleteProperty(propertyId: number): Promise<boolean> {
    // 1. 원본 데이터 백업
    const originalProperties = [...propertyDefinitions.value]

    // 2. Store 먼저 갱신 (Optimistic Update)
    _removeProperty(propertyId)

    try {
      // 3. API 호출
      const response = await propertyApi.deleteProperty(propertyId)
      if (response.success) {
        return true
      }

      // 4. 실패 시 롤백
      propertyDefinitions.value = originalProperties
      error.value = response.message || '속성 삭제에 실패했습니다.'
      return false
    } catch (e) {
      // 4. 실패 시 롤백
      propertyDefinitions.value = originalProperties
      error.value = '속성 삭제에 실패했습니다.'
      return false
    }
  }

  /**
   * 속성 숨기기/보이기 토글 (Optimistic Update)
   * DB는 visibleYn 사용, 기존 hiddenYn 호환성 유지
   */
  async function togglePropertyVisibility(propertyId: number): Promise<boolean> {
    const property = propertyDefinitions.value.find(p => p.propertyId === propertyId)
    if (!property) return false

    // visibleYn 우선, 없으면 hiddenYn 참조
    const currentlyVisible = property.visibleYn === 'Y' || (property.visibleYn === undefined && property.hiddenYn !== 'Y')
    const newVisibleYn = currentlyVisible ? 'N' : 'Y'
    return updateProperty(propertyId, { visibleYn: newVisibleYn })
  }

  /**
   * 속성 순서 변경 (Optimistic Update)
   */
  async function reorderProperty(propertyId: number, newSortOrder: number): Promise<boolean> {
    return updateProperty(propertyId, { sortOrder: newSortOrder })
  }

  // ==================== Option Actions ====================

  /**
   * 옵션 생성
   */
  async function createOption(propertyId: number, data: OptionCreateRequest): Promise<PropertyOption | null> {
    try {
      const response = await propertyApi.createOption(propertyId, data)
      if (response.success && response.data) {
        _addOption(propertyId, response.data)
        return response.data
      }
      error.value = response.message || '옵션 생성에 실패했습니다.'
      return null
    } catch (e) {
      error.value = '옵션 생성에 실패했습니다.'
      return null
    }
  }

  /**
   * 옵션 수정 (Optimistic Update)
   */
  async function updateOption(
    propertyId: number,
    optionId: number,
    data: OptionUpdateRequest
  ): Promise<boolean> {
    // 1. 원본 데이터 백업
    const property = propertyDefinitions.value.find(p => p.propertyId === propertyId)
    const originalOption = property?.options?.find(o => o.optionId === optionId)
    if (!originalOption) return false

    const originalData = { ...originalOption }

    // 2. Store 먼저 갱신 (Optimistic Update)
    _updateOption(propertyId, optionId, data)

    try {
      // 3. API 호출
      const response = await propertyApi.updateOption(optionId, data)
      if (response.success && response.data) {
        // 서버 응답으로 최종 업데이트
        _updateOption(propertyId, optionId, response.data)
        return true
      }

      // 4. 실패 시 롤백
      _updateOption(propertyId, optionId, originalData)
      error.value = response.message || '옵션 수정에 실패했습니다.'
      return false
    } catch (e) {
      // 4. 실패 시 롤백
      _updateOption(propertyId, optionId, originalData)
      error.value = '옵션 수정에 실패했습니다.'
      return false
    }
  }

  /**
   * 옵션 삭제 (Optimistic Update)
   */
  async function deleteOption(propertyId: number, optionId: number): Promise<boolean> {
    // 1. 원본 데이터 백업
    const property = propertyDefinitions.value.find(p => p.propertyId === propertyId)
    const originalOptions = property?.options ? [...property.options] : []

    // 2. Store 먼저 갱신 (Optimistic Update)
    _removeOption(propertyId, optionId)

    try {
      // 3. API 호출
      const response = await propertyApi.deleteOption(optionId)
      if (response.success) {
        return true
      }

      // 4. 실패 시 롤백
      if (property) {
        property.options = originalOptions
      }
      error.value = response.message || '옵션 삭제에 실패했습니다.'
      return false
    } catch (e) {
      // 4. 실패 시 롤백
      if (property) {
        property.options = originalOptions
      }
      error.value = '옵션 삭제에 실패했습니다.'
      return false
    }
  }

  /**
   * 옵션 순서 변경 (Optimistic Update)
   */
  async function reorderOption(
    propertyId: number,
    optionId: number,
    newSortOrder: number
  ): Promise<boolean> {
    return updateOption(propertyId, optionId, { sortOrder: newSortOrder })
  }

  // ==================== Utility Functions ====================
  function setBoardId(boardId: number | null) {
    currentBoardId.value = boardId
  }

  function getPropertyById(propertyId: number): PropertyDef | undefined {
    return propertyDefinitions.value.find(p => p.propertyId === propertyId)
  }

  /**
   * @deprecated propertyCode는 DB에 없음. propertyName으로 대체 검색
   */
  function getPropertyByCode(propertyCode: string): PropertyDef | undefined {
    // propertyCode가 DB에 없으므로 propertyName으로 fallback
    return propertyDefinitions.value.find(p => p.propertyCode === propertyCode || p.propertyName === propertyCode)
  }

  function getOptionsByPropertyId(propertyId: number): PropertyOption[] {
    const property = getPropertyById(propertyId)
    return property?.options?.filter(o => o.useYn === 'Y') || []
  }

  function getOptionById(propertyId: number, optionId: number): PropertyOption | undefined {
    const property = getPropertyById(propertyId)
    return property?.options?.find(o => o.optionId === optionId)
  }

  function clearProperties() {
    propertyDefinitions.value = []
    currentBoardId.value = null
    error.value = null
  }

  function clearError() {
    error.value = null
  }

  // SSE 이벤트 핸들러용
  function handleSsePropertyCreated(property: PropertyDef) {
    if (property.boardId === currentBoardId.value) {
      // 이미 존재하는지 확인
      if (!propertyDefinitions.value.find(p => p.propertyId === property.propertyId)) {
        _addProperty(property)
      }
    }
  }

  function handleSsePropertyUpdated(property: PropertyDef) {
    _updateProperty(property.propertyId, property)
  }

  function handleSsePropertyDeleted(propertyId: number) {
    _removeProperty(propertyId)
  }

  return {
    // State
    propertyDefinitions,
    loading,
    error,
    currentBoardId,
    // Getters
    activeProperties,
    visibleProperties,
    sortedProperties,
    systemProperties,
    customProperties,
    selectProperties,
    // Actions - Property
    fetchProperties,
    createProperty,
    updateProperty,
    deleteProperty,
    togglePropertyVisibility,
    reorderProperty,
    // Actions - Option
    createOption,
    updateOption,
    deleteOption,
    reorderOption,
    // Utility
    setBoardId,
    getPropertyById,
    getPropertyByCode,
    getOptionsByPropertyId,
    getOptionById,
    clearProperties,
    clearError,
    // SSE Handlers
    handleSsePropertyCreated,
    handleSsePropertyUpdated,
    handleSsePropertyDeleted
  }
})
