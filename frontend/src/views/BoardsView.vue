<script setup lang="ts">
/**
 * 보드 관리 메뉴 (Boards View)
 * - 보드 목록 조회 (소유/공유 분리)
 * - 보드 CRUD
 * - 보드 삭제 시 업무 이관
 */
import { ref, computed, onMounted } from 'vue'
import { useBoardStore } from '@/stores/board'
import { useUiStore } from '@/stores/ui'
import { userApi } from '@/api/user'
import ShareUserSearch from '@/components/share/ShareUserSearch.vue'
import ShareUserList from '@/components/share/ShareUserList.vue'
import type { Board, BoardListResponse, BoardCreateRequest, TransferPreviewResponse, BoardShare, BoardShareRequest } from '@/types/board'
import type { User } from '@/types/user'

const boardStore = useBoardStore()
const uiStore = useUiStore()

// 상태
const loading = ref(false)
const boardList = ref<BoardListResponse | null>(null)
const users = ref<User[]>([])

// 모달 상태
const showCreateModal = ref(false)
const showEditModal = ref(false)
const showDeleteModal = ref(false)
const showShareModal = ref(false)
const selectedBoard = ref<Board | null>(null)
const transferPreview = ref<TransferPreviewResponse | null>(null)

// 공유 관련
const boardShares = ref<BoardShare[]>([])
const sharesLoading = ref(false)
const shareAdding = ref(false)

// 생성/수정 폼
const formData = ref({
  boardName: '',
  description: '',
  color: '#3B82F6'
})

// 삭제 폼
const deleteFormData = ref({
  targetUserId: null as number | null,
  forceDelete: false
})

// 색상 옵션
const colorOptions = [
  '#3B82F6', // Blue
  '#10B981', // Green
  '#F59E0B', // Yellow
  '#EF4444', // Red
  '#8B5CF6', // Purple
  '#EC4899', // Pink
  '#6366F1', // Indigo
  '#14B8A6', // Teal
  '#F97316', // Orange
  '#6B7280'  // Gray
]

// 초기 로드
onMounted(async () => {
  await loadBoards()
  await loadUsers()
})

