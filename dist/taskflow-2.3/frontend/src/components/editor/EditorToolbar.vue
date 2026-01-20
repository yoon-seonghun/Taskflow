<script setup lang="ts">
/**
 * 에디터 툴바 컴포넌트
 * - 서식 버튼 그룹
 * - 활성 상태 표시
 * - 이미지 업로드 (URL, 파일 업로드)
 */
import { ref, computed } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import { fileApi } from '@/api/file'
import { IMAGE_MIME_TYPES, MAX_FILE_SIZE, formatFileSize } from '@/types/file'

interface Props {
  editor: Editor
  /** 이미지 업로드 시 연관 타입 (ITEM 등) */
  relatedType?: string
  /** 이미지 업로드 시 연관 ID */
  relatedId?: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'imageUploading', uploading: boolean): void
}>()

// 링크 모달 상태
const showLinkModal = ref(false)
const linkUrl = ref('')
const linkText = ref('')

// 이미지 모달 상태
const showImageModal = ref(false)
const imageUrl = ref('')
const imageTab = ref<'url' | 'upload'>('upload')  // 기본값: 파일 업로드 탭
const isUploading = ref(false)
const uploadProgress = ref(0)
const uploadError = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)

// 색상 선택 상태
const showColorPicker = ref(false)
const colors = [
  '#000000', '#ef4444', '#f97316', '#eab308', '#22c55e', '#14b8a6',
  '#3b82f6', '#6366f1', '#a855f7', '#ec4899', '#6b7280', '#ffffff'
]

// 현재 텍스트 색상
const currentColor = computed(() => {
  return props.editor.getAttributes('textStyle').color || '#000000'
})

// 링크 추가
function setLink() {
  if (!linkUrl.value) {
    props.editor.chain().focus().extendMarkRange('link').unsetLink().run()
    closeLinkModal()
    return
  }

  let url = linkUrl.value
  if (!url.startsWith('http://') && !url.startsWith('https://')) {
    url = 'https://' + url
  }

  props.editor
    .chain()
    .focus()
    .extendMarkRange('link')
    .setLink({ href: url })
    .run()

  closeLinkModal()
}

function openLinkModal() {
  const previousUrl = props.editor.getAttributes('link').href
  linkUrl.value = previousUrl || ''

  const { from, to } = props.editor.state.selection
  linkText.value = props.editor.state.doc.textBetween(from, to, '')

  showLinkModal.value = true
}

function closeLinkModal() {
  showLinkModal.value = false
  linkUrl.value = ''
  linkText.value = ''
}

// 이미지 추가 (URL)
function addImageByUrl() {
  if (imageUrl.value) {
    props.editor.chain().focus().setImage({ src: imageUrl.value }).run()
  }
  closeImageModal()
}

function openImageModal() {
  imageUrl.value = ''
  imageTab.value = 'upload'
  uploadError.value = ''
  uploadProgress.value = 0
  showImageModal.value = true
}

function closeImageModal() {
  showImageModal.value = false
  imageUrl.value = ''
  uploadError.value = ''
  uploadProgress.value = 0
}

// 파일 선택 트리거
function triggerFileInput() {
  fileInputRef.value?.click()
}

// 파일 선택 핸들러
async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    await uploadImage(file)
  }
  // 같은 파일 다시 선택 가능하도록 초기화
  input.value = ''
}

