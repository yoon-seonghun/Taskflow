/**
 * 드래그 앤 드롭 정렬 공통 Composable
 *
 * 사용처:
 * - BoardsView.vue: 보드 순서
 * - PropertiesContent.vue: 속성 정의 순서
 * - GroupList.vue: 그룹 순서
 * - BoardPropertySelector.vue: 선택된 속성 순서
 * - CategoryDetailModal.vue: 카테고리 내 속성 순서
 * - ItemPropertyModal.vue: 아이템 속성 순서
 */
import { ref, type Ref } from 'vue'

// =============================================
// 타입 정의
// =============================================

export interface SortableDragOptions<T> {
  /** 아이템 배열 (필수) */
  items: Ref<T[]>

  /** 아이템의 고유 키 필드명 (기본: 'id') */
  itemKey?: keyof T

  /** 드래그 허용 여부를 결정하는 함수 */
  canDrag?: (item: T, index: number) => boolean

  /** 드롭 허용 여부를 결정하는 함수 */
  canDrop?: (draggedItem: T, targetItem: T, draggedIndex: number, targetIndex: number) => boolean

  /** 재정렬 완료 후 콜백 */
  onReorder?: (reorderedItems: T[], fromIndex: number, toIndex: number) => void | Promise<void>

  /** 드래그 시작 시 콜백 */
  onDragStart?: (item: T, index: number) => void

  /** 드래그 종료 시 콜백 */
  onDragEnd?: () => void
}

export interface SortableDragReturn<T> {
  /** 현재 드래그 중인 아이템 인덱스 */
  draggedIndex: Ref<number | null>

  /** 드래그 오버 중인 아이템 인덱스 */
  dragOverIndex: Ref<number | null>

  /** 드래그 진행 중 여부 */
  isDragging: Ref<boolean>

  /** 재정렬 처리 중 여부 (API 호출 등) */
  isReordering: Ref<boolean>

  /** 드래그 시작 핸들러 */
  handleDragStart: (index: number, event: DragEvent) => void

  /** 드래그 오버 핸들러 */
  handleDragOver: (index: number, event: DragEvent) => void

  /** 드래그 리브 핸들러 */
  handleDragLeave: () => void

  /** 드래그 종료 핸들러 */
  handleDragEnd: (event?: DragEvent) => void

  /** 드롭 핸들러 */
  handleDrop: (targetIndex: number, event: DragEvent) => Promise<void>

  /** 드래그 상태 초기화 */
  resetDragState: () => void

  /** CSS 클래스 헬퍼 - 드래그 중인 아이템인지 */
  isDraggedItem: (index: number) => boolean

  /** CSS 클래스 헬퍼 - 드롭 타겟인지 */
  isDropTarget: (index: number) => boolean

  /** CSS 클래스 객체 반환 */
  getDragClass: (index: number) => Record<string, boolean>
}

// =============================================
// Composable 구현
// =============================================

