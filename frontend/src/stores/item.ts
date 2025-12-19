import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { itemApi } from '@/api/item'
import { isItemOverdue } from '@/utils/item'
import type {
  Item,
  ItemCreateRequest,
  ItemUpdateRequest,
  ItemSearchRequest
} from '@/types/item'

export const useItemStore = defineStore('item', () => {
  // ==================== State ====================
  const items = ref<Item[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const selectedItemId = ref<number | null>(null)
  const currentBoardId = ref<number | null>(null)

  // 필터 상태
  const filters = ref<ItemSearchRequest>({
    includeCompleted: false,
    includeDeleted: false
  })

  // 편집 상태 추적 (충돌 감지용)
  const editingItemId = ref<number | null>(null)
  const editingItemData = ref<Partial<Item> | null>(null)

  // ==================== Getters ====================
  const selectedItem = computed(() =>
    items.value.find(item => item.itemId === selectedItemId.value) || null
  )

  // 활성 아이템 (완료/삭제 제외)
  const activeItems = computed(() =>
    items.value.filter(item => item.status !== 'COMPLETED' && item.status !== 'DELETED')
  )

  // 완료된 아이템
  const completedItems = computed(() =>
    items.value.filter(item => item.status === 'COMPLETED')
  )

  // 삭제된 아이템
  const deletedItems = computed(() =>
    items.value.filter(item => item.status === 'DELETED')
  )

  // 대기 중인 아이템
  const pendingItems = computed(() =>
    items.value.filter(item => item.status === 'PENDING')
  )

  // 지연된 아이템 (dueDate 기준, 완료/삭제 제외)
  const overdueItems = computed(() =>
    items.value.filter(item => isItemOverdue(item))
  )

  // 지연 아이템 수
  const overdueItemCount = computed(() => overdueItems.value.length)

  // 오늘 완료/삭제된 아이템 (당일 기준 Hidden 처리용)
  const todayCompletedItems = computed(() => {
    const today = new Date().toISOString().split('T')[0]
    return items.value.filter(item => {
      if (item.status === 'COMPLETED' && item.completedAt) {
        return item.completedAt.startsWith(today)
      }
      if (item.status === 'DELETED' && item.deletedAt) {
        return item.deletedAt.startsWith(today)
      }
      return false
    })
  })

  // 그룹별 아이템
  const itemsByGroup = computed(() => {
    const grouped: Record<number, Item[]> = {}
    for (const item of activeItems.value) {
      const groupId = item.groupId || 0
      if (!grouped[groupId]) {
        grouped[groupId] = []
      }
      grouped[groupId].push(item)
    }
    return grouped
  })

  // 상태별 아이템 (칸반 뷰용)
  const itemsByStatus = computed(() => {
    return {
      NOT_STARTED: items.value.filter(i => i.status === 'NOT_STARTED'),
      IN_PROGRESS: items.value.filter(i => i.status === 'IN_PROGRESS'),
      PENDING: items.value.filter(i => i.status === 'PENDING'),
      COMPLETED: items.value.filter(i => i.status === 'COMPLETED'),
      DELETED: items.value.filter(i => i.status === 'DELETED')
    }
  })

  // 아이템 수
  const itemCount = computed(() => items.value.length)
  const activeItemCount = computed(() => activeItems.value.length)

  // ==================== Internal Mutations ====================
  function _setItems(newItems: Item[]) {
    items.value = newItems
  }

  function _addItem(item: Item) {
    items.value.unshift(item)
  }

  function _updateItem(itemId: number, data: Partial<Item>) {
    const index = items.value.findIndex(item => item.itemId === itemId)
    if (index !== -1) {
      items.value[index] = { ...items.value[index], ...data }
    }
  }

  function _removeItem(itemId: number) {
    items.value = items.value.filter(item => item.itemId !== itemId)
  }

  // ==================== Actions ====================

  /**
   * 아이템 목록 조회
   */
  async function fetchItems(boardId: number, params?: ItemSearchRequest): Promise<boolean> {
    loading.value = true
    error.value = null
    currentBoardId.value = boardId

    try {
      const response = await itemApi.getItems(boardId, { ...filters.value, ...params })
      if (response.success && response.data) {
        _setItems(response.data.content)
        return true
      }
      error.value = response.message || '아이템 목록을 불러오는데 실패했습니다.'
      return false
    } catch (e) {
      error.value = '아이템 목록을 불러오는데 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 아이템 상세 조회
   */
  async function fetchItem(boardId: number, itemId: number): Promise<Item | null> {
    try {
      const response = await itemApi.getItem(boardId, itemId)
      if (response.success && response.data) {
        _updateItem(itemId, response.data)
        return response.data
      }
      return null
    } catch (e) {
      return null
    }
  }

  /**
   * 아이템 생성
   */
  async function createItem(boardId: number, data: ItemCreateRequest): Promise<Item | null> {
    loading.value = true
    error.value = null

    try {
      const response = await itemApi.createItem(boardId, data)
      if (response.success && response.data) {
        _addItem(response.data)
        return response.data
      }
      error.value = response.message || '아이템 생성에 실패했습니다.'
      return null
    } catch (e) {
      error.value = '아이템 생성에 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 아이템 수정 (Optimistic Update)
   * @returns 성공 시 업데이트된 Item 객체, 실패 시 null
   */
  async function updateItem(
    boardId: number,
    itemId: number,
    data: ItemUpdateRequest
  ): Promise<Item | null> {
    // 1. 원본 데이터 백업
    const originalItem = items.value.find(item => item.itemId === itemId)

    // 목록에 없는 경우 (개별 조회된 아이템) - API만 호출
    if (!originalItem) {
      try {
        const response = await itemApi.updateItem(boardId, itemId, data)
        if (response.success && response.data) {
          return response.data
        }
        error.value = response.message || '아이템 수정에 실패했습니다.'
        return null
      } catch (e) {
        error.value = '아이템 수정에 실패했습니다.'
        return null
      }
    }

    const originalData = { ...originalItem }

    // 2. Store 먼저 갱신 (Optimistic Update)
    _updateItem(itemId, data)

    try {
      // 3. API 호출
      const response = await itemApi.updateItem(boardId, itemId, data)
      if (response.success && response.data) {
        // 서버 응답으로 최종 업데이트
        _updateItem(itemId, response.data)
        return response.data
      }

      // 4. 실패 시 롤백
      _updateItem(itemId, originalData)
      error.value = response.message || '아이템 수정에 실패했습니다.'
      return null
    } catch (e) {
      // 4. 실패 시 롤백
      _updateItem(itemId, originalData)
      error.value = '아이템 수정에 실패했습니다.'
      return null
    }
  }

  /**
   * 아이템 삭제 (Optimistic Update)
   */
  async function deleteItem(boardId: number, itemId: number): Promise<boolean> {
    // 1. 원본 데이터 백업
    const originalItems = [...items.value]

    // 2. Store 먼저 갱신 (Optimistic Update) - 소프트 삭제로 상태 변경
    _updateItem(itemId, {
      status: 'DELETED',
      deletedAt: new Date().toISOString()
    })

    try {
      // 3. API 호출
      const response = await itemApi.deleteItem(boardId, itemId)
      if (response.success) {
        return true
      }

      // 4. 실패 시 롤백
      items.value = originalItems
      error.value = response.message || '아이템 삭제에 실패했습니다.'
      return false
    } catch (e) {
      // 4. 실패 시 롤백
      items.value = originalItems
      error.value = '아이템 삭제에 실패했습니다.'
      return false
    }
  }

  /**
   * 아이템 완료 처리 (Optimistic Update)
   */
  async function completeItem(boardId: number, itemId: number): Promise<boolean> {
    // 1. 원본 데이터 백업
    const originalItem = items.value.find(item => item.itemId === itemId)
    if (!originalItem) return false

    const originalData = { ...originalItem }

    // 2. Store 먼저 갱신 (Optimistic Update)
    _updateItem(itemId, {
      status: 'COMPLETED',
      completedAt: new Date().toISOString()
    })

    try {
      // 3. API 호출
      const response = await itemApi.completeItem(boardId, itemId)
      if (response.success && response.data) {
        _updateItem(itemId, response.data)
        return true
      }

      // 4. 실패 시 롤백
      _updateItem(itemId, originalData)
      error.value = response.message || '완료 처리에 실패했습니다.'
      return false
    } catch (e) {
      // 4. 실패 시 롤백
      _updateItem(itemId, originalData)
      error.value = '완료 처리에 실패했습니다.'
      return false
    }
  }

  /**
   * 아이템 복원 (Optimistic Update)
   */
  async function restoreItem(boardId: number, itemId: number): Promise<boolean> {
    // 1. 원본 데이터 백업
    const originalItem = items.value.find(item => item.itemId === itemId)
    if (!originalItem) return false

    const originalData = { ...originalItem }

    // 2. Store 먼저 갱신 (Optimistic Update)
    _updateItem(itemId, {
      status: 'NOT_STARTED',
      completedAt: undefined,
      deletedAt: undefined
    })

    try {
      // 3. API 호출
      const response = await itemApi.restoreItem(boardId, itemId)
      if (response.success && response.data) {
        _updateItem(itemId, response.data)
        return true
      }

      // 4. 실패 시 롤백
      _updateItem(itemId, originalData)
      error.value = response.message || '복원에 실패했습니다.'
      return false
    } catch (e) {
      // 4. 실패 시 롤백
      _updateItem(itemId, originalData)
      error.value = '복원에 실패했습니다.'
      return false
    }
  }

  /**
   * 아이템 속성값 수정 (Optimistic Update)
   */
  async function updateItemProperty(
    boardId: number,
    itemId: number,
    propertyId: number,
    value: unknown
  ): Promise<boolean> {
    const item = items.value.find(i => i.itemId === itemId)
    if (!item) return false

    // 원본 백업
    const originalProperties = item.propertyValues ? { ...item.propertyValues } : {}

    // Optimistic Update
    const newProperties = { ...originalProperties, [propertyId]: value }
    _updateItem(itemId, { propertyValues: newProperties })

    try {
      const response = await itemApi.updateItem(boardId, itemId, {
        properties: { [propertyId]: value }
      })

      if (response.success && response.data) {
        _updateItem(itemId, response.data)
        return true
      }

      // 롤백
      _updateItem(itemId, { propertyValues: originalProperties })
      return false
    } catch (e) {
      // 롤백
      _updateItem(itemId, { propertyValues: originalProperties })
      return false
    }
  }

  /**
   * 아이템 순서 변경 (Optimistic Update)
   */
  async function reorderItem(
    boardId: number,
    itemId: number,
    newSortOrder: number,
    newGroupId?: number
  ): Promise<boolean> {
    const originalItem = items.value.find(i => i.itemId === itemId)
    if (!originalItem) return false

    const originalData = { ...originalItem }

    // Optimistic Update
    _updateItem(itemId, {
      sortOrder: newSortOrder,
      groupId: newGroupId ?? originalItem.groupId
    })

    try {
      const response = await itemApi.updateItem(boardId, itemId, {
        sortOrder: newSortOrder,
        groupId: newGroupId
      })

      if (response.success && response.data) {
        _updateItem(itemId, response.data)
        return true
      }

      _updateItem(itemId, originalData)
      return false
    } catch (e) {
      _updateItem(itemId, originalData)
      return false
    }
  }

  // ==================== Utility Functions ====================
  function selectItem(itemId: number | null) {
    selectedItemId.value = itemId
  }

  function setBoardId(boardId: number | null) {
    currentBoardId.value = boardId
  }

  function setFilters(newFilters: Partial<ItemSearchRequest>) {
    filters.value = { ...filters.value, ...newFilters }
  }

  function getItemById(itemId: number): Item | undefined {
    return items.value.find(item => item.itemId === itemId)
  }

  function clearItems() {
    items.value = []
    selectedItemId.value = null
    error.value = null
  }

  function clearError() {
    error.value = null
  }

  // SSE 이벤트 핸들러용
  function handleSseItemCreated(item: Item) {
    if (item.boardId === currentBoardId.value) {
      // 이미 존재하는지 확인
      if (!items.value.find(i => i.itemId === item.itemId)) {
        _addItem(item)
      }
    }
  }

  function handleSseItemUpdated(item: Item) {
    _updateItem(item.itemId, item)
  }

  function handleSseItemDeleted(itemId: number) {
    _removeItem(itemId)
  }

  /**
   * SSE 이벤트용 로컬 아이템 업데이트 (API 호출 없이 Store만 갱신)
   * 댓글 수 증가 등 SSE 이벤트에서 사용
   */
  function updateItemLocal(itemId: number, data: Partial<Item>) {
    _updateItem(itemId, data)
  }

  // ==================== 편집 상태 관리 (충돌 감지용) ====================

  /**
   * 편집 시작 - 편집 중인 아이템과 데이터 추적
   */
  function startEditing(itemId: number, data?: Partial<Item>) {
    editingItemId.value = itemId
    // 현재 아이템의 원본 데이터 저장
    const item = items.value.find(i => i.itemId === itemId)
    editingItemData.value = data || (item ? { ...item } : null)
  }

  /**
   * 편집 데이터 업데이트 (사용자가 값을 변경할 때)
   */
  function updateEditingData(data: Partial<Item>) {
    if (editingItemData.value) {
      editingItemData.value = { ...editingItemData.value, ...data }
    }
  }

  /**
   * 편집 종료
   */
  function stopEditing() {
    editingItemId.value = null
    editingItemData.value = null
  }

  /**
   * 특정 아이템이 현재 편집 중인지 확인
   */
  function isEditing(itemId: number): boolean {
    return editingItemId.value === itemId
  }

  /**
   * 현재 편집 중인 데이터 가져오기
   */
  function getEditingData(): Partial<Item> | null {
    return editingItemData.value
  }

  return {
    // State
    items,
    loading,
    error,
    selectedItemId,
    currentBoardId,
    filters,
    editingItemId,
    editingItemData,
    // Getters
    selectedItem,
    activeItems,
    completedItems,
    deletedItems,
    pendingItems,
    overdueItems,
    overdueItemCount,
    todayCompletedItems,
    itemsByGroup,
    itemsByStatus,
    itemCount,
    activeItemCount,
    // Actions
    fetchItems,
    fetchItem,
    createItem,
    updateItem,
    deleteItem,
    completeItem,
    restoreItem,
    updateItemProperty,
    reorderItem,
    // Utility
    selectItem,
    setBoardId,
    setFilters,
    getItemById,
    clearItems,
    clearError,
    // SSE Handlers
    handleSseItemCreated,
    handleSseItemUpdated,
    handleSseItemDeleted,
    updateItemLocal,
    // Editing State (충돌 감지용)
    startEditing,
    updateEditingData,
    stopEditing,
    isEditing,
    getEditingData
  }
})
