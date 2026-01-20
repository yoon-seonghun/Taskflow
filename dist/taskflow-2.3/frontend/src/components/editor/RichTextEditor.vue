<script setup lang="ts">
/**
 * TipTap 리치 텍스트 에디터 컴포넌트
 * - 다양한 서식 지원
 * - 자동 저장 (debounce)
 * - 읽기 전용 모드 지원
 * - 이미지: 파일 업로드, 클립보드 붙여넣기, 드래그앤드롭 지원
 */
import { ref, watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Highlight from '@tiptap/extension-highlight'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import TextAlign from '@tiptap/extension-text-align'
import Color from '@tiptap/extension-color'
import TextStyle from '@tiptap/extension-text-style'
import Placeholder from '@tiptap/extension-placeholder'
import CharacterCount from '@tiptap/extension-character-count'
import EditorToolbar from './EditorToolbar.vue'
import { fileApi } from '@/api/file'
import { IMAGE_MIME_TYPES, MAX_FILE_SIZE } from '@/types/file'

interface Props {
  modelValue: string
  contentType?: 'HTML' | 'JSON' | 'MARKDOWN'
  placeholder?: string
  readonly?: boolean
  minHeight?: string
  maxHeight?: string
  autofocus?: boolean
  /** 이미지 업로드 시 연관 타입 (ITEM 등) */
  relatedType?: string
  /** 이미지 업로드 시 연관 ID */
  relatedId?: number
}

const props = withDefaults(defineProps<Props>(), {
  contentType: 'HTML',
  placeholder: '내용을 입력하세요...',
  readonly: false,
  minHeight: '200px',
  maxHeight: '600px',
  autofocus: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string, plainText: string): void
}>()

// 툴바 ref
const toolbarRef = ref<InstanceType<typeof EditorToolbar> | null>(null)

// 이미지 업로드 상태
const isImageUploading = ref(false)
const isDragOver = ref(false)

// 에디터 초기화
const editor = useEditor({
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [1, 2, 3]
      }
    }),
    Underline,
    Highlight.configure({ multicolor: true }),
    TaskList,
    TaskItem.configure({ nested: true }),
    Table.configure({ resizable: true }),
    TableRow,
    TableCell,
    TableHeader,
    Link.configure({
      openOnClick: false,
      HTMLAttributes: {
        class: 'text-primary-600 underline cursor-pointer'
      }
    }),
    Image.configure({
      inline: false,
      allowBase64: true
    }),
    TextAlign.configure({
      types: ['heading', 'paragraph']
    }),
    Color,
    TextStyle,
    Placeholder.configure({
      placeholder: props.placeholder
    }),
    CharacterCount.configure({
      limit: 100000
    })
  ],
  content: props.modelValue,
  editable: !props.readonly,
  autofocus: props.autofocus,
  editorProps: {
    attributes: {
      class: 'prose prose-sm max-w-none focus:outline-none p-4'
    }
  },
  onUpdate: ({ editor }) => {
    const html = editor.getHTML()
    const plainText = editor.getText()
    emit('update:modelValue', html)
    emit('change', html, plainText)
  }
})

// 외부 값 변경 시 동기화
watch(() => props.modelValue, (newValue) => {
  if (editor.value && newValue !== editor.value.getHTML()) {
    editor.value.commands.setContent(newValue, false)
  }
})

// readonly 변경 시 반영
watch(() => props.readonly, (newValue) => {
  if (editor.value) {
    editor.value.setEditable(!newValue)
  }
})

// 클린업
onBeforeUnmount(() => {
  editor.value?.destroy()
})

// 이미지 파일 업로드 처리
async function uploadImageFile(file: File): Promise<string | null> {
  // 이미지 파일 검증
  if (!IMAGE_MIME_TYPES.includes(file.type as any)) {
    console.warn('Not an image file:', file.type)
    return null
  }

  if (file.size > MAX_FILE_SIZE) {
    console.warn('File too large:', file.size)
    return null
  }

  isImageUploading.value = true

  try {
    const response = await fileApi.upload(file, {
      relatedType: props.relatedType,
      relatedId: props.relatedId
    })

    if (response.success && response.data?.url) {
      return response.data.url
    }
  } catch (error) {
    console.error('Image upload failed:', error)
  } finally {
    isImageUploading.value = false
  }

  return null
}

// 클립보드에서 이미지 붙여넣기 처리
async function handlePaste(event: ClipboardEvent) {
  if (props.readonly) return

  const items = event.clipboardData?.items
  if (!items) return

  for (const item of items) {
    if (item.type.startsWith('image/')) {
      event.preventDefault()
      const file = item.getAsFile()
      if (file) {
        const url = await uploadImageFile(file)
        if (url && editor.value) {
          editor.value.chain().focus().setImage({ src: url }).run()
        }
      }
      return
    }
  }
}

