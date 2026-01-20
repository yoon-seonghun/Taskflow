<script setup lang="ts">
/**
 * 간단한 카테고리 선택 모달 (v2.0)
 * - 테이블 행에서 카테고리 변경 시 사용
 * - 카테고리 선택만 제공 (cap_3.jpg 스타일)
 */
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { boardApi } from '@/api/board'
import { Button, Modal } from '@/components/common'
import type { BoardCategory } from '@/types/board'

interface Props {
  modelValue: boolean  // 모달 열림/닫힘
  boardId: number
  initialCategoryId?: number | null
  itemTitle?: string  // 업무 제목 (표시용)
}

const props = withDefaults(defineProps<Props>(), {
  initialCategoryId: null,
  itemTitle: ''
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'apply', categoryId: number | null): void
  (e: 'cancel'): void
}>()

// 상태
const loading = ref(false)
const boardCategories = ref<BoardCategory[]>([])
const selectedCategoryId = ref<number | null>(props.initialCategoryId)

let isComponentMounted = false

// 데이터 로드
async function loadCategories() {
  if (!props.boardId) return

  loading.value = true
  try {
    const response = await boardApi.getBoardCategories(props.boardId)
    if (!isComponentMounted) return

    if (response.success && response.data) {
      boardCategories.value = response.data
    }
  } catch (error) {
    console.error('Failed to load board categories:', error)
  } finally {
    if (isComponentMounted) {
      loading.value = false
    }
  }
}

// 적용
function handleApply() {
  emit('apply', selectedCategoryId.value)
  emit('update:modelValue', false)
}

// 취소
function handleCancel() {
  emit('cancel')
  emit('update:modelValue', false)
}

// 닫기
function handleClose() {
  emit('update:modelValue', false)
}

// 모달 열림 감지
watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    selectedCategoryId.value = props.initialCategoryId
    loadCategories()
  }
})

onMounted(() => {
  isComponentMounted = true
  if (props.modelValue) {
    loadCategories()
  }
})

onUnmounted(() => {
  isComponentMounted = false
})
</script>

<template>
  <Modal
    :model-value="modelValue"
    title="카테고리 변경"
    size="sm"
    @update:model-value="handleClose"
  >
    <div class="space-y-4">
      <!-- 업무 제목 표시 -->
      <div v-if="itemTitle" class="px-3 py-2 bg-gray-50 rounded-lg dark:bg-gray-800">
        <p class="text-sm text-gray-600 dark:text-gray-400">
          <span class="font-medium text-gray-700 dark:text-gray-300">{{ itemTitle }}</span>
        </p>
      </div>

      <!-- 로딩 -->
      <div v-if="loading" class="flex items-center justify-center py-4">
        <svg class="animate-spin h-6 w-6 text-blue-600" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      </div>

      <!-- 카테고리 선택 -->
      <div v-else class="space-y-2">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">카테고리</label>
        <select
          v-model="selectedCategoryId"
          class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-200"
        >
          <option :value="null">선택 안함</option>
          <option
            v-for="cat in boardCategories"
            :key="cat.categoryId"
            :value="cat.categoryId"
          >
            {{ cat.categoryName }}
            <template v-if="cat.isDefault">(기본)</template>
          </option>
        </select>
        <p class="text-xs text-gray-500 dark:text-gray-400">
          이 업무에 적용할 카테고리를 선택하세요
        </p>
      </div>
    </div>

    <!-- 푸터 버튼 -->
    <template #footer>
      <div class="flex justify-end gap-2">
        <Button variant="secondary" @click="handleCancel">
          취소
        </Button>
        <Button @click="handleApply">
          적용
        </Button>
      </div>
    </template>
  </Modal>
</template>
