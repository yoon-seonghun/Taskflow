<script setup lang="ts">
/**
 * 댓글 목록 컴포넌트
 * - 댓글 표시/수정/삭제
 * - 새 댓글 입력
 * Compact UI 적용
 */
import { ref, computed, watch } from 'vue'
import { commentApi } from '@/api/comment'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import { Spinner } from '@/components/common'
import CommentInput from './CommentInput.vue'
import type { Comment } from '@/types/comment'

interface Props {
  itemId: number
}

const props = defineProps<Props>()

const toast = useToast()
const confirm = useConfirm()

// 상태
const comments = ref<Comment[]>([])
const isLoading = ref(false)
const isSubmitting = ref(false)
const editingId = ref<number | null>(null)
const editContent = ref('')

// 댓글 수
const commentCount = computed(() => comments.value.length)

// 댓글 목록 로드
async function loadComments() {
  if (!props.itemId) return

  isLoading.value = true
  try {
    const response = await commentApi.getComments(props.itemId)
    if (response.success && response.data) {
      comments.value = response.data
    }
  } catch (error) {
    toast.error('댓글을 불러오는데 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

// 댓글 등록
async function handleSubmit(content: string) {
  if (!props.itemId) return

  isSubmitting.value = true
  try {
    const response = await commentApi.createComment(props.itemId, { content })
    if (response.success && response.data) {
      comments.value.push(response.data)
      toast.success('댓글이 등록되었습니다.')
    } else {
      toast.error(response.message || '댓글 등록에 실패했습니다.')
    }
  } catch (error) {
    toast.error('댓글 등록에 실패했습니다.')
  } finally {
    isSubmitting.value = false
  }
}

// 수정 모드 시작
function startEdit(comment: Comment) {
  editingId.value = comment.commentId
  editContent.value = comment.content
}

// 수정 취소
function cancelEdit() {
  editingId.value = null
  editContent.value = ''
}

// 댓글 수정
async function handleUpdate(commentId: number) {
  if (!editContent.value.trim()) {
    toast.warning('내용을 입력해주세요.')
    return
  }

  try {
    const response = await commentApi.updateComment(commentId, {
      content: editContent.value.trim()
    })
    if (response.success && response.data) {
      const index = comments.value.findIndex(c => c.commentId === commentId)
      if (index !== -1) {
        comments.value[index] = response.data
      }
      toast.success('댓글이 수정되었습니다.')
      cancelEdit()
    } else {
      toast.error(response.message || '댓글 수정에 실패했습니다.')
    }
  } catch (error) {
    toast.error('댓글 수정에 실패했습니다.')
  }
}

// 댓글 삭제
async function handleDelete(commentId: number) {
  const confirmed = await confirm.show({
    title: '댓글 삭제',
    message: '이 댓글을 삭제하시겠습니까?',
    confirmText: '삭제',
    confirmType: 'danger'
  })

  if (confirmed) {
    try {
      const response = await commentApi.deleteComment(commentId)
      if (response.success) {
        comments.value = comments.value.filter(c => c.commentId !== commentId)
        toast.success('댓글이 삭제되었습니다.')
      } else {
        toast.error(response.message || '댓글 삭제에 실패했습니다.')
      }
    } catch (error) {
      toast.error('댓글 삭제에 실패했습니다.')
    }
  }
}

// 시간 포맷
function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMinutes = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffMinutes < 1) return '방금 전'
  if (diffMinutes < 60) return `${diffMinutes}분 전`
  if (diffHours < 24) return `${diffHours}시간 전`
  if (diffDays < 7) return `${diffDays}일 전`

  return date.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// itemId 변경 시 댓글 재로드
watch(() => props.itemId, () => {
  loadComments()
}, { immediate: true })

// 외부에 commentCount 노출
defineExpose({ commentCount, refresh: loadComments })
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- 헤더 -->
    <div class="flex items-center justify-between pb-3 border-b border-gray-200">
      <h3 class="text-[14px] font-medium text-gray-900">
        댓글
        <span v-if="commentCount > 0" class="text-gray-500 font-normal">
          ({{ commentCount }})
        </span>
      </h3>
    </div>

    <!-- 댓글 목록 -->
    <div class="flex-1 overflow-y-auto py-3 space-y-3">
      <!-- 로딩 -->
      <div v-if="isLoading" class="flex items-center justify-center py-8">
        <Spinner size="md" />
      </div>

      <!-- 빈 상태 -->
      <div v-else-if="comments.length === 0" class="text-center py-8">
        <svg class="w-10 h-10 mx-auto text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <p class="text-[13px] text-gray-500">댓글이 없습니다.</p>
      </div>

      <!-- 댓글 아이템들 -->
      <template v-else>
        <div
          v-for="comment in comments"
          :key="comment.commentId"
          class="group"
        >
          <!-- 댓글 헤더 -->
          <div class="flex items-center justify-between mb-1">
            <div class="flex items-center gap-2">
              <!-- 아바타 -->
              <div class="w-6 h-6 rounded-full bg-primary-100 flex items-center justify-center">
                <span class="text-[11px] font-medium text-primary-700">
                  {{ (comment.createdByName || '?').charAt(0) }}
                </span>
              </div>
              <span class="text-[13px] font-medium text-gray-900">
                {{ comment.createdByName || '알 수 없음' }}
              </span>
              <span class="text-[12px] text-gray-400">
                {{ formatTime(comment.createdAt) }}
              </span>
              <span v-if="comment.edited" class="text-[11px] text-gray-400">
                (수정됨)
              </span>
            </div>

            <!-- 액션 버튼 -->
            <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                v-if="editingId !== comment.commentId"
                class="p-1 text-gray-400 hover:text-gray-600 rounded"
                title="수정"
                @click="startEdit(comment)"
              >
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                </svg>
              </button>
              <button
                class="p-1 text-gray-400 hover:text-red-500 rounded"
                title="삭제"
                @click="handleDelete(comment.commentId)"
              >
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </div>

          <!-- 댓글 내용 -->
          <div class="pl-8">
            <!-- 수정 모드 -->
            <div v-if="editingId === comment.commentId" class="space-y-2">
              <textarea
                v-model="editContent"
                class="w-full px-3 py-2 text-[13px] border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                rows="2"
                @keydown.enter.exact.prevent="handleUpdate(comment.commentId)"
                @keydown.escape="cancelEdit"
              />
              <div class="flex justify-end gap-2">
                <button
                  class="px-2.5 py-1 text-[12px] text-gray-600 hover:bg-gray-100 rounded transition-colors"
                  @click="cancelEdit"
                >
                  취소
                </button>
                <button
                  class="px-2.5 py-1 text-[12px] text-white bg-primary-500 hover:bg-primary-600 rounded transition-colors"
                  @click="handleUpdate(comment.commentId)"
                >
                  저장
                </button>
              </div>
            </div>

            <!-- 읽기 모드 -->
            <p v-else class="text-[13px] text-gray-700 whitespace-pre-wrap break-words">
              {{ comment.content }}
            </p>
          </div>
        </div>
      </template>
    </div>

    <!-- 댓글 입력 -->
    <div class="pt-3 border-t border-gray-200">
      <CommentInput
        :loading="isSubmitting"
        @submit="handleSubmit"
      />
    </div>
  </div>
</template>
