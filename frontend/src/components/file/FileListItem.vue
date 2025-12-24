<script setup lang="ts">
/**
 * 파일 목록 아이템 컴포넌트
 * - 파일 아이콘, 이름, 크기 표시
 * - 다운로드, 삭제 버튼
 */
import { computed } from 'vue'
import { fileApi } from '@/api/file'
import { getFileIcon } from '@/types/file'
import type { FileInfoResponse } from '@/types/file'

interface Props {
  file: FileInfoResponse
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emit = defineEmits<{
  (e: 'delete', fileId: number): void
}>()

// 파일 아이콘 정보
const iconInfo = computed(() => getFileIcon(props.file.extension))

// 다운로드 URL
const downloadUrl = computed(() => fileApi.getFileUrl(props.file.fileId, true))

// 파일 다운로드
function handleDownload() {
  window.open(downloadUrl.value, '_blank')
}

// 파일 삭제
function handleDelete() {
  emit('delete', props.file.fileId)
}
</script>

<template>
  <div class="file-list-item flex items-center gap-2 px-2 py-1.5 hover:bg-gray-50 rounded group">
    <!-- 파일 아이콘 -->
    <div
      class="flex-shrink-0 w-8 h-8 rounded flex items-center justify-center"
      :class="iconInfo.bgColor"
    >
      <svg
        class="w-4 h-4"
        :class="iconInfo.color"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          :d="iconInfo.pathD"
        />
      </svg>
    </div>

    <!-- 파일 정보 -->
    <div class="flex-1 min-w-0">
      <div class="text-[13px] text-gray-900 truncate" :title="file.originalName">
        {{ file.originalName }}
      </div>
      <div class="text-[11px] text-gray-400">
        {{ file.fileSizeDisplay }}
      </div>
    </div>

    <!-- 액션 버튼 -->
    <div class="flex-shrink-0 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
      <!-- 다운로드 -->
      <button
        type="button"
        class="p-1 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded"
        title="다운로드"
        @click="handleDownload"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
        </svg>
      </button>

      <!-- 삭제 -->
      <button
        v-if="!disabled"
        type="button"
        class="p-1 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded"
        title="삭제"
        @click="handleDelete"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
      </button>
    </div>
  </div>
</template>
