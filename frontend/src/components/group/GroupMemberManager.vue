<script setup lang="ts">
/**
 * 그룹 멤버 관리 컴포넌트
 * - 멤버 목록 표시
 * - 사용자 검색 및 멤버 추가
 * - 멤버 제거
 */
import { ref, computed, watch, onUnmounted } from 'vue'
import type { Group, GroupMember } from '@/types/group'
import type { User } from '@/types/user'
import { useGroupStore } from '@/stores/group'
import { useUiStore } from '@/stores/ui'
import { userApi } from '@/api/user'

interface Props {
  group: Group
}

const props = defineProps<Props>()

const groupStore = useGroupStore()
const uiStore = useUiStore()

// 검색 상태
const searchKeyword = ref('')
const searchResults = ref<User[]>([])
const isSearching = ref(false)
const showSearchDropdown = ref(false)

// 추가 중 상태
const addingUserId = ref<number | null>(null)

// 멤버 목록
const members = computed(() => groupStore.groupMembers)

// 이미 멤버인 사용자 ID 목록
const memberUserIds = computed(() => {
  return new Set(members.value.map(m => m.userId))
})

// 검색 결과에서 이미 멤버인 사용자 필터링
const filteredSearchResults = computed(() => {
  return searchResults.value.filter(u => !memberUserIds.value.has(u.userId))
})

// 디바운스 타이머
let searchTimeout: ReturnType<typeof setTimeout> | null = null

// 컴포넌트 언마운트 시 타이머 정리
onUnmounted(() => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
    searchTimeout = null
  }
})

// 검색 수행
async function performSearch() {
  if (searchKeyword.value.length < 2) {
    searchResults.value = []
    showSearchDropdown.value = false
    return
  }

  isSearching.value = true
  try {
    const response = await userApi.getUsers({
      keyword: searchKeyword.value,
      useYn: 'Y',
      size: 10
    })
    if (response.success && response.data) {
      searchResults.value = response.data.content || []
      showSearchDropdown.value = true
    }
  } catch (error) {
    console.error('Failed to search users:', error)
  } finally {
    isSearching.value = false
  }
}

// 검색어 변경 감지 (디바운스)
watch(searchKeyword, () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    performSearch()
  }, 300)
})

// 멤버 추가
async function handleAddMember(user: User) {
  addingUserId.value = user.userId

  try {
    const success = await groupStore.addGroupMember(props.group.groupId, user.userId)
    if (success) {
      uiStore.showSuccess(`${user.userName}님이 그룹에 추가되었습니다.`)
      // 검색 초기화
      searchKeyword.value = ''
      searchResults.value = []
      showSearchDropdown.value = false
    } else {
      uiStore.showError(groupStore.error || '멤버 추가에 실패했습니다.')
    }
  } catch (error) {
    console.error('Failed to add member:', error)
    uiStore.showError('멤버 추가에 실패했습니다.')
  } finally {
    addingUserId.value = null
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
    const success = await groupStore.removeGroupMember(props.group.groupId, member.userId)
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

// 검색 드롭다운 닫기
function closeSearchDropdown() {
  setTimeout(() => {
    showSearchDropdown.value = false
  }, 200)
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

    <!-- 멤버 검색/추가 -->
    <div class="search-section">
      <div class="search-field">
        <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="사용자 이름 또는 아이디로 검색..."
          @focus="showSearchDropdown = searchResults.length > 0"
          @blur="closeSearchDropdown"
        />
        <svg v-if="isSearching" class="animate-spin search-loading" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      </div>

      <!-- 검색 결과 드롭다운 -->
      <div v-if="showSearchDropdown && filteredSearchResults.length > 0" class="search-dropdown">
        <button
          v-for="user in filteredSearchResults"
          :key="user.userId"
          type="button"
          class="search-result-item"
          :disabled="addingUserId === user.userId"
          @mousedown.prevent="handleAddMember(user)"
        >
          <div class="user-avatar">{{ user.userName.charAt(0) }}</div>
          <div class="user-info">
            <span class="user-name">{{ user.userName }}</span>
            <span class="user-id">{{ user.username }}</span>
          </div>
          <span v-if="user.departmentName" class="user-dept">{{ user.departmentName }}</span>
          <svg v-if="addingUserId === user.userId" class="animate-spin w-4 h-4 text-primary-600" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <svg v-else class="w-4 h-4 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
        </button>
      </div>

      <!-- 검색 결과 없음 -->
      <div v-else-if="showSearchDropdown && searchKeyword.length >= 2 && !isSearching" class="search-empty">
        <p>검색 결과가 없습니다.</p>
      </div>
    </div>

    <!-- 멤버 목록 -->
    <div class="members-section">
      <h4 class="section-title">그룹 멤버</h4>

      <div v-if="members.length === 0" class="empty-members">
        <svg class="w-10 h-10 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
        <p class="mt-2">그룹에 멤버가 없습니다.</p>
        <p class="text-xs">위 검색창에서 사용자를 검색하여 추가하세요.</p>
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

.search-section {
  @apply p-4 border-b border-gray-200 relative;
}

.search-field {
  @apply relative;
}

.search-icon {
  @apply absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400;
}

.search-input {
  @apply w-full pl-10 pr-10 py-2 text-sm border border-gray-300 rounded-lg
         focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent;
}

.search-loading {
  @apply absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400;
}

.search-dropdown {
  @apply absolute z-10 left-4 right-4 mt-1 bg-white rounded-lg shadow-lg border border-gray-200
         max-h-60 overflow-y-auto;
}

.search-result-item {
  @apply w-full flex items-center gap-3 px-3 py-2 text-left
         hover:bg-gray-50 transition-colors disabled:opacity-50;
}

.search-empty {
  @apply absolute z-10 left-4 right-4 mt-1 p-4 bg-white rounded-lg shadow-lg border border-gray-200
         text-center text-sm text-gray-500;
}

.user-avatar {
  @apply w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center
         text-sm font-medium text-gray-600 flex-shrink-0;
}

.user-info {
  @apply flex-1 min-w-0;
}

.user-name {
  @apply block text-sm font-medium text-gray-900 truncate;
}

.user-id {
  @apply block text-xs text-gray-500;
}

.user-dept {
  @apply text-xs text-gray-400 flex-shrink-0;
}

.members-section {
  @apply p-4;
}

.section-title {
  @apply text-sm font-medium text-gray-900 mb-3;
}

.empty-members {
  @apply text-center py-8 text-sm text-gray-500;
}

.members-list {
  @apply space-y-2 max-h-[400px] overflow-y-auto;
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
