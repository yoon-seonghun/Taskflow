import { get } from './client'
import type {
  ItemHistory,
  ItemHistorySearchRequest,
  TemplateHistory,
  TemplateHistorySearchRequest,
  AuditLog,
  AuditLogSearchRequest
} from '@/types/history'
import type { PageResponse } from '@/types/api'

export const historyApi = {
  // 작업 처리 이력
  getItemHistory(params?: ItemHistorySearchRequest) {
    return get<PageResponse<ItemHistory>>('/history/items', params)
  },

  // 작업 등록 이력 (템플릿)
  getTemplateHistory(params?: TemplateHistorySearchRequest) {
    return get<PageResponse<TemplateHistory>>('/history/templates', params)
  },

  // 관리 이력 (감사 로그)
  getManagementHistory(params?: AuditLogSearchRequest) {
    return get<PageResponse<AuditLog>>('/audit-logs', params)
  },

  // 최근 관리 이력
  getRecentManagementHistory(limit: number = 20) {
    return get<AuditLog[]>('/audit-logs/recent', { limit })
  }
}
