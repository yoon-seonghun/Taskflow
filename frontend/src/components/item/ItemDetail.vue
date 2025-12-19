<script setup lang="ts">
/**
 * 아이템 상세 패널 컴포넌트
 * - 아이템 정보 표시 및 편집
 * - 속성값 인라인 편집
 * - 댓글 기능
 * Compact UI 적용
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useItemStore } from '@/stores/item'
import { usePropertyStore } from '@/stores/property'
import { useBoardStore } from '@/stores/board'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import { Button, Input, Select, DatePicker, Badge, Spinner } from '@/components/common'
import { commentApi } from '@/api/comment'
import type { Item, ItemStatus, Priority } from '@/types/item'
import type { PropertyDef } from '@/types/property'
import type { Comment } from '@/types/comment'

interface Props {
  itemId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const itemStore = useItemStore()
const propertyStore = usePropertyStore()
const boardStore = useBoardStore()
const toast = useToast()
const confirm = useConfirm()

// 상태
const isLoading = ref(false)
const isSaving = ref(false)
const editedTitle = ref('')
const editedContent = ref('')
const isEditingTitle = ref(false)
const isEditingContent = ref(false)
const newComment = ref('')
const isAddingComment = ref(false)
const comments = ref<Comment[]>([])
const isLoadingComments = ref(false)

// 현재 아이템
const item = computed(() => itemStore.getItemById(props.itemId))
const boardId = computed(() => boardStore.currentBoardId)

// 속성 목록
const properties = computed(() => propertyStore.sortedProperties)

// 상태 옵션
const statusOptions = [
  { value: 'NOT_STARTED', label: '시작전', color: '#6B7280' },
  { value: 'IN_PROGRESS', label: '진행중', color: '#3B82F6' },
  { value: 'COMPLETED', label: '완료', color: '#10B981' },
  { value: 'DELETED', label: '삭제', color: '#EF4444' }
]

// 우선순위 옵션
const priorityOptions = [
  { value: 'URGENT', label: '긴급', color: '#EF4444' },
  { value: 'HIGH', label: '높음', color: '#F97316' },
  { value: 'NORMAL', label: '보통', color: '#3B82F6' },
  { value: 'LOW', label: '낮음', color: '#6B7280' }
]

// 상태 배지 색상
const statusColors: Record<ItemStatus, string> = {
  NOT_STARTED: 'gray',
  IN_PROGRESS: 'blue',
  COMPLETED: 'green',
  DELETED: 'red'
}

// 우선순위 배지 색상
const priorityColors: Record<Priority, string> = {
  URGENT: 'red',
  HIGH: 'orange',
  NORMAL: 'blue',
  LOW: 'gray'
}

// 데이터 로드
async function loadItem() {
  if (!boardId.value) return

  isLoading.value = true
  try {
    await itemStore.fetchItem(boardId.value, props.itemId)
    if (item.value) {
      editedTitle.value = item.value.title
      editedContent.value = item.value.content || ''
    }
    // 댓글 로드
    await loadComments()
  } catch (error) {
    toast.error('아이템 정보를 불러오는데 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

// 댓글 로드
async function loadComments() {
  isLoadingComments.value = true
  try {
    const response = await commentApi.getComments(props.itemId)
    if (response.success && response.data) {
      comments.value = response.data
    }
  } catch (error) {
    console.error('Failed to load comments:', error)
  } finally {
    isLoadingComments.value = false
  }
}

// 제목 편집 시작
function startEditTitle() {
  editedTitle.value = item.value?.title || ''
  isEditingTitle.value = true
}

// 제목 저장
async function saveTitle() {
  if (!item.value || !boardId.value) return
  if (editedTitle.value.trim() === item.value.title) {
    isEditingTitle.value = false
    return
  }

  isSaving.value = true
  try {
    const success = await itemStore.updateItem(boardId.value, props.itemId, {
      title: editedTitle.value.trim()
    })
    if (success) {
      toast.success('저장되었습니다.')
    } else {
      toast.error('저장에 실패했습니다.')
    }
  } finally {
    isSaving.value = false
    isEditingTitle.value = false
  }
}

// 내용 편집 시작
function startEditContent() {
  editedContent.value = item.value?.content || ''
  isEditingContent.value = true
}

// 내용 저장
async function saveContent() {
  if (!item.value || !boardId.value) return
  if (editedContent.value === item.value.content) {
    isEditingContent.value = false
    return
  }

  isSaving.value = true
  try {
    const success = await itemStore.updateItem(boardId.value, props.itemId, {
      content: editedContent.value
    })
    if (success) {
      toast.success('저장되었습니다.')
    } else {
      toast.error('저장에 실패했습니다.')
    }
  } finally {
    isSaving.value = false
    isEditingContent.value = false
  }
}

// 필드 업데이트
async function updateField(field: string, value: unknown) {
  if (!boardId.value) return

  const success = await itemStore.updateItem(boardId.value, props.itemId, {
    [field]: value
  })

  if (!success) {
    toast.error('업데이트에 실패했습니다.')
  }
}

// 속성값 업데이트
async function updateProperty(propertyId: number, value: unknown) {
  if (!boardId.value) return

  const success = await itemStore.updateItemProperty(boardId.value, props.itemId, propertyId, value)

  if (!success) {
    toast.error('속성값 업데이트에 실패했습니다.')
  }
}

// 속성값 가져오기
function getPropertyValue(propertyId: number): unknown {
  return item.value?.propertyValues?.[propertyId] ?? null
}

// 속성 옵션 가져오기
function getPropertyOptions(property: PropertyDef) {
  if (!property.options) return []
  return property.options
    .filter(opt => opt.useYn === 'Y')
    .map(opt => ({
      value: opt.optionId,
      label: opt.optionName,
      color: opt.color
    }))
}

// 완료 처리
async function handleComplete() {
  if (!boardId.value) return

  const success = await itemStore.completeItem(boardId.value, props.itemId)
  if (success) {
    toast.success('완료 처리되었습니다.')
  } else {
    toast.error('완료 처리에 실패했습니다.')
  }
}

// 삭제 처리
async function handleDelete() {
  if (!boardId.value) return

  const confirmed = await confirm.show({
    title: '업무 삭제',
    message: '이 업무를 삭제하시겠습니까?',
    confirmText: '삭제',
    confirmType: 'danger'
  })

  if (confirmed) {
    const success = await itemStore.deleteItem(boardId.value, props.itemId)
    if (success) {
      toast.success('삭제되었습니다.')
      emit('close')
    } else {
      toast.error('삭제에 실패했습니다.')
    }
  }
}

// 복원 처리
async function handleRestore() {
  if (!boardId.value) return

  const success = await itemStore.restoreItem(boardId.value, props.itemId)
  if (success) {
    toast.success('복원되었습니다.')
  } else {
    toast.error('복원에 실패했습니다.')
  }
}

// 댓글 추가
async function addComment() {
  if (!newComment.value.trim()) return

  isAddingComment.value = true
  try {
    const response = await commentApi.createComment(props.itemId, {
      content: newComment.value.trim()
    })
    if (response.success && response.data) {
      comments.value.unshift(response.data)
      newComment.value = ''
      toast.success('댓글이 등록되었습니다.')
    }
  } catch (error: any) {
    console.error('Failed to add comment:', error)
    const message = error.response?.data?.message || '댓글 등록에 실패했습니다.'
    toast.error(message)
  } finally {
    isAddingComment.value = false
  }
}

// 댓글 삭제
async function deleteComment(commentId: number) {
  const confirmed = await confirm.show({
    title: '댓글 삭제',
    message: '이 댓글을 삭제하시겠습니까?',
    confirmText: '삭제',
    cancelText: '취소',
    type: 'danger'
  })

  if (!confirmed) return

  try {
    const response = await commentApi.deleteComment(commentId)
    if (response.success) {
      comments.value = comments.value.filter(c => c.commentId !== commentId)
      toast.success('댓글이 삭제되었습니다.')
    }
  } catch (error: any) {
    console.error('Failed to delete comment:', error)
    const message = error.response?.data?.message || '댓글 삭제에 실패했습니다.'
    toast.error(message)
  }
}

// 날짜 포맷
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// itemId 변경 감지
watch(() => props.itemId, () => {
  loadItem()
}, { immediate: true })

onMounted(() => {
  loadItem()
})
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 로딩 상태 -->
    <div v-if="isLoading" class="flex-1 flex items-center justify-center">
      <Spinner size="lg" />
    </div>

    <!-- 아이템이 없는 경우 -->
    <div v-else-if="!item" class="flex-1 flex items-center justify-center">
      <p class="text-gray-500">아이템을 찾을 수 없습니다.</p>
    </div>

    <!-- 아이템 상세 -->
    <template v-else>
      <!-- 컨텐츠 영역 -->
      <div class="flex-1 overflow-y-auto">
        <!-- 제목 -->
        <div class="px-4 py-4 border-b border-gray-200">
          <template v-if="isEditingTitle">
            <div class="flex gap-2">
              <Input
                v-model="editedTitle"
                class="flex-1"
                @keydown.enter="saveTitle"
                @keydown.escape="isEditingTitle = false"
                autofocus
              />
              <Button size="sm" :loading="isSaving" @click="saveTitle">저장</Button>
              <Button size="sm" variant="ghost" @click="isEditingTitle = false">취소</Button>
            </div>
          </template>
          <template v-else>
            <h3
              class="text-lg font-semibold text-gray-900 cursor-pointer hover:bg-gray-50 rounded px-2 py-1 -mx-2"
              @click="startEditTitle"
            >
              {{ item.title }}
            </h3>
          </template>
        </div>

        <!-- 속성 섹션 -->
        <div class="px-4 py-4 space-y-3 border-b border-gray-200">
          <!-- 상태 -->
          <div class="flex items-center">
            <span class="w-24 text-[13px] text-gray-500">상태</span>
            <Select
              :model-value="item.status"
              :options="statusOptions"
              size="sm"
              class="flex-1"
              @update:model-value="updateField('status', $event)"
            />
          </div>

          <!-- 우선순위 -->
          <div class="flex items-center">
            <span class="w-24 text-[13px] text-gray-500">우선순위</span>
            <Select
              :model-value="item.priority"
              :options="priorityOptions"
              size="sm"
              class="flex-1"
              @update:model-value="updateField('priority', $event)"
            />
          </div>

          <!-- 담당자 -->
          <div class="flex items-center">
            <span class="w-24 text-[13px] text-gray-500">담당자</span>
            <span class="text-[13px] text-gray-700">{{ item.assigneeName || '-' }}</span>
          </div>

          <!-- 시작시간 -->
          <div class="flex items-center">
            <span class="w-24 text-[13px] text-gray-500">시작시간</span>
            <DatePicker
              :model-value="item.startTime"
              mode="datetime"
              size="sm"
              placeholder="선택"
              class="flex-1"
              @update:model-value="updateField('startTime', $event)"
            />
          </div>

          <!-- 완료시간 -->
          <div class="flex items-center">
            <span class="w-24 text-[13px] text-gray-500">완료시간</span>
            <DatePicker
              :model-value="item.endTime"
              mode="datetime"
              size="sm"
              placeholder="선택"
              class="flex-1"
              @update:model-value="updateField('endTime', $event)"
            />
          </div>

          <!-- 동적 속성 -->
          <template v-for="property in properties" :key="property.propertyId">
            <div class="flex items-center">
              <span class="w-24 text-[13px] text-gray-500">{{ property.propertyName }}</span>

              <!-- TEXT 타입 -->
              <template v-if="property.propertyType === 'TEXT'">
                <Input
                  :model-value="getPropertyValue(property.propertyId) as string"
                  size="sm"
                  class="flex-1"
                  @update:model-value="updateProperty(property.propertyId, $event)"
                />
              </template>

              <!-- NUMBER 타입 -->
              <template v-else-if="property.propertyType === 'NUMBER'">
                <Input
                  :model-value="getPropertyValue(property.propertyId) as string"
                  type="number"
                  size="sm"
                  class="flex-1"
                  @update:model-value="updateProperty(property.propertyId, $event)"
                />
              </template>

              <!-- DATE 타입 -->
              <template v-else-if="property.propertyType === 'DATE'">
                <DatePicker
                  :model-value="getPropertyValue(property.propertyId) as string"
                  size="sm"
                  class="flex-1"
                  @update:model-value="updateProperty(property.propertyId, $event)"
                />
              </template>

              <!-- SELECT 타입 -->
              <template v-else-if="property.propertyType === 'SELECT'">
                <Select
                  :model-value="getPropertyValue(property.propertyId) as number"
                  :options="getPropertyOptions(property)"
                  size="sm"
                  clearable
                  class="flex-1"
                  @update:model-value="updateProperty(property.propertyId, $event)"
                />
              </template>

              <!-- CHECKBOX 타입 -->
              <template v-else-if="property.propertyType === 'CHECKBOX'">
                <input
                  type="checkbox"
                  :checked="getPropertyValue(property.propertyId) as boolean"
                  class="w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                  @change="updateProperty(property.propertyId, ($event.target as HTMLInputElement).checked)"
                />
              </template>
            </div>
          </template>
        </div>

        <!-- 내용 섹션 -->
        <div class="px-4 py-4 border-b border-gray-200">
          <h4 class="text-[13px] font-medium text-gray-700 mb-2">내용</h4>
          <template v-if="isEditingContent">
            <textarea
              v-model="editedContent"
              class="w-full h-32 px-3 py-2 text-[13px] border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              @keydown.escape="isEditingContent = false"
            />
            <div class="flex gap-2 mt-2">
              <Button size="sm" :loading="isSaving" @click="saveContent">저장</Button>
              <Button size="sm" variant="ghost" @click="isEditingContent = false">취소</Button>
            </div>
          </template>
          <template v-else>
            <div
              class="min-h-[80px] text-[13px] text-gray-700 whitespace-pre-wrap cursor-pointer hover:bg-gray-50 rounded p-2 -mx-2"
              @click="startEditContent"
            >
              {{ item.content || '내용을 입력하세요...' }}
            </div>
          </template>
        </div>

        <!-- 댓글 섹션 -->
        <div class="px-4 py-4">
          <h4 class="text-[13px] font-medium text-gray-700 mb-3">
            댓글 ({{ item.commentCount || 0 }})
          </h4>

          <!-- 댓글 입력 -->
          <div class="flex gap-2 mb-4">
            <Input
              v-model="newComment"
              placeholder="댓글을 입력하세요..."
              class="flex-1"
              @keydown.enter="addComment"
            />
            <Button
              size="sm"
              :loading="isAddingComment"
              :disabled="!newComment.trim()"
              @click="addComment"
            >
              등록
            </Button>
          </div>

          <!-- 댓글 목록 -->
          <div v-if="isLoadingComments" class="text-center py-4">
            <Spinner size="sm" />
          </div>
          <div v-else-if="comments.length === 0" class="text-center text-[13px] text-gray-500 py-4">
            댓글이 없습니다.
          </div>
          <div v-else class="space-y-3">
            <div
              v-for="comment in comments"
              :key="comment.commentId"
              class="p-3 bg-gray-50 rounded-lg"
            >
              <div class="flex items-start justify-between gap-2">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="text-[13px] font-medium text-gray-900">
                      {{ comment.createdByName || '사용자' }}
                    </span>
                    <span class="text-[11px] text-gray-400">
                      {{ formatDateTime(comment.createdAt) }}
                    </span>
                    <span v-if="comment.edited" class="text-[11px] text-gray-400">(수정됨)</span>
                  </div>
                  <p class="text-[13px] text-gray-700 whitespace-pre-wrap break-words">
                    {{ comment.content }}
                  </p>
                </div>
                <button
                  type="button"
                  class="p-1 text-gray-400 hover:text-red-500 transition-colors"
                  title="삭제"
                  @click="deleteComment(comment.commentId)"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 메타 정보 -->
        <div class="px-4 py-3 bg-gray-50 text-[12px] text-gray-500">
          <div class="flex items-center gap-4">
            <span>등록: {{ item.createdByName }} ({{ formatDateTime(item.createdAt) }})</span>
            <span v-if="item.updatedAt">수정: {{ item.updatedByName }} ({{ formatDateTime(item.updatedAt) }})</span>
          </div>
        </div>
      </div>

      <!-- 하단 액션 버튼 -->
      <div class="flex-shrink-0 px-4 py-3 border-t border-gray-200 bg-white">
        <div class="flex justify-between">
          <div>
            <Button
              v-if="item.status === 'COMPLETED' || item.status === 'DELETED'"
              variant="secondary"
              size="sm"
              @click="handleRestore"
            >
              <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              복원
            </Button>
          </div>

          <div class="flex gap-2">
            <Button
              v-if="item.status !== 'COMPLETED' && item.status !== 'DELETED'"
              variant="secondary"
              size="sm"
              @click="handleComplete"
            >
              <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              완료
            </Button>
            <Button
              v-if="item.status !== 'DELETED'"
              variant="danger"
              size="sm"
              @click="handleDelete"
            >
              <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              삭제
            </Button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
