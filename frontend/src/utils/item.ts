/**
 * 아이템 관련 유틸리티 함수
 */
import type { Item, ItemStatus } from '@/types/item'

/**
 * 아이템이 지연(Overdue) 상태인지 확인
 * - dueDate가 오늘보다 이전이고
 * - 완료/삭제 상태가 아닌 경우
 */
export function isItemOverdue(item: Item): boolean {
  if (!item.dueDate) return false

  // 완료 또는 삭제된 아이템은 지연으로 표시하지 않음
  if (item.status === 'COMPLETED' || item.status === 'DELETED') {
    return false
  }

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const dueDate = new Date(item.dueDate)
  dueDate.setHours(0, 0, 0, 0)

  return dueDate < today
}

/**
 * 지연 일수 계산
 * @returns 지연 일수 (양수), 지연이 아니면 0
 */
export function getOverdueDays(item: Item): number {
  if (!item.dueDate) return 0
  if (!isItemOverdue(item)) return 0

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const dueDate = new Date(item.dueDate)
  dueDate.setHours(0, 0, 0, 0)

  const diffTime = today.getTime() - dueDate.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  return diffDays
}

/**
 * 상태 텍스트 반환
 */
export function getStatusText(status: ItemStatus): string {
  const statusMap: Record<ItemStatus, string> = {
    NOT_STARTED: '시작전',
    IN_PROGRESS: '진행중',
    PENDING: '대기',
    COMPLETED: '완료',
    DELETED: '삭제'
  }
  return statusMap[status] || status
}

/**
 * 상태별 색상 클래스 반환
 */
export function getStatusColor(status: ItemStatus): string {
  const colorMap: Record<ItemStatus, string> = {
    NOT_STARTED: 'gray',
    IN_PROGRESS: 'blue',
    PENDING: 'yellow',
    COMPLETED: 'green',
    DELETED: 'red'
  }
  return colorMap[status] || 'gray'
}

/**
 * 상태별 배지 variant 반환
 */
export function getStatusBadgeVariant(status: ItemStatus): 'default' | 'primary' | 'success' | 'warning' | 'danger' {
  const variantMap: Record<ItemStatus, 'default' | 'primary' | 'success' | 'warning' | 'danger'> = {
    NOT_STARTED: 'default',
    IN_PROGRESS: 'primary',
    PENDING: 'warning',
    COMPLETED: 'success',
    DELETED: 'danger'
  }
  return variantMap[status] || 'default'
}

/**
 * 활성 상태인지 확인 (메인 목록에 표시할 상태)
 */
export function isActiveStatus(status: ItemStatus): boolean {
  return status !== 'COMPLETED' && status !== 'DELETED'
}

/**
 * 상태 옵션 목록 반환
 */
export function getStatusOptions(): { value: ItemStatus; label: string }[] {
  return [
    { value: 'NOT_STARTED', label: '시작전' },
    { value: 'IN_PROGRESS', label: '진행중' },
    { value: 'PENDING', label: '대기' },
    { value: 'COMPLETED', label: '완료' },
    { value: 'DELETED', label: '삭제' }
  ]
}

/**
 * 활성 상태 옵션만 반환 (완료/삭제 제외)
 */
export function getActiveStatusOptions(): { value: ItemStatus; label: string }[] {
  return [
    { value: 'NOT_STARTED', label: '시작전' },
    { value: 'IN_PROGRESS', label: '진행중' },
    { value: 'PENDING', label: '대기' }
  ]
}
