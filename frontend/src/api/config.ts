import { get, put, post } from './client'

export interface SystemConfig {
  userManagementMode: 'internal' | 'external'
  userCrudEnabled: boolean
  departmentCrudEnabled: boolean
  passwordChangeEnabled: boolean
  positionCrudEnabled: boolean
  headManagementEnabled: boolean
}

export interface SmtpConfigItem {
  key: string
  value: string
  description: string
  encrypted: boolean
}

export interface SmtpConfigResponse {
  group: string
  configs: SmtpConfigItem[]
}

export interface SmtpConfigUpdateRequest {
  configs: { key: string; value: string }[]
}

export interface SmtpTestResult {
  success: boolean
  message: string
  emailLogId?: number
  status?: string
}

export const configApi = {
  /**
   * 시스템 설정 조회
   * - 사용자 관리 모드 (internal/external)
   * - CRUD 활성화 여부
   */
  getSystemConfig() {
    return get<SystemConfig>('/config/system')
  },

  /**
   * SMTP 설정 조회
   */
  getSmtpConfig() {
    return get<SmtpConfigResponse>('/config/smtp')
  },

  /**
   * SMTP 설정 업데이트
   */
  updateSmtpConfig(configs: SmtpConfigUpdateRequest) {
    return put<void>('/config/smtp', configs)
  },

  /**
   * SMTP 비밀번호 업데이트
   */
  updateSmtpPassword(password: string) {
    return put<void>('/config/smtp/password', { password })
  },

  /**
   * SMTP 연결 테스트
   */
  testSmtpConnection() {
    return post<SmtpTestResult>('/config/smtp/test-connection')
  },

  /**
   * 테스트 메일 발송
   */
  sendTestEmail(testEmail: string) {
    return post<SmtpTestResult>('/config/smtp/test-send', { testEmail })
  }
}
