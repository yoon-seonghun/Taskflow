<script setup lang="ts">
/**
 * 신규 업무 입력 컴포넌트 (v2.0)
 * - 업무내용 입력 필드
 * - 작업 템플릿 자동완성
 * - 속성 선택 패널 (접힘/펼침)
 * - 신규등록 버튼
 * Compact UI 적용
 *
 * 설계서 섹션 5.3, 5.5 기반:
 * - 업무 생성: 생성 폼 내 접힘/펼침 | 없음 (등록 버튼으로 통합)
 *
 * v2.0 변경: 속성 값 입력 → 속성 선택 방식으로 변경 (EntityEditModal 사용)
 */
import { ref, computed, watch } from 'vue'
import { useItemStore } from '@/stores/item'
import { useToast } from '@/composables/useToast'
import { templateApi } from '@/api/template'
import { boardApi } from '@/api/board'
import { Button, EntityEditModal } from '@/components/common'
import Autocomplete from '@/components/common/Autocomplete.vue'
import type { AutocompleteOption } from '@/components/common/Autocomplete.vue'
import type { ItemCreateRequest } from '@/types/item'
import type { TaskTemplateSearchResult } from '@/types/template'
import type { Category } from '@/types/category'

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

// v2.0: 속성 선택 모달 상태
const showPropertyModal = ref(false)
const selectedCategoryId = ref<number | null>(null)
const selectedPropertyIds = ref<number[]>([])
const selectedPropertyDefaults = ref<Record<number, string>>({})

// 보드 카테고리 목록
const boardCategories = ref<Category[]>([])

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

// 보드 카테고리 로드
async function loadBoardCategories() {
  if (!props.boardId) {
    boardCategories.value = []
    return
  }
  try {
    const response = await boardApi.getBoardCategories(props.boardId)
    if (response.success && response.data) {
      // BoardCategory를 Category 형태로 변환
      // 외부 연동 키 정책: ownerId 대신 ownerUsername 사용
      boardCategories.value = response.data.map(bc => ({
        categoryId: bc.categoryId,
        categoryCode: bc.categoryCode || '',
        categoryName: bc.categoryName,
        categoryColor: bc.categoryColor,
        description: bc.description || '',
        ownerType: bc.ownerType || 'USER',
        ownerUsername: bc.ownerUsername || '',
        sortOrder: bc.sortOrder || 0,
        useYn: 'Y'
      }))
    }
  } catch (error) {
    console.error('Failed to load board categories:', error)
  }
}

// boardId 변경 시 카테고리 로드
watch(() => props.boardId, () => {
  loadBoardCategories()
}, { immediate: true })

// 업무 등록 버튼 클릭 - 모달 열기
function handleSubmit() {
  if (!canSubmit.value) return
  // v2.0: 속성 선택 모달 열기
  showPropertyModal.value = true
}

// v2.0: 속성 선택 모달에서 저장 클릭
async function handlePropertySave(data: {
  name: string
  description: string
  color: string
  categoryIds: number[]
  categoryId: number | null
  propertyIds: number[]
  propertyDefaults: Record<number, string>
}) {
  isLoading.value = true
  try {
    // 모달에서 입력한 name 사용 (비어있으면 기존 inputValue 사용)
    const titleToUse = data.name.trim() || inputValue.value.trim()
    if (!titleToUse) {
      toast.error('업무명을 입력해주세요.')
      return
    }

    // DEBUG: 모달에서 전달받은 데이터 확인
    console.log('[NewItemInput] handlePropertySave 호출됨')
    console.log('[NewItemInput] data.propertyIds:', data.propertyIds)
    console.log('[NewItemInput] data.propertyDefaults:', data.propertyDefaults)

    // 선택된 속성들을 properties로 변환
    // 값이 없어도 빈 문자열로 설정하여 DB에 레코드 생성 (속성 "선택됨" 상태 유지)
    const properties: Record<number, unknown> = {}
    for (const propId of data.propertyIds) {
      if (data.propertyDefaults[propId] !== undefined && data.propertyDefaults[propId] !== '') {
        properties[propId] = data.propertyDefaults[propId]
      } else {
        // 값이 없어도 빈 문자열로 설정하여 DB에 레코드 생성
        properties[propId] = ''
      }
    }

    console.log('[NewItemInput] 변환된 properties:', properties)

    const requestData: ItemCreateRequest = {
      title: titleToUse,
      description: data.description || undefined,
      status: 'NOT_STARTED',
      priority: 'NORMAL',
      categoryId: data.categoryId ?? undefined,
      propertyIds: data.propertyIds.length > 0 ? data.propertyIds : undefined,
      properties: Object.keys(properties).length > 0 ? properties : undefined
    }

    console.log('[NewItemInput] API 전송 requestData:', requestData)

    const item = await itemStore.createItem(props.boardId, requestData)
    console.log('[NewItemInput] API 응답 item:', item)
    console.log('[NewItemInput] item.properties:', item?.properties)
    console.log('[NewItemInput] item.propertyValues:', item?.propertyValues)

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
      selectedCategoryId.value = data.categoryId  // 카테고리 유지 (연속 등록 편의)
      selectedPropertyIds.value = data.propertyIds  // 속성 선택 유지 (연속 등록 편의)
      selectedPropertyDefaults.value = {}
      showPropertyModal.value = false
    } else {
      toast.error('업무 등록에 실패했습니다.')
    }
  } catch (error) {
    toast.error('업무 등록에 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

// v2.0: 속성 편집 모달에서 취소 클릭
function handlePropertyCancel() {
  // 모달만 닫힘, 입력값은 유지
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
  // v2.0: 속성 초기화
  selectedCategoryId.value = null
  selectedPropertyIds.value = []
  selectedPropertyDefaults.value = {}
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

    <!-- v2.0: 속성 선택 모달 (EntityEditModal 사용) -->
    <EntityEditModal
      v-model="showPropertyModal"
      mode="item"
      title="새 업무 등록"
      :categories="boardCategories"
      :show-color="false"
      name-label="업무명"
      :initial-name="inputValue"
      :initial-category-id="selectedCategoryId"
      :initial-property-ids="selectedPropertyIds"
      :initial-property-defaults="selectedPropertyDefaults"
      enable-property-drag-drop
      @save="handlePropertySave"
      @cancel="handlePropertyCancel"
    />
  </div>
</template>
