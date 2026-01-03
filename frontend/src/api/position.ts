import { get, post, put, del } from './client'
import type {
  Position,
  PositionCreateRequest,
  PositionUpdateRequest,
  PositionOrderRequest
} from '@/types/position'

export const positionApi = {
  // 직급 목록 조회
  getPositions(params?: { useYn?: string }) {
    return get<Position[]>('/positions', params)
  },

  // 직급 상세 조회
  getPosition(positionCode: string) {
    return get<Position>(`/positions/${positionCode}`)
  },

  // 직급 생성
  createPosition(data: PositionCreateRequest) {
    return post<Position>('/positions', data)
  },

  // 직급 수정
  updatePosition(positionCode: string, data: PositionUpdateRequest) {
    return put<Position>(`/positions/${positionCode}`, data)
  },

  // 직급 삭제
  deletePosition(positionCode: string) {
    return del<void>(`/positions/${positionCode}`)
  },

  // 직급 순서 변경
  updatePositionOrder(positionCode: string, sortOrder: number) {
    return put<void>(`/positions/${positionCode}/order`, { sortOrder })
  },

  // 직급 순서 일괄 변경
  updatePositionOrders(orders: PositionOrderRequest[]) {
    return put<void>('/positions/orders', orders)
  }
}
