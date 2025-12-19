import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { groupApi } from '@/api/group'
import type {
  Group,
  GroupCreateRequest,
  GroupUpdateRequest,
  GroupMember
} from '@/types/group'

export const useGroupStore = defineStore('group', () => {
  // ==================== State ====================
  const groups = ref<Group[]>([])
  const selectedGroup = ref<Group | null>(null)
  const groupMembers = ref<GroupMember[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // ==================== Getters ====================
  // 활성 그룹만
  const activeGroups = computed(() => {
    return groups.value.filter(g => g.useYn === 'Y')
  })

  // 선택된 그룹 ID
  const selectedGroupId = computed(() => selectedGroup.value?.groupId || null)

  // ==================== Actions ====================

  /**
   * 그룹 목록 조회
   */
  async function fetchGroups(params?: { useYn?: string }): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await groupApi.getGroups(params)
      if (response.success && response.data) {
        groups.value = response.data
        return true
      }
      error.value = response.message || '그룹 목록을 불러오는데 실패했습니다.'
      return false
    } catch (e) {
      error.value = '그룹 목록을 불러오는데 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 그룹 생성
   */
  async function createGroup(data: GroupCreateRequest): Promise<Group | null> {
    loading.value = true
    error.value = null

    try {
      const response = await groupApi.createGroup(data)
      if (response.success && response.data) {
        // 목록 새로고침
        await fetchGroups()
        return response.data
      }
      error.value = response.message || '그룹 생성에 실패했습니다.'
      return null
    } catch (e) {
      error.value = '그룹 생성에 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 그룹 수정
   */
  async function updateGroup(groupId: number, data: GroupUpdateRequest): Promise<boolean> {
    error.value = null

    try {
      const response = await groupApi.updateGroup(groupId, data)
      if (response.success && response.data) {
        // 목록에서 해당 그룹 업데이트
        const index = groups.value.findIndex(g => g.groupId === groupId)
        if (index !== -1) {
          groups.value[index] = response.data
        }
        // 선택된 그룹 업데이트
        if (selectedGroup.value?.groupId === groupId) {
          selectedGroup.value = response.data
        }
        return true
      }
      error.value = response.message || '그룹 수정에 실패했습니다.'
      return false
    } catch (e) {
      error.value = '그룹 수정에 실패했습니다.'
      return false
    }
  }

  /**
   * 그룹 삭제
   */
  async function deleteGroup(groupId: number): Promise<boolean> {
    error.value = null

    try {
      const response = await groupApi.deleteGroup(groupId)
      if (response.success) {
        // 목록에서 제거
        groups.value = groups.value.filter(g => g.groupId !== groupId)
        // 선택 해제
        if (selectedGroup.value?.groupId === groupId) {
          selectedGroup.value = null
          groupMembers.value = []
        }
        return true
      }
      error.value = response.message || '그룹 삭제에 실패했습니다.'
      return false
    } catch (e) {
      error.value = '그룹 삭제에 실패했습니다.'
      return false
    }
  }

  /**
   * 그룹 순서 변경
   */
  async function updateGroupOrder(groupId: number, sortOrder: number): Promise<boolean> {
    try {
      const response = await groupApi.updateGroupOrder(groupId, sortOrder)
      if (response.success) {
        await fetchGroups()
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 그룹 멤버 목록 조회
   */
  async function fetchGroupMembers(groupId: number): Promise<boolean> {
    try {
      const response = await groupApi.getGroupMembers(groupId)
      if (response.success && response.data) {
        groupMembers.value = response.data
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 그룹 멤버 추가
   */
  async function addGroupMember(groupId: number, userId: number): Promise<boolean> {
    error.value = null

    try {
      const response = await groupApi.addGroupMember(groupId, { userId })
      if (response.success) {
        // 멤버 목록 새로고침
        await fetchGroupMembers(groupId)
        // 그룹 목록도 새로고침 (멤버 수 갱신)
        await fetchGroups()
        return true
      }
      error.value = response.message || '멤버 추가에 실패했습니다.'
      return false
    } catch (e) {
      error.value = '멤버 추가에 실패했습니다.'
      return false
    }
  }

  /**
   * 그룹 멤버 제거
   */
  async function removeGroupMember(groupId: number, userId: number): Promise<boolean> {
    error.value = null

    try {
      const response = await groupApi.removeGroupMember(groupId, userId)
      if (response.success) {
        // 멤버 목록에서 제거
        groupMembers.value = groupMembers.value.filter(m => m.userId !== userId)
        // 그룹 목록도 새로고침 (멤버 수 갱신)
        await fetchGroups()
        return true
      }
      error.value = response.message || '멤버 제거에 실패했습니다.'
      return false
    } catch (e) {
      error.value = '멤버 제거에 실패했습니다.'
      return false
    }
  }

  // ==================== Selection Functions ====================
  function selectGroup(group: Group | null) {
    selectedGroup.value = group
    if (group) {
      fetchGroupMembers(group.groupId)
    } else {
      groupMembers.value = []
    }
  }

  function getGroupById(groupId: number): Group | undefined {
    return groups.value.find(g => g.groupId === groupId)
  }

  function clearSelection() {
    selectedGroup.value = null
    groupMembers.value = []
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    groups,
    selectedGroup,
    groupMembers,
    loading,
    error,
    // Getters
    activeGroups,
    selectedGroupId,
    // Actions
    fetchGroups,
    createGroup,
    updateGroup,
    deleteGroup,
    updateGroupOrder,
    fetchGroupMembers,
    addGroupMember,
    removeGroupMember,
    // Selection
    selectGroup,
    getGroupById,
    clearSelection,
    clearError
  }
})
