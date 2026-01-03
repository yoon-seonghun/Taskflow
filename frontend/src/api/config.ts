import { get } from './client'

export interface SystemConfig {
  userManagementMode: 'internal' | 'external'
  userCrudEnabled: boolean
  departmentCrudEnabled: boolean
  passwordChangeEnabled: boolean
  positionCrudEnabled: boolean
  headManagementEnabled: boolean
}

export const configApi = {
  /**
   * 시스템 설정 조회
   * - 사용자 관리 모드 (internal/external)
   * - CRUD 활성화 여부
   */
  getSystemConfig() {
    return get<SystemConfig>('/config/system')
  }
}
