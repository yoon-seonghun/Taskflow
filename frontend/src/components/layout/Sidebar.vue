<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useBoardStore } from '@/stores/board'
import type { Board } from '@/types/board'

defineProps<{
  open: boolean
  isMobile?: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const route = useRoute()
const router = useRouter()
const boardStore = useBoardStore()

// 보드 목록 로드
onMounted(() => {
  if (boardStore.boards.length === 0) {
    boardStore.fetchBoards()
  }
})

// 보드 분류
// 내 보드 (공유하지 않은)
const myPrivateBoards = computed(() =>
  boardStore.boards.filter(b => b.isOwner === true && (b.shareCount || 0) === 0)
)

// 공유해준 보드 (내 보드인데 다른 사람에게 공유)
const mySharedBoards = computed(() =>
  boardStore.boards.filter(b => b.isOwner === true && (b.shareCount || 0) > 0)
)

// 공유받은 보드 (다른 사람이 나에게 공유)
const receivedBoards = computed(() =>
  boardStore.boards.filter(b => b.isOwner !== true)
)

// 현재 선택된 보드 ID
const currentBoardId = computed(() => boardStore.currentBoard?.boardId)

// 섹션 접기/펼치기 상태
const showMyBoards = ref(true)
const showMySharedBoards = ref(true)
const showReceivedBoards = ref(true)

// 메뉴 항목 클릭 시 모바일에서 사이드바 닫기
function handleMenuClick() {
  emit('close')
}

// 보드 선택 및 페이지 이동
function selectBoard(board: Board) {
  boardStore.setCurrentBoard(board)
  router.push({ name: 'Tasks' })
  emit('close')
}

// 보드 색상 가져오기
function getBoardColor(board: Board): string {
  return board.boardColor || board.color || '#9CA3AF'
}

const menuItems = [
  { name: 'Tasks', label: '업무 페이지', icon: 'clipboard' },
  { name: 'Overdue', label: '지연 업무', icon: 'alert', warning: true },
  { name: 'Pending', label: '보류 업무', icon: 'pause' },
  { name: 'Completed', label: '완료 작업', icon: 'check' },
  { name: 'Deleted', label: '삭제된 작업', icon: 'trash' },
  { divider: true },
  { name: 'Templates', label: '작업 등록', icon: 'document' },
  { name: 'History', label: '이력관리', icon: 'history' },
  { divider: true },
  { name: 'Users', label: '사용자 등록', icon: 'user' },
  { name: 'Shares', label: '공유 사용자', icon: 'users' },
  { divider: true },
  { name: 'Departments', label: '부서 관리', icon: 'building' },
  { name: 'Groups', label: '그룹 관리', icon: 'folder' },
  { name: 'Boards', label: '보드 관리', icon: 'board' }
]
</script>

<template>
  <!-- 모바일 오버레이 배경 -->
  <Transition
    enter-active-class="transition-opacity duration-300"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition-opacity duration-200"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div
      v-if="open && isMobile"
      class="fixed inset-0 bg-black/30 z-30 md:hidden"
      @click="emit('close')"
    />
  </Transition>

  <aside
    class="fixed left-0 top-14 bottom-0 w-60 bg-white border-r border-gray-200 transition-transform duration-300 z-40"
    :class="[
      open ? 'translate-x-0' : '-translate-x-full',
      { 'md:translate-x-0': open }
    ]"
  >
    <nav class="p-2">
      <template v-for="(item, index) in menuItems" :key="index">
        <div v-if="item.divider" class="my-2 border-t border-gray-100" />
        <RouterLink
          v-else
          :to="{ name: item.name }"
          class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors"
          :class="route.name === item.name
            ? 'bg-primary-50 text-primary-700'
            : item.warning
              ? 'text-red-600 hover:bg-red-50'
              : 'text-gray-700 hover:bg-gray-100'"
          @click="handleMenuClick"
        >
          <!-- Icons -->
          <span class="w-5 h-5 flex items-center justify-center">
            <!-- Clipboard -->
            <svg v-if="item.icon === 'clipboard'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
            </svg>
            <!-- Alert (지연) -->
            <svg v-else-if="item.icon === 'alert'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <!-- Pause (보류) -->
            <svg v-else-if="item.icon === 'pause'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <!-- Check -->
            <svg v-else-if="item.icon === 'check'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <!-- Document -->
            <svg v-else-if="item.icon === 'document'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <!-- History -->
            <svg v-else-if="item.icon === 'history'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <!-- User -->
            <svg v-else-if="item.icon === 'user'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <!-- Users -->
            <svg v-else-if="item.icon === 'users'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            <!-- Building -->
            <svg v-else-if="item.icon === 'building'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
            <!-- Folder -->
            <svg v-else-if="item.icon === 'folder'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
            </svg>
            <!-- Trash -->
            <svg v-else-if="item.icon === 'trash'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            <!-- Board -->
            <svg v-else-if="item.icon === 'board'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2" />
            </svg>
          </span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </template>

      <!-- 보드 목록 섹션 -->
      <div v-if="boardStore.boards.length > 0" class="mt-1">
        <div class="my-2 border-t border-gray-100" />

        <!-- 내 보드 섹션 -->
        <template v-if="myPrivateBoards.length > 0">
          <div
            class="flex items-center gap-1 px-3 py-1.5 cursor-pointer select-none hover:bg-gray-50 rounded"
            @click="showMyBoards = !showMyBoards"
          >
            <svg
              class="w-3 h-3 text-gray-400 transition-transform duration-200"
              :class="{ '-rotate-90': !showMyBoards }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
            <span class="text-xs font-medium text-gray-500">내 보드 ({{ myPrivateBoards.length }})</span>
          </div>
          <div v-show="showMyBoards" class="space-y-0.5">
            <div
              v-for="board in myPrivateBoards"
              :key="board.boardId"
              class="flex items-center gap-2 px-3 pl-6 py-1.5 cursor-pointer rounded-md mx-1 transition-colors"
              :class="currentBoardId === board.boardId
                ? 'bg-primary-50 text-primary-700'
                : 'text-gray-700 hover:bg-gray-100'"
              @click="selectBoard(board)"
            >
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :style="{ backgroundColor: getBoardColor(board) }"
              />
              <span class="text-sm truncate">{{ board.boardName }}</span>
            </div>
          </div>
        </template>

        <!-- 공유해준 보드 섹션 -->
        <template v-if="mySharedBoards.length > 0">
          <div
            class="flex items-center gap-1 px-3 py-1.5 cursor-pointer select-none hover:bg-gray-50 rounded mt-1"
            @click="showMySharedBoards = !showMySharedBoards"
          >
            <svg
              class="w-3 h-3 text-gray-400 transition-transform duration-200"
              :class="{ '-rotate-90': !showMySharedBoards }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
            <span class="text-xs font-medium text-gray-500">공유해준 보드 ({{ mySharedBoards.length }})</span>
          </div>
          <div v-show="showMySharedBoards" class="space-y-0.5">
            <div
              v-for="board in mySharedBoards"
              :key="board.boardId"
              class="flex items-center gap-2 px-3 pl-6 py-1.5 cursor-pointer rounded-md mx-1 transition-colors"
              :class="currentBoardId === board.boardId
                ? 'bg-primary-50 text-primary-700'
                : 'text-gray-700 hover:bg-gray-100'"
              @click="selectBoard(board)"
            >
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :style="{ backgroundColor: getBoardColor(board) }"
              />
              <span class="text-sm truncate flex-1">{{ board.boardName }}</span>
              <span class="flex items-center gap-0.5 text-xs text-gray-400">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
                {{ board.shareCount }}
              </span>
            </div>
          </div>
        </template>

        <!-- 공유받은 보드 섹션 -->
        <template v-if="receivedBoards.length > 0">
          <div
            class="flex items-center gap-1 px-3 py-1.5 cursor-pointer select-none hover:bg-gray-50 rounded mt-1"
            @click="showReceivedBoards = !showReceivedBoards"
          >
            <svg
              class="w-3 h-3 text-gray-400 transition-transform duration-200"
              :class="{ '-rotate-90': !showReceivedBoards }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
            <span class="text-xs font-medium text-gray-500">공유받은 보드 ({{ receivedBoards.length }})</span>
          </div>
          <div v-show="showReceivedBoards" class="space-y-0.5">
            <div
              v-for="board in receivedBoards"
              :key="board.boardId"
              class="flex items-center gap-2 px-3 pl-6 py-1.5 cursor-pointer rounded-md mx-1 transition-colors"
              :class="currentBoardId === board.boardId
                ? 'bg-primary-50 text-primary-700'
                : 'text-gray-700 hover:bg-gray-100'"
              @click="selectBoard(board)"
            >
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :style="{ backgroundColor: getBoardColor(board) }"
              />
              <span class="text-sm truncate flex-1">{{ board.boardName }}</span>
              <span class="text-xs text-gray-400 truncate max-w-[60px]">@{{ board.ownerName }}</span>
            </div>
          </div>
        </template>
      </div>
    </nav>
  </aside>
</template>
