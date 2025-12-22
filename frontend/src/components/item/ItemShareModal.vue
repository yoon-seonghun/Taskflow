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
const newUserId = ref<number | null>(null)
const selectedUser = ref<User | null>(null)
const newPermission = ref<SharePermission>('VIEW')

// 권한 옵션
const permissionOptions: { value: SharePermission; label: string; description: string }[] = [
  { value: 'VIEW', label: '조회', description: '업무 내용 조회만 가능' },
  { value: 'EDIT', label: '편집', description: '업무 내용 수정 가능' },
  { value: 'FULL', label: '전체', description: '이관/공유/삭제 포함 전체 권한' }
]

// 이미 공유된 사용자 ID 목록
const excludeUserIds = computed(() => {
  return shares.value.map(s => s.userId)
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
  if (!newUserId.value) {
    toast.error('공유할 사용자를 선택해주세요.')
    return
  }

  isSubmitting.value = true
  try {
    const success = await itemStore.addItemShare(props.item.itemId, {
      userId: newUserId.value,
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
    const success = await itemStore.updateItemShare(props.item.itemId, share.userId, {
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
    message: `${share.userName || share.loginId}님의 공유를 해제하시겠습니까?`,
    confirmText: '해제',
    confirmType: 'danger'
  })

  if (!confirmed) return

  try {
    const success = await itemStore.removeItemShare(props.item.itemId, share.userId)

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
      <div class="p-3 bg-gray-50 rounded-lg border border-gray-200">
        <p class="text-[12px] text-gray-500 mb-1">공유할 업무</p>
        <p class="text-[14px] font-medium text-gray-900">{{ item.title }}</p>
      </div>

      <!-- 로딩 -->
      <div v-if="isLoading" class="py-8 flex justify-center">
        <Spinner size="lg" />
      </div>

      <template v-else>
        <!-- 공유 목록 -->
        <div>
          <div class="flex items-center justify-between mb-3">
            <label class="text-[13px] font-medium text-gray-700">
              공유된 사용자
              <span v-if="shares.length > 0" class="text-gray-400 font-normal">({{ shares.length }}명)</span>
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
          <div v-if="showAddForm" class="mb-4 p-4 bg-blue-50 rounded-lg border border-blue-100">
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
                <label class="block text-[13px] font-medium text-gray-700 mb-2">권한</label>
                <div class="space-y-2">
                  <label
                    v-for="opt in permissionOptions"
                    :key="opt.value"
                    class="flex items-center gap-3 p-3 bg-white border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
                    :class="newPermission === opt.value ? 'border-primary-500 ring-1 ring-primary-500' : 'border-gray-200'"
                  >
                    <input
                      type="radio"
                      v-model="newPermission"
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
              :key="share.userId"
              class="flex items-center justify-between p-3 bg-white border border-gray-200 rounded-lg hover:border-gray-300 transition-colors"
            >
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 bg-primary-100 rounded-full flex items-center justify-center">
                  <span class="text-[13px] font-medium text-primary-700">
                    {{ (share.userName || share.loginId || '?').charAt(0).toUpperCase() }}
                  </span>
                </div>
                <div>
                  <p class="text-[13px] font-medium text-gray-900">
                    {{ share.userName || share.loginId }}
                  </p>
                  <p class="text-[11px] text-gray-500">
                    {{ share.departmentName || '소속없음' }}
                  </p>
                </div>
              </div>

              <div class="flex items-center gap-2">
                <select
                  :value="share.permission"
                  class="px-2 py-1.5 text-[12px] border border-gray-300 rounded-lg focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
                  @change="handleUpdatePermission(share, ($event.target as HTMLSelectElement).value as SharePermission)"
                >
                  <option v-for="opt in permissionOptions" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </option>
                </select>

                <button
                  class="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
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
            <svg class="w-12 h-12 mx-auto text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <p class="text-[13px] text-gray-500 mb-3">공유된 사용자가 없습니다.</p>
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
