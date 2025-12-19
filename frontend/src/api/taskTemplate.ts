/**
 * @deprecated 이 파일은 더 이상 사용되지 않습니다.
 * 대신 '@/api/template'의 templateApi를 사용하세요.
 *
 * 마이그레이션 가이드:
 * - import { taskTemplateApi } from '@/api/taskTemplate'
 *   → import { templateApi } from '@/api/template'
 * - 타입: '@/types/taskTemplate' → '@/types/template'
 */

// Re-export from template.ts for backward compatibility
export { templateApi as taskTemplateApi } from './template'
