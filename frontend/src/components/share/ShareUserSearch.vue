<script setup lang="ts">
/**
 * 공유 사용자 검색/추가 컴포넌트
 * - 사용자 검색 (아이디/이름)
 * - 권한 선택 후 추가
 */
import { ref, computed } from 'vue'
import Input from '@/components/common/Input.vue'
import Select from '@/components/common/Select.vue'
import type { SelectOption } from '@/components/common/Select.vue'
import type { User } from '@/types/user'
import type { BoardPermission, BoardShareRequest } from '@/types/board'
import { userApi } from '@/api/user'
import { useUiStore } from '@/stores/ui'

interface Props {
  existingUserIds?: number[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  existingUserIds: () => [],
  loading: false
})

const emit = defineEmits<{
  (e: 'add', data: BoardShareRequest): void
}>()

const uiStore = useUiStore()

// 검색
const searchKeyword = ref('')
const searchResults = ref<User[]>([])
const searching = ref(false)
const showResults = ref(false)

// 선택된 사용자
const selectedUser = ref<User | null>(null)

// 권한
const selectedPermission = ref<BoardPermission>('EDIT')

// 권한 옵션
const permissionOptions: SelectOption[] = [
  { value: 'VIEW', label: '조회 (보기만)' },
  { value: 'EDIT', label: '편집 (조회/수정)' },
  { value: 'FULL', label: '전체 (조회/수정/삭제)' }
]

// 추가 버튼 활성화
const canAdd = computed(() => {
  return selectedUser.value && !props.loading
})

// 사용자 검색
async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    showResults.value = false
    return
  }

  searching.value = true
  try {
    const response = await userApi.getUsers({
      keyword: searchKeyword.value.trim(),
      useYn: 'Y',
      size: 10
    })

    // 이미 공유된 사용자 제외
    searchResults.value = response.data.content.filter(
      user => !props.existingUserIds.includes(user.userId)
    )
    showResults.value = true
  } catch (error) {
    console.error('Failed to search users:', error)
    uiStore.showError('사용자 검색에 실패했습니다.')
  } finally {
    searching.value = false
  }
}

// 사용자 선택
function selectUser(user: User) {
  selectedUser.value = user
  searchKeyword.value = ''
  searchResults.value = []
  showResults.value = false
}

// 선택 취소
function clearSelection() {
  selectedUser.value = null
}

// 사용자 추가
function handleAdd() {
  if (!selectedUser.value) return

  const data: BoardShareRequest = {
    userId: selectedUser.value.userId,
    permission: selectedPermission.value
  }

  emit('add', data)

  // 초기화
  selectedUser.value = null
  selectedPermission.value = 'EDIT'
}

// 외부 클릭 시 결과 닫기
function handleBlur() {
  // 약간의 딜레이를 주어 클릭 이벤트가 먼저 처리되도록 함
  setTimeout(() => {
    showResults.value = false
  }, 200)
}
</script>

<template>
  <div class="share-user-search">
    <h3 class="text-sm font-medium text-gray-700 mb-3">사용자 추가</h3>

    <!-- 선택된 사용자가 없을 때: 검색 -->
    <div v-if="!selectedUser" class="space-y-3">
      <div class="relative">
        <Input
          v-model="searchKeyword"
          placeholder="사용자 이름 또는 아이디로 검색..."
          :clearable="true"
          @enter="handleSearch"
          @blur="handleBlur"
        />

        <!-- 검색 버튼 -->
        <button
          type="button"
          class="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600"
          :disabled="searching"
          @click="handleSearch"
        >
          <svg v-if="searching" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </button>

        <!-- 검색 결과 -->
        <div
          v-if="showResults"
          class="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto"
        >
          <div v-if="searchResults.length === 0" class="px-4 py-3 text-sm text-gray-500">
            검색 결과가 없습니다.
          </div>
          <button
            v-for="user in searchResults"
            :key="user.userId"
            type="button"
            class="w-full px-4 py-2 text-left hover:bg-gray-50 flex items-center gap-3 border-b border-gray-100 last:border-b-0"
            @click="selectUser(user)"
          >
            <div class="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center">
              <span class="text-sm font-medium text-primary-700">
                {{ user.userName.charAt(0) }}
              </span>
            </div>
            <div>
              <div class="text-sm font-medium text-gray-900">{{ user.userName }}</div>
              <div class="text-xs text-gray-500">{{ user.username }}</div>
            </div>
            <div v-if="user.departmentName" class="ml-auto text-xs text-gray-400">
              {{ user.departmentName }}
            </div>
          </button>
        </div>
      </div>

      <p class="text-xs text-gray-500">
        사용자를 검색하여 공유 목록에 추가하세요.
      </p>
    </div>

    <!-- 선택된 사용자가 있을 때: 권한 선택 및 추가 -->
    <div v-else class="space-y-3">
      <!-- 선택된 사용자 표시 -->
      <div class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
        <div class="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center">
          <span class="text-base font-medium text-primary-700">
            {{ selectedUser.userName.charAt(0) }}
          </span>
        </div>
        <div class="flex-1">
          <div class="text-sm font-medium text-gray-900">{{ selectedUser.userName }}</div>
          <div class="text-xs text-gray-500">{{ selectedUser.username }}</div>
        </div>
        <button
          type="button"
          class="p-1 text-gray-400 hover:text-gray-600"
          title="선택 취소"
          @click="clearSelection"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- 권한 선택 -->
      <Select
        v-model="selectedPermission"
        :options="permissionOptions"
        label="권한"
      />

      <!-- 추가 버튼 -->
      <button
        type="button"
        class="btn-primary w-full"
        :disabled="!canAdd"
        @click="handleAdd"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
        </svg>
        공유 사용자 추가
      </button>
    </div>
  </div>
</template>

<style scoped>
.share-user-search {
  @apply p-4 bg-white rounded-lg border border-gray-200;
}

.btn-primary {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed
         transition-colors duration-150;
}
</style>
