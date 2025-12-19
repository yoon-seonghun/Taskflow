/**
 * @deprecated 이 파일은 더 이상 사용되지 않습니다.
 * 대신 '@/types/template'를 사용하세요.
 *
 * 마이그레이션:
 * import type { ... } from '@/types/taskTemplate'
 * → import type { ... } from '@/types/template'
 */

// Re-export from template.ts for backward compatibility
export type {
  TaskTemplate,
  TaskTemplateCreateRequest,
  TaskTemplateUpdateRequest,
  TaskTemplateSearchResult
} from './template'

// 검색 요청 파라미터 타입 (template.ts 시그니처와 호환)
export interface TaskTemplateSearchRequest {
  keyword?: string
  limit?: number
}