// 보드 목록 로드
async function loadBoards() {
  loading.value = true
  try {
    boardList.value = await boardStore.fetchBoardList()
  } catch (error) {
    console.error('Failed to load boards:', error)
    uiStore.showError('보드 목록을 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

// 사용자 목록 로드 (이관 대상자 선택용)
async function loadUsers() {
  try {
    const response = await userApi.getUsers({ useYn: 'Y' })
    if (response.success && response.data && response.data.content) {
      users.value = response.data.content
    }
  } catch (error) {
    console.error('Failed to load users:', error)
  }
}

// 보드 생성 모달 열기
function openCreateModal() {
  formData.value = {
    boardName: '',
    description: '',
    color: '#3B82F6'
  }
  showCreateModal.value = true
}

// 보드 수정 모달 열기
function openEditModal(board: Board) {
  selectedBoard.value = board
  formData.value = {
    boardName: board.boardName,
    description: board.description || '',
    color: board.color || '#3B82F6'
  }
  showEditModal.value = true
}

// 보드 삭제 모달 열기
async function openDeleteModal(board: Board) {
  selectedBoard.value = board
  deleteFormData.value = {
    targetUserId: null,
    forceDelete: false
  }

  // 미완료 업무 미리보기 로드
  try {
    transferPreview.value = await boardStore.getTransferPreview(board.boardId)
  } catch (error) {
    console.error('Failed to load transfer preview:', error)
    transferPreview.value = null
  }

  showDeleteModal.value = true
}

// 보드 생성
async function handleCreate() {
  if (!formData.value.boardName.trim()) {
    uiStore.showError('보드 이름을 입력해주세요.')
    return
  }

  try {
    const request: BoardCreateRequest = {
      boardName: formData.value.boardName,
      boardDescription: formData.value.description,
      boardColor: formData.value.color
    }

    const result = await boardStore.createBoard(request)
    if (result) {
      uiStore.showSuccess('보드가 생성되었습니다.')
      showCreateModal.value = false
      await loadBoards()
    } else {
      uiStore.showError(boardStore.error || '보드 생성에 실패했습니다.')
    }
  } catch (error) {
    console.error('Failed to create board:', error)
    uiStore.showError('보드 생성에 실패했습니다.')
  }
}

// 보드 수정
async function handleUpdate() {
  if (!selectedBoard.value) return
  if (!formData.value.boardName.trim()) {
    uiStore.showError('보드 이름을 입력해주세요.')
    return
  }

  try {
    const success = await boardStore.updateBoard(selectedBoard.value.boardId, {
      boardName: formData.value.boardName,
      boardDescription: formData.value.description,
      boardColor: formData.value.color
    })

    if (success) {
      uiStore.showSuccess('보드가 수정되었습니다.')
      showEditModal.value = false
      await loadBoards()
    } else {
      uiStore.showError(boardStore.error || '보드 수정에 실패했습니다.')
    }
  } catch (error) {
    console.error('Failed to update board:', error)
    uiStore.showError('보드 수정에 실패했습니다.')
  }
}

// 보드 삭제
async function handleDelete() {
  if (!selectedBoard.value) return

  // 미완료 업무가 있는데 이관 대상자가 없고 강제 삭제도 아닌 경우
  if (transferPreview.value && transferPreview.value.totalCount > 0) {
    if (!deleteFormData.value.targetUserId && !deleteFormData.value.forceDelete) {
      uiStore.showError('미완료 업무가 있습니다. 이관 대상자를 선택하거나 강제 삭제를 선택해주세요.')
      return
    }
  }

  try {
    const result = await boardStore.deleteBoardWithTransfer(
      selectedBoard.value.boardId,
      {
        targetUserId: deleteFormData.value.targetUserId || undefined,
        forceDelete: deleteFormData.value.forceDelete
      }
    )

    if (result) {
      uiStore.showSuccess(`보드가 삭제되었습니다. ${result.message || ''}`)
    } else {
      uiStore.showSuccess('보드가 삭제되었습니다.')
    }

    showDeleteModal.value = false
    await loadBoards()
  } catch (error) {
    console.error('Failed to delete board:', error)
    uiStore.showError(boardStore.error || '보드 삭제에 실패했습니다.')
  }
}

// 권한 라벨
function getPermissionLabel(permission: string): string {
  switch (permission) {
    case 'OWNER': return '소유자'
    case 'VIEW': return '조회'
    case 'EDIT': return '편집'
    case 'FULL': return '전체'
    default: return permission
  }
}

// 공유 모달 열기
async function openShareModal(board: Board) {
  selectedBoard.value = board
  boardShares.value = []
  showShareModal.value = true
  await loadBoardShares(board.boardId)
}

// 공유 사용자 목록 로드
async function loadBoardShares(boardId: number) {
  sharesLoading.value = true
  try {
    boardShares.value = await boardStore.fetchBoardShares(boardId)
  } catch (error) {
    console.error('Failed to load board shares:', error)
    uiStore.showError('공유 사용자 목록을 불러오는데 실패했습니다.')
  } finally {
    sharesLoading.value = false
  }
}

// 공유된 사용자 ID 목록 (검색 시 제외용)
const existingShareUserIds = computed(() => {
  return boardShares.value.map(share => share.userId)
})

// 공유 사용자 추가
async function handleAddShare(data: BoardShareRequest) {
  if (!selectedBoard.value) return

  shareAdding.value = true
  try {
    const result = await boardStore.addBoardShare(selectedBoard.value.boardId, data)
    if (result) {
      uiStore.showSuccess('공유 사용자가 추가되었습니다.')
      await loadBoardShares(selectedBoard.value.boardId)
      await loadBoards() // 공유 카운트 갱신
    } else {
      uiStore.showError(boardStore.error || '공유 사용자 추가에 실패했습니다.')
    }
  } catch (error) {
    console.error('Failed to add share:', error)
    uiStore.showError('공유 사용자 추가에 실패했습니다.')
  } finally {
    shareAdding.value = false
  }
}

// 공유 사용자 제거
async function handleRemoveShare(share: BoardShare) {
  if (!selectedBoard.value) return

  if (!confirm(`${share.userName}님의 공유를 해제하시겠습니까?`)) {
    return
  }

  try {
    const success = await boardStore.removeBoardShare(selectedBoard.value.boardId, share.userId)
    if (success) {
      uiStore.showSuccess('공유가 해제되었습니다.')
      await loadBoardShares(selectedBoard.value.boardId)
      await loadBoards() // 공유 카운트 갱신
    } else {
      uiStore.showError(boardStore.error || '공유 해제에 실패했습니다.')
    }
  } catch (error) {
    console.error('Failed to remove share:', error)
    uiStore.showError('공유 해제에 실패했습니다.')
  }
}
</script>

<template>
  <div class="boards-view">
    <!-- 헤더 -->
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-gray-900">보드 관리</h1>
        <p class="mt-1 text-sm text-gray-500">
          보드를 관리하고 공유 설정을 관리합니다.
        </p>
      </div>
      <button
        @click="openCreateModal"
        class="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        새 보드
      </button>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <svg class="animate-spin h-8 w-8 text-blue-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>

    <template v-else-if="boardList">
      <!-- 소유한 보드 -->
      <div class="mb-8">
        <h2 class="text-lg font-medium text-gray-900 mb-4">
          내 보드 ({{ boardList.totalOwnedCount }}개)
        </h2>

        <div v-if="boardList.ownedBoards.length === 0" class="text-center py-8 bg-gray-50 rounded-lg">
          <p class="text-gray-500">소유한 보드가 없습니다.</p>
          <button @click="openCreateModal" class="mt-2 text-blue-600 hover:text-blue-700 text-sm font-medium">
            새 보드 만들기
          </button>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="board in boardList.ownedBoards"
            :key="board.boardId"
            class="board-card bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow"
          >
            <div class="flex items-start justify-between mb-3">
              <div class="flex items-center gap-3">
                <div
                  class="w-3 h-3 rounded-full"
                  :style="{ backgroundColor: board.color || '#3B82F6' }"
                />
                <h3 class="font-medium text-gray-900">{{ board.boardName }}</h3>
              </div>
              <div class="flex items-center gap-1">
                <button
                  @click="openShareModal(board)"
                  class="p-1.5 text-gray-400 hover:text-blue-600 rounded"
                  title="공유 관리"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </button>
                <button
                  @click="openEditModal(board)"
                  class="p-1.5 text-gray-400 hover:text-gray-600 rounded"
                  title="수정"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </button>
                <button
                  @click="openDeleteModal(board)"
                  class="p-1.5 text-gray-400 hover:text-red-600 rounded"
                  title="삭제"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>

            <p v-if="board.description" class="text-sm text-gray-500 mb-3 line-clamp-2">
              {{ board.description }}
            </p>

            <div class="flex items-center gap-4 text-sm text-gray-500">
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
                {{ board.itemCount || 0 }}건
              </span>
              <span v-if="board.pendingItemCount" class="flex items-center gap-1 text-orange-600">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                미완료 {{ board.pendingItemCount }}건
              </span>
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
                공유 {{ board.shareCount || 0 }}명
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 공유받은 보드 -->
      <div>
        <h2 class="text-lg font-medium text-gray-900 mb-4">
          공유받은 보드 ({{ boardList.totalSharedCount }}개)
        </h2>

        <div v-if="boardList.sharedBoards.length === 0" class="text-center py-8 bg-gray-50 rounded-lg">
          <p class="text-gray-500">공유받은 보드가 없습니다.</p>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="board in boardList.sharedBoards"
            :key="board.boardId"
            class="board-card bg-white rounded-lg border border-gray-200 p-4"
          >
            <div class="flex items-start justify-between mb-3">
              <div class="flex items-center gap-3">
                <div
                  class="w-3 h-3 rounded-full"
                  :style="{ backgroundColor: board.color || '#3B82F6' }"
                />
                <h3 class="font-medium text-gray-900">{{ board.boardName }}</h3>
              </div>
              <span
                class="px-2 py-0.5 text-xs font-medium rounded-full"
                :class="{
                  'bg-gray-100 text-gray-700': board.currentUserPermission === 'VIEW',
                  'bg-blue-100 text-blue-700': board.currentUserPermission === 'EDIT',
                  'bg-green-100 text-green-700': board.currentUserPermission === 'FULL'
                }"
              >
                {{ getPermissionLabel(board.currentUserPermission || 'VIEW') }}
              </span>
            </div>

            <p v-if="board.description" class="text-sm text-gray-500 mb-3 line-clamp-2">
              {{ board.description }}
            </p>

            <div class="flex items-center justify-between text-sm">
              <span class="text-gray-500">
                소유자: {{ board.ownerName }}
              </span>
              <span class="flex items-center gap-1 text-gray-500">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
                {{ board.itemCount || 0 }}건
              </span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 보드 생성 모달 -->
    <Teleport to="body">
      <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="showCreateModal = false" />
        <div class="relative bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">새 보드 만들기</h3>

          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">보드 이름 *</label>
              <input
                v-model="formData.boardName"
                type="text"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="보드 이름을 입력하세요"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
              <textarea
                v-model="formData.description"
                rows="3"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="보드 설명을 입력하세요"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">색상</label>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="color in colorOptions"
                  :key="color"
                  @click="formData.color = color"
                  class="w-8 h-8 rounded-full border-2 transition-transform hover:scale-110"
                  :class="formData.color === color ? 'border-gray-900 scale-110' : 'border-transparent'"
                  :style="{ backgroundColor: color }"
                />
              </div>
            </div>
          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button
              @click="showCreateModal = false"
              class="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-lg"
            >
              취소
            </button>
            <button
              @click="handleCreate"
              class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg"
            >
              생성
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 보드 수정 모달 -->
    <Teleport to="body">
      <div v-if="showEditModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="showEditModal = false" />
        <div class="relative bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">보드 수정</h3>

          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">보드 이름 *</label>
              <input
                v-model="formData.boardName"
                type="text"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
              <textarea
                v-model="formData.description"
                rows="3"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">색상</label>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="color in colorOptions"
                  :key="color"
                  @click="formData.color = color"
                  class="w-8 h-8 rounded-full border-2 transition-transform hover:scale-110"
                  :class="formData.color === color ? 'border-gray-900 scale-110' : 'border-transparent'"
                  :style="{ backgroundColor: color }"
                />
              </div>
            </div>
          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button
              @click="showEditModal = false"
              class="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-lg"
            >
              취소
            </button>
            <button
              @click="handleUpdate"
              class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg"
            >
              저장
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 보드 삭제 모달 -->
    <Teleport to="body">
      <div v-if="showDeleteModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="showDeleteModal = false" />
        <div class="relative bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">보드 삭제</h3>

          <div class="mb-4">
            <p class="text-gray-700">
              <span class="font-medium">{{ selectedBoard?.boardName }}</span> 보드를 삭제하시겠습니까?
            </p>
          </div>

          <!-- 미완료 업무가 있는 경우 -->
          <div v-if="transferPreview && transferPreview.totalCount > 0" class="mb-4">
            <div class="p-4 bg-yellow-50 rounded-lg border border-yellow-200">
              <div class="flex items-start gap-3">
                <svg class="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <div>
                  <p class="text-sm font-medium text-yellow-800">
                    미완료 업무가 {{ transferPreview.totalCount }}건 있습니다.
                  </p>
                  <p class="mt-1 text-sm text-yellow-700">
                    삭제 전에 업무를 다른 사용자에게 이관하거나, 강제 삭제를 선택할 수 있습니다.
                  </p>
                </div>
              </div>
            </div>

            <div class="mt-4 space-y-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">업무 이관 대상자</label>
                <select
                  v-model="deleteFormData.targetUserId"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                >
                  <option :value="null">선택 안함</option>
                  <option v-for="user in users" :key="user.userId" :value="user.userId">
                    {{ user.name }} ({{ user.departmentName || '-' }})
                  </option>
                </select>
                <p class="mt-1 text-xs text-gray-500">
                  이관 대상자를 선택하면 미완료 업무가 해당 사용자의 새 보드로 이동됩니다.
                </p>
              </div>

              <label class="flex items-center gap-2">
                <input
                  v-model="deleteFormData.forceDelete"
                  type="checkbox"
                  class="w-4 h-4 text-red-600 border-gray-300 rounded focus:ring-red-500"
                />
                <span class="text-sm text-gray-700">이관 없이 강제 삭제 (미완료 업무 포함 삭제)</span>
              </label>
            </div>
          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button
              @click="showDeleteModal = false"
              class="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-lg"
            >
              취소
            </button>
            <button
              @click="handleDelete"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg"
            >
              삭제
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 공유 관리 모달 -->
    <Teleport to="body">
      <div v-if="showShareModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="showShareModal = false" />
        <div class="relative bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-hidden flex flex-col">
          <!-- 모달 헤더 -->
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">공유 관리</h3>
              <p class="mt-0.5 text-sm text-gray-500">
                <span class="font-medium">{{ selectedBoard?.boardName }}</span> 보드
              </p>
            </div>
            <button
              @click="showShareModal = false"
              class="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 모달 본문 -->
          <div class="flex-1 overflow-y-auto p-6 space-y-6">
            <!-- 사용자 추가 -->
            <ShareUserSearch
              :existing-user-ids="existingShareUserIds"
              :loading="shareAdding"
              @add="handleAddShare"
            />

            <!-- 공유 사용자 목록 -->
            <div>
              <h4 class="text-sm font-medium text-gray-700 mb-3">
                공유된 사용자 ({{ boardShares.length }}명)
              </h4>
              <ShareUserList
                :shares="boardShares"
                :loading="sharesLoading"
                @remove="handleRemoveShare"
              />
            </div>
          </div>

          <!-- 모달 푸터 -->
          <div class="flex justify-end px-6 py-4 border-t bg-gray-50">
            <button
              @click="showShareModal = false"
              class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              닫기
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.boards-view {
  @apply max-w-7xl mx-auto;
}

.board-card {
  min-height: 120px;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
