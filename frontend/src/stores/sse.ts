import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { SseEventType } from '@/types/sse'
import type { Item } from '@/types/item'

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'error'

export interface SseState {
  status: ConnectionStatus
  lastEventTime: Date | null
  reconnectAttempts: number
  subscribedBoardId: number | null
}

// 충돌 정보 인터페이스
export interface ConflictInfo {
  itemId: number
  localData: Partial<Item>
  serverData: Item
  updatedBy: string
  updatedAt: string
}

export const useSseStore = defineStore('sse', () => {
  const status = ref<ConnectionStatus>('disconnected')
  const lastEventTime = ref<Date | null>(null)
  const reconnectAttempts = ref(0)
  const subscribedBoardId = ref<number | null>(null)
  const eventSource = ref<EventSource | null>(null)

  // 충돌 상태
  const hasConflict = ref(false)
  const conflictInfo = ref<ConflictInfo | null>(null)

  const isConnected = computed(() => status.value === 'connected')
  const isConnecting = computed(() => status.value === 'connecting')
  const hasError = computed(() => status.value === 'error')

  function setStatus(newStatus: ConnectionStatus) {
    status.value = newStatus
  }

  function setLastEventTime(time: Date) {
    lastEventTime.value = time
  }

  function incrementReconnectAttempts() {
    reconnectAttempts.value++
  }

  function resetReconnectAttempts() {
    reconnectAttempts.value = 0
  }

  function setSubscribedBoardId(boardId: number | null) {
    subscribedBoardId.value = boardId
  }

  function setEventSource(source: EventSource | null) {
    eventSource.value = source
  }

  function disconnect() {
    if (eventSource.value) {
      eventSource.value.close()
      eventSource.value = null
    }
    status.value = 'disconnected'
    subscribedBoardId.value = null
  }

  function reset() {
    disconnect()
    lastEventTime.value = null
    reconnectAttempts.value = 0
    clearConflict()
  }

  // 충돌 설정
  function setConflict(info: ConflictInfo) {
    conflictInfo.value = info
    hasConflict.value = true
  }

  // 충돌 해제
  function clearConflict() {
    conflictInfo.value = null
    hasConflict.value = false
  }

  return {
    status,
    lastEventTime,
    reconnectAttempts,
    subscribedBoardId,
    eventSource,
    hasConflict,
    conflictInfo,
    isConnected,
    isConnecting,
    hasError,
    setStatus,
    setLastEventTime,
    incrementReconnectAttempts,
    resetReconnectAttempts,
    setSubscribedBoardId,
    setEventSource,
    disconnect,
    reset,
    setConflict,
    clearConflict
  }
})
