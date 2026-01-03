import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { configApi, type SystemConfig } from '@/api/config'

/**
 * 시스템 설정 스토어
 *
 * 사용자 관리 모드에 따른 UI 조건부 렌더링 지원
 * - internal 모드: 사용자/부서 CRUD 가능
 * - external 모드: 조회만 가능
 */
export const useConfigStore = defineStore('config', () => {
  // ==================== State ====================
  const config = ref<SystemConfig | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // ==================== Getters ====================

  /**
   * 사용자 관리 모드
   */
  const userManagementMode = computed(() => config.value?.userManagementMode || 'internal')

  /**
   * Internal 모드 여부
   */
  const isInternalMode = computed(() => userManagementMode.value === 'internal')

  /**
   * External 모드 여부
   */
  const isExternalMode = computed(() => userManagementMode.value === 'external')

  /**
   * 사용자 CRUD 활성화 여부
   */
  const userCrudEnabled = computed(() => config.value?.userCrudEnabled ?? true)

  /**
   * 부서 CRUD 활성화 여부
   */
  const departmentCrudEnabled = computed(() => config.value?.departmentCrudEnabled ?? true)

  /**
   * 비밀번호 변경 가능 여부
   */
  const passwordChangeEnabled = computed(() => config.value?.passwordChangeEnabled ?? true)

  /**
   * 직급 CRUD 활성화 여부
   */
  const positionCrudEnabled = computed(() => config.value?.positionCrudEnabled ?? true)

  /**
   * 팀장 지정/해제 가능 여부
   */
  const headManagementEnabled = computed(() => config.value?.headManagementEnabled ?? true)

  // ==================== Actions ====================

  /**
   * 시스템 설정 로드
   */
  async function fetchConfig(): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await configApi.getSystemConfig()
      if (response.success && response.data) {
        config.value = response.data
        return true
      }
      error.value = response.message || '설정을 불러오는데 실패했습니다.'
      return false
    } catch (e) {
      error.value = '설정을 불러오는데 실패했습니다.'
      // 기본값으로 fallback (internal 모드)
      config.value = {
        userManagementMode: 'internal',
        userCrudEnabled: true,
        departmentCrudEnabled: true,
        passwordChangeEnabled: true,
        positionCrudEnabled: true,
        headManagementEnabled: true
      }
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 설정 초기화
   */
  function clearConfig() {
    config.value = null
    error.value = null
  }

  return {
    // State
    config,
    loading,
    error,
    // Getters
    userManagementMode,
    isInternalMode,
    isExternalMode,
    userCrudEnabled,
    departmentCrudEnabled,
    passwordChangeEnabled,
    positionCrudEnabled,
    headManagementEnabled,
    // Actions
    fetchConfig,
    clearConfig
  }
})
