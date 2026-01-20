<script setup lang="ts">
/**
 * 캘린더 공유 모달
 * - 캘린더 공유 사용자 관리
 * - 공유 추가/제거/권한 변경
 */
import { ref, watch } from 'vue'
import { calendarApi, type UserCalendarResponse, type CalendarShareResponse } from '@/api/calendar'
import Modal from '@/components/common/Modal.vue'
import UserSearchSelector from '@/components/common/UserSearchSelector.vue'
import Spinner from '@/components/common/Spinner.vue'

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
  (e: 'changed'): void
}>()

// 상태
const loading = ref(false)
const shares = ref<CalendarShareResponse[]>([])
const addingShare = ref(false)
const selectedUserId = ref<number | null>(null)
const selectedUsername = ref('')

// 공유 타입 옵션
const shareTypeOptions = [
  { value: 'VIEW', label: '보기' },
  { value: 'EDIT', label: '편집' }
]
const selectedShareType = ref('VIEW')

// 공유 목록 로드
async function loadShares() {
  if (!props.calendar) return

  loading.value = true
  try {
    const response = await calendarApi.getCalendarShares(props.calendar.calendarId)
    shares.value = response.data || []
  } catch (err) {
    console.error('Failed to load shares:', err)
  } finally {
    loading.value = false
  }
}

// 공유 추가
async function handleAddShare() {
  if (!props.calendar || !selectedUsername.value) return

  addingShare.value = true
  try {
    await calendarApi.addCalendarShare(props.calendar.calendarId, {
      username: selectedUsername.value,
      shareType: selectedShareType.value
    })
    selectedUserId.value = null
    selectedUsername.value = ''
    await loadShares()
    emit('changed')
  } catch (err: any) {
    console.error('Failed to add share:', err)
    alert(err.message || '공유 추가에 실패했습니다.')
  } finally {
    addingShare.value = false
  }
}

// 공유 제거
async function handleRemoveShare(username: string) {
  if (!props.calendar) return
  if (!confirm('이 사용자의 공유를 해제하시겠습니까?')) return

  try {
    await calendarApi.removeCalendarShare(props.calendar.calendarId, username)
    await loadShares()
    emit('changed')
  } catch (err: any) {
    console.error('Failed to remove share:', err)
    alert(err.message || '공유 해제에 실패했습니다.')
  }
}

// 공유 권한 변경
async function handleUpdateShareType(username: string, newShareType: string) {
  if (!props.calendar) return

  try {
    await calendarApi.updateCalendarShareType(props.calendar.calendarId, username, newShareType)
    await loadShares()
    emit('changed')
  } catch (err: any) {
    console.error('Failed to update share type:', err)
    alert(err.message || '권한 변경에 실패했습니다.')
  }
}

// 사용자 선택 핸들러
function handleUserSelect(user: { userId: number; username: string } | null) {
  if (user) {
    selectedUserId.value = user.userId
    selectedUsername.value = user.username
  } else {
    selectedUserId.value = null
    selectedUsername.value = ''
  }
}

// 닫기
function close() {
  emit('update:visible', false)
}

// visible 변경 시 데이터 로드
watch(() => props.visible, (newVal) => {
  if (newVal && props.calendar) {
    loadShares()
  }
})

// calendar 변경 시 데이터 로드
watch(() => props.calendar, (newVal) => {
  if (props.visible && newVal) {
    loadShares()
  }
})
</script>

<template>
  <Modal
    :model-value="visible"
    title="캘린더 공유"
    size="md"
    @close="close"
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="space-y-4">
      <!-- 캘린더 정보 -->
      <div class="flex items-center gap-2 pb-3 border-b border-gray-200 dark:border-gray-700">
        <span
          class="w-4 h-4 rounded-sm flex-shrink-0"
          :style="{ backgroundColor: calendar?.color || '#3b82f6' }"
        />
        <span class="font-medium text-gray-900 dark:text-gray-100">
          {{ calendar?.calendarName }}
        </span>
      </div>

      <!-- 공유 추가 -->
      <div class="space-y-2">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
          사용자 추가
        </label>
        <div class="flex gap-2">
          <div class="flex-1">
            <UserSearchSelector
              v-model="selectedUserId"
              placeholder="사용자 검색..."
              @select="handleUserSelect"
            />
          </div>
          <select
            v-model="selectedShareType"
            class="px-3 py-2 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
          >
            <option v-for="opt in shareTypeOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
          <button
            class="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!selectedUsername || addingShare"
            @click="handleAddShare"
          >
            <span v-if="addingShare">추가 중...</span>
            <span v-else>추가</span>
          </button>
        </div>
      </div>

      <!-- 공유 목록 -->
      <div class="space-y-2">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
          공유 사용자 목록
        </label>

        <!-- 로딩 -->
        <div v-if="loading" class="py-4 text-center">
          <Spinner size="sm" />
        </div>

        <!-- 공유 없음 -->
        <div
          v-else-if="shares.length === 0"
          class="py-4 text-center text-sm text-gray-500 dark:text-gray-400"
        >
          공유된 사용자가 없습니다.
        </div>

        <!-- 공유 목록 -->
        <div v-else class="divide-y divide-gray-100 dark:divide-gray-700">
          <div
            v-for="share in shares"
            :key="share.username"
            class="flex items-center justify-between py-2"
          >
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                <span class="text-sm font-medium text-gray-600 dark:text-gray-300">
                  {{ share.userName?.charAt(0) || '?' }}
                </span>
              </div>
              <div>
                <p class="text-sm font-medium text-gray-900 dark:text-gray-100">
                  {{ share.userName }}
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-400">
                  {{ share.departmentName || share.username }}
                </p>
              </div>
            </div>

            <div class="flex items-center gap-2">
              <!-- 권한 변경 -->
              <select
                :value="share.shareType"
                class="px-2 py-1 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-primary-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
                @change="handleUpdateShareType(share.username, ($event.target as HTMLSelectElement).value)"
              >
                <option v-for="opt in shareTypeOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>

              <!-- 삭제 버튼 -->
              <button
                class="p-1 text-gray-400 hover:text-red-500 transition-colors"
                title="공유 해제"
                @click="handleRemoveShare(share.username)"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end">
        <button
          class="px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-lg dark:text-gray-300 dark:hover:bg-gray-700"
          @click="close"
        >
          닫기
        </button>
      </div>
    </template>
  </Modal>
</template>
