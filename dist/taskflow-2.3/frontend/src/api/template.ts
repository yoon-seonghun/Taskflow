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
  },

  // =============================================
  // 소유 유형별 조회
  // =============================================

  /**
   * 글로벌 템플릿 목록 조회
   */
  getGlobalTemplates() {
    return get<TaskTemplate[]>('/task-templates/global')
  },

  /**
   * 매니저 템플릿 목록 조회
   */
  getManagerTemplates() {
    return get<TaskTemplate[]>('/task-templates/manager')
  },

  /**
   * 개인 템플릿 목록 조회
   */
  getUserTemplates() {
    return get<TaskTemplate[]>('/task-templates/user')
  },

  /**
   * 접근 가능한 전체 템플릿 목록 조회
   */
  getAccessibleTemplates() {
    return get<TaskTemplate[]>('/task-templates/accessible')
  },

  // =============================================
  // 소유 유형별 등록
  // =============================================

  /**
   * 글로벌 템플릿 등록
   */
  createGlobalTemplate(data: TaskTemplateCreateRequest) {
    return post<TaskTemplate>('/task-templates/global', data)
  },

  /**
   * 매니저 템플릿 등록
   */
  createManagerTemplate(data: TaskTemplateCreateRequest) {
    return post<TaskTemplate>('/task-templates/manager', data)
  },

  /**
   * 개인 템플릿 등록
   */
  createUserTemplate(data: TaskTemplateCreateRequest) {
    return post<TaskTemplate>('/task-templates/user', data)
  }
}
