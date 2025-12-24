<script setup lang="ts">
/**
 * 마크다운 에디터 컴포넌트
 * - 에디터/미리보기 탭 전환
 * - marked 라이브러리로 마크다운 렌더링
 * - 툴바로 간편 서식 적용
 * - 디바운스 자동 저장
 * - 이미지 업로드 (드래그앤드롭, 붙여넣기, 버튼 클릭)
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { marked } from 'marked'
import { fileApi } from '@/api/file'
import { validateFile, isImageFile as checkImageFile } from '@/types/file'

interface Props {
  modelValue?: string
  placeholder?: string
  disabled?: boolean
  minHeight?: string
  autoSaveDelay?: number
  relatedType?: string  // 연관 엔티티 타입 (예: ITEM)
  relatedId?: number    // 연관 엔티티 ID
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '마크다운 형식으로 내용을 입력하세요...',
  disabled: false,
  minHeight: '300px',
  autoSaveDelay: 1000,
  relatedType: undefined,
  relatedId: undefined
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'save', value: string): void
}>()

// 상태
const activeTab = ref<'edit' | 'preview'>('edit')
const localValue = ref(props.modelValue || '')
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const saveTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const isUploading = ref(false)
const uploadProgress = ref(0)
const isDragging = ref(false)
const uploadError = ref<string | null>(null)

// 마크다운 렌더링
const renderedContent = computed(() => {
  if (!localValue.value) return '<p class="text-gray-400">미리보기할 내용이 없습니다.</p>'
  try {
    return marked(localValue.value, { breaks: true, gfm: true })
  } catch {
    return '<p class="text-red-500">마크다운 렌더링 오류</p>'
  }
})

// props 변경 감지
watch(() => props.modelValue, (newVal) => {
  if (newVal !== localValue.value) {
    localValue.value = newVal || ''
  }
})

// 입력 처리 (디바운스 자동 저장)
function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  localValue.value = target.value
  emit('update:modelValue', target.value)

  // 디바운스 저장
  if (saveTimer.value) {
    clearTimeout(saveTimer.value)
  }
  saveTimer.value = setTimeout(() => {
    emit('save', localValue.value)
  }, props.autoSaveDelay)
}

// blur 시 즉시 저장
function handleBlur() {
  if (saveTimer.value) {
    clearTimeout(saveTimer.value)
    saveTimer.value = null
  }
  emit('save', localValue.value)
}

// 툴바 액션: 텍스트 감싸기
function wrapText(before: string, after: string) {
  if (!textareaRef.value || props.disabled) return

  const textarea = textareaRef.value
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = localValue.value.substring(start, end)

  const newText = localValue.value.substring(0, start)
    + before + selectedText + after
    + localValue.value.substring(end)

  localValue.value = newText
  emit('update:modelValue', newText)

  // 커서 위치 조정
  setTimeout(() => {
    textarea.focus()
    if (selectedText) {
      textarea.setSelectionRange(start + before.length, end + before.length)
    } else {
      textarea.setSelectionRange(start + before.length, start + before.length)
    }
  }, 0)
}

// 툴바 액션: 줄 시작에 삽입
function insertAtLineStart(prefix: string) {
  if (!textareaRef.value || props.disabled) return

  const textarea = textareaRef.value
  const start = textarea.selectionStart

  // 현재 줄의 시작 위치 찾기
  let lineStart = start
  while (lineStart > 0 && localValue.value[lineStart - 1] !== '\n') {
    lineStart--
  }

  const newText = localValue.value.substring(0, lineStart)
    + prefix
    + localValue.value.substring(lineStart)

  localValue.value = newText
  emit('update:modelValue', newText)

  setTimeout(() => {
    textarea.focus()
    textarea.setSelectionRange(start + prefix.length, start + prefix.length)
  }, 0)
}

// 이미지 업로드 처리
async function handleImageUpload(file: File) {
  // 유효성 검사
  const validation = validateFile(file)
  if (!validation.valid) {
    uploadError.value = validation.error || '파일 업로드 실패'
    setTimeout(() => { uploadError.value = null }, 3000)
    return
  }

  // 이미지 파일인지 확인
  if (!checkImageFile(file.type)) {
    uploadError.value = '이미지 파일만 업로드 가능합니다.'
    setTimeout(() => { uploadError.value = null }, 3000)
    return
  }

  isUploading.value = true
  uploadProgress.value = 0
  uploadError.value = null

  try {
    const response = await fileApi.upload(file, {
      relatedType: props.relatedType,
      relatedId: props.relatedId,
      onProgress: (progress) => {
        uploadProgress.value = progress
      }
    })

    if (response.success && response.data) {
      // 마크다운 이미지 문법 삽입
      insertMarkdownImage(response.data.markdown)
    } else {
      uploadError.value = response.message || '파일 업로드 실패'
      setTimeout(() => { uploadError.value = null }, 3000)
    }
  } catch (error: any) {
    console.error('File upload error:', error)
    uploadError.value = error.message || '파일 업로드 중 오류가 발생했습니다.'
    setTimeout(() => { uploadError.value = null }, 3000)
  } finally {
    isUploading.value = false
    uploadProgress.value = 0
  }
}

// 마크다운 이미지 삽입
function insertMarkdownImage(markdown: string) {
  if (!textareaRef.value) return

  const textarea = textareaRef.value
  const start = textarea.selectionStart
  const end = textarea.selectionEnd

  // 현재 커서 위치에 이미지 마크다운 삽입
  const newText = localValue.value.substring(0, start)
    + '\n' + markdown + '\n'
    + localValue.value.substring(end)

  localValue.value = newText
  emit('update:modelValue', newText)

  // 커서 위치 조정
  setTimeout(() => {
    textarea.focus()
    const newPosition = start + markdown.length + 2
    textarea.setSelectionRange(newPosition, newPosition)
  }, 0)
}

// 이미지 버튼 클릭 핸들러
function triggerFileInput() {
  if (props.disabled || isUploading.value) return
  fileInputRef.value?.click()
}

// 파일 선택 핸들러
function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (files && files.length > 0) {
    handleImageUpload(files[0])
  }
  // input 초기화 (같은 파일 재선택 가능하도록)
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

  const files = event.dataTransfer?.files
  if (files && files.length > 0) {
    // 이미지 파일만 처리
    const imageFile = Array.from(files).find(f => checkImageFile(f.type))
    if (imageFile) {
      handleImageUpload(imageFile)
    } else {
      uploadError.value = '이미지 파일만 업로드 가능합니다.'
      setTimeout(() => { uploadError.value = null }, 3000)
    }
  }
}

// 붙여넣기 핸들러 (클립보드 이미지)
function handlePaste(event: ClipboardEvent) {
  if (props.disabled || isUploading.value) return

  const items = event.clipboardData?.items
  if (!items) return

  for (const item of items) {
    if (item.type.startsWith('image/')) {
      event.preventDefault()
      const file = item.getAsFile()
      if (file) {
        handleImageUpload(file)
      }
      break
    }
  }
}

// 툴바 버튼들
const toolbarButtons = [
  { icon: 'B', action: () => wrapText('**', '**'), title: '굵게 (Ctrl+B)' },
  { icon: 'I', action: () => wrapText('*', '*'), title: '기울임 (Ctrl+I)' },
  { icon: '~', action: () => wrapText('~~', '~~'), title: '취소선' },
  { icon: 'H1', action: () => insertAtLineStart('# '), title: '제목 1' },
  { icon: 'H2', action: () => insertAtLineStart('## '), title: '제목 2' },
  { icon: 'H3', action: () => insertAtLineStart('### '), title: '제목 3' },
  { icon: '-', action: () => insertAtLineStart('- '), title: '목록' },
  { icon: '1.', action: () => insertAtLineStart('1. '), title: '번호 목록' },
  { icon: '[ ]', action: () => insertAtLineStart('- [ ] '), title: '체크박스' },
  { icon: '`', action: () => wrapText('`', '`'), title: '인라인 코드' },
  { icon: '```', action: () => wrapText('```\n', '\n```'), title: '코드 블록' },
  { icon: '>', action: () => insertAtLineStart('> '), title: '인용' },
  { icon: '---', action: () => insertAtLineStart('\n---\n'), title: '구분선' },
]

// 키보드 단축키
function handleKeydown(event: KeyboardEvent) {
  if (props.disabled) return

  if (event.ctrlKey || event.metaKey) {
    switch (event.key.toLowerCase()) {
      case 'b':
        event.preventDefault()
        wrapText('**', '**')
        break
      case 'i':
        event.preventDefault()
        wrapText('*', '*')
        break
    }
  }
}

// 컴포넌트 언마운트 시 타이머 정리
onUnmounted(() => {
  if (saveTimer.value) {
    clearTimeout(saveTimer.value)
  }
})
</script>

<template>
  <div
    class="markdown-editor border border-gray-300 rounded-lg overflow-hidden bg-white h-full flex flex-col relative"
    :class="{ 'ring-2 ring-primary-500 ring-opacity-50': isDragging }"
    @dragenter="handleDragEnter"
    @dragleave="handleDragLeave"
    @dragover="handleDragOver"
    @drop="handleDrop"
  >
    <!-- 숨겨진 파일 입력 -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/*"
      class="hidden"
      @change="handleFileSelect"
    />

    <!-- 드래그 오버레이 -->
    <div
      v-if="isDragging"
      class="absolute inset-0 bg-primary-50 bg-opacity-90 flex items-center justify-center z-10 pointer-events-none"
    >
      <div class="text-center">
        <svg class="w-12 h-12 mx-auto text-primary-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
        </svg>
        <p class="text-primary-600 font-medium">이미지를 여기에 놓으세요</p>
      </div>
    </div>

    <!-- 업로드 진행률 -->
    <div
      v-if="isUploading"
      class="absolute inset-0 bg-white bg-opacity-80 flex items-center justify-center z-20"
    >
      <div class="text-center">
        <div class="w-48 h-2 bg-gray-200 rounded-full overflow-hidden mb-2">
          <div
            class="h-full bg-primary-500 transition-all duration-300"
            :style="{ width: `${uploadProgress}%` }"
          />
        </div>
        <p class="text-sm text-gray-600">업로드 중... {{ uploadProgress }}%</p>
      </div>
    </div>

    <!-- 에러 메시지 -->
    <div
      v-if="uploadError"
      class="absolute top-2 left-1/2 -translate-x-1/2 bg-red-100 text-red-700 px-4 py-2 rounded-lg shadow-lg z-30 text-sm"
    >
      {{ uploadError }}
    </div>

    <!-- 헤더: 탭 + 툴바 -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-2 py-1">
      <!-- 탭 -->
      <div class="flex gap-1">
        <button
          type="button"
          class="px-3 py-1.5 text-[12px] font-medium rounded transition-colors"
          :class="activeTab === 'edit'
            ? 'bg-white text-gray-900 shadow-sm'
            : 'text-gray-500 hover:text-gray-700'"
          @click="activeTab = 'edit'"
        >
          편집
        </button>
        <button
          type="button"
          class="px-3 py-1.5 text-[12px] font-medium rounded transition-colors"
          :class="activeTab === 'preview'
            ? 'bg-white text-gray-900 shadow-sm'
            : 'text-gray-500 hover:text-gray-700'"
          @click="activeTab = 'preview'"
        >
          미리보기
        </button>
      </div>

      <!-- 툴바 (편집 모드에서만) -->
      <div v-if="activeTab === 'edit'" class="flex items-center gap-0.5">
        <button
          v-for="btn in toolbarButtons"
          :key="btn.icon"
          type="button"
          class="px-1.5 py-1 text-[11px] font-mono text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded transition-colors"
          :class="{ 'opacity-50 cursor-not-allowed': disabled }"
          :title="btn.title"
          :disabled="disabled"
          @click="btn.action"
        >
          {{ btn.icon }}
        </button>
        <!-- 구분선 -->
        <div class="w-px h-4 bg-gray-300 mx-1" />
        <!-- 이미지 업로드 버튼 -->
        <button
          type="button"
          class="px-1.5 py-1 text-[11px] text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded transition-colors flex items-center gap-1"
          :class="{ 'opacity-50 cursor-not-allowed': disabled || isUploading }"
          title="이미지 첨부 (드래그앤드롭 또는 붙여넣기 가능)"
          :disabled="disabled || isUploading"
          @click="triggerFileInput"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 컨텐츠 영역 -->
    <div class="flex-1 min-h-0 overflow-hidden" :style="{ minHeight }">
      <!-- 편집 모드 -->
      <textarea
        v-show="activeTab === 'edit'"
        ref="textareaRef"
        :value="localValue"
        :placeholder="placeholder"
        :disabled="disabled"
        class="w-full h-full p-3 text-[13px] font-mono resize-none focus:outline-none disabled:bg-gray-100 disabled:cursor-not-allowed"
        @input="handleInput"
        @blur="handleBlur"
        @keydown="handleKeydown"
        @paste="handlePaste"
      />

      <!-- 미리보기 모드 -->
      <div
        v-show="activeTab === 'preview'"
        class="prose prose-sm max-w-none p-3 h-full overflow-auto"
        v-html="renderedContent"
      />
    </div>
  </div>
</template>

<style scoped>
/* 마크다운 렌더링 스타일 */
.prose {
  @apply text-gray-900;
}

