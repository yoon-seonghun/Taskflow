<script setup lang="ts">
/**
 * 충돌 해결 다이얼로그 컴포넌트
 * SSE로 감지된 동시 편집 충돌을 사용자에게 알리고 해결 옵션 제공
 */
import { computed } from 'vue'
import { useSseStore } from '@/stores/sse'
import { useItemStore } from '@/stores/item'
import { useSse } from '@/composables/useSse'

const sseStore = useSseStore()
const itemStore = useItemStore()
const { resolveKeepLocal, resolveUseServer, resolveIgnore } = useSse()

const isOpen = computed(() => sseStore.hasConflict)
const conflict = computed(() => sseStore.conflictInfo)

// 현재 보드 ID
const currentBoardId = computed(() => itemStore.currentBoardId)

// 날짜 포맷
function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 내 변경사항 유지
async function handleKeepLocal() {
  if (currentBoardId.value) {
    await resolveKeepLocal(currentBoardId.value)
  }
}

// 서버 버전 사용
function handleUseServer() {
  resolveUseServer()
}

// 충돌 무시
function handleIgnore() {
  resolveIgnore()
}
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="isOpen"
        class="conflict-overlay"
        @click.self="handleIgnore"
      >
        <div class="conflict-dialog">
          <!-- 헤더 -->
          <div class="dialog-header">
            <div class="header-icon">
              <svg class="w-6 h-6 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <div>
              <h3 class="dialog-title">편집 충돌 발생</h3>
              <p class="dialog-subtitle">다른 사용자가 같은 아이템을 수정했습니다.</p>
            </div>
          </div>

          <!-- 충돌 정보 -->
          <div v-if="conflict" class="conflict-info">
            <div class="info-row">
              <span class="info-label">수정자</span>
              <span class="info-value">{{ conflict.updatedBy }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">수정 시간</span>
              <span class="info-value">{{ formatDate(conflict.updatedAt) }}</span>
            </div>
          </div>

          <!-- 변경 내용 비교 (간략) -->
          <div v-if="conflict" class="comparison-section">
            <div class="comparison-box local">
              <div class="box-header">
                <span class="box-title">내 변경사항</span>
                <span class="box-badge">로컬</span>
              </div>
              <div class="box-content">
                <div v-if="conflict.localData.title" class="content-item">
                  <span class="item-label">제목:</span>
                  <span class="item-value">{{ conflict.localData.title }}</span>
                </div>
                <div v-if="conflict.localData.status" class="content-item">
                  <span class="item-label">상태:</span>
                  <span class="item-value">{{ conflict.localData.status }}</span>
                </div>
                <p v-if="!conflict.localData.title && !conflict.localData.status" class="text-xs text-gray-400">
                  (속성 변경)
                </p>
              </div>
            </div>

            <div class="comparison-box server">
              <div class="box-header">
                <span class="box-title">서버 버전</span>
                <span class="box-badge">최신</span>
              </div>
              <div class="box-content">
                <div v-if="conflict.serverData.title" class="content-item">
                  <span class="item-label">제목:</span>
                  <span class="item-value">{{ conflict.serverData.title }}</span>
                </div>
                <div v-if="conflict.serverData.status" class="content-item">
                  <span class="item-label">상태:</span>
                  <span class="item-value">{{ conflict.serverData.status }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 해결 옵션 -->
          <div class="resolution-section">
            <p class="resolution-label">해결 방법을 선택하세요:</p>
            <div class="resolution-buttons">
              <button
                type="button"
                class="btn btn-local"
                @click="handleKeepLocal"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M5 13l4 4L19 7" />
                </svg>
                내 변경사항 유지
              </button>
              <button
                type="button"
                class="btn btn-server"
                @click="handleUseServer"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                서버 버전 사용
              </button>
              <button
                type="button"
                class="btn btn-ignore"
                @click="handleIgnore"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M6 18L18 6M6 6l12 12" />
                </svg>
                무시하고 계속 편집
              </button>
            </div>
          </div>

          <!-- 안내 문구 -->
          <p class="help-text">
            '내 변경사항 유지'를 선택하면 서버의 변경사항을 덮어씁니다.
          </p>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.conflict-overlay {
  @apply fixed inset-0 z-50 flex items-center justify-center
         bg-black/50 backdrop-blur-sm;
}

.conflict-dialog {
  @apply w-full max-w-md mx-4 p-0 bg-white rounded-xl shadow-2xl;
}

.dialog-header {
  @apply flex items-start gap-3 p-4 border-b border-gray-200 bg-yellow-50 rounded-t-xl;
}

.header-icon {
  @apply flex-shrink-0 p-2 bg-yellow-100 rounded-full;
}

.dialog-title {
  @apply text-lg font-semibold text-gray-900;
}

.dialog-subtitle {
  @apply text-sm text-gray-600 mt-0.5;
}

.conflict-info {
  @apply px-4 py-3 bg-gray-50 border-b border-gray-100;
}

.info-row {
  @apply flex items-center justify-between text-sm;
}

.info-row + .info-row {
  @apply mt-1;
}

.info-label {
  @apply text-gray-500;
}

.info-value {
  @apply font-medium text-gray-700;
}

.comparison-section {
  @apply p-4 grid grid-cols-2 gap-3;
}

.comparison-box {
  @apply rounded-lg border overflow-hidden;
}

.comparison-box.local {
  @apply border-blue-200 bg-blue-50/50;
}

.comparison-box.server {
  @apply border-green-200 bg-green-50/50;
}

.box-header {
  @apply flex items-center justify-between px-3 py-1.5 bg-white/50;
}

.box-title {
  @apply text-xs font-medium text-gray-700;
}

.box-badge {
  @apply px-1.5 py-0.5 text-xs rounded;
}

.comparison-box.local .box-badge {
  @apply bg-blue-100 text-blue-700;
}

.comparison-box.server .box-badge {
  @apply bg-green-100 text-green-700;
}

.box-content {
  @apply p-3 min-h-[60px];
}

.content-item {
  @apply text-sm;
}

.content-item + .content-item {
  @apply mt-1;
}

.item-label {
  @apply text-gray-500 mr-1;
}

.item-value {
  @apply text-gray-900;
}

.resolution-section {
  @apply px-4 py-3 border-t border-gray-100;
}

.resolution-label {
  @apply text-sm font-medium text-gray-700 mb-3;
}

.resolution-buttons {
  @apply flex flex-col gap-2;
}

.btn {
  @apply flex items-center justify-center gap-2 w-full px-4 py-2.5
         text-sm font-medium rounded-lg transition-colors;
}

.btn-local {
  @apply bg-blue-600 text-white hover:bg-blue-700;
}

.btn-server {
  @apply bg-white text-gray-700 border border-gray-300 hover:bg-gray-50;
}

.btn-ignore {
  @apply bg-white text-gray-500 border border-gray-200 hover:bg-gray-50;
}

.help-text {
  @apply px-4 pb-4 text-xs text-gray-400 text-center;
}

/* 애니메이션 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-enter-active .conflict-dialog {
  animation: dialog-in 0.2s ease-out;
}

@keyframes dialog-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
