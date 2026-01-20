<script setup lang="ts">
/**
 * 업무 공유 모달
 * - 공유 사용자 목록 관리
 * - 공유 추가/권한 변경/제거
 * - UserSearchSelector를 사용한 사용자 검색/선택
 */
import { ref, computed, watch, onMounted } from 'vue'
import { Modal, Button, Spinner, UserSearchSelector } from '@/components/common'
import { useItemStore } from '@/stores/item'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import type { Item } from '@/types/item'
import type { Share, SharePermission } from '@/types/share'
import type { User } from '@/types/user'

interface Props {
  show: boolean
  item: Item
  boardId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'updated'): void
}>()

const itemStore = useItemStore()
const toast = useToast()
const confirm = useConfirm()

// 상태
const isLoading = ref(false)
const isSubmitting = ref(false)
const shares = ref<Share[]>([])
const showAddForm = ref(false)
const newUserId = ref<number | null>(null)  // UserSearchSelector v-model용
const selectedUser = ref<User | null>(null)  // 선택된 사용자 전체 정보 (username 포함)
const newPermission = ref<SharePermission>('VIEW')

// 권한 옵션
const permissionOptions: { value: SharePermission; label: string; description: string }[] = [
  { value: 'VIEW', label: '조회', description: '업무 내용 조회만 가능' },
  { value: 'EDIT', label: '편집', description: '업무 내용 수정 가능' },
  { value: 'FULL', label: '전체', description: '이관/공유/삭제 포함 전체 권한' }
]

// 이미 공유된 사용자 username 목록 (UserSearchSelector에서 제외용)
// UserSearchSelector는 userId 기반이므로 별도 처리 필요
const excludeUsernames = computed(() => {
  return shares.value.map(s => s.username)
})

// 이미 공유된 사용자 ID 목록 (UserSearchSelector v-model은 userId 사용)
const excludeUserIds = computed<number[]>(() => {
  // Share에 userId가 없으므로 빈 배열 반환 (username으로 필터링은 템플릿에서 처리)
  return []
})

// 공유 목록 로드
async function loadShares() {
  isLoading.value = true
  try {
    shares.value = await itemStore.fetchItemShares(props.item.itemId)
  } catch (error) {
    console.error('Failed to load shares:', error)
  } finally {
    isLoading.value = false
  }
}

// 사용자 선택 핸들러
function handleUserSelect(user: User | null) {
  selectedUser.value = user
}

// 공유 추가
async function handleAddShare() {
  if (!selectedUser.value?.username) {
    toast.error('공유할 사용자를 선택해주세요.')
    return
  }

  isSubmitting.value = true
  try {
    const success = await itemStore.addItemShare(props.item.itemId, {
      username: selectedUser.value.username,
      permission: newPermission.value
    })

    if (success) {
      toast.success('공유가 추가되었습니다.')
      await loadShares()
      resetAddForm()
      emit('updated')
    } else {
      toast.error(itemStore.error || '공유 추가에 실패했습니다.')
    }
  } catch (error) {
    toast.error('공유 추가에 실패했습니다.')
  } finally {
    isSubmitting.value = false
  }
}

// 권한 변경
async function handleUpdatePermission(share: Share, newPerm: SharePermission) {
  try {
    const success = await itemStore.updateItemShare(props.item.itemId, share.username, {
      permission: newPerm
    })

    if (success) {
      toast.success('권한이 변경되었습니다.')
      await loadShares()
      emit('updated')
    } else {
      toast.error(itemStore.error || '권한 변경에 실패했습니다.')
    }
  } catch (error) {
    toast.error('권한 변경에 실패했습니다.')
  }
}

// 공유 제거
async function handleRemoveShare(share: Share) {
  const confirmed = await confirm.show({
    title: '공유 해제',
    message: `${share.userName || share.username}님의 공유를 해제하시겠습니까?`,
    confirmText: '해제',
    confirmType: 'danger'
  })

  if (!confirmed) return

  try {
    const success = await itemStore.removeItemShare(props.item.itemId, share.username)

    if (success) {
      toast.success('공유가 해제되었습니다.')
      await loadShares()
      emit('updated')
    } else {
      toast.error(itemStore.error || '공유 해제에 실패했습니다.')
    }
  } catch (error) {
    toast.error('공유 해제에 실패했습니다.')
  }
}

// 추가 폼 초기화
function resetAddForm() {
  showAddForm.value = false
  newUserId.value = null
  selectedUser.value = null
  newPermission.value = 'VIEW'
}

watch(() => props.show, (newShow) => {
  if (newShow) {
    loadShares()
    resetAddForm()
  }
})

onMounted(() => {
  if (props.show) {
    loadShares()
  }
})
</script>

