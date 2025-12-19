<script setup lang="ts">
/**
 * 댓글 입력 컴포넌트
 * - 텍스트 입력 + 등록 버튼
 * - Enter로 등록 (Shift+Enter는 줄바꿈)
 * Compact UI 적용
 */
import { ref, computed } from 'vue'
import { Button } from '@/components/common'

interface Props {
  placeholder?: string
  loading?: boolean
  disabled?: boolean
  autofocus?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '댓글을 입력하세요...',
  loading: false,
  disabled: false,
  autofocus: false
})

const emit = defineEmits<{
  (e: 'submit', content: string): void
}>()

const content = ref('')
const textareaRef = ref<HTMLTextAreaElement | null>(null)

// 유효성 검사
const isValid = computed(() => content.value.trim().length > 0)

// 제출
function handleSubmit() {
  if (!isValid.value || props.loading || props.disabled) return

  emit('submit', content.value.trim())
  content.value = ''

  // 높이 초기화
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }
}

// 키보드 핸들러
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSubmit()
  }
}

// 자동 높이 조절
function adjustHeight() {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    textareaRef.value.style.height = `${Math.min(textareaRef.value.scrollHeight, 120)}px`
  }
}

// 포커스
function focus() {
  textareaRef.value?.focus()
}

// 클리어
function clear() {
  content.value = ''
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }
}

defineExpose({ focus, clear })
</script>

<template>
  <div class="flex gap-2 items-end">
    <div class="flex-1 relative">
      <textarea
        ref="textareaRef"
        v-model="content"
        :placeholder="placeholder"
        :disabled="disabled || loading"
        :autofocus="autofocus"
        rows="1"
        class="w-full px-3 py-2 text-[13px] border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent disabled:bg-gray-100 disabled:cursor-not-allowed"
        style="min-height: 38px; max-height: 120px;"
        @input="adjustHeight"
        @keydown="handleKeydown"
      />
    </div>
    <Button
      size="sm"
      :loading="loading"
      :disabled="!isValid || disabled"
      @click="handleSubmit"
    >
      등록
    </Button>
  </div>
</template>
