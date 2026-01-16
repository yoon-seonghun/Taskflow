/**
 * 전체 업무 스토어 (v2.1)
 * - 소유 보드 + 공유 보드 + 개별 공유 + 배당받은 업무 통합 조회
 * - item store와 독립적으로 운영
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { allTasksApi } from '@/api/allTasks'
import type {
  AllItemResponse,
  AllItemsStats,
  AllItemsFilter,
  AllItemsSearchRequest,
  SourceType,
  ChildDisplayMode
} from '@/types/allTasks'
import { defaultAllItemsFilter } from '@/types/allTasks'

export const useAllTasksStore = defineStore('allTasks', () => {
  // ==================== State ====================
  const items = ref<AllItemResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const stats = ref<AllItemsStats | null>(null)

  // 필터 상태
  const filters = ref<AllItemsFilter>({ ...defaultAllItemsFilter })

  // 페이지네이션
  const pagination = ref({
    page: 0,
    size: 50,
    totalElements: 0,
    totalPages: 0
  })

  // 선택된 아이템
  const selectedItemId = ref<number | null>(null)

  // 확장된 아이템 (tree 모드에서 사용)
  const expandedItemIds = ref<Set<number>>(new Set())

  // ==================== Getters ====================

  /**
   * 선택된 아이템
   */
  const selectedItem = computed(() =>
    findItemById(items.value, selectedItemId.value)
  )

  /**
   * 출처별 아이템
   */
  const itemsBySource = computed(() => {
    return {
      OWNED: items.value.filter(i => i.sourceType === 'OWNED'),
      BOARD_SHARED: items.value.filter(i => i.sourceType === 'BOARD_SHARED'),
      ITEM_SHARED: items.value.filter(i => i.sourceType === 'ITEM_SHARED'),
      ASSIGNED: items.value.filter(i => i.sourceType === 'ASSIGNED')
    }
  })

  /**
   * 루트 아이템만 (depth = 0)
   */
  const rootItems = computed(() =>
    items.value.filter(item => !item.parentItemId || item.itemDepth === 0)
  )

  /**
   * 활성 아이템 수 (완료/삭제 제외)
   */
  const activeItemCount = computed(() =>
    items.value.filter(item => item.status !== 'COMPLETED' && item.status !== 'DELETED').length
  )

  /**
   * 현재 필터가 기본값인지 확인
   */
  const isDefaultFilter = computed(() => {
    return JSON.stringify(filters.value) === JSON.stringify(defaultAllItemsFilter)
  })

  /**
   * tree 모드에서 표시할 아이템 (확장 상태 적용)
   */
  const displayItems = computed(() => {
    if (filters.value.childDisplayMode === 'flat') {
      return items.value
    }
    // tree/collapse 모드: 루트 아이템만 반환 (하위 업무는 children으로 포함됨)
    return rootItems.value
  })

  // ==================== Internal Helpers ====================

  /**
   * 플랫 목록을 트리 구조로 변환
   */
  function buildTree(flatItems: AllItemResponse[]): AllItemResponse[] {
    // 모든 아이템을 맵에 저장
    const itemMap = new Map<number, AllItemResponse>()
    for (const item of flatItems) {
      // children 배열 초기화
      itemMap.set(item.itemId, { ...item, children: [] })
    }

    // 트리 구조 생성
    const rootItems: AllItemResponse[] = []
    for (const item of itemMap.values()) {
      if (item.parentItemId && itemMap.has(item.parentItemId)) {
        // 부모가 있으면 부모의 children에 추가
        const parent = itemMap.get(item.parentItemId)!
        if (!parent.children) parent.children = []
        parent.children.push(item)
      } else {
        // 부모가 없으면 루트 아이템
        rootItems.push(item)
      }
    }

    // 각 부모의 children을 childSortOrder로 정렬
    const sortChildren = (items: AllItemResponse[]) => {
      for (const item of items) {
        if (item.children && item.children.length > 0) {
          item.children.sort((a, b) => (a.childSortOrder ?? 0) - (b.childSortOrder ?? 0))
          sortChildren(item.children)
        }
      }
    }
    sortChildren(rootItems)

    return rootItems
  }

  /**
   * 아이템 트리에서 ID로 아이템 찾기 (재귀)
   */
  function findItemById(itemList: AllItemResponse[], itemId: number | null): AllItemResponse | null {
    if (!itemId) return null

    for (const item of itemList) {
      if (item.itemId === itemId) {
        return item
      }
      if (item.children && item.children.length > 0) {
        const found = findItemById(item.children, itemId)
        if (found) return found
      }
    }
    return null
  }

  /**
   * 필터를 API 요청 파라미터로 변환
   */
  function buildSearchRequest(): AllItemsSearchRequest {
    const request: AllItemsSearchRequest = {
      page: pagination.value.page,
      size: pagination.value.size,
      sortField: filters.value.sortField,
      sortDirection: filters.value.sortDirection
    }

    // 필터 적용
    if (filters.value.keyword) {
      request.keyword = filters.value.keyword
    }
    if (filters.value.status) {
      request.status = filters.value.status
    }
    if (filters.value.priority) {
      request.priority = filters.value.priority
    }
    if (filters.value.sourceType !== 'ALL') {
      request.sourceType = filters.value.sourceType
    }
    if (filters.value.boardId) {
      request.boardId = filters.value.boardId
    }
    if (filters.value.groupId) {
      request.groupId = filters.value.groupId
    }
    if (filters.value.assigneeUsername) {
      request.assigneeUsername = filters.value.assigneeUsername
    }
    if (filters.value.assignedByUsername) {
      request.assignedByUsername = filters.value.assignedByUsername
    }
    if (filters.value.startDate) {
      request.startDate = filters.value.startDate
    }
    if (filters.value.endDate) {
      request.endDate = filters.value.endDate
    }
    if (filters.value.includeCompleted) {
      request.includeCompleted = true
    }
    if (filters.value.includeDeleted) {
      request.includeDeleted = true
    }
    if (filters.value.includeChildren !== undefined) {
      request.includeChildren = filters.value.includeChildren
    }
    if (filters.value.depthFilter !== null) {
      request.depthFilter = filters.value.depthFilter
    }

    return request
  }

  // ==================== Actions ====================

  /**
   * 전체 업무 목록 조회
   */
  async function fetchAllItems(): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const request = buildSearchRequest()
      // 하위 업무 포함 조회
      request.includeChildren = true
      const response = await allTasksApi.getAllItems(request)

      if (response.success && response.data) {
        // 플랫 목록을 트리 구조로 변환
        const flatItems = response.data.content
        const treeItems = buildTree(flatItems)
        items.value = treeItems

        stats.value = response.data.stats
        pagination.value = {
          page: response.data.page,
          size: response.data.size,
          totalElements: response.data.totalElements,
          totalPages: response.data.totalPages
        }
        return true
      }

      error.value = response.message || '전체 업무 목록을 불러오는데 실패했습니다.'
      return false
    } catch (e) {
      error.value = '전체 업무 목록을 불러오는데 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 통계 조회
   */
  async function fetchStats(): Promise<boolean> {
    try {
      const request = buildSearchRequest()
      const response = await allTasksApi.getStats(request)

      if (response.success && response.data) {
        stats.value = response.data
        return true
      }
      return false
    } catch {
      return false
    }
  }

  /**
   * 필터 업데이트 (자동 재조회)
   */
  async function updateFilter(newFilter: Partial<AllItemsFilter>, autoFetch: boolean = true): Promise<void> {
    filters.value = { ...filters.value, ...newFilter }
    // 필터 변경 시 페이지 초기화
    pagination.value.page = 0

    if (autoFetch) {
      await fetchAllItems()
    }
  }

  /**
   * 출처 타입 변경
   */
  async function setSourceType(sourceType: SourceType): Promise<void> {
    await updateFilter({ sourceType })
  }

  /**
   * 하위 업무 표시 모드 변경
   */
  async function setChildDisplayMode(mode: ChildDisplayMode): Promise<void> {
    await updateFilter({ childDisplayMode: mode })
  }

  /**
   * 정렬 변경
   */
  async function setSort(field: string, direction: 'asc' | 'desc'): Promise<void> {
    await updateFilter({
      sortField: field,
      sortDirection: direction
    })
  }

  /**
   * 페이지 변경
   */
  async function setPage(page: number): Promise<void> {
    pagination.value.page = page
    await fetchAllItems()
  }

  /**
   * 페이지 크기 변경
   */
  async function setPageSize(size: number): Promise<void> {
    pagination.value.size = size
    pagination.value.page = 0 // 페이지 크기 변경 시 첫 페이지로
    await fetchAllItems()
  }

  /**
   * 필터 초기화
   */
  async function resetFilters(): Promise<void> {
    filters.value = { ...defaultAllItemsFilter }
    pagination.value.page = 0
    await fetchAllItems()
  }

  /**
   * 키워드 검색
   */
  async function search(keyword: string): Promise<void> {
    await updateFilter({ keyword })
  }

  // ==================== 아이템 선택 및 확장 ====================

  /**
   * 아이템 선택
   */
  function selectItem(itemId: number | null): void {
    selectedItemId.value = itemId
  }

  /**
   * 아이템 확장/축소 토글 (tree 모드)
   */
  function toggleExpand(itemId: number): void {
    if (expandedItemIds.value.has(itemId)) {
      expandedItemIds.value.delete(itemId)
    } else {
      expandedItemIds.value.add(itemId)
    }
  }

  /**
   * 아이템이 확장되었는지 확인
   */
  function isExpanded(itemId: number): boolean {
    return expandedItemIds.value.has(itemId)
  }

  /**
   * 모든 아이템 확장
   */
  function expandAll(): void {
    const allIds = new Set<number>()
    const collectIds = (itemList: AllItemResponse[]) => {
      for (const item of itemList) {
        if (item.children && item.children.length > 0) {
          allIds.add(item.itemId)
          collectIds(item.children)
        }
      }
    }
    collectIds(items.value)
    expandedItemIds.value = allIds
  }

  /**
   * 모든 아이템 축소
   */
  function collapseAll(): void {
    expandedItemIds.value = new Set()
  }

  // ==================== 아이템 로컬 업데이트 (SSE용) ====================

  /**
   * 아이템 로컬 업데이트 (트리 구조 지원)
   */
  function updateItemLocal(itemId: number, data: Partial<AllItemResponse>): void {
    const updateInList = (itemList: AllItemResponse[]): boolean => {
      for (let i = 0; i < itemList.length; i++) {
        if (itemList[i].itemId === itemId) {
          itemList[i] = { ...itemList[i], ...data }
          return true
        }
        if (itemList[i].children && itemList[i].children!.length > 0) {
          if (updateInList(itemList[i].children!)) {
            return true
          }
        }
      }
      return false
    }
    updateInList(items.value)
  }

  /**
   * 아이템 로컬 제거
   */
  function removeItemLocal(itemId: number): void {
    const removeFromList = (itemList: AllItemResponse[]): boolean => {
      const index = itemList.findIndex(i => i.itemId === itemId)
      if (index !== -1) {
        itemList.splice(index, 1)
        return true
      }
      for (const item of itemList) {
        if (item.children && item.children.length > 0) {
          if (removeFromList(item.children)) {
            return true
          }
        }
      }
      return false
    }
    removeFromList(items.value)
  }

  // ==================== 유틸리티 ====================

  /**
   * ID로 아이템 조회
   */
  function getItemById(itemId: number): AllItemResponse | null {
    return findItemById(items.value, itemId)
  }

  /**
   * 스토어 초기화
   */
  function reset(): void {
    items.value = []
    loading.value = false
    error.value = null
    stats.value = null
    filters.value = { ...defaultAllItemsFilter }
    pagination.value = {
      page: 0,
      size: 50,
      totalElements: 0,
      totalPages: 0
    }
    selectedItemId.value = null
    expandedItemIds.value = new Set()
  }

  /**
   * 에러 초기화
   */
  function clearError(): void {
    error.value = null
  }

  return {
    // State
    items,
    loading,
    error,
    stats,
    filters,
    pagination,
    selectedItemId,
    expandedItemIds,

    // Getters
    selectedItem,
    itemsBySource,
    rootItems,
    activeItemCount,
    isDefaultFilter,
    displayItems,

    // Actions
    fetchAllItems,
    fetchStats,
    updateFilter,
    setSourceType,
    setChildDisplayMode,
    setSort,
    setPage,
    setPageSize,
    resetFilters,
    search,

    // Selection & Expansion
    selectItem,
    toggleExpand,
    isExpanded,
    expandAll,
    collapseAll,

    // Local Updates (SSE)
    updateItemLocal,
    removeItemLocal,

    // Utility
    getItemById,
    reset,
    clearError
  }
})
