/**
 * SSE 이벤트 타입 정의
 */

export type SseEventType =
  | 'item:created'
  | 'item:updated'
  | 'item:deleted'
  | 'property:updated'
  | 'comment:created'
  | 'connection'
  | 'heartbeat'

export interface SseEvent<T = unknown> {
  type: SseEventType
  boardId?: number
  data: T
  timestamp: string
  triggeredBy?: number
}

export interface SseStatus {
  connected: boolean
  totalConnections: number
}

export interface SseConnectionState {
  connected: boolean
  reconnecting: boolean
  subscribedBoards: Set<number>
  lastEventTime?: Date
}

// SSE 이벤트 데이터 (MessageEvent.data를 파싱한 결과)
export interface SseEventData<T = unknown> {
  data: T
  triggeredBy?: number
  triggeredByName?: string
  timestamp?: string
}
