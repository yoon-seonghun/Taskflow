<script setup lang="ts">
/**
 * 업무 이관 모달
 *
 * 이관 대상:
 * 1. 본인 보드로 이관 - 본인 보드 목록에서 선택
 * 2. 다른 사용자에게 이관 - 사용자 선택 (자동으로 "업무이관" 보드로 이관)
 */
import { ref, computed, watch, onMounted } from 'vue'
import { Modal, Button, Spinner, UserSearchSelector } from '@/components/common'
import { useItemStore } from '@/stores/item'
import { useBoardStore } from '@/stores/board'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import type { Item, ItemTransferRequest } from '@/types/item'
import type { User } from '@/types/user'

type TransferTarget = 'self' | 'user'

interface Props {
  show: boolean
  item: Item
  boardId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'transferred', item: Item): void
}>()

const itemStore = useItemStore()
const boardStore = useBoardStore()
const authStore = useAuthStore()
const toast = useToast()

// 상태
const isSubmitting = ref(false)
const transferTarget = ref<TransferTarget>('self')
const selectedBoardId = ref<number | null>(null)
const selectedUserId = ref<number | null>(null)
const selectedUser = ref<User | null>(null)
const reason = ref('')

// 현재 사용자 ID
const currentUserId = computed(() => authStore.user?.userId)

// 본인 보드 목록 (현재 보드 제외)
const ownBoards = computed(() => {
  return boardStore.accessibleBoards.filter(b =>
    b.boardId !== props.boardId && b.ownerId === currentUserId.value
  )
})

// 제외할 사용자 (본인)
const excludeUserIds = computed(() => {
  return currentUserId.value ? [currentUserId.value] : []
})

// 유효성 검사
const isValid = computed(() => {
  if (transferTarget.value === 'self') {
    return selectedBoardId.value !== null
  } else {
    return selectedUserId.value !== null
  }
})

// 이관 실행
async function handleSubmit() {
  if (!isValid.value) {
    if (transferTarget.value === 'self') {
      toast.error('이관할 보드를 선택해주세요.')
    } else {
      toast.error('이관할 사용자를 선택해주세요.')
    }
    return
  }

  isSubmitting.value = true

  try {
    const request: ItemTransferRequest = {
      reason: reason.value || undefined
    }

    if (transferTarget.value === 'self') {
      request.targetBoardId = selectedBoardId.value!
    } else {
      request.targetUserId = selectedUserId.value!
    }

    const result = await itemStore.transferItem(props.boardId, props.item.itemId, request)

    if (result) {
      toast.success('업무가 이관되었습니다.')
      emit('transferred', result)
      emit('close')
    } else {
      toast.error(itemStore.error || '이관에 실패했습니다.')
    }
  } catch (error) {
    toast.error('이관에 실패했습니다.')
  } finally {
    isSubmitting.value = false
  }
}

// 사용자 선택 핸들러
function handleUserSelect(user: User | null) {
  selectedUser.value = user
}

// 초기화
function resetForm() {
  transferTarget.value = 'self'
  selectedBoardId.value = null
  selectedUserId.value = null
  selectedUser.value = null
  reason.value = ''
}

watch(() => props.show, (newShow) => {
  if (newShow) {
    resetForm()
  }
})
</script>

<template>
  <Modal
    :model-value="show"
    title="업무 이관"
    size="lg"
    @close="$emit('close')"
    @update:model-value="(val) => !val && $emit('close')"
  >
    <div class="space-y-5">
      <!-- 업무 정보 -->
      <div class="p-3 bg-gray-50 rounded-lg border border-gray-200">
        <p class="text-[12px] text-gray-500 mb-1">이관할 업무</p>
        <p class="text-[14px] font-medium text-gray-900">{{ item.title }}</p>
      </div>

      <!-- 이관 대상 선택 (라디오 버튼) -->
      <div>
        <label class="block text-[13px] font-medium text-gray-700 mb-3">이관 대상</label>
        <div class="space-y-2">
          <!-- 본인 보드로 이관 -->
          <label class="flex items-center gap-3 p-3 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
            :class="transferTarget === 'self' ? 'border-primary-500 bg-primary-50' : 'border-gray-200'"
          >
            <input
              type="radio"
              v-model="transferTarget"
              value="self"
              class="w-4 h-4 text-primary-600 border-gray-300 focus:ring-primary-500"
            />
            <div>
              <p class="text-[13px] font-medium text-gray-700">본인 보드로 이관</p>
              <p class="text-[12px] text-gray-500">내 다른 보드로 업무를 이관합니다</p>
            </div>
          </label>

          <!-- 다른 사용자에게 이관 -->
          <label class="flex items-center gap-3 p-3 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
            :class="transferTarget === 'user' ? 'border-primary-500 bg-primary-50' : 'border-gray-200'"
          >
            <input
              type="radio"
              v-model="transferTarget"
              value="user"
              class="w-4 h-4 text-primary-600 border-gray-300 focus:ring-primary-500"
            />
            <div>
              <p class="text-[13px] font-medium text-gray-700">다른 사용자에게 이관</p>
              <p class="text-[12px] text-gray-500">다른 사용자에게 업무를 이관합니다</p>
            </div>
          </label>
        </div>
      </div>

      <!-- 본인 보드 선택 -->
      <div v-if="transferTarget === 'self'" class="animate-fadeIn">
        <label class="block text-[13px] font-medium text-gray-700 mb-2">이관할 보드</label>
        <select
          v-model="selectedBoardId"
          class="w-full px-3 py-2 text-[13px] border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
        >
          <option :value="null">보드를 선택하세요</option>
          <option v-for="board in ownBoards" :key="board.boardId" :value="board.boardId">
            {{ board.boardName }}
            <template v-if="board.itemCount !== undefined"> ({{ board.itemCount }}건)</template>
          </option>
        </select>
        <p v-if="ownBoards.length === 0" class="mt-2 text-[12px] text-amber-600">
          이관 가능한 보드가 없습니다. 새 보드를 생성해주세요.
        </p>
      </div>

      <!-- 사용자 선택 -->
      <div v-if="transferTarget === 'user'" class="animate-fadeIn">
        <UserSearchSelector
          v-model="selectedUserId"
          :exclude-user-ids="excludeUserIds"
          label="이관할 사용자"
          placeholder="사용자를 선택하세요"
          @select="handleUserSelect"
        />
        <div class="mt-3 flex items-start gap-2 p-3 bg-blue-50 rounded-lg border border-blue-200">
          <svg class="w-5 h-5 text-blue-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p class="text-[12px] text-blue-700">
            선택한 사용자의 <strong>"업무이관"</strong> 보드로 업무가 이관됩니다.
            해당 보드가 없으면 자동으로 생성됩니다.
          </p>
        </div>
      </div>

      <!-- 사유 입력 (선택) -->
      <div>
        <label class="block text-[13px] font-medium text-gray-700 mb-2">이관 사유 (선택)</label>
        <textarea
          v-model="reason"
          rows="2"
          class="w-full px-3 py-2 text-[13px] border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none"
          placeholder="이관 사유를 입력하세요..."
        />
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button variant="outline" @click="$emit('close')">취소</Button>
        <Button
          variant="primary"
          :disabled="isSubmitting || !isValid"
          @click="handleSubmit"
        >
          <Spinner v-if="isSubmitting" size="sm" class="mr-2" />
          이관
        </Button>
      </div>
    </template>
  </Modal>
</template>

<style scoped>
.animate-fadeIn {
  animation: fadeIn 0.2s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
