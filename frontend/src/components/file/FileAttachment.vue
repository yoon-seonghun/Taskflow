<script setup lang="ts">
/**
 * 파일 첨부 영역 컴포넌트
 * - 접기/펼치기 가능한 헤더
 * - 드래그 앤 드롭 업로드
 * - 파일 목록 (스크롤 영역)
 */
import { ref, onMounted, watch } from 'vue'
import { fileApi } from '@/api/file'
import { validateFile, ALLOWED_FILES_SHORT, ALLOWED_FILES_DETAIL, MAX_FILE_SIZE_TEXT } from '@/types/file'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import FileList from './FileList.vue'
import type { FileInfoResponse } from '@/types/file'

// 파일 형식 안내 텍스트
const fileGuideShort = `${ALLOWED_FILES_SHORT} (최대 ${MAX_FILE_SIZE_TEXT})`
const fileGuideTooltip = `허용 형식: ${ALLOWED_FILES_DETAIL}\n최대 크기: ${MAX_FILE_SIZE_TEXT}`

interface Props {
  relatedType: string
  relatedId: number
  disabled?: boolean
  maxFiles?: number
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  maxFiles: 10
})

const emit = defineEmits<{
  (e: 'change', files: FileInfoResponse[]): void
}>()

const toast = useToast()
const confirm = useConfirm()

// 상태
const files = ref<FileInfoResponse[]>([])
const isLoading = ref(false)
const isUploading = ref(false)
const isCollapsed = ref(false)
const isDragging = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

// 파일 목록 로드
async function loadFiles() {
  if (!props.relatedId) return

  isLoading.value = true
  try {
    const response = await fileApi.getRelatedFiles(props.relatedType, props.relatedId)
    if (response.success && response.data) {
      files.value = response.data
      emit('change', files.value)
    }
  } catch (error) {
    console.error('Failed to load files:', error)
  } finally {
    isLoading.value = false
  }
}

// 파일 업로드
async function uploadFile(file: File) {
  // 유효성 검사
  const validation = validateFile(file)
  if (!validation.valid) {
    toast.error(validation.error || '파일 업로드 실패')
    return
  }

  // 최대 파일 수 체크
  if (files.value.length >= props.maxFiles) {
    toast.error(`최대 ${props.maxFiles}개까지 첨부할 수 있습니다.`)
    return
  }

  isUploading.value = true
  try {
    const response = await fileApi.upload(file, {
      relatedType: props.relatedType,
      relatedId: props.relatedId
    })

    if (response.success && response.data) {
      // 목록 새로고침
      await loadFiles()
      toast.success('파일이 첨부되었습니다.')
    } else {
      toast.error(response.message || '파일 업로드 실패')
    }
  } catch (error: any) {
    toast.error(error.message || '파일 업로드 중 오류가 발생했습니다.')
  } finally {
    isUploading.value = false
  }
}

// 파일 삭제
async function handleDelete(fileId: number) {
  const confirmed = await confirm.show({
    title: '파일 삭제',
    message: '이 파일을 삭제하시겠습니까?',
    confirmText: '삭제',
    confirmType: 'danger'
  })

  if (!confirmed) return

  try {
    const response = await fileApi.deleteFile(fileId)
    if (response.success) {
      files.value = files.value.filter(f => f.fileId !== fileId)
      emit('change', files.value)
      toast.success('파일이 삭제되었습니다.')
    } else {
      toast.error(response.message || '파일 삭제 실패')
    }
  } catch (error) {
    toast.error('파일 삭제 중 오류가 발생했습니다.')
  }
}

// 파일 선택 트리거
function triggerFileInput() {
  if (props.disabled || isUploading.value) return
  fileInputRef.value?.click()
}

// 파일 선택 핸들러
function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const selectedFiles = input.files
  if (selectedFiles) {
    Array.from(selectedFiles).forEach(file => uploadFile(file))
  }
  input.value = ''
}

// 드래그 앤 드롭 핸들러
function handleDragEnter(event: DragEvent) {
  event.preventDefault()
  if (!props.disabled && !isUploading.value) {
    isDragging.value = true
  }
}

