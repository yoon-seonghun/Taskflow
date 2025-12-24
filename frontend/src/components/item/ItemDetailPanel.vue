<script setup lang="ts">
/**
 * 아이템 상세/편집 패널 컴포넌트
 * - PC: 3컬럼 레이아웃 (속성 | 마크다운 에디터 | 댓글)
 * - 각 컬럼 사이에 리사이즈 핸들로 폭 조절 가능
 * - Mobile: 탭 전환
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useItemStore } from '@/stores/item'
import { useBoardStore } from '@/stores/board'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import { Button, Spinner, MarkdownEditor } from '@/components/common'
import { FileAttachment } from '@/components/file'
import ItemForm from './ItemForm.vue'
import ItemTransferModal from './ItemTransferModal.vue'
import ItemShareModal from './ItemShareModal.vue'
import { CommentList } from '@/components/comment'
import type { Item, ItemUpdateRequest } from '@/types/item'

interface Props {
  itemId: number
  boardId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'updated', item: Item): void
  (e: 'deleted', itemId: number): void
}>()

const itemStore = useItemStore()
const boardStore = useBoardStore()
const toast = useToast()
const confirm = useConfirm()

// 상태
const item = ref<Item | null>(null)
const isLoading = ref(false)
const isSaving = ref(false)
const hasChanges = ref(false)
const activeTab = ref<'detail' | 'content' | 'comments'>('detail')
const commentListRef = ref<InstanceType<typeof CommentList> | null>(null)

// 에디터 content 상태
const editorContent = ref('')

// 모달 상태
const showTransferModal = ref(false)
const showShareModal = ref(false)
const canTransfer = ref(false)
const canShare = ref(false)

// 컬럼 폭 상태 (리사이즈용)
const PROP_WIDTH_KEY = 'taskflow_prop_width'
const COMMENT_WIDTH_KEY = 'taskflow_comment_width'
const propWidth = ref(240)  // 속성 패널 폭
const commentWidth = ref(280)  // 댓글 패널 폭
const isResizingProp = ref(false)
const isResizingComment = ref(false)
const containerRef = ref<HTMLDivElement | null>(null)

// 저장된 폭 불러오기
function loadColumnWidths() {
  const savedProp = localStorage.getItem(PROP_WIDTH_KEY)
  const savedComment = localStorage.getItem(COMMENT_WIDTH_KEY)
  if (savedProp) propWidth.value = Math.max(180, Math.min(400, parseInt(savedProp, 10)))
  if (savedComment) commentWidth.value = Math.max(200, Math.min(450, parseInt(savedComment, 10)))
}

// 폭 저장
function saveColumnWidths() {
  localStorage.setItem(PROP_WIDTH_KEY, propWidth.value.toString())
  localStorage.setItem(COMMENT_WIDTH_KEY, commentWidth.value.toString())
}

// 속성 패널 리사이즈
function startResizeProp(event: MouseEvent) {
  event.preventDefault()
  isResizingProp.value = true
  document.body.style.cursor = 'ew-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', handleResizeProp)
  document.addEventListener('mouseup', stopResizeProp)
}

function handleResizeProp(event: MouseEvent) {
  if (!isResizingProp.value || !containerRef.value) return
  const containerRect = containerRef.value.getBoundingClientRect()
  const newWidth = event.clientX - containerRect.left
  propWidth.value = Math.max(180, Math.min(400, newWidth))
}

function stopResizeProp() {
  isResizingProp.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  saveColumnWidths()
  document.removeEventListener('mousemove', handleResizeProp)
  document.removeEventListener('mouseup', stopResizeProp)
}

// 댓글 패널 리사이즈
function startResizeComment(event: MouseEvent) {
  event.preventDefault()
  isResizingComment.value = true
  document.body.style.cursor = 'ew-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', handleResizeComment)
  document.addEventListener('mouseup', stopResizeComment)
}

function handleResizeComment(event: MouseEvent) {
  if (!isResizingComment.value || !containerRef.value) return
  const containerRect = containerRef.value.getBoundingClientRect()
  const newWidth = containerRect.right - event.clientX
  commentWidth.value = Math.max(200, Math.min(450, newWidth))
}

function stopResizeComment() {
  isResizingComment.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  saveColumnWidths()
  document.removeEventListener('mousemove', handleResizeComment)
  document.removeEventListener('mouseup', stopResizeComment)
}

// 수정 정보
const modifiedInfo = computed(() => {
  if (!item.value) return null
  return {
    name: item.value.updatedByName || item.value.createdByName || '알 수 없음',
    time: formatDateTime(item.value.updatedAt || item.value.createdAt)
  }
})

// 댓글 수
const commentCount = computed(() => {
  return item.value?.commentCount || 0
})

// 아이템 로드
async function loadItem() {
  if (!props.itemId || !props.boardId) return

  isLoading.value = true
  try {
    const result = await itemStore.fetchItem(props.boardId, props.itemId)
    if (result) {
      item.value = result
      editorContent.value = result.description || ''
      itemStore.startEditing(props.itemId, result)

      // 권한 확인
      checkPermissions()
    } else {
      toast.error('업무를 불러오는데 실패했습니다.')
      emit('close')
    }
  } catch (error) {
    toast.error('업무를 불러오는데 실패했습니다.')
    emit('close')
  } finally {
    isLoading.value = false
  }
}

// 이관/공유 권한 확인
async function checkPermissions() {
  try {
    canTransfer.value = await itemStore.canTransferItem(props.boardId, props.itemId)
    canShare.value = await itemStore.canShareItem(props.boardId, props.itemId)
  } catch {
    canTransfer.value = false
    canShare.value = false
  }
}

// 이관 완료 핸들러
function handleTransferred(transferredItem: Item) {
  emit('deleted', props.itemId) // 현재 보드에서 제거
  emit('close')
}

// 공유 업데이트 핸들러
function handleShareUpdated() {
  // 필요시 아이템 새로고침
}

// 아이템 업데이트 (속성 변경) - description은 별도 저장 (병합하지 않음)
async function handleUpdate(data: ItemUpdateRequest) {
  if (!item.value || !props.boardId) return

  hasChanges.value = true
  isSaving.value = true

  // description은 별도로 저장하므로 여기서 포함하지 않음
  const updateData = { ...data }
  itemStore.updateEditingData(updateData as Partial<Item>)

  try {
    const result = await itemStore.updateItem(props.boardId, item.value.itemId, updateData)
    if (result) {
      // 현재 에디터 내용 유지 (서버 응답으로 덮어쓰지 않음)
      const currentDescription = editorContent.value
      item.value = result
      // 에디터 내용이 변경되지 않았으면 현재 값 유지
      if (result.description !== currentDescription) {
        editorContent.value = result.description || ''
      }
      hasChanges.value = false
      emit('updated', result)
    } else {
      toast.error('저장에 실패했습니다.')
    }
  } catch (error) {
    toast.error('저장에 실패했습니다.')
  } finally {
    isSaving.value = false
  }
}

// 마크다운 에디터 description 저장
async function handleContentSave(description: string) {
  if (!item.value || !props.boardId) return
  if (description === item.value.description) return

  hasChanges.value = true
  isSaving.value = true

  try {
    const result = await itemStore.updateItem(props.boardId, item.value.itemId, { description })
    if (result) {
      item.value = result
      hasChanges.value = false
      emit('updated', result)
    } else {
      toast.error('내용 저장에 실패했습니다.')
    }
  } catch (error) {
    toast.error('내용 저장에 실패했습니다.')
  } finally {
    isSaving.value = false
  }
}

// 완료 처리
async function handleComplete() {
  if (!item.value || !props.boardId) return

  const confirmed = await confirm.show({
    title: '완료 처리',
    message: '이 업무를 완료 처리하시겠습니까?',
    confirmText: '완료',
    confirmType: 'primary'
  })

  if (confirmed) {
    const result = await itemStore.completeItem(props.boardId, item.value.itemId)
    if (result) {
      toast.success('완료 처리되었습니다.')
      item.value = { ...item.value, status: 'COMPLETED' }
      emit('updated', item.value)
    } else {
      toast.error('완료 처리에 실패했습니다.')
    }
  }
}

// 삭제 처리
async function handleDelete() {
  if (!item.value || !props.boardId) return

  const confirmed = await confirm.show({
    title: '업무 삭제',
    message: '이 업무를 삭제하시겠습니까?',
    confirmText: '삭제',
    confirmType: 'danger'
  })

  if (confirmed) {
    const success = await itemStore.deleteItem(props.boardId, item.value.itemId)
    if (success) {
      toast.success('삭제되었습니다.')
      emit('deleted', item.value.itemId)
      emit('close')
    } else {
      toast.error('삭제에 실패했습니다.')
    }
  }
}

// 닫기
function handleClose() {
  itemStore.stopEditing()
  emit('close')
}

// 날짜 포맷
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// itemId 변경 시 아이템 재로드
watch(() => props.itemId, () => {
  loadItem()
}, { immediate: true })

// 모바일 감지
const isMobile = ref(window.innerWidth < 768)

function handleResize() {
  isMobile.value = window.innerWidth < 768
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadColumnWidths()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  itemStore.stopEditing()
})
</script>

<template>
  <div class="item-detail-panel h-full flex flex-col bg-white">
    <!-- 헤더 -->
    <div class="flex-shrink-0 px-4 py-3 border-b border-gray-200">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <button
            v-if="isMobile"
            class="p-1 -ml-1 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded"
            @click="handleClose"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <h2 class="text-[15px] font-semibold text-gray-900">업무 상세</h2>
        </div>

        <div class="flex items-center gap-1">
          <span v-if="isSaving" class="text-[12px] text-gray-400 mr-2">저장 중...</span>

          <!-- 이관 버튼 -->
          <Button
            v-if="item && canTransfer && item.status !== 'DELETED'"
            variant="ghost"
            size="sm"
            title="업무 이관"
            @click="showTransferModal = true"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
            </svg>
            이관
          </Button>

          <!-- 공유 버튼 -->
          <Button
            v-if="item && canShare && item.status !== 'DELETED'"
            variant="ghost"
            size="sm"
            title="업무 공유"
            @click="showShareModal = true"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
            </svg>
            공유
          </Button>

          <Button
            v-if="item && item.status !== 'COMPLETED' && item.status !== 'DELETED'"
            variant="ghost"
            size="sm"
            @click="handleComplete"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            완료
          </Button>

          <Button
            v-if="item && item.status !== 'DELETED'"
            variant="ghost"
            size="sm"
            class="text-red-600 hover:text-red-700 hover:bg-red-50"
            @click="handleDelete"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            삭제
          </Button>

          <button
            v-if="!isMobile"
            class="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded"
            @click="handleClose"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <div v-if="modifiedInfo" class="mt-2 text-[11px] text-gray-400">
        {{ modifiedInfo.name }}님이 {{ modifiedInfo.time }}에 수정
      </div>
    </div>

    <!-- 탭 (모바일) -->
    <div v-if="isMobile" class="flex-shrink-0 border-b border-gray-200">
      <div class="flex">
        <button
          class="flex-1 px-3 py-2.5 text-[13px] font-medium"
          :class="activeTab === 'detail' ? 'text-primary-600 border-b-2 border-primary-600' : 'text-gray-500'"
          @click="activeTab = 'detail'"
        >속성</button>
        <button
          class="flex-1 px-3 py-2.5 text-[13px] font-medium"
          :class="activeTab === 'content' ? 'text-primary-600 border-b-2 border-primary-600' : 'text-gray-500'"
          @click="activeTab = 'content'"
        >내용</button>
        <button
          class="flex-1 px-3 py-2.5 text-[13px] font-medium relative"
          :class="activeTab === 'comments' ? 'text-primary-600 border-b-2 border-primary-600' : 'text-gray-500'"
          @click="activeTab = 'comments'"
        >
          댓글
          <span v-if="commentCount > 0" class="ml-1 px-1.5 py-0.5 text-[10px] rounded-full bg-gray-100">{{ commentCount }}</span>
        </button>
      </div>
    </div>

    <!-- 메인 컨텐츠 -->
    <div class="flex-1 min-h-0 overflow-hidden">
      <!-- 로딩 -->
      <div v-if="isLoading" class="h-full flex items-center justify-center">
        <Spinner size="lg" />
      </div>

      <!-- 아이템 없음 -->
      <div v-else-if="!item" class="h-full flex items-center justify-center">
        <p class="text-[14px] text-gray-500">업무를 찾을 수 없습니다.</p>
      </div>

      <!-- PC 레이아웃: 3컬럼 + 리사이즈 핸들 -->
      <template v-else-if="!isMobile">
        <div ref="containerRef" class="h-full flex">
          <!-- 속성 패널 -->
          <div
            class="flex-shrink-0 overflow-y-auto p-3 bg-gray-50"
            :style="{ width: `${propWidth}px` }"
          >
            <ItemForm
              :item="item"
              :disabled="item.status === 'DELETED'"
              @update="handleUpdate"
            />
          </div>

          <!-- 리사이즈 핸들 (속성-에디터) -->
          <div
            class="w-2 flex-shrink-0 bg-gray-200 hover:bg-primary-300 cursor-ew-resize flex items-center justify-center group"
            :class="{ 'bg-primary-400': isResizingProp }"
            @mousedown="startResizeProp"
          >
            <div class="flex flex-col gap-0.5">
              <div class="w-0.5 h-4 bg-gray-400 group-hover:bg-primary-600 rounded-full" />
            </div>
          </div>

          <!-- 에디터 패널 -->
          <div class="flex-1 min-w-0 overflow-hidden flex flex-col">
            <!-- 마크다운 에디터 영역 -->
            <div class="flex-1 min-h-[200px] p-4 pb-0 flex flex-col">
              <label class="text-[12px] font-medium text-gray-600 mb-2 flex-shrink-0">내용</label>
              <div class="flex-1 min-h-0">
                <MarkdownEditor
                  v-model="editorContent"
                  :disabled="item.status === 'DELETED'"
                  :related-type="'ITEM'"
                  :related-id="item.itemId"
                  placeholder="마크다운 형식으로 상세 내용을 입력하세요..."
                  min-height="100%"
                  @save="handleContentSave"
                />
              </div>
            </div>

            <!-- 파일 첨부 영역 -->
            <div class="flex-shrink-0">
              <FileAttachment
                related-type="ITEM"
                :related-id="item.itemId"
                :disabled="item.status === 'DELETED'"
              />
            </div>
          </div>

          <!-- 리사이즈 핸들 (에디터-댓글) -->
          <div
            class="w-2 flex-shrink-0 bg-gray-200 hover:bg-primary-300 cursor-ew-resize flex items-center justify-center group"
            :class="{ 'bg-primary-400': isResizingComment }"
            @mousedown="startResizeComment"
          >
            <div class="flex flex-col gap-0.5">
              <div class="w-0.5 h-4 bg-gray-400 group-hover:bg-primary-600 rounded-full" />
            </div>
          </div>

          <!-- 댓글 패널 -->
          <div
            class="flex-shrink-0 overflow-y-auto p-3 bg-gray-50"
            :style="{ width: `${commentWidth}px` }"
          >
            <CommentList ref="commentListRef" :item-id="itemId" />
          </div>
        </div>
      </template>

      <!-- 모바일 레이아웃 -->
      <template v-else>
        <div v-show="activeTab === 'detail'" class="h-full overflow-y-auto p-4">
          <ItemForm :item="item" :disabled="item.status === 'DELETED'" @update="handleUpdate" />
        </div>
        <div v-show="activeTab === 'content'" class="h-full overflow-y-auto flex flex-col">
          <div class="flex-1 p-4">
            <MarkdownEditor
              v-model="editorContent"
              :disabled="item.status === 'DELETED'"
              :related-type="'ITEM'"
              :related-id="item.itemId"
              placeholder="마크다운 형식으로 상세 내용을 입력하세요..."
              min-height="calc(100vh - 350px)"
              @save="handleContentSave"
            />
          </div>
          <div class="flex-shrink-0">
            <FileAttachment
              related-type="ITEM"
              :related-id="item.itemId"
              :disabled="item.status === 'DELETED'"
            />
          </div>
        </div>
        <div v-show="activeTab === 'comments'" class="h-full overflow-y-auto p-4">
          <CommentList ref="commentListRef" :item-id="itemId" />
        </div>
      </template>
    </div>

    <!-- 하단 버튼 (모바일) -->
    <div
      v-if="isMobile && item && item.status !== 'DELETED'"
      class="flex-shrink-0 px-4 py-3 border-t border-gray-200 bg-white"
    >
      <div class="flex gap-2">
        <Button v-if="item.status !== 'COMPLETED'" variant="primary" class="flex-1" @click="handleComplete">완료 처리</Button>
        <Button variant="outline" class="flex-1" @click="handleClose">닫기</Button>
      </div>
    </div>

    <!-- 이관 모달 -->
    <ItemTransferModal
      v-if="item"
      :show="showTransferModal"
      :item="item"
      :board-id="boardId"
      @close="showTransferModal = false"
      @transferred="handleTransferred"
    />

    <!-- 공유 모달 -->
    <ItemShareModal
      v-if="item"
      :show="showShareModal"
      :item="item"
      :board-id="boardId"
      @close="showShareModal = false"
      @updated="handleShareUpdated"
    />
  </div>
</template>
