<script setup lang="ts">
/**
 * 공유 사용자 관리 메뉴 (Share Users View)
 * - 보드별 공유 사용자 관리
 * - 공유 사용자 추가/제거
 */
import { ref, computed, watch, onMounted } from 'vue'
import ShareUserSearch from '@/components/share/ShareUserSearch.vue'
import ShareUserList from '@/components/share/ShareUserList.vue'
import Select from '@/components/common/Select.vue'
import type { SelectOption } from '@/components/common/Select.vue'
import type { BoardShare, BoardShareRequest } from '@/types/board'
import { useBoardStore } from '@/stores/board'
import { useUiStore } from '@/stores/ui'

const boardStore = useBoardStore()
const uiStore = useUiStore()

// 선택된 보드 ID
const selectedBoardId = ref<number | null>(null)

// 로딩 상태
const loadingBoards = ref(false)
const loadingShares = ref(false)
const addingShare = ref(false)

// 보드 옵션 (소유한 보드만)
const boardOptions = computed<SelectOption[]>(() => {
  return boardStore.ownedBoards.map(board => ({
    value: board.boardId,
    label: board.boardName
  }))
})

// 현재 공유 사용자 목록
const shareUsers = computed(() => boardStore.boardShares)

// 이미 공유된 사용자 ID 목록
const existingUserIds = computed(() => {
  return shareUsers.value.map(share => share.userId)
})

// 보드 목록 로드
async function loadBoards() {
  loadingBoards.value = true
  try {
    await boardStore.fetchBoards()

    // 소유한 보드가 있으면 첫 번째 보드 선택
    if (boardStore.ownedBoards.length > 0 && !selectedBoardId.value) {
      selectedBoardId.value = boardStore.ownedBoards[0].boardId
    }
  } catch (error) {
    console.error('Failed to load boards:', error)
    uiStore.showError('보드 목록을 불러오는데 실패했습니다.')
  } finally {
    loadingBoards.value = false
  }
}

// 공유 사용자 목록 로드
async function loadShareUsers() {
  if (!selectedBoardId.value) return

  loadingShares.value = true
  try {
    await boardStore.fetchBoardShares(selectedBoardId.value)
  } catch (error) {
    console.error('Failed to load share users:', error)
    uiStore.showError('공유 사용자 목록을 불러오는데 실패했습니다.')
  } finally {
    loadingShares.value = false
  }
}

// 공유 사용자 추가
async function handleAddShare(data: BoardShareRequest) {
  if (!selectedBoardId.value) return

  addingShare.value = true
  try {
    const result = await boardStore.addBoardShare(selectedBoardId.value, data)
    if (result) {
      uiStore.showSuccess('공유 사용자가 추가되었습니다.')
    } else {
      uiStore.showError(boardStore.error || '공유 사용자 추가에 실패했습니다.')
    }
  } catch (error: any) {
    console.error('Failed to add share user:', error)
    const message = error.response?.data?.message || '공유 사용자 추가에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    addingShare.value = false
  }
}

// 공유 사용자 제거
async function handleRemoveShare(share: BoardShare) {
  if (!selectedBoardId.value) return

  const confirmed = await uiStore.confirm({
    title: '공유 해제',
    message: `'${share.userName}' 사용자의 공유를 해제하시겠습니까?`,
    confirmText: '해제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  try {
    const success = await boardStore.removeBoardShare(selectedBoardId.value, share.userId)
    if (success) {
      uiStore.showSuccess('공유가 해제되었습니다.')
    } else {
      uiStore.showError(boardStore.error || '공유 해제에 실패했습니다.')
    }
  } catch (error: any) {
    console.error('Failed to remove share user:', error)
    const message = error.response?.data?.message || '공유 해제에 실패했습니다.'
    uiStore.showError(message)
  }
}

// 보드 변경 시 공유 사용자 다시 로드
watch(selectedBoardId, (newBoardId) => {
  if (newBoardId) {
    loadShareUsers()
  }
})

// 초기 로드
onMounted(() => {
  loadBoards()
})
</script>

<template>
  <div class="share-users-view">
    <!-- 헤더 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">공유 사용자 관리</h1>
      <p class="mt-1 text-sm text-gray-500">
        보드별로 공유 사용자를 관리합니다. 공유된 사용자는 해당 보드의 업무를 조회/수정할 수 있습니다.
      </p>
    </div>

    <!-- 보드 선택 -->
    <div class="board-selector mb-6">
      <div class="flex items-center gap-4">
        <div class="flex-1 max-w-md">
          <Select
            v-model="selectedBoardId"
            :options="boardOptions"
            label="보드 선택"
            placeholder="공유할 보드를 선택하세요"
            :searchable="true"
            :disabled="loadingBoards"
          />
        </div>
        <div v-if="loadingBoards" class="flex items-center gap-2 text-sm text-gray-500">
          <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          보드 목록 로딩중...
        </div>
      </div>

      <!-- 보드가 없을 때 안내 -->
      <div v-if="!loadingBoards && boardOptions.length === 0" class="mt-4 p-4 bg-yellow-50 rounded-lg">
        <div class="flex items-start gap-3">
          <svg class="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <div>
            <p class="text-sm font-medium text-yellow-800">소유한 보드가 없습니다.</p>
            <p class="mt-1 text-sm text-yellow-700">
              공유 사용자를 관리하려면 보드 소유자여야 합니다.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 보드가 선택되었을 때 공유 관리 UI -->
    <template v-if="selectedBoardId">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- 왼쪽: 사용자 검색/추가 -->
        <div class="lg:col-span-1">
          <div class="sticky top-4">
            <ShareUserSearch
              :existing-user-ids="existingUserIds"
              :loading="addingShare"
              @add="handleAddShare"
            />
          </div>
        </div>

        <!-- 오른쪽: 공유 사용자 목록 -->
        <div class="lg:col-span-2">
          <div class="flex items-center justify-between mb-3">
            <h2 class="text-base font-medium text-gray-900">공유된 사용자</h2>
            <p class="text-sm text-gray-500">
              총 <span class="font-medium text-gray-900">{{ shareUsers.length }}</span>명
            </p>
          </div>

          <div class="table-container">
            <ShareUserList
              :shares="shareUsers"
              :loading="loadingShares"
              @remove="handleRemoveShare"
            />
          </div>
        </div>
      </div>
    </template>

    <!-- 보드가 선택되지 않았을 때 안내 -->
    <div v-else-if="!loadingBoards && boardOptions.length > 0" class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">공유할 보드를 선택하세요.</p>
    </div>
  </div>
</template>

<style scoped>
.share-users-view {
  @apply max-w-7xl mx-auto;
}

.board-selector {
  @apply p-4 bg-white rounded-lg border border-gray-200;
}

.table-container {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden;
}
</style>