// 드래그 오버 핸들러
function handleDragOver(event: DragEvent) {
  if (props.readonly) return

  event.preventDefault()
  const hasImage = event.dataTransfer?.types.includes('Files')
  if (hasImage) {
    isDragOver.value = true
  }
}

// 드래그 떠남 핸들러
function handleDragLeave(event: DragEvent) {
  isDragOver.value = false
}

// 드롭 핸들러
async function handleDrop(event: DragEvent) {
  if (props.readonly) return

  event.preventDefault()
  isDragOver.value = false

  const files = event.dataTransfer?.files
  if (!files || files.length === 0) return

  // 이미지 파일 찾기
  for (const file of files) {
    if (file.type.startsWith('image/')) {
      const url = await uploadImageFile(file)
      if (url && editor.value) {
        editor.value.chain().focus().setImage({ src: url }).run()
      }
      return  // 첫 번째 이미지만 처리
    }
  }
}

// 이미지 업로드 상태 변경 핸들러
function handleImageUploading(uploading: boolean) {
  isImageUploading.value = uploading
}

// 외부에서 사용할 수 있도록 editor 인스턴스 노출
defineExpose({
  editor,
  uploadImageFile
})
</script>

<template>
  <!-- v2.2.1: h-full + flex로 높이 100% 지원 -->
  <div
    class="rich-text-editor border rounded-lg overflow-hidden bg-white dark:bg-gray-800 transition-colors h-full flex flex-col"
    :class="[
      isDragOver ? 'border-primary-400 ring-2 ring-primary-200 dark:ring-primary-800' : 'border-gray-200 dark:border-gray-700'
    ]"
  >
    <!-- 툴바 -->
    <EditorToolbar
      v-if="!readonly && editor"
      ref="toolbarRef"
      :editor="editor"
      :related-type="relatedType"
      :related-id="relatedId"
      @image-uploading="handleImageUploading"
    />

    <!-- 에디터 본문 (v2.2.1: flex-1 + min-h-0 추가) -->
    <div
      :class="[
        'editor-content-wrapper relative flex-1 min-h-0',
        readonly ? 'bg-gray-50 dark:bg-gray-900' : 'bg-white dark:bg-gray-800'
      ]"
      :style="{
        minHeight: minHeight !== '100%' ? minHeight : undefined,
        maxHeight: maxHeight !== '100%' ? maxHeight : undefined,
        overflowY: 'auto'
      }"
      @paste="handlePaste"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
    >
      <EditorContent :editor="editor" />

      <!-- 드래그 오버레이 -->
      <div
        v-if="isDragOver"
        class="absolute inset-0 bg-primary-50/80 dark:bg-primary-900/80 flex items-center justify-center pointer-events-none"
      >
        <div class="text-center">
          <svg class="w-12 h-12 mx-auto text-primary-500 dark:text-primary-400 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <p class="text-primary-600 dark:text-primary-400 font-medium">이미지를 놓으세요</p>
        </div>
      </div>

      <!-- 업로드 중 오버레이 -->
      <div
        v-if="isImageUploading"
        class="absolute inset-0 bg-white/80 dark:bg-gray-800/80 flex items-center justify-center"
      >
        <div class="text-center">
          <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin mb-2" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="text-gray-600 dark:text-gray-400 text-sm">이미지 업로드 중...</p>
        </div>
      </div>
    </div>

    <!-- 글자 수 표시 -->
    <div
      v-if="editor && !readonly"
      class="px-3 py-1.5 border-t border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 text-[12px] text-gray-500 dark:text-gray-400 flex justify-between"
    >
      <span>{{ editor.storage.characterCount.characters() }}자</span>
      <span v-if="editor.storage.characterCount.characters() >= 100000" class="text-red-500 dark:text-red-400">
        최대 글자 수에 도달했습니다
      </span>
    </div>
  </div>
</template>

<style>
/* TipTap 에디터 기본 스타일 */
.editor-content-wrapper .ProseMirror {
  min-height: inherit;
  outline: none;
  color: inherit;
}

.editor-content-wrapper .ProseMirror p.is-editor-empty:first-child::before {
  content: attr(data-placeholder);
  float: left;
  color: #9ca3af;
  pointer-events: none;
  height: 0;
}

:is(.dark) .editor-content-wrapper .ProseMirror p.is-editor-empty:first-child::before {
  color: #6b7280;
}

/* 목록 스타일 */
.editor-content-wrapper .ProseMirror ul {
  list-style-type: disc;
  padding-left: 1.5rem;
}