export function useSortableDrag<T>(options: SortableDragOptions<T>): SortableDragReturn<T> {
  const { items, itemKey = 'id' as keyof T, canDrag, canDrop, onReorder, onDragStart, onDragEnd } = options

  // 상태
  const draggedIndex = ref<number | null>(null)
  const dragOverIndex = ref<number | null>(null)
  const isDragging = ref(false)
  const isReordering = ref(false)

  /**
   * 드래그 상태 초기화
   */
  function resetDragState() {
    draggedIndex.value = null
    dragOverIndex.value = null
    isDragging.value = false
  }

  /**
   * 드래그 시작
   */
  function handleDragStart(index: number, event: DragEvent) {
    const item = items.value[index]

    // 드래그 허용 여부 확인
    if (canDrag && !canDrag(item, index)) {
      event.preventDefault()
      return
    }

    draggedIndex.value = index
    isDragging.value = true

    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/plain', String(index))
    }

    // 드래그 중 시각적 피드백 (투명도)
    const target = event.target as HTMLElement
    requestAnimationFrame(() => {
      target.classList.add('sortable-dragging')
    })

    onDragStart?.(item, index)
  }

  /**
   * 드래그 오버
   */
  function handleDragOver(index: number, event: DragEvent) {
    event.preventDefault()

    if (draggedIndex.value === null) return
    if (draggedIndex.value === index) return

    const draggedItem = items.value[draggedIndex.value]
    const targetItem = items.value[index]

    // 드롭 허용 여부 확인
    if (canDrop && !canDrop(draggedItem, targetItem, draggedIndex.value, index)) {
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'none'
      }
      return
    }

    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move'
    }

    dragOverIndex.value = index
  }

  /**
   * 드래그 리브
   */
  function handleDragLeave() {
    dragOverIndex.value = null
  }

  /**
   * 드래그 종료
   */
  function handleDragEnd(event?: DragEvent) {
    if (event) {
      const target = event.target as HTMLElement
      target.classList.remove('sortable-dragging')
    }

    resetDragState()
    onDragEnd?.()
  }

  /**
   * 드롭 - 배열 재정렬
   */
  async function handleDrop(targetIndex: number, event: DragEvent) {
    event.preventDefault()

    if (draggedIndex.value === null || draggedIndex.value === targetIndex) {
      handleDragEnd(event)
      return
    }

    const fromIndex = draggedIndex.value
    const draggedItem = items.value[fromIndex]
    const targetItem = items.value[targetIndex]

    // 드롭 허용 여부 확인
    if (canDrop && !canDrop(draggedItem, targetItem, fromIndex, targetIndex)) {
      handleDragEnd(event)
      return
    }

    // 배열 재정렬
    const newItems = [...items.value]
    const [movedItem] = newItems.splice(fromIndex, 1)
    newItems.splice(targetIndex, 0, movedItem)

    // 상태 업데이트
    items.value = newItems

    // 드래그 상태 초기화
    resetDragState()

    // 콜백 호출 (API 호출 등)
    if (onReorder) {
      isReordering.value = true
      try {
        await onReorder(newItems, fromIndex, targetIndex)
      } finally {
        isReordering.value = false
      }
    }
  }

  /**
   * CSS 클래스 헬퍼 - 드래그 중인 아이템인지
   */
  function isDraggedItem(index: number): boolean {
    return draggedIndex.value === index
  }

  /**
   * CSS 클래스 헬퍼 - 드롭 타겟인지
   */
  function isDropTarget(index: number): boolean {
    return dragOverIndex.value === index && draggedIndex.value !== index
  }

  /**
   * CSS 클래스 객체 반환
   */
  function getDragClass(index: number): Record<string, boolean> {
    return {
      'sortable-dragging': isDraggedItem(index),
      'sortable-drag-over': isDropTarget(index)
    }
  }

  return {
    draggedIndex,
    dragOverIndex,
    isDragging,
    isReordering,
    handleDragStart,
    handleDragOver,
    handleDragLeave,
    handleDragEnd,
    handleDrop,
    resetDragState,
    isDraggedItem,
    isDropTarget,
    getDragClass
  }
}

// =============================================
// 그룹별 정렬 Composable (PropertiesContent 등)
// =============================================

export interface GroupedSortableDragOptions<T, G = string> {
  /** 아이템의 고유 키 필드명 */
  itemKey?: keyof T

  /** 같은 그룹 내에서만 이동 허용 여부 (기본: true) */
  sameGroupOnly?: boolean

  /** 재정렬 완료 후 콜백 */
  onReorder?: (
    items: T[],
    fromIndex: number,
    toIndex: number,
    group: G
  ) => void | Promise<void>
}

export interface GroupedSortableDragReturn<T, G = string> {
  /** 현재 드래그 중인 아이템 */
  draggedItem: Ref<T | null>

  /** 현재 드래그 중인 그룹 */
  draggedGroup: Ref<G | null>

  /** 드롭 타겟 인덱스 */
  dragOverIndex: Ref<number | null>

  /** 재정렬 처리 중 여부 */
  isReordering: Ref<boolean>

  /** 드래그 시작 핸들러 */
  handleDragStart: (item: T, group: G, event: DragEvent) => void

  /** 드래그 오버 핸들러 */
  handleDragOver: (index: number, group: G, event: DragEvent) => void

  /** 드래그 리브 핸들러 */
  handleDragLeave: () => void

