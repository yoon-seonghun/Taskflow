<script setup lang="ts">
/**
 * 이벤트 이관 모달
 * - 이벤트 소유권을 다른 사용자에게 이관
 */
import { ref, watch } from 'vue'
import { calendarApi, type EventDetailResponse } from '@/api/calendar'
import Modal from '@/components/common/Modal.vue'
import UserSearchSelector from '@/components/common/UserSearchSelector.vue'

interface Props {
  visible: boolean
  event: EventDetailResponse | null
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  event: null
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'transferred', event: EventDetailResponse): void
}>()

// 상태
const transferring = ref(false)
const targetUsername = ref('')
const targetUserName = ref('')

// 사용자 선택 핸들러
function handleUserSelect(user: { username: string; name: string }) {
  targetUsername.value = user.username
  targetUserName.value = user.name
}

// 이관 처리
async function handleTransfer() {
  if (!props.event || !targetUsername.value) return

  if (!confirm(`"${props.event.title}" 이벤트를 ${targetUserName.value}님에게 이관하시겠습니까?\n이관 후에는 해당 이벤트의 소유권이 이전됩니다.`)) {
    return
  }

  transferring.value = true
  try {
    const response = await calendarApi.transferEvent(props.event.eventId, targetUsername.value)
    emit('transferred', response.data)
    emit('update:visible', false)
  } catch (err: any) {
    console.error('Failed to transfer event:', err)
    alert(err.message || '이벤트 이관에 실패했습니다.')
  } finally {
    transferring.value = false
  }
}

// 닫기
function handleClose() {
  emit('update:visible', false)
}

// visible 변경 감시 - 초기화
watch(() => props.visible, (newVal) => {
  if (newVal) {
    targetUsername.value = ''
    targetUserName.value = ''
  }
})
</script>

<template>
  <Modal
    :model-value="visible"
    title="이벤트 이관"
    size="md"
    @close="handleClose"
    @update:model-value="(val) => emit('update:visible', val)"
  >
    <div class="space-y-4">
      <!-- 경고 메시지 -->
      <div class="flex items-start gap-3 p-4 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg">
        <svg class="w-5 h-5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        <div>
          <p class="text-sm font-medium text-amber-800 dark:text-amber-200">
            이관 시 주의사항
          </p>
          <ul class="mt-1 text-xs text-amber-700 dark:text-amber-300 space-y-0.5">
            <li>- 이벤트의 소유권이 대상 사용자에게 완전히 이전됩니다.</li>
            <li>- 이관 후에는 해당 이벤트를 수정/삭제할 수 없습니다.</li>
            <li>- 기존 공유 설정은 유지됩니다.</li>
          </ul>
        </div>
      </div>

      <!-- 이벤트 정보 -->
      <div v-if="event" class="p-3 bg-gray-50 dark:bg-gray-900 rounded-lg">
        <p class="font-medium text-gray-900 dark:text-gray-100">{{ event.title }}</p>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          {{ event.startDate }}
          <span v-if="event.endDate && event.endDate !== event.startDate">
            ~ {{ event.endDate }}
          </span>
        </p>
        <p class="text-xs text-gray-400 dark:text-gray-500 mt-1">
          현재 소유자: {{ event.ownerName }}
        </p>
      </div>

      <!-- 이관 대상 선택 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          이관 대상 사용자 <span class="text-red-500">*</span>
        </label>
        <UserSearchSelector
          v-model="targetUsername"
          placeholder="사용자를 검색하세요..."
          @select="handleUserSelect"
        />
        <p v-if="targetUserName" class="mt-2 text-sm text-gray-600 dark:text-gray-400">
          선택됨: <span class="font-medium text-gray-900 dark:text-gray-100">{{ targetUserName }}</span>
        </p>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <button
          type="button"
          class="px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-lg transition-colors dark:text-gray-300 dark:hover:bg-gray-700"
          @click="handleClose"
        >
          취소
        </button>
        <button
          type="button"
          class="px-4 py-2 text-sm bg-amber-600 text-white hover:bg-amber-700 rounded-lg transition-colors disabled:opacity-50"
          :disabled="!targetUsername || transferring"
          @click="handleTransfer"
        >
          {{ transferring ? '이관 중...' : '이관' }}
        </button>
      </div>
    </template>
  </Modal>
</template>
