<script setup lang="ts">
/**
 * 보드 이관 모달
 *
 * 이관 방법:
 * 1. 다른 보드로 업무 병합 - 본인의 다른 보드로 업무 이동 후 현재 보드 삭제
 * 2. 다른 사용자에게 보드 이관 - 보드명 "보드이관"으로 변경, 소유권 이전
 */
import { ref, computed, watch } from 'vue'
import { Modal, Button, Spinner, UserSearchSelector } from '@/components/common'
import { useBoardStore } from '@/stores/board'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { boardApi } from '@/api/board'
import type { Board, BoardTransferRequest } from '@/types/board'
import type { User } from '@/types/user'

type TransferMethod = 'merge' | 'transfer'

interface Props {
  show: boolean
  board: Board
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'transferred'): void
  (e: 'merged'): void
}>()

const boardStore = useBoardStore()
const authStore = useAuthStore()
const toast = useToast()

// 상태
const isSubmitting = ref(false)
const transferMethod = ref<TransferMethod>('merge')
const selectedBoardId = ref<number | null>(null)
const selectedUserId = ref<number | null>(null)
const selectedUser = ref<User | null>(null)
const reason = ref('')

// 현재 사용자 ID
const currentUserId = computed(() => authStore.user?.userId)

// 본인 보드 목록 (현재 보드 제외)
const otherBoards = computed(() => {
  return boardStore.accessibleBoards.filter(b =>
    b.boardId !== props.board.boardId && b.ownerId === currentUserId.value
  )
})

// 제외할 사용자 (본인)
const excludeUserIds = computed(() => {
  return currentUserId.value ? [currentUserId.value] : []
})

// 유효성 검사
const isValid = computed(() => {
  if (transferMethod.value === 'merge') {
    return selectedBoardId.value !== null
  } else {
    return selectedUserId.value !== null
  }
})