.prose :deep(h1) {
  @apply text-xl font-bold mt-4 mb-2 pb-1 border-b border-gray-200;
}

.prose :deep(h2) {
  @apply text-lg font-bold mt-3 mb-2;
}

.prose :deep(h3) {
  @apply text-base font-semibold mt-2 mb-1;
}

.prose :deep(p) {
  @apply my-2 text-[13px] leading-relaxed;
}

.prose :deep(ul),
.prose :deep(ol) {
  @apply my-2 pl-5;
}

.prose :deep(ul) {
  @apply list-disc;
}

.prose :deep(ol) {
  @apply list-decimal;
}

.prose :deep(li) {
  @apply my-0.5 text-[13px];
}

.prose :deep(code) {
  @apply bg-gray-100 text-red-600 px-1 py-0.5 rounded text-[12px] font-mono;
}

.prose :deep(pre) {
  @apply bg-gray-800 text-gray-100 p-3 rounded-lg my-2 overflow-x-auto;
}

.prose :deep(pre code) {
  @apply bg-transparent text-inherit p-0;
}

.prose :deep(blockquote) {
  @apply border-l-4 border-gray-300 pl-3 my-2 text-gray-600 italic;
}

.prose :deep(hr) {
  @apply my-4 border-gray-200;
}

.prose :deep(a) {
  @apply text-primary-600 hover:underline;
}

.prose :deep(strong) {
  @apply font-bold;
}

.prose :deep(em) {
  @apply italic;
}

.prose :deep(del) {
  @apply line-through text-gray-400;
}

/* 체크박스 스타일 */
.prose :deep(input[type="checkbox"]) {
  @apply mr-1.5;
}

/* 테이블 스타일 */
.prose :deep(table) {
  @apply w-full border-collapse my-2;
}

.prose :deep(th),
.prose :deep(td) {
  @apply border border-gray-300 px-2 py-1 text-[12px];
}

.prose :deep(th) {
  @apply bg-gray-100 font-semibold;
}

/* 이미지 스타일 */
.prose :deep(img) {
  @apply max-w-full h-auto rounded-lg my-2 border border-gray-200;
}
</style>
