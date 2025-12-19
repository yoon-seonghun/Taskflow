/**
 * SSE 실시간 동기화 composable
 */
import { onMounted, onUnmounted, watch } from 'vue'
import { useSseStore } from '@/stores/sse'
import { useItemStore } from '@/stores/item'
import { usePropertyStore } from '@/stores/property'
import { useAuthStore } from '@/stores/auth'
import { useToast } from './useToast'
import type { SseEventData, SseEventType } from '@/types/sse'
import type { Item, ItemUpdateRequest } from '@/types/item'
import type { PropertyDef } from '@/types/property'
import type { Comment } from '@/types/comment'

const SSE_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const RECONNECT_DELAY = 3000 // 3초
const MAX_RECONNECT_ATTEMPTS = 10

export function useSse() {
  const sseStore = useSseStore()
  const itemStore = useItemStore()
  const propertyStore = usePropertyStore()
  const authStore = useAuthStore()
  const toast = useToast()

  let reconnectTimeout: ReturnType<typeof setTimeout> | null = null

  function connect(boardId?: number) {
    // 기존 연결이 남아있으면 정리
    if (sseStore.eventSource) {
      sseStore.eventSource.close()
      sseStore.setEventSource(null)
    }

    // 이미 연결 중이거나 연결된 상태면 무시
    if (sseStore.isConnecting || sseStore.isConnected) {
      return
    }

    // 인증되지 않은 상태면 연결하지 않음
    if (!authStore.isAuthenticated) {
      return
    }

    sseStore.setStatus('connecting')

    const token = authStore.accessToken
    let url = `${SSE_BASE_URL}/sse/subscribe`

    // 토큰과 보드ID를 쿼리 파라미터로 전달
    const params = new URLSearchParams()
    if (token) {
      params.append('token', token)
    }
    if (boardId) {
      params.append('boardId', boardId.toString())
    }

    if (params.toString()) {
      url += `?${params.toString()}`
    }

    const eventSource = new EventSource(url)
    sseStore.setEventSource(eventSource)

    eventSource.onopen = () => {
      sseStore.setStatus('connected')
      sseStore.resetReconnectAttempts()
      sseStore.setSubscribedBoardId(boardId ?? null)
      console.log('[SSE] Connected')
    }

    eventSource.onerror = (event) => {
      console.error('[SSE] Error:', event)
      eventSource.close()
      sseStore.setEventSource(null)
      sseStore.setStatus('error')

      // 재연결 시도
      scheduleReconnect(boardId)
    }

    // 이벤트 타입별 핸들러 등록
    setupEventHandlers(eventSource)
  }

  function setupEventHandlers(eventSource: EventSource) {
    // item:created
    eventSource.addEventListener('item:created', (event: MessageEvent) => {
      handleItemCreated(JSON.parse(event.data))
    })

    // item:updated
    eventSource.addEventListener('item:updated', (event: MessageEvent) => {
      handleItemUpdated(JSON.parse(event.data))
    })

    // item:deleted
    eventSource.addEventListener('item:deleted', (event: MessageEvent) => {
      handleItemDeleted(JSON.parse(event.data))
    })

    // property:updated
    eventSource.addEventListener('property:updated', (event: MessageEvent) => {
      handlePropertyUpdated(JSON.parse(event.data))
    })

    // comment:created
    eventSource.addEventListener('comment:created', (event: MessageEvent) => {
      handleCommentCreated(JSON.parse(event.data))
    })

    // heartbeat (연결 유지)
    eventSource.addEventListener('heartbeat', () => {
      sseStore.setLastEventTime(new Date())
    })
  }

  function handleItemCreated(data: SseEventData) {
    const item = data.data as Item
    sseStore.setLastEventTime(new Date())

    // 현재 보드의 아이템인 경우에만 추가
    if (item.boardId === itemStore.currentBoardId) {
      itemStore.handleSseItemCreated(item)
    }
  }

  function handleItemUpdated(data: SseEventData) {
    const item = data.data as Item
    sseStore.setLastEventTime(new Date())

    // 편집 중인 아이템인지 확인
    if (itemStore.isEditing(item.itemId)) {
      // 충돌 감지: 다른 사용자가 같은 아이템을 수정함
      const localData = itemStore.getEditingData()
      if (localData) {
        // 충돌 정보 설정
        sseStore.setConflict({
          itemId: item.itemId,
          localData,
          serverData: item,
          updatedBy: item.updatedByName || '다른 사용자',
          updatedAt: item.updatedAt || new Date().toISOString()
        })
        // 충돌 알림
        toast.warning('다른 사용자가 이 아이템을 수정했습니다. 충돌 해결이 필요합니다.')
        return // 서버 데이터로 자동 덮어쓰지 않음
      }
    }

    // 편집 중이 아닌 경우 정상 업데이트
    itemStore.handleSseItemUpdated(item)
  }

  function handleItemDeleted(data: SseEventData) {
    const item = data.data as Item
    sseStore.setLastEventTime(new Date())

    if (item.status === 'DELETED') {
      // 소프트 삭제인 경우 상태만 업데이트 (내부 함수 사용, API 호출 없음)
      itemStore.handleSseItemUpdated(item)
    } else {
      // 하드 삭제인 경우 목록에서 제거
      itemStore.handleSseItemDeleted(item.itemId)
    }
  }

  function handlePropertyUpdated(data: SseEventData) {
    const property = data.data as PropertyDef & { deleted?: boolean }
    sseStore.setLastEventTime(new Date())

    if (property.deleted) {
      propertyStore.removePropertyDefinition(property.propertyId)
    } else {
      const existing = propertyStore.getPropertyById(property.propertyId)
      if (existing) {
        propertyStore.updatePropertyDefinition(property.propertyId, property)
      } else {
        propertyStore.addPropertyDefinition(property)
      }
    }
  }

  function handleCommentCreated(data: SseEventData) {
    const comment = data.data as Comment
    sseStore.setLastEventTime(new Date())

    // 현재 선택된 아이템의 댓글인 경우 알림
    if (comment.itemId === itemStore.selectedItemId) {
      toast.info('새 댓글이 등록되었습니다.')
    }

    // 댓글 수 증가 (SSE용 로컬 업데이트 - API 호출 없음)
    const item = itemStore.getItemById(comment.itemId)
    if (item) {
      itemStore.updateItemLocal(comment.itemId, {
        commentCount: (item.commentCount || 0) + 1
      })
    }
  }

  function scheduleReconnect(boardId?: number) {
    if (reconnectTimeout) {
      clearTimeout(reconnectTimeout)
    }

    if (sseStore.reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      console.error('[SSE] Max reconnect attempts reached')
      toast.error('실시간 연결이 끊어졌습니다. 페이지를 새로고침해주세요.')
      return
    }

    sseStore.incrementReconnectAttempts()
    console.log(`[SSE] Reconnecting in ${RECONNECT_DELAY}ms... (attempt ${sseStore.reconnectAttempts})`)

    reconnectTimeout = setTimeout(() => {
      connect(boardId)
    }, RECONNECT_DELAY)
  }

  function disconnect() {
    if (reconnectTimeout) {
      clearTimeout(reconnectTimeout)
      reconnectTimeout = null
    }
    sseStore.disconnect()
    console.log('[SSE] Disconnected')
  }

  function subscribeToBoardId(boardId: number) {
    // 다른 보드로 변경하는 경우 재연결
    if (sseStore.subscribedBoardId !== boardId) {
      disconnect()
      connect(boardId)
    }
  }

  // 자동 연결/해제 (컴포넌트 라이프사이클)
  function useAutoConnect(boardId?: number) {
    onMounted(() => {
      connect(boardId)
    })

    onUnmounted(() => {
      // 재연결 타임아웃 명시적 정리
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout)
        reconnectTimeout = null
      }
      disconnect()
    })
  }

  // ==================== 충돌 해결 메서드 ====================

  /**
   * 내 변경사항 유지 - 서버 데이터 무시하고 로컬 데이터 저장
   */
  async function resolveKeepLocal(boardId: number): Promise<boolean> {
    const conflict = sseStore.conflictInfo
    if (!conflict) return false

    const { itemId, localData } = conflict

    try {
      // 로컬 데이터를 서버에 저장
      const response = await itemStore.updateItem(boardId, itemId, localData as ItemUpdateRequest)
      if (response) {
        sseStore.clearConflict()
        toast.success('내 변경사항이 저장되었습니다.')
        return true
      }
      return false
    } catch (e) {
      toast.error('변경사항 저장에 실패했습니다.')
      return false
    }
  }

  /**
   * 서버 버전으로 덮어쓰기 - 로컬 변경 폐기
   */
  function resolveUseServer(): void {
    const conflict = sseStore.conflictInfo
    if (!conflict) return

    const { itemId, serverData } = conflict

    // 서버 데이터로 업데이트
    itemStore.handleSseItemUpdated(serverData)
    // 편집 상태 종료
    itemStore.stopEditing()
    sseStore.clearConflict()
    toast.info('서버 버전으로 적용되었습니다.')
  }

  /**
   * 충돌 무시 - 편집 계속 (나중에 저장 시 충돌 가능)
   */
  function resolveIgnore(): void {
    sseStore.clearConflict()
    toast.info('충돌을 무시하고 편집을 계속합니다.')
  }

  return {
    // State (from store)
    status: sseStore.status,
    isConnected: sseStore.isConnected,
    isConnecting: sseStore.isConnecting,
    hasError: sseStore.hasError,
    lastEventTime: sseStore.lastEventTime,
    hasConflict: sseStore.hasConflict,
    conflictInfo: sseStore.conflictInfo,
    // Methods
    connect,
    disconnect,
    subscribeToBoardId,
    useAutoConnect,
    // Conflict Resolution
    resolveKeepLocal,
    resolveUseServer,
    resolveIgnore
  }
}
