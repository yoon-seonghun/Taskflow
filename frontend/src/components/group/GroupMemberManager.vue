<script setup lang="ts">
/**
 * 그룹 멤버 관리 컴포넌트
 * - 멤버 목록 표시
 * - 부서 트리 + 이름 검색으로 사용자 선택 (UserSearchSelector)
 * - 멤버 추가/제거
 */
import { ref, computed } from 'vue'
import type { Group, GroupMember } from '@/types/group'
import type { User } from '@/types/user'
import { useGroupStore } from '@/stores/group'
import { useUiStore } from '@/stores/ui'
import UserSearchSelector from '@/components/common/UserSearchSelector.vue'

interface Props {
  group: Group
}

const props = defineProps<Props>()

const groupStore = useGroupStore()
const uiStore = useUiStore()

// 선택된 사용자
const selectedUser = ref<User | null>(null)

// 추가 중 상태
const isAdding = ref(false)

// 멤버 목록
const members = computed(() => groupStore.groupMembers)

// 이미 멤버인 사용자 username 목록 (UserSearchSelector 제외 목록용)
const memberUsernames = computed(() => {
  return members.value.map(m => m.username)
})

// 사용자 선택 핸들러
function handleUserSelect(user: User | null) {
  selectedUser.value = user
}

// 멤버 추가
async function handleAddMember() {
  if (!selectedUser.value) return

  isAdding.value = true
  const user = selectedUser.value

  try {
    const success = await groupStore.addGroupMember(props.group.groupId, user.username)
    if (success) {
      uiStore.showSuccess(`${user.userName || user.name}님이 그룹에 추가되었습니다.`)
      // 선택 초기화
      selectedUser.value = null
    } else {
      uiStore.showError(groupStore.error || '멤버 추가에 실패했습니다.')
    }
  } catch (error) {
    console.error('Failed to add member:', error)
    uiStore.showError('멤버 추가에 실패했습니다.')
  } finally {
    isAdding.value = false
  }
}

// 멤버 제거
async function handleRemoveMember(member: GroupMember) {
  const confirmed = await uiStore.confirm({
    title: '멤버 제거',
    message: `'${member.userName}'님을 그룹에서 제거하시겠습니까?`,
    confirmText: '제거',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  try {
    const success = await groupStore.removeGroupMember(props.group.groupId, member.username)
    if (success) {
      uiStore.showSuccess(`${member.userName}님이 그룹에서 제거되었습니다.`)
    } else {
      uiStore.showError(groupStore.error || '멤버 제거에 실패했습니다.')
    }
  } catch (error) {
    console.error('Failed to remove member:', error)
    uiStore.showError('멤버 제거에 실패했습니다.')
  }
}

// 날짜 포맷
function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}
</script>

<template>
  <div class="member-manager">
    <!-- 헤더 -->
    <div class="manager-header">
      <div class="header-info">
        <div class="group-badge" :style="{ backgroundColor: group.groupColor || '#3B82F6' }">
          {{ group.groupName.charAt(0) }}
        </div>
        <div>
          <h3 class="group-name">{{ group.groupName }}</h3>
          <p class="group-code">{{ group.groupCode }}</p>
        </div>
      </div>
      <span class="member-count">{{ members.length }}명</span>
    </div>

    <!-- 멤버 추가 섹션 -->
    <div class="add-section">
      <h4 class="section-title">멤버 추가</h4>

      <!-- UserSearchSelector 컴포넌트 -->
      <UserSearchSelector
        :exclude-usernames="memberUsernames"
        placeholder="그룹에 추가할 사용자를 선택하세요"
        @select="handleUserSelect"
      />

      <!-- 추가 버튼 -->
      <div class="add-button-area" v-if="selectedUser">
        <button
          type="button"
          class="add-button"
          :disabled="isAdding"
          @click="handleAddMember"
        >
          <svg v-if="isAdding" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          <span>{{ isAdding ? '추가 중...' : '그룹에 추가' }}</span>
        </button>
      </div>
    </div>

    <!-- 멤버 목록 -->
    <div class="members-section">
      <h4 class="section-title">
        현재 멤버
        <span class="member-badge">{{ members.length }}명</span>
      </h4>

      <div v-if="members.length === 0" class="empty-members">
        <svg class="w-10 h-10 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
        <p class="mt-2">그룹에 멤버가 없습니다.</p>
        <p class="text-xs">위에서 사용자를 검색하여 추가하세요.</p>
      </div>

      <div v-else class="members-list">
        <div
          v-for="member in members"
          :key="member.userId"
          class="member-item"
        >
          <div class="member-avatar">{{ member.userName.charAt(0) }}</div>
          <div class="member-info">
            <span class="member-name">{{ member.userName }}</span>
            <span v-if="member.departmentName" class="member-dept">{{ member.departmentName }}</span>
          </div>
          <span class="joined-date">{{ formatDate(member.joinedAt) }} 추가</span>
          <button
            type="button"
            class="remove-btn"
            title="멤버 제거"
            @click="handleRemoveMember(member)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.member-manager {
  @apply bg-white rounded-lg border border-gray-200;
}

.manager-header {
  @apply flex items-center justify-between p-4 border-b border-gray-200;
}

.header-info {
  @apply flex items-center gap-3;
}

.group-badge {
  @apply w-10 h-10 rounded-lg flex items-center justify-center
         text-white font-medium text-lg;
}

.group-name {
  @apply text-base font-medium text-gray-900;
}

.group-code {
  @apply text-xs text-gray-500;
}

.member-count {
  @apply px-2 py-1 text-sm font-medium text-gray-600 bg-gray-100 rounded;
}

.add-section {
  @apply p-4 border-b border-gray-200;
}

.section-title {
  @apply text-sm font-medium text-gray-900 mb-3 flex items-center gap-2;
}

.member-badge {
  @apply px-1.5 py-0.5 text-xs font-normal text-gray-500 bg-gray-100 rounded;
}

.add-button-area {
  @apply mt-4 flex justify-end;
}

.add-button {
  @apply inline-flex items-center gap-2 px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500
         disabled:opacity-50 disabled:cursor-not-allowed transition-colors;
}

.members-section {
  @apply p-4;
}

.empty-members {
  @apply text-center py-8 text-sm text-gray-500;
}

.members-list {
  @apply space-y-2 max-h-[300px] overflow-y-auto;
}

.member-item {
  @apply flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50;
}

.member-avatar {
  @apply w-9 h-9 rounded-full bg-primary-100 flex items-center justify-center
         text-sm font-medium text-primary-600 flex-shrink-0;
}

.member-info {
  @apply flex-1 min-w-0;
}

.member-name {
  @apply block text-sm font-medium text-gray-900 truncate;
}

.member-dept {
  @apply block text-xs text-gray-500;
}

.joined-date {
  @apply text-xs text-gray-400 flex-shrink-0;
}

.remove-btn {
  @apply p-1.5 rounded hover:bg-red-50 text-gray-400 hover:text-red-600
         opacity-0 transition-all duration-150;
}

.member-item:hover .remove-btn {
  @apply opacity-100;
}
</style>