  /** 드래그 종료 핸들러 */
  handleDragEnd: (event?: DragEvent) => void

  /** 드롭 핸들러 */
  handleDrop: (
    targetIndex: number,
    group: G,
    groupItems: T[],
    event: DragEvent
  ) => Promise<T[] | null>

  /** 상태 초기화 */
  resetDragState: () => void

  /** CSS 클래스 헬퍼 */
  isDropTarget: (index: number) => boolean
}

export function useGroupedSortableDrag<T, G = string>(
  options: GroupedSortableDragOptions<T, G> = {}
): GroupedSortableDragReturn<T, G> {
  const { itemKey = 'id' as keyof T, sameGroupOnly = true, onReorder } = options

  // 상태
  const draggedItem = ref<T | null>(null) as Ref<T | null>
  const draggedGroup = ref<G | null>(null) as Ref<G | null>
  const dragOverIndex = ref<number | null>(null)
  const isReordering = ref(false)

  /**
   * 상태 초기화
   */
  function resetDragState() {
    draggedItem.value = null
    draggedGroup.value = null
    dragOverIndex.value = null
  }

  /**
   * 드래그 시작
   */
  function handleDragStart(item: T, group: G, event: DragEvent) {
    draggedItem.value = item
    draggedGroup.value = group

    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/plain', String((item as Record<string, unknown>)[itemKey as string]))
    }

    const target = event.target as HTMLElement
    requestAnimationFrame(() => {
      target.classList.add('sortable-dragging')
    })
  }

  /**
   * 드래그 오버
   */
  function handleDragOver(index: number, group: G, event: DragEvent) {
    event.preventDefault()

    if (!draggedItem.value) return

    // 같은 그룹 내에서만 허용
    if (sameGroupOnly && draggedGroup.value !== group) {
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'none'
      }
      return
    }

    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move'
    }

    dragOverIndex.value = index
  }

  /**
   * 드래그 리브
   */
  function handleDragLeave() {
    dragOverIndex.value = null
  }

  /**
   * 드래그 종료
   */
  function handleDragEnd(event?: DragEvent) {
    if (event) {
      const target = event.target as HTMLElement
      target.classList.remove('sortable-dragging')
    }
    resetDragState()
  }

  /**
   * 드롭 - 그룹 내 배열 재정렬
   * @returns 재정렬된 배열 또는 null (변경 없음)
   */
  async function handleDrop(
    targetIndex: number,
    group: G,
    groupItems: T[],
    event: DragEvent
  ): Promise<T[] | null> {
    event.preventDefault()
    dragOverIndex.value = null

    if (!draggedItem.value) {
      handleDragEnd()
      return null
    }

    // 같은 그룹 내에서만 허용
    if (sameGroupOnly && draggedGroup.value !== group) {
      handleDragEnd()
      return null
    }

    const items = [...groupItems]
    const draggedKey = (draggedItem.value as Record<string, unknown>)[itemKey as string]

    // 현재 인덱스 찾기
    const currentIndex = items.findIndex(
      item => (item as Record<string, unknown>)[itemKey as string] === draggedKey
    )

    if (currentIndex === -1 || currentIndex === targetIndex) {
      handleDragEnd()
      return null
    }

    // 배열 재정렬
    const [movedItem] = items.splice(currentIndex, 1)
    items.splice(targetIndex, 0, movedItem)

    // 콜백 호출
    if (onReorder) {
      isReordering.value = true
      try {
        await onReorder(items, currentIndex, targetIndex, group)
      } finally {
        isReordering.value = false
      }
    }

    handleDragEnd()
    return items
  }

  /**
   * CSS 클래스 헬퍼
   */
  function isDropTarget(index: number): boolean {
    return dragOverIndex.value === index
  }

  return {
    draggedItem,
    draggedGroup,
    dragOverIndex,
    isReordering,
    handleDragStart,
    handleDragOver,
    handleDragLeave,
    handleDragEnd,
    handleDrop,
    resetDragState,
    isDropTarget
  }
}

// =============================================
// 아이템 기반 드래그 Composable (ID로 식별)
// GroupList, BoardsView 등에서 사용
// =============================================