// 이미지 업로드
async function uploadImage(file: File) {
  // 이미지 파일 검증
  if (!IMAGE_MIME_TYPES.includes(file.type as any)) {
    uploadError.value = '이미지 파일만 업로드 가능합니다. (jpg, png, gif, webp, svg)'
    return
  }

  if (file.size > MAX_FILE_SIZE) {
    uploadError.value = `파일 크기가 너무 큽니다. (최대 ${formatFileSize(MAX_FILE_SIZE)})`
    return
  }

  uploadError.value = ''
  isUploading.value = true
  uploadProgress.value = 0
  emit('imageUploading', true)

  try {
    const response = await fileApi.upload(file, {
      relatedType: props.relatedType,
      relatedId: props.relatedId,
      onProgress: (progress) => {
        uploadProgress.value = progress
      }
    })

    if (response.success && response.data?.url) {
      // 에디터에 이미지 삽입
      props.editor.chain().focus().setImage({ src: response.data.url }).run()
      closeImageModal()
    } else {
      uploadError.value = '이미지 업로드에 실패했습니다.'
    }
  } catch (error: any) {
    console.error('Image upload failed:', error)
    uploadError.value = error.message || '이미지 업로드에 실패했습니다.'
  } finally {
    isUploading.value = false
    emit('imageUploading', false)
  }
}

// 외부에서 호출 가능하도록 노출
defineExpose({
  uploadImage
})

// 색상 설정
function setColor(color: string) {
  props.editor.chain().focus().setColor(color).run()
  showColorPicker.value = false
}

function toggleColorPicker() {
  showColorPicker.value = !showColorPicker.value
}

// 외부 클릭 시 닫기
function handleOutsideClick(event: Event) {
  const target = event.target as HTMLElement
  if (!target.closest('.color-picker-container')) {
    showColorPicker.value = false
  }
}