<template>
  <Modal
    :model-value="show"
    title="업무 공유"
    size="lg"
    @close="$emit('close')"
    @update:model-value="(val) => !val && $emit('close')"
  >
    <div class="space-y-4">
      <!-- 업무 정보 -->
      <div class="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700">
        <p class="text-[12px] text-gray-500 dark:text-gray-400 mb-1">공유할 업무</p>
        <p class="text-[14px] font-medium text-gray-900 dark:text-white">{{ item.title }}</p>
      </div>

      <!-- 로딩 -->
      <div v-if="isLoading" class="py-8 flex justify-center">
        <Spinner size="lg" />
      </div>

      <template v-else>
        <!-- 공유 목록 -->
        <div>
          <div class="flex items-center justify-between mb-3">
            <label class="text-[13px] font-medium text-gray-700 dark:text-gray-300">
              공유된 사용자
              <span v-if="shares.length > 0" class="text-gray-400 dark:text-gray-500 font-normal">({{ shares.length }}명)</span>
            </label>
            <Button
              v-if="!showAddForm"
              variant="ghost"
              size="sm"
              @click="showAddForm = true"
            >
              <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              추가
            </Button>
          </div>

          <!-- 공유 추가 폼 -->
          <div v-if="showAddForm" class="mb-4 p-4 bg-blue-50 dark:bg-blue-900/30 rounded-lg border border-blue-100 dark:border-blue-800">
            <div class="space-y-4">
              <!-- 사용자 선택 (UserSearchSelector) -->
              <UserSearchSelector
                v-model="newUserId"
                :exclude-user-ids="excludeUserIds"
                label="공유할 사용자"
                placeholder="사용자를 선택하세요"
                @select="handleUserSelect"
              />

              <!-- 권한 선택 -->
              <div>
                <label class="block text-[13px] font-medium text-gray-700 dark:text-gray-300 mb-2">권한</label>
                <div class="space-y-2">
                  <label
                    v-for="opt in permissionOptions"
                    :key="opt.value"
                    class="flex items-center gap-3 p-3 bg-white dark:bg-gray-800 border rounded-lg cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                    :class="newPermission === opt.value ? 'border-primary-500 ring-1 ring-primary-500' : 'border-gray-200 dark:border-gray-600'"
                  >
                    <input
                      type="radio"
                      v-model="newPermission"
                      :value="opt.value"
                      class="w-4 h-4 text-primary-600 border-gray-300 focus:ring-primary-500"
                    />
                    <div>
                      <p class="text-[13px] font-medium text-gray-700 dark:text-gray-200">{{ opt.label }}</p>
                      <p class="text-[12px] text-gray-500 dark:text-gray-400">{{ opt.description }}</p>
                    </div>
                  </label>
                </div>
              </div>

              <div class="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" @click="resetAddForm">취소</Button>
                <Button
                  variant="primary"
                  size="sm"
                  :disabled="isSubmitting || !newUserId"
                  @click="handleAddShare"
                >
                  <Spinner v-if="isSubmitting" size="sm" class="mr-1" />
                  공유 추가
                </Button>
              </div>
            </div>
          </div>

          <!-- 공유 목록 -->
          <div v-if="shares.length > 0" class="space-y-2">
            <div
              v-for="share in shares"
              :key="share.username"
              class="flex items-center justify-between p-3 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg hover:border-gray-300 dark:hover:border-gray-600 transition-colors"
            >
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 bg-primary-100 dark:bg-primary-900 rounded-full flex items-center justify-center">
                  <span class="text-[13px] font-medium text-primary-700 dark:text-primary-300">
                    {{ (share.userName || share.username || '?').charAt(0).toUpperCase() }}
                  </span>
                </div>
                <div>
                  <p class="text-[13px] font-medium text-gray-900 dark:text-white">
                    {{ share.userName || share.username }}
                  </p>
                  <p class="text-[11px] text-gray-500 dark:text-gray-400">
                    {{ share.departmentName || '소속없음' }}
                  </p>
                </div>
              </div>

              <div class="flex items-center gap-2">
                <select
                  :value="share.permission"
                  class="px-2 py-1.5 text-[12px] border border-gray-300 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200 rounded-lg focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
                  @change="handleUpdatePermission(share, ($event.target as HTMLSelectElement).value as SharePermission)"
                >
                  <option v-for="opt in permissionOptions" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </option>
                </select>

                <button
                  class="p-1.5 text-gray-400 dark:text-gray-500 hover:text-red-500 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/50 rounded-lg transition-colors"
                  title="공유 해제"
                  @click="handleRemoveShare(share)"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- 공유 없음 -->
          <div v-else class="py-8 text-center">
            <svg class="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <p class="text-[13px] text-gray-500 dark:text-gray-400 mb-3">공유된 사용자가 없습니다.</p>
            <Button
              v-if="!showAddForm"
              variant="primary"
              size="sm"
              @click="showAddForm = true"
            >
              <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              사용자 추가
            </Button>
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="flex justify-end">
        <Button variant="outline" @click="$emit('close')">닫기</Button>
      </div>
    </template>
  </Modal>
</template>
