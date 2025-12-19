import { get, post, put, del } from './client'
import type {
  TaskTemplate,
  TaskTemplateCreateRequest,
  TaskTemplateUpdateRequest,
  TaskTemplateSearchResult
} from '@/types/template'

export const templateApi = {
  /**
   * 템플릿 목록 조회
   * 백엔드는 List<TaskTemplateResponse> 반환 (페이지네이션 없음)
   */
  getTemplates() {
    return get<TaskTemplate[]>('/task-templates')
  },

  getTemplate(templateId: number) {
    return get<TaskTemplate>(`/task-templates/${templateId}`)
  },

  createTemplate(data: TaskTemplateCreateRequest) {
    return post<TaskTemplate>('/task-templates', data)
  },

  updateTemplate(templateId: number, data: TaskTemplateUpdateRequest) {
    return put<TaskTemplate>(`/task-templates/${templateId}`, data)
  },

  deleteTemplate(templateId: number) {
    return del<void>(`/task-templates/${templateId}`)
  },

  /**
   * 자동완성 검색
   */
  searchTemplates(params?: { keyword?: string; limit?: number }) {
    return get<TaskTemplateSearchResult[]>('/task-templates/search', params)
  },

  /**
   * 템플릿 사용 (사용 횟수 증가)
   */
  useTemplate(templateId: number) {
    return post<void>(`/task-templates/${templateId}/use`)
  }
}
