/**
 * 직급 타입 정의
 */

export interface Position {
  positionId: number
  positionCode: string
  positionName: string
  sortOrder: number
  useYn: string
  lastSyncedAt?: string
  createdAt?: string
  createdBy?: string
  updatedAt?: string
  updatedBy?: string
}

export interface PositionCreateRequest {
  positionCode: string
  positionName: string
  sortOrder?: number
}

export interface PositionUpdateRequest {
  positionName?: string
  sortOrder?: number
  useYn?: string
}

export interface PositionOrderRequest {
  positionCode: string
  sortOrder: number
}
