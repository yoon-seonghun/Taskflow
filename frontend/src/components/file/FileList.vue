<script setup lang="ts">
/**
 * 파일 목록 컴포넌트
 * - 첨부된 파일 목록 표시
 * - 스크롤 가능한 영역 (max-height 제한)
 */
import FileListItem from './FileListItem.vue'
import type { FileInfoResponse } from '@/types/file'

interface Props {
  files: FileInfoResponse[]
  disabled?: boolean
  maxHeight?: string
}

withDefaults(defineProps<Props>(), {
  disabled: false,
  maxHeight: '150px'
})

const emit = defineEmits<{
  (e: 'delete', fileId: number): void
}>()

function handleDelete(fileId: number) {
  emit('delete', fileId)
}
</script>

<template>
  <div
    class="file-list overflow-y-auto"
    :style="{ maxHeight }"
  >
    <template v-if="files.length > 0">
      <FileListItem
        v-for="file in files"
        :key="file.fileId"
        :file="file"
        :disabled="disabled"
        @delete="handleDelete"
      />
    </template>
    <div v-else class="px-2 py-3 text-center text-[12px] text-gray-400">
      첨부된 파일이 없습니다
    </div>
  </div>
</template>
