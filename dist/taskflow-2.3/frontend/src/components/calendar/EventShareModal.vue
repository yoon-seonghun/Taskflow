<script setup lang="ts">
/**
 * 이벤트 공유 모달
 * - 이벤트 공유 사용자 관리
 * - 공유 추가/제거
 */
import { ref, computed, watch, onMounted } from 'vue'
import { calendarApi, type EventDetailResponse } from '@/api/calendar'
import Modal from '@/components/common/Modal.vue'
import UserSearchSelector from '@/components/common/UserSearchSelector.vue'
import Spinner from '@/components/common/Spinner.vue'

interface ShareUser {
  username: string
  userName: string
  departmentName: string | null
  shareType: string
  sharedAt: string
}

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
  (e: 'changed'): void
}>()

// 상태
const loading = ref(false)
const shares = ref<ShareUser[]>([])
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
  if (!props.event) return

  loading.value = true
  try {
    const response = await calendarApi.getEventShares(props.event.eventId)
    shares.value = response.data || []
  } catch (err) {
    console.error('Failed to load shares:', err)
  } finally {
    loading.value = false
  }
}

// 공유 추가
async function handleAddShare() {
  if (!props.event || !selectedUsername.value) return

  addingShare.value = true
  try {
    await calendarApi.addEventShare(props.event.eventId, {
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
  if (!props.event) return
  if (!confirm('이 사용자의 공유를 해제하시겠습니까?')) return

  try {
    await calendarApi.removeEventShare(props.event.eventId, username)
    await loadShares()
    emit('changed')
  } catch (err: any) {
    console.error('Failed to remove share:', err)
    alert(err.message || '공유 해제에 실패했습니다.')
  }
}

// 공유 권한 변경
async function handleUpdateShareType(username: string, newShareType: string) {
  if (!props.event) return

  try {
    await calendarApi.updateEventShareType(props.event.eventId, username, newShareType)
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
function handleClose() {
  emit('update:visible', false)
}

// visible 변경 감시
watch(() => props.visible, (newVal) => {
  if (newVal && props.event) {
    loadShares()
  }
})

// 공유 타입 라벨
function getShareTypeLabel(type: string): string {
  return type === 'EDIT' ? '편집' : '보기'
}

// 날짜 포맷
function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}
</script>

<template>
  <Modal
    :model-value="visible"
    title="이벤트 공유"
    size="md"
    @close="handleClose"
    @update:model-value="(val) => emit('update:visible', val)"
  >
    <div class="space-y-4">
      <!-- 이벤트 정보 -->
      <div v-if="event" class="p-3 bg-gray-50 dark:bg-gray-900 rounded-lg">
        <p class="font-medium text-gray-900 dark:text-gray-100">{{ event.title }}</p>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          {{ event.startDate }}
          <span v-if="event.endDate && event.endDate !== event.startDate">
            ~ {{ event.endDate }}
          </span>
        </p>
      </div>

      <!-- 공유 추가 -->
      <div class="border-b border-gray-200 dark:border-gray-700 pb-4">
        <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
          사용자 추가
        </h3>
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
            class="px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50"
            :disabled="!selectedUsername || addingShare"
            @click="handleAddShare"
          >
            {{ addingShare ? '추가 중...' : '추가' }}
          </button>
        </div>
      </div>

      <!-- 공유 목록 -->
      <div>
        <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
          공유 사용자 목록
        </h3>

        <!-- 로딩 -->
        <div v-if="loading" class="flex justify-center py-8">
          <Spinner size="md" />
        </div>

        <!-- 공유 없음 -->
        <div v-else-if="shares.length === 0" class="text-center py-8 text-gray-500 dark:text-gray-400">
          공유된 사용자가 없습니다.
        </div>

        <!-- 공유 목록 -->
        <div v-else class="space-y-2">
          <div
            v-for="share in shares"
            :key="share.username"
            class="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-900 rounded-lg"
          >
            <div class="flex items-center gap-3">
              <!-- 아바타 -->
              <div class="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900 flex items-center justify-center">
                <span class="text-sm font-medium text-primary-700 dark:text-primary-300">
                  {{ share.userName.charAt(0) }}
                </span>
              </div>
              <!-- 정보 -->
              <div>
                <p class="text-sm font-medium text-gray-900 dark:text-gray-100">
                  {{ share.userName }}
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-400">
                  {{ share.username }}
                  <span v-if="share.departmentName">
                    · {{ share.departmentName }}
                  </span>
                </p>
              </div>
            </div>

            <div class="flex items-center gap-2">
              <!-- 권한 변경 드롭다운 -->
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
                class="p-1 text-gray-400 hover:text-red-500 rounded transition-colors"
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
          type="button"
          class="px-4 py-2 text-sm bg-gray-100 text-gray-700 hover:bg-gray-200 rounded-lg transition-colors dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600"
          @click="handleClose"
        >
          닫기
        </button>
      </div>
    </template>
  </Modal>
</template>
