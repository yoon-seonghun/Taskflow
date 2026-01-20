<script setup lang="ts">
/**
 * 캘린더 이관 모달
 * - 캘린더 소유권 이전
 */
import { ref, watch } from 'vue'
import { calendarApi, type UserCalendarResponse } from '@/api/calendar'
import Modal from '@/components/common/Modal.vue'
import UserSearchSelector from '@/components/common/UserSearchSelector.vue'

interface Props {
  visible: boolean
  calendar: UserCalendarResponse | null
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  calendar: null
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'transferred'): void
}>()

// 상태
const selectedUserId = ref<number | null>(null)
const selectedUsername = ref('')
const selectedUserName = ref('')
const transferring = ref(false)
const confirmText = ref('')

// 이관 실행
async function handleTransfer() {
  if (!props.calendar || !selectedUsername.value) return

  if (confirmText.value !== props.calendar.calendarName) {
    alert('캘린더 이름을 정확히 입력해주세요.')
    return
  }

  if (!confirm(`정말 "${props.calendar.calendarName}" 캘린더를 ${selectedUserName.value}님에게 이관하시겠습니까?\n\n이관 후에는 해당 캘린더에 대한 소유권을 잃게 됩니다.`)) {
    return
  }

  transferring.value = true
  try {
    await calendarApi.transferCalendar(props.calendar.calendarId, selectedUsername.value)
    emit('transferred')
    close()
  } catch (err: any) {
    console.error('Failed to transfer calendar:', err)
    alert(err.message || '캘린더 이관에 실패했습니다.')
  } finally {
    transferring.value = false
  }
}

// 사용자 선택 핸들러
function handleUserSelect(user: { userId: number; username: string; name?: string; userName?: string } | null) {
  if (user) {
    selectedUserId.value = user.userId
    selectedUsername.value = user.username
    selectedUserName.value = user.name || user.userName || ''
  } else {
    selectedUserId.value = null
    selectedUsername.value = ''
    selectedUserName.value = ''
  }
}

// 닫기
function close() {
  selectedUserId.value = null
  selectedUsername.value = ''
  selectedUserName.value = ''
  confirmText.value = ''
  emit('update:visible', false)
}

// visible 변경 시 초기화
watch(() => props.visible, (newVal) => {
  if (newVal) {
    selectedUserId.value = null
    selectedUsername.value = ''
    selectedUserName.value = ''
    confirmText.value = ''
  }
})
</script>

<template>
  <Modal
    :model-value="visible"
    title="캘린더 이관"
    size="sm"
    @close="close"
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="space-y-4">
      <!-- 경고 메시지 -->
      <div class="p-3 bg-amber-50 border border-amber-200 rounded-lg dark:bg-amber-900/20 dark:border-amber-700">
        <div class="flex items-start gap-2">
          <svg class="w-5 h-5 text-amber-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <div class="text-sm text-amber-700 dark:text-amber-300">
            <p class="font-medium">이 작업은 되돌릴 수 없습니다.</p>
            <p class="mt-1">캘린더를 이관하면 해당 캘린더의 소유권이 선택한 사용자에게 이전되며, 캘린더에 포함된 모든 이벤트도 함께 이관됩니다.</p>
          </div>
        </div>
      </div>

      <!-- 캘린더 정보 -->
      <div class="flex items-center gap-2 p-3 bg-gray-50 rounded-lg dark:bg-gray-800">
        <span
          class="w-4 h-4 rounded-sm flex-shrink-0"
          :style="{ backgroundColor: calendar?.color || '#3b82f6' }"
        />
        <span class="font-medium text-gray-900 dark:text-gray-100">
          {{ calendar?.calendarName }}
        </span>
      </div>

      <!-- 이관 대상 선택 -->
      <div class="space-y-2">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
          이관 대상 사용자 <span class="text-red-500">*</span>
        </label>
        <UserSearchSelector
          v-model="selectedUserId"
          placeholder="이관할 사용자 검색..."
          @select="handleUserSelect"
        />
        <p v-if="selectedUserName" class="text-sm text-gray-500 dark:text-gray-400">
          선택된 사용자: <span class="font-medium text-gray-700 dark:text-gray-300">{{ selectedUserName }}</span>
        </p>
      </div>

      <!-- 확인 입력 -->
      <div class="space-y-2">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
          확인을 위해 캘린더 이름을 입력해주세요
        </label>
        <input
          v-model="confirmText"
          type="text"
          :placeholder="calendar?.calendarName"
          class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100 dark:placeholder-gray-400"
        />
        <p class="text-xs text-gray-500 dark:text-gray-400">
          "{{ calendar?.calendarName }}"을(를) 정확히 입력해주세요.
        </p>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <button
          class="px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-lg dark:text-gray-300 dark:hover:bg-gray-700"
          @click="close"
        >
          취소
        </button>
        <button
          class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!selectedUsername || confirmText !== calendar?.calendarName || transferring"
          @click="handleTransfer"
        >
          <span v-if="transferring">이관 중...</span>
          <span v-else>이관</span>
        </button>
      </div>
    </template>
  </Modal>
</template>
