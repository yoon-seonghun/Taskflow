import { get, post, put, del } from './client'
import type {
  Share,
  ShareRequest,
  ShareUpdateRequest,
  AuditLog,
  AuditLogSearchRequest,
  AuditLogPageResponse
} from '@/types/share'

// =============================================
// 업무 공유 API
// =============================================

export const itemShareApi = {
  getShares(itemId: number) {
    return get<Share[]>(`/items/${itemId}/shares`)
  },

  addShare(itemId: number, data: ShareRequest) {
    return post<void>(`/items/${itemId}/shares`, data)
  },

  updatePermission(itemId: number, username: string, data: ShareUpdateRequest) {
    return put<void>(`/items/${itemId}/shares/${username}`, data)
  },

  removeShare(itemId: number, username: string) {
    return del<void>(`/items/${itemId}/shares/${username}`)
  }
}

// =============================================
// 감사 로그 API
// =============================================

export const auditLogApi = {
  search(params: AuditLogSearchRequest) {
    return get<AuditLogPageResponse>('/audit-logs', params)
  },

  getRecent(limit?: number) {
    return get<AuditLog[]>('/audit-logs/recent', { limit: limit || 20 })
  },

  getByBoard(boardId: number) {
    return get<AuditLog[]>(`/audit-logs/boards/${boardId}`)
  },

  getByItem(itemId: number) {
    return get<AuditLog[]>(`/audit-logs/items/${itemId}`)
  }
}