export interface ItemDragOptions<T, K = number> {
  /** 아이템의 고유 키를 추출하는 함수 */
  getItemKey: (item: T) => K

  /** 드롭 시 순서 변경 콜백 */
  onDrop?: (draggedItem: T, targetItem: T) => void | Promise<void>

  /** 드래그 허용 여부 */
  canDrag?: (item: T) => boolean

  /** 드롭 허용 여부 (다른 그룹 간 이동 등 제어) */
  canDrop?: (draggedItem: T, targetItem: T) => boolean
}

export interface ItemDragReturn<T, K = number> {
  /** 드래그 중인 아이템 */
  draggedItem: Ref<T | null>

  /** 드래그 오버 중인 아이템의 키 */
  dragOverKey: Ref<K | null>

  /** 드래그 시작 핸들러 */
  handleDragStart: (item: T, event: DragEvent) => void

  /** 드래그 오버 핸들러 */
  handleDragOver: (item: T, event: DragEvent) => void

  /** 드래그 리브 핸들러 */
  handleDragLeave: () => void

  /** 드래그 종료 핸들러 */
  handleDragEnd: () => void

  /** 드롭 핸들러 */
  handleDrop: (targetItem: T, event: DragEvent) => Promise<void>

  /** 상태 초기화 */
  resetDragState: () => void

  /** 드래그 오버 상태인지 확인 */
  isDragOver: (item: T) => boolean
}

export function useItemDrag<T, K = number>(
  options: ItemDragOptions<T, K>
): ItemDragReturn<T, K> {
  const { getItemKey, onDrop, canDrag, canDrop } = options

  const draggedItem = ref<T | null>(null) as Ref<T | null>
  const dragOverKey = ref<K | null>(null) as Ref<K | null>

  function resetDragState() {
    draggedItem.value = null
    dragOverKey.value = null
  }

  function handleDragStart(item: T, event: DragEvent) {
    if (canDrag && !canDrag(item)) {
      event.preventDefault()
      return
    }

    draggedItem.value = item

    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/plain', String(getItemKey(item)))
    }

    // 드래그 시작 시 시각적 피드백
    const target = event.target as HTMLElement
    requestAnimationFrame(() => {
      target.classList.add('sortable-dragging')
    })
  }

  function handleDragOver(item: T, event: DragEvent) {
    event.preventDefault()

    if (!draggedItem.value) return

    const draggedKey = getItemKey(draggedItem.value)
    const targetKey = getItemKey(item)

    // 같은 아이템이면 무시
    if (draggedKey === targetKey) return

    // 드롭 허용 여부 확인
    if (canDrop && !canDrop(draggedItem.value, item)) {
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'none'
      }
      return
    }

    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move'
    }

    dragOverKey.value = targetKey
  }

  function handleDragLeave() {
    dragOverKey.value = null
  }

  function handleDragEnd() {
    // CSS 클래스 제거는 DOM 이벤트로 처리됨
    resetDragState()
  }

  async function handleDrop(targetItem: T, event: DragEvent) {
    event.preventDefault()

    if (!draggedItem.value) {
      resetDragState()
      return
    }

    const draggedKey = getItemKey(draggedItem.value)
    const targetKey = getItemKey(targetItem)

    // 같은 아이템이면 무시
    if (draggedKey === targetKey) {
      resetDragState()
      return
    }

    // 드롭 허용 여부 확인
    if (canDrop && !canDrop(draggedItem.value, targetItem)) {
      resetDragState()
      return
    }

    // 콜백 호출
    if (onDrop) {
      await onDrop(draggedItem.value, targetItem)
    }

    resetDragState()
  }

  function isDragOver(item: T): boolean {
    if (dragOverKey.value === null) return false
    return getItemKey(item) === dragOverKey.value
  }

  return {
    draggedItem,
    dragOverKey,
    handleDragStart,
    handleDragOver,
    handleDragLeave,
    handleDragEnd,
    handleDrop,
    resetDragState,
    isDragOver
  }
}

// =============================================
// 공통 CSS 스타일 (style.css에 추가 필요)
// =============================================
/*
.sortable-dragging {
  opacity: 0.5;
}

.sortable-drag-over {
  border-top: 2px solid #3B82F6;
}
*/