// 이관/병합 실행
async function handleSubmit() {
  if (!isValid.value) {
    if (transferMethod.value === 'merge') {
      toast.error('병합할 대상 보드를 선택해주세요.')
    } else {
      toast.error('이관할 사용자를 선택해주세요.')
    }
    return
  }

  isSubmitting.value = true

  try {
    if (transferMethod.value === 'merge') {
      // 업무 병합 후 보드 삭제
      await boardApi.deleteBoardWithTransfer(props.board.boardId, {
        targetUserId: currentUserId.value!,
        forceDelete: false
      })

      toast.success('업무가 병합되고 보드가 삭제되었습니다.')
      emit('merged')
      emit('close')
    } else {
      // 보드 소유권 이전
      const request: BoardTransferRequest = {
        targetUserId: selectedUserId.value!,
        reason: reason.value || undefined
      }

      await boardApi.transferBoardOwnership(props.board.boardId, request)

      toast.success('보드가 이관되었습니다.')
      emit('transferred')
      emit('close')
    }

    // 보드 목록 새로고침
    await boardStore.fetchBoards()
  } catch (error: any) {
    const message = error?.response?.data?.message || '처리에 실패했습니다.'
    toast.error(message)
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
  transferMethod.value = 'merge'
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
    title="보드 이관"
    size="lg"
    @close="$emit('close')"
    @update:model-value="(val) => !val && $emit('close')"
  >
    <div class="space-y-5">
      <!-- 보드 정보 -->
      <div class="p-3 bg-gray-50 rounded-lg border border-gray-200">
        <p class="text-[12px] text-gray-500 mb-1">이관할 보드</p>
        <div class="flex items-center gap-2">
          <span class="text-[14px] font-medium text-gray-900">{{ board.boardName }}</span>
          <span v-if="board.itemCount !== undefined" class="text-[12px] text-gray-500">
            (업무 {{ board.itemCount }}건)
          </span>
        </div>
      </div>

      <!-- 이관 방법 선택 (라디오 버튼) -->
      <div>
        <label class="block text-[13px] font-medium text-gray-700 mb-3">이관 방법</label>
        <div class="space-y-2">
          <!-- 다른 보드로 업무 병합 -->
          <label class="flex items-center gap-3 p-3 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
            :class="transferMethod === 'merge' ? 'border-primary-500 bg-primary-50' : 'border-gray-200'"
          >
            <input
              type="radio"
              v-model="transferMethod"
              value="merge"
              class="w-4 h-4 text-primary-600 border-gray-300 focus:ring-primary-500"
            />
            <div>
              <p class="text-[13px] font-medium text-gray-700">다른 보드로 업무 병합</p>
              <p class="text-[12px] text-gray-500">내 다른 보드로 업무를 옮기고 현재 보드를 삭제합니다</p>
            </div>
          </label>

          <!-- 다른 사용자에게 보드 이관 -->
          <label class="flex items-center gap-3 p-3 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
            :class="transferMethod === 'transfer' ? 'border-primary-500 bg-primary-50' : 'border-gray-200'"
          >
            <input
              type="radio"
              v-model="transferMethod"
              value="transfer"
              class="w-4 h-4 text-primary-600 border-gray-300 focus:ring-primary-500"
            />
            <div>
              <p class="text-[13px] font-medium text-gray-700">다른 사용자에게 보드 이관</p>
              <p class="text-[12px] text-gray-500">보드 전체를 다른 사용자에게 이관합니다</p>
            </div>
          </label>
        </div>
      </div>

      <!-- 대상 보드 선택 (병합) -->
      <div v-if="transferMethod === 'merge'" class="animate-fadeIn">
        <label class="block text-[13px] font-medium text-gray-700 mb-2">대상 보드</label>
        <select
          v-model="selectedBoardId"
          class="w-full px-3 py-2 text-[13px] border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
        >
          <option :value="null">병합할 보드를 선택하세요</option>
          <option v-for="b in otherBoards" :key="b.boardId" :value="b.boardId">
            {{ b.boardName }}
            <template v-if="b.itemCount !== undefined"> ({{ b.itemCount }}건)</template>
          </option>
        </select>
        <p v-if="otherBoards.length === 0" class="mt-2 text-[12px] text-amber-600">
          병합 가능한 보드가 없습니다. 새 보드를 생성해주세요.
        </p>

        <!-- 경고 메시지 -->
        <div class="mt-3 flex items-start gap-2 p-3 bg-amber-50 rounded-lg border border-amber-200">
          <svg class="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <p class="text-[12px] text-amber-700">
            현재 보드의 모든 업무가 선택한 보드로 이동됩니다.
            <strong>병합 후 현재 보드는 삭제됩니다.</strong>
          </p>
        </div>
      </div>

      <!-- 사용자 선택 (이관) -->
      <div v-if="transferMethod === 'transfer'" class="animate-fadeIn">
        <UserSearchSelector
          v-model="selectedUserId"
          :exclude-user-ids="excludeUserIds"
          label="이관할 사용자"
          placeholder="사용자를 선택하세요"
          @select="handleUserSelect"
        />

        <!-- 안내 메시지 -->
        <div class="mt-3 flex items-start gap-2 p-3 bg-blue-50 rounded-lg border border-blue-200">
          <svg class="w-5 h-5 text-blue-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div class="text-[12px] text-blue-700">
            <p>보드명이 <strong>"보드이관"</strong>으로 변경되어 선택한 사용자에게 이관됩니다.</p>
            <p class="mt-1">이관 후에는 해당 보드에 대한 소유권이 없어집니다.</p>
          </div>
        </div>

        <!-- 사유 입력 (선택) -->
        <div class="mt-4">
          <label class="block text-[13px] font-medium text-gray-700 mb-2">이관 사유 (선택)</label>
          <textarea
            v-model="reason"
            rows="2"
            class="w-full px-3 py-2 text-[13px] border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none"
            placeholder="이관 사유를 입력하세요..."
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button variant="outline" @click="$emit('close')">취소</Button>
        <Button
          :variant="transferMethod === 'merge' ? 'danger' : 'primary'"
          :disabled="isSubmitting || !isValid"
          @click="handleSubmit"
        >
          <Spinner v-if="isSubmitting" size="sm" class="mr-2" />
          {{ transferMethod === 'merge' ? '병합 및 삭제' : '이관' }}
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
