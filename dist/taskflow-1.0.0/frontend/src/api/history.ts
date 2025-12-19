import { get } from './client'
import type {
  ItemHistory,
  ItemHistorySearchRequest,
  TemplateHistory,
  TemplateHistorySearchRequest
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
  }
}
