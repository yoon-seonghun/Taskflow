<script setup lang="ts">
/**
 * 그룹 관리 메뉴 (Groups View)
 * - 그룹 카드 목록 표시
 * - 그룹 CRUD
 * - 그룹 멤버 관리
 */
import { ref, computed, onMounted } from 'vue'
import GroupList from '@/components/group/GroupList.vue'
import GroupForm from '@/components/group/GroupForm.vue'
import GroupMemberManager from '@/components/group/GroupMemberManager.vue'
import type { Group, GroupCreateRequest, GroupUpdateRequest } from '@/types/group'
import { useGroupStore } from '@/stores/group'
import { useUiStore } from '@/stores/ui'

const groupStore = useGroupStore()
const uiStore = useUiStore()

// 폼 상태
type FormMode = 'none' | 'create' | 'edit'
const formMode = ref<FormMode>('none')
const formLoading = ref(false)

// 편집 대상 그룹
const editingGroup = ref<Group | null>(null)

// 비활성 그룹 표시 여부
const showInactive = ref(true)

// 선택된 그룹
const selectedGroup = computed(() => groupStore.selectedGroup)

// Form ref
const formRef = ref<InstanceType<typeof GroupForm> | null>(null)

// 초기 로드
async function loadData() {
  await groupStore.fetchGroups()
}

// 그룹 선택
function handleSelect(group: Group) {
  // 폼이 열려있으면 닫기
  if (formMode.value !== 'none') {
    formMode.value = 'none'
    editingGroup.value = null
  }
}

// 그룹 수정 시작
function handleEdit(group: Group) {
  formMode.value = 'edit'
  editingGroup.value = group
  // 선택 해제
  groupStore.clearSelection()
}

// 그룹 추가 시작
function handleAdd() {
  formMode.value = 'create'
  editingGroup.value = null
  // 선택 해제
  groupStore.clearSelection()
}

// 활성/비활성 토글
async function handleToggleActive(group: Group) {
  const newUseYn = group.useYn === 'Y' ? 'N' : 'Y'
  const actionText = newUseYn === 'Y' ? '활성화' : '비활성화'

  try {
    const success = await groupStore.updateGroup(group.groupId, {
      ...group,
      groupDescription: group.groupDescription || undefined,
      useYn: newUseYn
    })
    if (success) {
      uiStore.showSuccess(`그룹이 ${actionText}되었습니다.`)
    } else {
      uiStore.showError(groupStore.error || `그룹 ${actionText}에 실패했습니다.`)
    }
  } catch (error) {
    console.error('Failed to toggle group status:', error)
    uiStore.showError(`그룹 ${actionText}에 실패했습니다.`)
  }
}

// 그룹 삭제
async function handleDelete(group: Group) {
  // 멤버가 있는지 확인
  if (group.memberCount && group.memberCount > 0) {
    uiStore.showWarning('멤버가 있는 그룹은 삭제할 수 없습니다. 먼저 멤버를 제거해주세요.')
    return
  }

  const confirmed = await uiStore.confirm({
    title: '그룹 삭제',
    message: `'${group.groupName}' 그룹을 삭제하시겠습니까?`,
    confirmText: '삭제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  try {
    const success = await groupStore.deleteGroup(group.groupId)
    if (success) {
      uiStore.showSuccess('그룹이 삭제되었습니다.')
    } else {
      uiStore.showError(groupStore.error || '그룹 삭제에 실패했습니다.')
    }
  } catch (error) {
    console.error('Failed to delete group:', error)
    uiStore.showError('그룹 삭제에 실패했습니다.')
  }
}

// 폼 제출
async function handleFormSubmit(data: GroupCreateRequest | GroupUpdateRequest) {
  formLoading.value = true

  try {
    if (formMode.value === 'edit' && editingGroup.value) {
      // 수정
      const success = await groupStore.updateGroup(
        editingGroup.value.groupId,
        data as GroupUpdateRequest
      )
      if (success) {
        uiStore.showSuccess('그룹이 수정되었습니다.')
        handleFormCancel()
      } else {
        uiStore.showError(groupStore.error || '그룹 수정에 실패했습니다.')
      }
    } else {
      // 생성
      const result = await groupStore.createGroup(data as GroupCreateRequest)
      if (result) {
        uiStore.showSuccess('그룹이 등록되었습니다.')
        handleFormCancel()
      } else {
        uiStore.showError(groupStore.error || '그룹 등록에 실패했습니다.')
      }
    }
  } catch (error) {
    console.error('Failed to save group:', error)
    uiStore.showError('그룹 저장에 실패했습니다.')
  } finally {
    formLoading.value = false
  }
}

// 폼 취소
function handleFormCancel() {
  formMode.value = 'none'
  editingGroup.value = null
  formRef.value?.resetForm()
}

// 초기화
onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="groups-view">
    <!-- 헤더 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">그룹 관리</h1>
      <p class="mt-1 text-sm text-gray-500">
        업무 그룹을 등록하고 멤버를 관리합니다.
      </p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <!-- 왼쪽: 그룹 목록 -->
      <div class="lg:col-span-5">
        <!-- 필터 옵션 -->
        <div class="flex items-center justify-between mb-3">
          <label class="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
            <input
              v-model="showInactive"
              type="checkbox"
              class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            비활성 그룹 표시
          </label>
        </div>

        <GroupList
          :show-inactive="showInactive"
          @select="handleSelect"
          @edit="handleEdit"
          @delete="handleDelete"
          @add="handleAdd"
          @toggle-active="handleToggleActive"
        />
      </div>

      <!-- 오른쪽: 폼/멤버 관리 -->
      <div class="lg:col-span-7">
        <!-- 폼 모드 -->
        <template v-if="formMode !== 'none'">
          <GroupForm
            ref="formRef"
            :group="editingGroup"
            :loading="formLoading"
            @submit="handleFormSubmit"
            @cancel="handleFormCancel"
          />
        </template>

        <!-- 멤버 관리 모드 -->
        <template v-else-if="selectedGroup">
          <GroupMemberManager :group="selectedGroup" />
        </template>

        <!-- 선택 안내 -->
        <template v-else>
          <div class="empty-state">
            <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <p class="mt-2 text-sm text-gray-500">그룹을 선택하면 멤버를 관리할 수 있습니다.</p>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.groups-view {
  @apply max-w-7xl mx-auto;
}

.empty-state {
  @apply text-center py-12 bg-white rounded-lg border border-gray-200;
}
</style>
