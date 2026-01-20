import { ref, computed, watch, type Ref } from 'vue'
import { assignmentApi } from '@/api/assignment'
import type { ItemAccessInfo, PermissionLevel } from '@/types/assignment'
import { basicPropertyCodes } from '@/types/assignment'

/**
 * 업무 권한 관리 Composable
 */
export function useItemPermission(boardId: Ref<number | null>, itemId: Ref<number | null>) {
  // State
  const accessInfo = ref<ItemAccessInfo | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Computed
  const isOwner = computed(() => accessInfo.value?.isOwner ?? false)

  const shareType = computed(() => accessInfo.value?.shareType ?? null)

  const permissionLevel = computed(() => accessInfo.value?.permissionLevel ?? null)

  const canEditTitle = computed(() => accessInfo.value?.canEditTitle ?? false)

  const canEditBasicProperties = computed(() => accessInfo.value?.canEditBasicProperties ?? false)

  const canEditUserProperties = computed(() => accessInfo.value?.canEditUserProperties ?? false)

  const canAssign = computed(() => accessInfo.value?.canAssign ?? false)

  const canDelete = computed(() => accessInfo.value?.canDelete ?? false)

  const canComplete = computed(() => accessInfo.value?.canComplete ?? false)

  const isAssigned = computed(() => shareType.value === 'ASSIGN')

  const isShared = computed(() => shareType.value === 'SHARE')

  const assignedBy = computed(() => accessInfo.value?.assignedBy ?? null)

  const assignedByName = computed(() => accessInfo.value?.assignedByName ?? null)

  // Methods
  /**
   * 접근 권한 정보 조회
   */
  async function fetchAccessInfo() {
    if (!boardId.value || !itemId.value) {
      accessInfo.value = null
      return
    }

    loading.value = true
    error.value = null

    try {
      const response = await assignmentApi.getAccessInfo(boardId.value, itemId.value)
      if (response.success && response.data) {
        accessInfo.value = response.data
      }
    } catch (err) {
      // API 실패 시 조용히 처리 (fallback 로직 사용)
      // console.error('접근 권한 정보 조회 실패:', err)
      error.value = null  // 에러 표시 안 함
    } finally {
      loading.value = false
    }
  }

  /**
   * 특정 속성 편집 가능 여부 확인
   */
  function canEditProperty(propertyCode: string): boolean {
    if (!accessInfo.value) return false
    if (isOwner.value) return true

    if (propertyCode === 'title') return canEditTitle.value
    if (basicPropertyCodes.includes(propertyCode)) return canEditBasicProperties.value
    return canEditUserProperties.value
  }

  /**
   * 권한 수준 라벨 반환
   */
  function getPermissionLabel(): string {
    if (isOwner.value) return '소유자'

    const labels: Record<PermissionLevel, string> = {
      VIEW: '조회',
      EDIT: '편집',
      FULL: '전체'
    }

    return permissionLevel.value ? labels[permissionLevel.value] : ''
  }

  /**
   * 수동으로 접근 정보 설정 (API 호출 없이)
   */
  function setAccessInfo(info: ItemAccessInfo | null) {
    accessInfo.value = info
  }

  /**
   * 소유자 권한으로 설정
   */
  function setAsOwner() {
    accessInfo.value = {
      isOwner: true,
      shareType: null,
      permissionLevel: null,
      assignedBy: null,
      assignedByName: null,
      assignedAt: null,
      canEditTitle: true,
      canEditBasicProperties: true,
      canEditUserProperties: true,
      canAssign: true,
      canDelete: true,
      canComplete: true
    }
  }

  // itemId 변경 시 자동으로 권한 정보 갱신
  watch([boardId, itemId], () => {
    if (boardId.value && itemId.value) {
      fetchAccessInfo()
    } else {
      accessInfo.value = null
    }
  }, { immediate: true })

  return {
    // State
    accessInfo,
    loading,
    error,

    // Computed
    isOwner,
    shareType,
    permissionLevel,
    canEditTitle,
    canEditBasicProperties,
    canEditUserProperties,
    canAssign,
    canDelete,
    canComplete,
    isAssigned,
    isShared,
    assignedBy,
    assignedByName,

    // Methods
    fetchAccessInfo,
    canEditProperty,
    getPermissionLabel,
    setAccessInfo,
    setAsOwner
  }
}