function handleDragLeave(event: DragEvent) {
  event.preventDefault()
  isDragging.value = false
}

function handleDragOver(event: DragEvent) {
  event.preventDefault()
}

function handleDrop(event: DragEvent) {
  event.preventDefault()
  isDragging.value = false

  if (props.disabled || isUploading.value) return

  const droppedFiles = event.dataTransfer?.files
  if (droppedFiles) {
    Array.from(droppedFiles).forEach(file => uploadFile(file))
  }
}

// 접기/펼치기
function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

// relatedId 변경 시 파일 목록 새로고침
watch(() => props.relatedId, () => {
  if (props.relatedId) {
    loadFiles()
  }
}, { immediate: true })

onMounted(() => {
  if (props.relatedId) {
    loadFiles()
  }
})
</script>

<template>
  <div
    class="file-attachment border-t border-gray-200 bg-gray-50"
    :class="{ 'ring-2 ring-primary-500 ring-inset': isDragging }"
    @dragenter="handleDragEnter"
    @dragleave="handleDragLeave"
    @dragover="handleDragOver"
    @drop="handleDrop"
  >
    <!-- 숨겨진 파일 입력 -->
    <input
      ref="fileInputRef"
      type="file"
      multiple
      class="hidden"
      @change="handleFileSelect"
    />

    <!-- 헤더 -->
    <div
      class="flex items-center justify-between px-3 py-2 cursor-pointer hover:bg-gray-100 select-none"
      @click="toggleCollapse"
    >
      <div class="flex items-center gap-2">
        <!-- 접기/펼치기 아이콘 -->
        <svg
          class="w-4 h-4 text-gray-400 transition-transform"
          :class="{ '-rotate-90': isCollapsed }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>

        <span class="text-[12px] font-medium text-gray-600">
          첨부파일
          <span v-if="files.length > 0" class="ml-1 text-gray-400">({{ files.length }})</span>
        </span>

        <!-- 로딩/업로드 표시 -->
        <svg
          v-if="isLoading || isUploading"
          class="w-3 h-3 text-gray-400 animate-spin"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
        </svg>
      </div>

      <!-- 파일 추가 버튼 (옵션 1: 툴팁) -->
      <button
        v-if="!disabled"
        type="button"
        class="flex items-center gap-1 px-2 py-1 text-[11px] text-primary-600 hover:text-primary-700 hover:bg-primary-50 rounded transition-colors"
        :class="{ 'opacity-50 cursor-not-allowed': isUploading }"
        :disabled="isUploading"
        :title="fileGuideTooltip"
        @click.stop="triggerFileInput"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
        </svg>
        파일 추가
      </button>
    </div>

    <!-- 옵션 2: 헤더 아래 안내 텍스트 -->
    <div v-show="!isCollapsed" class="px-3 py-1 text-[11px] text-gray-400 bg-gray-50 border-t border-gray-100">
      <span class="inline-flex items-center gap-1">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        {{ fileGuideShort }}
      </span>
    </div>

    <!-- 파일 목록 (접힘 상태가 아닐 때) -->
    <div v-show="!isCollapsed" class="border-t border-gray-200">
      <!-- 드래그 오버레이 -->
      <div
        v-if="isDragging"
        class="px-3 py-4 text-center"
      >
        <div class="text-[12px] text-primary-600">
          여기에 파일을 놓으세요
        </div>
      </div>

      <!-- 옵션 3: 빈 목록일 때 드래그 안내 -->
      <div
        v-else-if="files.length === 0 && !isLoading"
        class="px-3 py-4 text-center cursor-pointer hover:bg-gray-100 transition-colors"
        @click="triggerFileInput"
      >
        <div class="flex flex-col items-center gap-1">
          <svg class="w-6 h-6 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
          </svg>
          <span class="text-[12px] text-gray-400">
            파일을 드래그하거나 클릭하여 추가
          </span>
        </div>
      </div>

      <!-- 파일 목록 -->
      <FileList
        v-else
        :files="files"
        :disabled="disabled"
        max-height="150px"
        @delete="handleDelete"
      />
    </div>
  </div>
</template>
