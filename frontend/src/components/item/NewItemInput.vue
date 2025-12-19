<script setup lang="ts">
/**
 * 신규 업무 입력 컴포넌트
 * - 업무내용 입력 필드
 * - 작업 템플릿 자동완성
 * - 신규등록 버튼
 * Compact UI 적용
 */
import { ref, computed } from 'vue'
import { useItemStore } from '@/stores/item'
import { useToast } from '@/composables/useToast'
import { templateApi } from '@/api/template'
import { Button } from '@/components/common'
import Autocomplete from '@/components/common/Autocomplete.vue'
import type { AutocompleteOption } from '@/components/common/Autocomplete.vue'
import type { ItemCreateRequest } from '@/types/item'
import type { TaskTemplateSearchResult } from '@/types/template'

interface Props {
  boardId: number
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emit = defineEmits<{
  (e: 'created', itemId: number): void
}>()

const itemStore = useItemStore()
const toast = useToast()

// 상태
const inputValue = ref('')
const isLoading = ref(false)
const isSearching = ref(false)
const searchResults = ref<AutocompleteOption[]>([])
const selectedTemplate = ref<TaskTemplateSearchResult | null>(null)
const autocompleteRef = ref<InstanceType<typeof Autocomplete> | null>(null)

// 등록 버튼 활성화 여부
const canSubmit = computed(() => {
  return inputValue.value.trim().length > 0 && !isLoading.value && !props.disabled && props.boardId
})

// 검색 핸들러 (자동완성)
async function handleSearch(query: string) {
  if (query.length < 2) {
    searchResults.value = []
    return
  }

  isSearching.value = true
  try {
    const response = await templateApi.searchTemplates({
      keyword: query,
      limit: 10
    })

    if (response.success && response.data) {
      searchResults.value = response.data.map(template => ({
        value: template.templateId,
        label: template.content,
        description: `사용 ${template.useCount}회`,
        data: template
      }))
    } else {
      searchResults.value = []
    }
  } catch (error) {
    searchResults.value = []
  } finally {
    isSearching.value = false
  }
}

// 템플릿 선택 핸들러
function handleSelect(option: AutocompleteOption) {
  inputValue.value = option.label
  selectedTemplate.value = option.data as TaskTemplateSearchResult
}

// 신규 생성 핸들러 (자동완성에서 새로 등록)
async function handleCreate(value: string) {
  inputValue.value = value
  selectedTemplate.value = null
  // 바로 등록하지 않고 입력값만 설정
}

// 업무 등록
async function handleSubmit() {
  if (!canSubmit.value) return

  isLoading.value = true
  try {
    const data: ItemCreateRequest = {
      title: inputValue.value.trim(),
      status: 'NOT_STARTED',
      priority: 'NORMAL'
    }

    const item = await itemStore.createItem(props.boardId, data)
    if (item) {
      // 템플릿 사용 시 사용 횟수 증가
      if (selectedTemplate.value) {
        try {
          await templateApi.useTemplate(selectedTemplate.value.templateId)
        } catch {
          // 사용 횟수 증가 실패는 무시
        }
      }

      toast.success('업무가 등록되었습니다.')
      emit('created', item.itemId)

      // 입력값 초기화
      inputValue.value = ''
      selectedTemplate.value = null
      searchResults.value = []
    } else {
      toast.error('업무 등록에 실패했습니다.')
    }
  } catch (error) {
    toast.error('업무 등록에 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

// Enter 키 핸들러
function handleEnter() {
  if (canSubmit.value) {
    handleSubmit()
  }
}

// 포커스
function focus() {
  autocompleteRef.value?.focus()
}

// 클리어
function clear() {
  inputValue.value = ''
  selectedTemplate.value = null
  searchResults.value = []
  autocompleteRef.value?.clear()
}

defineExpose({ focus, clear })
</script>

<template>
  <div class="new-item-input">
    <div class="flex gap-2">
      <!-- 입력 필드 (자동완성) -->
      <div class="flex-1">
        <Autocomplete
          ref="autocompleteRef"
          v-model="inputValue"
          :options="searchResults"
          :loading="isSearching"
          :disabled="disabled || isLoading"
          placeholder="새 업무를 입력하세요..."
          :min-chars="2"
          :debounce-ms="300"
          :max-results="10"
          empty-message="일치하는 템플릿이 없습니다"
          @search="handleSearch"
          @select="handleSelect"
          @create="handleCreate"
          @enter="handleEnter"
        >
          <template #prefix>
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
          </template>
        </Autocomplete>
      </div>

      <!-- 등록 버튼 -->
      <Button
        :loading="isLoading"
        :disabled="!canSubmit"
        @click="handleSubmit"
      >
        등록
      </Button>
    </div>

    <!-- 선택된 템플릿 표시 -->
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0 -translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-1"
    >
      <div
        v-if="selectedTemplate"
        class="mt-2 px-3 py-2 bg-primary-50 border border-primary-200 rounded-lg flex items-center justify-between"
      >
        <div class="flex items-center gap-2">
          <svg class="w-4 h-4 text-primary-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <span class="text-[13px] text-primary-700">
            템플릿 선택됨
          </span>
          <span v-if="selectedTemplate.useCount > 0" class="text-[11px] text-primary-500">
            (사용 {{ selectedTemplate.useCount }}회)
          </span>
        </div>
        <button
          class="p-1 text-primary-400 hover:text-primary-600 rounded transition-colors"
          @click="selectedTemplate = null"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </Transition>
  </div>
</template>
