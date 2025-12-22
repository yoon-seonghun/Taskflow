<script setup lang="ts">
/**
 * 공유 사용자 검색/추가 컴포넌트
 * - UserSearchSelector를 사용한 사용자 검색/선택
 * - 부서 트리 또는 이름으로 검색
 * - 권한 선택 후 추가
 */
import { ref, computed } from 'vue'
import { UserSearchSelector } from '@/components/common'
import type { User } from '@/types/user'
import type { BoardPermission, BoardShareRequest } from '@/types/board'

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

// 선택된 사용자 ID
const selectedUserId = ref<number | null>(null)

// 선택된 사용자 정보
const selectedUser = ref<User | null>(null)

// 권한
const selectedPermission = ref<BoardPermission>('EDIT')

// 권한 옵션
const permissionOptions: { value: BoardPermission; label: string; description: string }[] = [
  { value: 'VIEW', label: '조회', description: '보기만 가능' },
  { value: 'EDIT', label: '편집', description: '조회 및 수정 가능' },
  { value: 'FULL', label: '전체', description: '조회/수정/삭제 가능' }
]

// 추가 버튼 활성화
const canAdd = computed(() => {
  return selectedUserId.value && !props.loading
})

// 사용자 선택 핸들러
function handleUserSelect(user: User | null) {
  selectedUser.value = user
}

// 사용자 추가
function handleAdd() {
  if (!selectedUserId.value) return

  const data: BoardShareRequest = {
    userId: selectedUserId.value,
    permission: selectedPermission.value
  }

  emit('add', data)

  // 초기화
  selectedUserId.value = null
  selectedUser.value = null
  selectedPermission.value = 'EDIT'
}
</script>

<template>
  <div class="share-user-search">
    <h3 class="text-sm font-medium text-gray-700 mb-4">사용자 추가</h3>

    <div class="space-y-4">
      <!-- 사용자 검색/선택 -->
      <UserSearchSelector
        v-model="selectedUserId"
        :exclude-user-ids="existingUserIds"
        placeholder="사용자를 선택하세요"
        @select="handleUserSelect"
      />

      <!-- 사용자가 선택되었을 때 권한 선택 및 추가 버튼 표시 -->
      <template v-if="selectedUserId">
        <!-- 권한 선택 -->
        <div>
          <label class="block text-[13px] font-medium text-gray-700 mb-2">권한</label>
          <div class="space-y-2">
            <label
              v-for="opt in permissionOptions"
              :key="opt.value"
              class="flex items-center gap-3 p-3 bg-white border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
              :class="selectedPermission === opt.value ? 'border-primary-500 ring-1 ring-primary-500' : 'border-gray-200'"
            >
              <input
                type="radio"
                v-model="selectedPermission"
                :value="opt.value"
                class="w-4 h-4 text-primary-600 border-gray-300 focus:ring-primary-500"
              />
              <div>
                <p class="text-[13px] font-medium text-gray-700">{{ opt.label }}</p>
                <p class="text-[12px] text-gray-500">{{ opt.description }}</p>
              </div>
            </label>
          </div>
        </div>

        <!-- 추가 버튼 -->
        <button
          type="button"
          class="btn-primary w-full"
          :disabled="!canAdd"
          @click="handleAdd"
        >
          <template v-if="loading">
            <svg class="animate-spin w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            추가 중...
          </template>
          <template v-else>
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
            </svg>
            공유 사용자 추가
          </template>
        </button>
      </template>

      <!-- 안내 메시지 (사용자 미선택 시) -->
      <div v-else class="p-3 bg-gray-50 rounded-lg">
        <p class="text-[12px] text-gray-500">
          부서에서 찾기 또는 이름으로 검색하여 공유할 사용자를 선택하세요.
        </p>
      </div>
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