.editor-content-wrapper .ProseMirror ol {
  list-style-type: decimal;
  padding-left: 1.5rem;
}

/* 체크리스트 스타일 */
.editor-content-wrapper .ProseMirror ul[data-type="taskList"] {
  list-style: none;
  padding-left: 0;
}

.editor-content-wrapper .ProseMirror ul[data-type="taskList"] li {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

.editor-content-wrapper .ProseMirror ul[data-type="taskList"] li > label {
  flex-shrink: 0;
  margin-top: 0.25rem;
}

.editor-content-wrapper .ProseMirror ul[data-type="taskList"] li > label input[type="checkbox"] {
  cursor: pointer;
}

.editor-content-wrapper .ProseMirror ul[data-type="taskList"] li > div {
  flex: 1;
}

/* 인용문 스타일 */
.editor-content-wrapper .ProseMirror blockquote {
  border-left: 3px solid #e5e7eb;
  padding-left: 1rem;
  margin-left: 0;
  color: #6b7280;
  font-style: italic;
}

:is(.dark) .editor-content-wrapper .ProseMirror blockquote {
  border-left-color: #4b5563;
  color: #9ca3af;
}

/* 코드 블록 스타일 */
.editor-content-wrapper .ProseMirror pre {
  background-color: #1f2937;
  color: #f3f4f6;
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  font-family: ui-monospace, monospace;
  font-size: 0.875rem;
}

.editor-content-wrapper .ProseMirror code {
  background-color: #f3f4f6;
  color: #ef4444;
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-family: ui-monospace, monospace;
  font-size: 0.875rem;
}

:is(.dark) .editor-content-wrapper .ProseMirror code {
  background-color: #374151;
  color: #f87171;
}

.editor-content-wrapper .ProseMirror pre code {
  background-color: transparent;
  color: inherit;
  padding: 0;
}

/* 테이블 스타일 */
.editor-content-wrapper .ProseMirror table {
  border-collapse: collapse;
  width: 100%;
  margin: 1rem 0;
}

.editor-content-wrapper .ProseMirror th,
.editor-content-wrapper .ProseMirror td {
  border: 1px solid #e5e7eb;
  padding: 0.5rem 0.75rem;
  text-align: left;
}

:is(.dark) .editor-content-wrapper .ProseMirror th,
:is(.dark) .editor-content-wrapper .ProseMirror td {
  border-color: #4b5563;
}

.editor-content-wrapper .ProseMirror th {
  background-color: #f9fafb;
  font-weight: 600;
}

:is(.dark) .editor-content-wrapper .ProseMirror th {
  background-color: #1f2937;
}

.editor-content-wrapper .ProseMirror .selectedCell {
  background-color: #dbeafe;
}

:is(.dark) .editor-content-wrapper .ProseMirror .selectedCell {
  background-color: #1e3a5f;
}

/* 이미지 스타일 */
.editor-content-wrapper .ProseMirror img {
  max-width: 100%;
  height: auto;
  border-radius: 0.25rem;
}

.editor-content-wrapper .ProseMirror img.ProseMirror-selectednode {
  outline: 2px solid #3b82f6;
}

/* 수평선 스타일 */
.editor-content-wrapper .ProseMirror hr {
  border: none;
  border-top: 2px solid #e5e7eb;
  margin: 1.5rem 0;
}

:is(.dark) .editor-content-wrapper .ProseMirror hr {
  border-top-color: #4b5563;
}

/* 하이라이트 스타일 */
.editor-content-wrapper .ProseMirror mark {
  background-color: #fef08a;
  padding: 0.125rem 0;
}

:is(.dark) .editor-content-wrapper .ProseMirror mark {
  background-color: #854d0e;
  color: #fef08a;
}

/* 제목 스타일 */
.editor-content-wrapper .ProseMirror h1 {
  font-size: 1.5rem;
  font-weight: 700;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
}

.editor-content-wrapper .ProseMirror h2 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-top: 1.25rem;
  margin-bottom: 0.5rem;
}

.editor-content-wrapper .ProseMirror h3 {
  font-size: 1.125rem;
  font-weight: 600;
  margin-top: 1rem;
  margin-bottom: 0.5rem;
}

/* 문단 스타일 */
.editor-content-wrapper .ProseMirror p {
  margin-bottom: 0.5rem;
}

/* 링크 스타일 */
.editor-content-wrapper .ProseMirror a {
  color: #2563eb;
  text-decoration: underline;
}

.editor-content-wrapper .ProseMirror a:hover {
  color: #1d4ed8;
}

:is(.dark) .editor-content-wrapper .ProseMirror a {
  color: #60a5fa;
}

:is(.dark) .editor-content-wrapper .ProseMirror a:hover {
  color: #93c5fd;
}
</style>
