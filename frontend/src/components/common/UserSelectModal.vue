<script setup lang="ts">
/**
 * 사용자 선택 모달
 * - UserSearchSelector를 모달로 감싸서 제공
 * - username (string) 값을 반환 (FK 정책 준수)
 * - 부서 트리 또는 이름으로 검색
 */
import { ref, watch } from 'vue'
import { Modal } from '@/components/common'
import UserSearchSelector from './UserSearchSelector.vue'
import type { User } from '@/types/user'

interface Props {
  show: boolean
  title?: string
  excludeUsernames?: string[]
  currentUsername?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  title: '담당자 선택',
  excludeUsernames: () => [],
  currentUsername: null
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'select', user: { username: string; name: string; departmentName?: string } | null): void
}>()

// 선택된 사용자 ID (UserSearchSelector용)
const selectedUserId = ref<number | null>(null)

// 선택된 사용자 정보
const selectedUser = ref<User | null>(null)

// 사용자 선택 핸들러
function handleUserSelect(user: User | null) {
  selectedUser.value = user
}

// 확인 버튼
function handleConfirm() {
  if (selectedUser.value) {
    emit('select', {
      username: selectedUser.value.username,
      name: selectedUser.value.name || selectedUser.value.userName || '',
      departmentName: selectedUser.value.departmentName
    })
  }
  emit('close')
}

// 선택 해제
function handleClear() {
  emit('select', null)
  emit('close')
}

// 모달이 열릴 때 초기화
watch(() => props.show, (newVal) => {
  if (newVal) {
    selectedUserId.value = null
    selectedUser.value = null
  }
})
</script>

<template>
  <Modal
    :model-value="show"
    :title="title"
    size="lg"
    @close="emit('close')"
  >
    <div class="p-4">
      <!-- 현재 담당자 표시 -->
      <div v-if="currentUsername" class="mb-4 px-3 py-2 bg-blue-50 rounded-lg border border-blue-200">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="text-xs text-blue-600">현재 담당자:</span>
            <span class="text-sm font-medium text-blue-800">{{ currentUsername }}</span>
          </div>
          <button
            type="button"
            class="text-xs text-blue-600 hover:text-blue-800 underline"
            @click="handleClear"
          >
            담당자 해제
          </button>
        </div>
      </div>

      <!-- 사용자 검색/선택 -->
      <UserSearchSelector
        v-model="selectedUserId"
        :exclude-usernames="excludeUsernames"
        placeholder="담당자를 선택하세요"
        @select="handleUserSelect"
      />
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <button
          type="button"
          class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500"
          @click="emit('close')"
        >
          취소
        </button>
        <button
          type="button"
          class="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!selectedUser"
          @click="handleConfirm"
        >
          선택
        </button>
      </div>
    </template>
  </Modal>
</template>
