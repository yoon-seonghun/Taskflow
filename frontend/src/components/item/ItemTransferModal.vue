<script setup lang="ts">
/**
 * 업무 이관 모달
 * - 다른 보드로 이관
 * - 다른 사용자에게 이관 (사용자의 기본 보드로)
 */
import { ref, computed, watch, onMounted } from 'vue'
import { Modal, Button, Spinner } from '@/components/common'
import { useItemStore } from '@/stores/item'
import { useBoardStore } from '@/stores/board'
import { userApi } from '@/api/user'
import { useToast } from '@/composables/useToast'
import type { Item, ItemTransferRequest } from '@/types/item'
import type { User } from '@/types/user'

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
const toast = useToast()

// 상태
const isLoading = ref(false)
const isSubmitting = ref(false)
const transferType = ref<'board' | 'user'>('board')
const selectedBoardId = ref<number | null>(null)
const selectedUserId = ref<number | null>(null)
const reason = ref('')
const users = ref<User[]>([])

// 접근 가능한 보드 목록 (현재 보드 제외)
const availableBoards = computed(() => {
  return boardStore.accessibleBoards.filter(b => b.boardId !== props.boardId)
})

// 사용자 목록 로드
async function loadUsers() {
  try {
    const response = await userApi.getUsers({ useYn: 'Y' })
    if (response.success && response.data?.content) {
      users.value = response.data.content
    }
  } catch (error) {
    console.error('Failed to load users:', error)
  }
}

// 이관 실행
async function handleSubmit() {
  if (transferType.value === 'board' && !selectedBoardId.value) {
    toast.error('이관할 보드를 선택해주세요.')
    return
  }
  if (transferType.value === 'user' && !selectedUserId.value) {
    toast.error('이관할 사용자를 선택해주세요.')
    return
  }

  isSubmitting.value = true

  try {
    const request: ItemTransferRequest = {
      reason: reason.value || undefined
    }

    if (transferType.value === 'board') {
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

// 초기화
function resetForm() {
  transferType.value = 'board'
  selectedBoardId.value = null
  selectedUserId.value = null
  reason.value = ''
}

watch(() => props.show, (newShow) => {
  if (newShow) {
    resetForm()
    loadUsers()
  }
})

onMounted(() => {
  if (props.show) {
    loadUsers()
  }
})
</script>

<template>
  <Modal
    :model-value="show"
    title="업무 이관"
    size="md"
    @close="$emit('close')"
    @update:model-value="(val) => !val && $emit('close')"
  >
    <div class="space-y-4">
      <!-- 업무 정보 -->
      <div class="p-3 bg-gray-50 rounded-lg">
        <p class="text-[12px] text-gray-500 mb-1">이관할 업무</p>
        <p class="text-[14px] font-medium text-gray-900">{{ item.title }}</p>
      </div>

      <!-- 이관 방식 선택 -->
      <div>
        <label class="block text-[13px] font-medium text-gray-700 mb-2">이관 방식</label>
        <div class="flex gap-2">
          <button
            :class="[
              'flex-1 px-3 py-2 text-[13px] font-medium rounded-lg border transition-colors',
              transferType === 'board'
                ? 'bg-primary-50 border-primary-500 text-primary-700'
                : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
            ]"
            @click="transferType = 'board'"
          >
            보드로 이관
          </button>
          <button
            :class="[
              'flex-1 px-3 py-2 text-[13px] font-medium rounded-lg border transition-colors',
              transferType === 'user'
                ? 'bg-primary-50 border-primary-500 text-primary-700'
                : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
            ]"
            @click="transferType = 'user'"
          >
            사용자에게 이관
          </button>
        </div>
      </div>

      <!-- 보드 선택 -->
      <div v-if="transferType === 'board'">
        <label class="block text-[13px] font-medium text-gray-700 mb-2">이관할 보드</label>
        <select
          v-model="selectedBoardId"
          class="w-full px-3 py-2 text-[13px] border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
        >
          <option :value="null">보드를 선택하세요</option>
          <option v-for="board in availableBoards" :key="board.boardId" :value="board.boardId">
            {{ board.boardName }}
          </option>
        </select>
        <p v-if="availableBoards.length === 0" class="mt-1 text-[12px] text-gray-500">
          이관 가능한 보드가 없습니다.
        </p>
      </div>

      <!-- 사용자 선택 -->
      <div v-if="transferType === 'user'">
        <label class="block text-[13px] font-medium text-gray-700 mb-2">이관할 사용자</label>
        <select
          v-model="selectedUserId"
          class="w-full px-3 py-2 text-[13px] border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
        >
          <option :value="null">사용자를 선택하세요</option>
          <option v-for="user in users" :key="user.userId" :value="user.userId">
            {{ user.name }} ({{ user.departmentName || '소속없음' }})
          </option>
        </select>
        <p class="mt-1 text-[12px] text-gray-500">
          선택한 사용자의 기본 보드로 업무가 이관됩니다.
        </p>
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
          :disabled="isSubmitting"
          @click="handleSubmit"
        >
          <Spinner v-if="isSubmitting" size="sm" class="mr-2" />
          이관
        </Button>
      </div>
    </template>
  </Modal>
</template>