// 테이블 삽입
function insertTable() {
  props.editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

// 테이블 컨트롤
function addColumnBefore() {
  props.editor.chain().focus().addColumnBefore().run()
}

function addColumnAfter() {
  props.editor.chain().focus().addColumnAfter().run()
}

function deleteColumn() {
  props.editor.chain().focus().deleteColumn().run()
}

function addRowBefore() {
  props.editor.chain().focus().addRowBefore().run()
}

function addRowAfter() {
  props.editor.chain().focus().addRowAfter().run()
}

function deleteRow() {
  props.editor.chain().focus().deleteRow().run()
}

function deleteTable() {
  props.editor.chain().focus().deleteTable().run()
}

// 테이블 활성화 여부
const isTableActive = computed(() => props.editor.isActive('table'))
</script>

<template>
  <div class="flex flex-wrap items-center gap-0.5 px-2 py-1.5 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
    <!-- 텍스트 서식 -->
    <div class="flex items-center gap-0.5">
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('bold') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="굵게 (Ctrl+B)"
        @click="editor.chain().focus().toggleBold().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('italic') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="기울임 (Ctrl+I)"
        @click="editor.chain().focus().toggleItalic().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 4h4m-2 0l-4 16m0 0h4"/>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('underline') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="밑줄 (Ctrl+U)"
        @click="editor.chain().focus().toggleUnderline().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 3v7a6 6 0 0 0 6 6 6 6 0 0 0 6-6V3 M4 21h16"/>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('strike') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="취소선"
        @click="editor.chain().focus().toggleStrike().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 12h16 M6 6s1-2 6-2c4.5 0 6 3 6 4 0 1.5-1.5 3-4 3 M18 18s-1 2-6 2c-4.5 0-6-3-6-4"/>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('highlight') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="하이라이트"
        @click="editor.chain().focus().toggleHighlight().run()"
      >
        <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
          <path d="M15.243 4.515l-6.738 6.737-.707 2.121-1.04 1.041 2.828 2.829 1.04-1.041 2.122-.707 6.737-6.738-4.242-4.242zm6.364 3.535a1 1 0 0 1 0 1.414l-7.778 7.778-2.122.707-1.414 1.414a1 1 0 0 1-1.414 0l-4.243-4.243a1 1 0 0 1 0-1.414l1.414-1.414.707-2.121 7.778-7.778a1 1 0 0 1 1.414 0l5.657 5.657z"/>
        </svg>
      </button>
    </div>

    <div class="w-px h-5 bg-gray-300 dark:bg-gray-600 mx-1" />

    <!-- 색상 선택 -->
    <div class="relative color-picker-container">
      <button
        type="button"
        class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600 flex items-center gap-1"
        title="글자 색상"
        @click="toggleColorPicker"
      >
        <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
          <path d="M11 2L5.5 16h2.25l1.12-3h6.25l1.12 3h2.25L13 2h-2zm-1.38 9L12 4.67 14.38 11H9.62z"/>
        </svg>
        <span
          class="w-4 h-1 rounded"
          :style="{ backgroundColor: currentColor }"
        />
      </button>

      <!-- 색상 팔레트 -->
      <div
        v-if="showColorPicker"
        class="absolute top-full left-0 mt-1 p-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg z-50 grid grid-cols-6 gap-1"
        @click.stop
      >
        <button
          v-for="color in colors"
          :key="color"
          type="button"
          class="w-6 h-6 rounded border border-gray-200 dark:border-gray-600 hover:scale-110 transition-transform"
          :style="{ backgroundColor: color }"
          :title="color"
          @click="setColor(color)"
        />
      </div>
    </div>

    <div class="w-px h-5 bg-gray-300 dark:bg-gray-600 mx-1" />

    <!-- 제목 -->
    <div class="flex items-center gap-0.5">
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 transition-colors text-[12px] font-bold',
          editor.isActive('heading', { level: 1 }) ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="제목 1"
        @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
      >
        H1
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 transition-colors text-[12px] font-bold',
          editor.isActive('heading', { level: 2 }) ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="제목 2"
        @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
      >
        H2
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 transition-colors text-[12px] font-bold',
          editor.isActive('heading', { level: 3 }) ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="제목 3"
        @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
      >
        H3
      </button>
    </div>

    <div class="w-px h-5 bg-gray-300 dark:bg-gray-600 mx-1" />

    <!-- 목록 -->
    <div class="flex items-center gap-0.5">
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('bulletList') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="불릿 목록"
        @click="editor.chain().focus().toggleBulletList().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
          <circle cx="2" cy="6" r="1" fill="currentColor"/>
          <circle cx="2" cy="12" r="1" fill="currentColor"/>
          <circle cx="2" cy="18" r="1" fill="currentColor"/>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('orderedList') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="번호 목록"
        @click="editor.chain().focus().toggleOrderedList().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 6h13M8 12h13M8 18h13"/>
          <text x="2" y="8" font-size="8" fill="currentColor">1</text>
          <text x="2" y="14" font-size="8" fill="currentColor">2</text>
          <text x="2" y="20" font-size="8" fill="currentColor">3</text>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('taskList') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="체크리스트"
        @click="editor.chain().focus().toggleTaskList().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/>
        </svg>
      </button>
    </div>

    <div class="w-px h-5 bg-gray-300 dark:bg-gray-600 mx-1" />

    <!-- 정렬 -->
    <div class="flex items-center gap-0.5">
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive({ textAlign: 'left' }) ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="왼쪽 정렬"
        @click="editor.chain().focus().setTextAlign('left').run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h10M4 18h14"/>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive({ textAlign: 'center' }) ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="가운데 정렬"
        @click="editor.chain().focus().setTextAlign('center').run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M7 12h10M5 18h14"/>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive({ textAlign: 'right' }) ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="오른쪽 정렬"
        @click="editor.chain().focus().setTextAlign('right').run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M10 12h10M6 18h14"/>
        </svg>
      </button>
    </div>

    <div class="w-px h-5 bg-gray-300 dark:bg-gray-600 mx-1" />

    <!-- 삽입 -->
    <div class="flex items-center gap-0.5">
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('link') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="링크"
        @click="openLinkModal"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/>
        </svg>
      </button>
      <button
        type="button"
        class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600"
        title="이미지"
        @click="openImageModal"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          isTableActive ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="테이블"
        @click="insertTable"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M3 14h18M10 3v18M14 3v18M3 6a3 3 0 013-3h12a3 3 0 013 3v12a3 3 0 01-3 3H6a3 3 0 01-3-3V6z"/>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('codeBlock') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="코드 블록"
        @click="editor.chain().focus().toggleCodeBlock().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/>
        </svg>
      </button>
      <button
        type="button"
        :class="[
          'p-1.5 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors',
          editor.isActive('blockquote') ? 'bg-gray-200 dark:bg-gray-700 text-primary-600 dark:text-primary-400' : 'text-gray-600 dark:text-gray-400'
        ]"
        title="인용문"
        @click="editor.chain().focus().toggleBlockquote().run()"
      >
        <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
          <path d="M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z"/>
        </svg>
      </button>
      <button
        type="button"
        class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600"
        title="수평선"
        @click="editor.chain().focus().setHorizontalRule().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 12h14"/>
        </svg>
      </button>
    </div>

    <div class="w-px h-5 bg-gray-300 dark:bg-gray-600 mx-1" />

    <!-- 실행 취소 -->
    <div class="flex items-center gap-0.5">
      <button
        type="button"
        class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
        title="실행 취소 (Ctrl+Z)"
        :disabled="!editor.can().undo()"
        @click="editor.chain().focus().undo().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6"/>
        </svg>
      </button>
      <button
        type="button"
        class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
        title="다시 실행 (Ctrl+Y)"
        :disabled="!editor.can().redo()"
        @click="editor.chain().focus().redo().run()"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 10h-10a8 8 0 00-8 8v2M21 10l-6 6m6-6l-6-6"/>
        </svg>
      </button>
    </div>

    <!-- 테이블 컨트롤 (테이블 내에서만 표시) -->
    <template v-if="isTableActive">
      <div class="w-px h-5 bg-gray-300 dark:bg-gray-600 mx-1" />
      <div class="flex items-center gap-0.5">
        <button
          type="button"
          class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600 text-[10px]"
          title="왼쪽에 열 추가"
          @click="addColumnBefore"
        >
          +열←
        </button>
        <button
          type="button"
          class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600 text-[10px]"
          title="오른쪽에 열 추가"
          @click="addColumnAfter"
        >
          +열→
        </button>
        <button
          type="button"
          class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600 text-[10px]"
          title="열 삭제"
          @click="deleteColumn"
        >
          -열
        </button>
        <button
          type="button"
          class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600 text-[10px]"
          title="위에 행 추가"
          @click="addRowBefore"
        >
          +행↑
        </button>
        <button
          type="button"
          class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600 text-[10px]"
          title="아래에 행 추가"
          @click="addRowAfter"
        >
          +행↓
        </button>
        <button
          type="button"
          class="p-1.5 rounded hover:bg-gray-200 transition-colors text-gray-600 text-[10px]"
          title="행 삭제"
          @click="deleteRow"
        >
          -행
        </button>
        <button
          type="button"
          class="p-1.5 rounded hover:bg-gray-200 transition-colors text-red-600 text-[10px]"
          title="테이블 삭제"
          @click="deleteTable"
        >
          삭제
        </button>
      </div>
    </template>
  </div>

  <!-- 링크 모달 -->
  <Teleport to="body">
    <div
      v-if="showLinkModal"
      class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
      @click.self="closeLinkModal"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md p-4">
        <h3 class="text-lg font-semibold mb-4 text-gray-900 dark:text-gray-100">링크 추가</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">URL</label>
            <input
              v-model="linkUrl"
              type="url"
              placeholder="https://example.com"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              @keyup.enter="setLink"
            />
          </div>
          <div v-if="linkText" class="text-sm text-gray-500 dark:text-gray-400">
            선택된 텍스트: {{ linkText }}
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-4">
          <button
            type="button"
            class="px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
            @click="closeLinkModal"
          >
            취소
          </button>
          <button
            v-if="editor.isActive('link')"
            type="button"
            class="px-4 py-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors"
            @click="() => { linkUrl = ''; setLink() }"
          >
            링크 제거
          </button>
          <button
            type="button"
            class="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
            @click="setLink"
          >
            적용
          </button>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- 이미지 모달 -->
  <Teleport to="body">
    <div
      v-if="showImageModal"
      class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
      @click.self="closeImageModal"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md">
        <div class="p-4 border-b border-gray-200 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100">이미지 추가</h3>
        </div>

        <!-- 탭 -->
        <div class="flex border-b border-gray-200 dark:border-gray-700">
          <button
            type="button"
            class="flex-1 px-4 py-2.5 text-sm font-medium transition-colors"
            :class="imageTab === 'upload' ? 'text-primary-600 dark:text-primary-400 border-b-2 border-primary-600 bg-primary-50 dark:bg-primary-900/30' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            @click="imageTab = 'upload'"
          >
            파일 업로드
          </button>
          <button
            type="button"
            class="flex-1 px-4 py-2.5 text-sm font-medium transition-colors"
            :class="imageTab === 'url' ? 'text-primary-600 dark:text-primary-400 border-b-2 border-primary-600 bg-primary-50 dark:bg-primary-900/30' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            @click="imageTab = 'url'"
          >
            URL 입력
          </button>
        </div>

        <div class="p-4">
          <!-- 파일 업로드 탭 -->
          <div v-if="imageTab === 'upload'" class="space-y-4">
            <!-- 숨겨진 파일 입력 -->
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              class="hidden"
              @change="handleFileSelect"
            />

            <!-- 업로드 영역 -->
            <div
              class="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg p-8 text-center hover:border-primary-400 dark:hover:border-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/30 transition-colors cursor-pointer"
              :class="{ 'pointer-events-none opacity-50': isUploading }"
              @click="triggerFileInput"
            >
              <svg class="w-12 h-12 mx-auto text-gray-400 dark:text-gray-500 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <p class="text-sm text-gray-600 dark:text-gray-300 mb-1">클릭하여 이미지 선택</p>
              <p class="text-xs text-gray-400 dark:text-gray-500">또는 에디터에 직접 붙여넣기/드래그</p>
            </div>

            <!-- 업로드 진행률 -->
            <div v-if="isUploading" class="space-y-2">
              <div class="flex items-center justify-between text-sm">
                <span class="text-gray-600 dark:text-gray-300">업로드 중...</span>
                <span class="text-primary-600 dark:text-primary-400 font-medium">{{ uploadProgress }}%</span>
              </div>
              <div class="h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                <div
                  class="h-full bg-primary-600 transition-all duration-300"
                  :style="{ width: `${uploadProgress}%` }"
                />
              </div>
            </div>

            <!-- 에러 메시지 -->
            <div v-if="uploadError" class="p-3 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg">
              <p class="text-sm text-red-600 dark:text-red-400">{{ uploadError }}</p>
            </div>

            <p class="text-xs text-gray-500 dark:text-gray-400 text-center">
              지원 형식: JPG, PNG, GIF, WEBP, SVG (최대 10MB)
            </p>
          </div>

          <!-- URL 입력 탭 -->
          <div v-else class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">이미지 URL</label>
              <input
                v-model="imageUrl"
                type="url"
                placeholder="https://example.com/image.jpg"
                class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                @keyup.enter="addImageByUrl"
              />
            </div>
            <p class="text-sm text-gray-500 dark:text-gray-400">
              외부 이미지 URL을 입력하세요.
            </p>
          </div>
        </div>

        <!-- 하단 버튼 -->
        <div class="flex justify-end gap-2 p-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
          <button
            type="button"
            class="px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700 rounded-lg transition-colors"
            :disabled="isUploading"
            @click="closeImageModal"
          >
            취소
          </button>
          <button
            v-if="imageTab === 'url'"
            type="button"
            class="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50"
            :disabled="!imageUrl"
            @click="addImageByUrl"
          >
            추가
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
/* 색상 선택기 외부 클릭 감지용 */
</style>
