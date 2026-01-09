<script setup lang="ts">
/**
 * 업무 속성 선택 컴포넌트 (v2.0 통합 속성 선택 패널)
 *
 * 설계서 섹션 5.3, 5.5 기반:
 * - 업무 생성 시 접힘/펼침 패널
 * - 기본 상태: 접힘 "▶ 속성 편집 (3개 사용 중 / 전체 12개)"
 * - 보드에서 상속된 속성 및 기본값 표시
 * - 속성 선택/해제 및 값 편집 가능
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { boardApi } from '@/api/board'
import { categoryApi } from '@/api/category'
import { propertyApi } from '@/api/property'
import { useToast } from '@/composables/useToast'
import type { BoardProperty, BoardCategory } from '@/types/board'
import type { CategoryProperty } from '@/types/category'
import type { PropertyDef, PropertyOption } from '@/types/property'

const toast = useToast()

interface Props {
  boardId: number
  modelValue?: Record<number, unknown>  // propertyId -> value
  categoryId?: number | null
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => ({}),
  categoryId: null
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: Record<number, unknown>): void
  (e: 'update:categoryId', value: number | null): void
}>()

// 상태
const loading = ref(false)
const loadError = ref<string | null>(null)
const expanded = ref(false)
const boardProperties = ref<BoardProperty[]>([])
const boardCategories = ref<BoardCategory[]>([])
const categoryProperties = ref<CategoryProperty[]>([])
const categoryLoading = ref(false)
const propertyOptions = ref<Map<number, PropertyOption[]>>(new Map())

// 컴포넌트 생명주기 추적 (비동기 작업 안전성)
let isComponentMounted = false

// 선택된 속성 ID 및 값
const selectedPropertyIds = ref<Set<number>>(new Set())
const propertyValues = ref<Record<number, unknown>>({})

// 기본 속성 정의 (TB_ITEM 고정 컬럼 - 설계서 기준)
const basicProperties = [
  { id: -1, name: '업무내용', type: 'TEXT', required: true },
  { id: -2, name: '상태', type: 'SELECT', required: true },
  { id: -3, name: '우선순위', type: 'SELECT', required: true },
  { id: -4, name: '요청일', type: 'DATE', required: false },
  { id: -5, name: '마감일', type: 'DATE', required: false },
  { id: -6, name: '담당자', type: 'USER', required: false },
  { id: -7, name: '그룹', type: 'SELECT', required: false },
]

// 선택된 카테고리
const selectedCategoryId = ref<number | null>(props.categoryId)

// 기본 카테고리 찾기
const defaultCategory = computed(() => {
  return boardCategories.value.find(c => c.isDefault)
})

// 선택된 속성 개수
const selectedCount = computed(() => selectedPropertyIds.value.size)

// 전체 속성 개수 (보드 속성 + 카테고리 속성, 중복 제외)
const totalCount = computed(() => {
  const boardPropIds = new Set(boardProperties.value.map(p => p.propertyId))
  const catPropIds = categoryProperties.value.filter(p => !boardPropIds.has(p.propertyId))
  return boardProperties.value.length + catPropIds.length
})

// 요약 텍스트
const summaryText = computed(() => {
  const catCount = categoryProperties.value.length
  if (catCount > 0) {
    return `${selectedCount.value}개 사용 중 / 전체 ${totalCount.value}개 (카테고리: ${catCount}개)`
  }
  return `${selectedCount.value}개 사용 중 / 전체 ${totalCount.value}개`
})

// 보드 속성 타입별 그룹핑
const globalProperties = computed(() => {
  return boardProperties.value.filter(p => p.ownerType === 'GLOBAL')
})

const managerProperties = computed(() => {
  return boardProperties.value.filter(p => p.ownerType === 'MANAGER')
})

const userProperties = computed(() => {
  return boardProperties.value.filter(p => p.ownerType === 'USER')
})

// 카테고리 속성 (보드 속성과 중복 제외)
const filteredCategoryProperties = computed(() => {
  const boardPropIds = new Set(boardProperties.value.map(p => p.propertyId))
  return categoryProperties.value.filter(p => !boardPropIds.has(p.propertyId))
})

// 토글
function toggleExpanded() {
  expanded.value = !expanded.value
}

// 속성 선택 토글
function toggleProperty(propertyId: number) {
  if (selectedPropertyIds.value.has(propertyId)) {
    selectedPropertyIds.value.delete(propertyId)
    // 값도 제거
    delete propertyValues.value[propertyId]
  } else {
    selectedPropertyIds.value.add(propertyId)
    // 기본값 적용
    const prop = boardProperties.value.find(p => p.propertyId === propertyId)
    if (prop?.defaultValue) {
      propertyValues.value[propertyId] = prop.defaultValue
    }
  }
  emitValue()
}

// 속성값 변경
function updatePropertyValue(propertyId: number, value: unknown) {
  propertyValues.value[propertyId] = value
  // 값이 있으면 자동 선택
  if (value !== null && value !== undefined && value !== '') {
    selectedPropertyIds.value.add(propertyId)
  }
  emitValue()
}

// 카테고리 변경
async function updateCategory(categoryId: number | null) {
  selectedCategoryId.value = categoryId
  emit('update:categoryId', categoryId)

  // 카테고리 속성 로드
  await loadCategoryProperties(categoryId)
}

// 카테고리 속성 로드
async function loadCategoryProperties(categoryId: number | null) {
  if (!categoryId) {
    categoryProperties.value = []
    return
  }

  categoryLoading.value = true
  try {
    const response = await categoryApi.getProperties(categoryId)
    if (!isComponentMounted) return

    if (response.success && response.data) {
      categoryProperties.value = response.data

      // 카테고리 속성을 선택 목록에 추가하고 기본값 적용
      const optionLoadPromises: Promise<void>[] = []

      for (const prop of response.data) {
        // 아직 선택되지 않은 속성만 추가 (보드 속성과 중복 방지)
        if (!selectedPropertyIds.value.has(prop.propertyId)) {
          selectedPropertyIds.value.add(prop.propertyId)
          if (prop.defaultValue) {
            propertyValues.value[prop.propertyId] = prop.defaultValue
          }
        }

        // SELECT/MULTI_SELECT 타입은 옵션 로드
        if (prop.propertyType === 'SELECT' || prop.propertyType === 'MULTI_SELECT') {
          optionLoadPromises.push(loadPropertyOptions(prop.propertyId))
        }
      }

      await Promise.allSettled(optionLoadPromises)
      emitValue()
    }
  } catch (error) {
    if (!isComponentMounted) return
    console.error('Failed to load category properties:', error)
    toast.error('카테고리 속성 로드 중 오류가 발생했습니다.')
  } finally {
    if (isComponentMounted) {
      categoryLoading.value = false
    }
  }
}

// 값 emit
function emitValue() {
  const result: Record<number, unknown> = {}
  for (const propertyId of selectedPropertyIds.value) {
    if (propertyValues.value[propertyId] !== undefined) {
      result[propertyId] = propertyValues.value[propertyId]
    }
  }
  emit('update:modelValue', result)
}

// 속성이 선택되었는지 확인
function isSelected(propertyId: number): boolean {
  return selectedPropertyIds.value.has(propertyId)
}

// 속성 옵션 로드
async function loadPropertyOptions(propertyId: number) {
  if (propertyOptions.value.has(propertyId)) return

  try {
    const response = await propertyApi.getOptions(propertyId)
    // 컴포넌트 파괴 후 상태 업데이트 방지
    if (!isComponentMounted) return

    if (response.success && response.data) {
      propertyOptions.value.set(propertyId, response.data)
    }
  } catch (error) {
    if (!isComponentMounted) return
    console.error('Failed to load property options:', error)
  }
}

// 데이터 로드
async function loadData() {
  if (!props.boardId) return

  loading.value = true
  loadError.value = null

  try {
    const [propertiesRes, categoriesRes] = await Promise.all([
      boardApi.getBoardProperties(props.boardId),
      boardApi.getBoardCategories(props.boardId)
    ])

    // 컴포넌트 파괴 후 상태 업데이트 방지
    if (!isComponentMounted) return

    if (propertiesRes.success && propertiesRes.data) {
      boardProperties.value = propertiesRes.data

      // 초기 선택 상태 설정 (모든 보드 속성은 기본 선택)
      selectedPropertyIds.value.clear()
      propertyValues.value = {}

      // 속성 옵션 병렬 로드
      const optionLoadPromises: Promise<void>[] = []

      for (const prop of propertiesRes.data) {
        selectedPropertyIds.value.add(prop.propertyId)
        // 기본값 적용
        if (prop.defaultValue) {
          propertyValues.value[prop.propertyId] = prop.defaultValue
        }

        // SELECT/MULTI_SELECT 타입은 옵션 로드
        if (prop.propertyType === 'SELECT' || prop.propertyType === 'MULTI_SELECT') {
          optionLoadPromises.push(loadPropertyOptions(prop.propertyId))
        }
      }

      // 병렬 로드 (일부 실패해도 진행)
      await Promise.allSettled(optionLoadPromises)

      emitValue()
    }

    if (categoriesRes.success && categoriesRes.data) {
      boardCategories.value = categoriesRes.data

      // 기본 카테고리 자동 선택
      const defaultCat = categoriesRes.data.find(c => c.isDefault)
      if (defaultCat && !selectedCategoryId.value) {
        selectedCategoryId.value = defaultCat.categoryId
        emit('update:categoryId', defaultCat.categoryId)

        // 기본 카테고리의 속성도 로드
        await loadCategoryProperties(defaultCat.categoryId)
      }
    }
  } catch (error) {
    if (!isComponentMounted) return
    console.error('Failed to load board data:', error)
    loadError.value = '보드 속성을 불러올 수 없습니다.'
    toast.error('보드 정보 로드 중 오류가 발생했습니다.')
  } finally {
    if (isComponentMounted) {
      loading.value = false
    }
  }
}

// 보드 ID 변경 감지
let previousBoardId: number | null = null
watch(() => props.boardId, (newId) => {
  if (newId !== previousBoardId) {
    previousBoardId = newId
    loadData()
  }
}, { immediate: true })

// props.modelValue 변경 감지
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    propertyValues.value = { ...newVal }
    selectedPropertyIds.value = new Set(Object.keys(newVal).map(Number))
  }
}, { deep: true })

// 컴포넌트 마운트
onMounted(() => {
  isComponentMounted = true
})

// 메모리 정리
onUnmounted(() => {
  isComponentMounted = false
  selectedPropertyIds.value.clear()
  propertyOptions.value.clear()
  categoryProperties.value = []
  previousBoardId = null
})
</script>

<template>
  <div class="item-property-selector">
    <!-- 접힘/펼침 헤더 -->
    <button
      type="button"
      class="w-full flex items-center justify-between px-3 py-2 text-left bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors"
      @click="toggleExpanded"
    >
      <div class="flex items-center gap-2">
        <!-- 화살표 아이콘 -->
        <svg
          class="w-4 h-4 text-gray-500 transition-transform duration-200"
          :class="{ 'rotate-90': expanded }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
        <span class="text-sm font-medium text-gray-700">속성 편집</span>
      </div>
      <span class="text-xs text-gray-500">({{ summaryText }})</span>
    </button>

    <!-- 펼침 내용 -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 -translate-y-2"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-2"
    >
      <div v-if="expanded" class="mt-2 space-y-3">
        <!-- 로딩 -->
        <div v-if="loading" class="flex items-center justify-center py-4">
          <svg class="animate-spin h-5 w-5 text-blue-600" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
        </div>

        <!-- 에러 표시 -->
        <div v-else-if="loadError" class="text-center py-4">
          <p class="text-red-500 text-xs mb-2">{{ loadError }}</p>
          <button
            type="button"
            class="text-xs text-blue-600 hover:underline"
            @click="loadData"
          >
            다시 시도
          </button>
        </div>

        <div v-else class="space-y-3 max-h-64 overflow-y-auto pr-1">
          <!-- 카테고리 선택 -->
          <div v-if="boardCategories.length > 0" class="property-group">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">카테고리</span>
              <span v-if="defaultCategory" class="text-xs text-gray-400">
                기본: {{ defaultCategory.categoryName }}
              </span>
            </div>
            <select
              :value="selectedCategoryId"
              class="w-full px-2.5 py-1.5 text-xs border border-gray-200 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
              @change="updateCategory(($event.target as HTMLSelectElement).value ? Number(($event.target as HTMLSelectElement).value) : null)"
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
          </div>

          <!-- 카테고리 속성 (선택한 카테고리에서 상속) -->
          <div v-if="categoryLoading" class="property-group">
            <div class="flex items-center justify-center py-2">
              <svg class="animate-spin h-4 w-4 text-orange-600" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              <span class="ml-2 text-xs text-gray-500">카테고리 속성 로딩 중...</span>
            </div>
          </div>
          <div v-else-if="filteredCategoryProperties.length > 0" class="property-group">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-medium text-orange-600 uppercase tracking-wide">카테고리 속성</span>
              <span class="text-xs text-gray-400">
                {{ filteredCategoryProperties.filter(p => isSelected(p.propertyId)).length }}/{{ filteredCategoryProperties.length }}
              </span>
            </div>
            <div class="space-y-1.5">
              <label
                v-for="prop in filteredCategoryProperties"
                :key="prop.propertyId"
                class="flex items-center gap-2 px-2 py-1.5 rounded text-xs cursor-pointer transition-colors"
                :class="isSelected(prop.propertyId)
                  ? 'bg-orange-50 border border-orange-200'
                  : 'bg-white border border-gray-200 hover:border-gray-300'"
              >
                <input
                  type="checkbox"
                  :checked="isSelected(prop.propertyId)"
                  class="w-3.5 h-3.5 text-orange-600 border-gray-300 rounded cursor-pointer focus:ring-orange-500"
                  @change="toggleProperty(prop.propertyId)"
                />
                <span class="flex-1" :class="isSelected(prop.propertyId) ? 'text-orange-700' : 'text-gray-600'">
                  {{ prop.propertyName }}
                  <span v-if="prop.requiredYn === 'Y'" class="text-red-500 ml-0.5">*</span>
                </span>
                <!-- 값 입력 (선택된 경우) -->
                <template v-if="isSelected(prop.propertyId)">
                  <select
                    v-if="prop.propertyType === 'SELECT'"
                    :value="propertyValues[prop.propertyId]"
                    class="px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-orange-500"
                    @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLSelectElement).value)"
                    @click.stop
                  >
                    <option value="">선택</option>
                    <option
                      v-for="opt in propertyOptions.get(prop.propertyId) || []"
                      :key="opt.optionId"
                      :value="opt.optionId"
                    >
                      {{ opt.optionName }}
                    </option>
                  </select>
                  <input
                    v-else-if="prop.propertyType === 'TEXT'"
                    type="text"
                    :value="propertyValues[prop.propertyId]"
                    class="w-24 px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-orange-500"
                    placeholder="값 입력"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <input
                    v-else-if="prop.propertyType === 'NUMBER'"
                    type="number"
                    :value="propertyValues[prop.propertyId]"
                    class="w-20 px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-orange-500"
                    placeholder="0"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <input
                    v-else-if="prop.propertyType === 'DATE'"
                    type="date"
                    :value="propertyValues[prop.propertyId]"
                    class="px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-orange-500"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <span v-else-if="prop.defaultValue" class="text-xs text-gray-400">
                    기본: {{ prop.defaultValue }}
                  </span>
                </template>
              </label>
            </div>
          </div>

          <!-- 기본 속성 (필수) -->
          <div class="property-group">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">기본 속성</span>
              <span class="text-xs text-gray-400">필수</span>
            </div>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="prop in basicProperties"
                :key="prop.id"
                class="inline-flex items-center gap-1 px-2 py-1 bg-gray-100 text-gray-500 rounded text-xs"
              >
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                {{ prop.name }}
              </span>
            </div>
          </div>

          <!-- 글로벌 속성 -->
          <div v-if="globalProperties.length > 0" class="property-group">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">글로벌 속성</span>
              <span class="text-xs text-gray-400">
                {{ globalProperties.filter(p => isSelected(p.propertyId)).length }}/{{ globalProperties.length }}
              </span>
            </div>
            <div class="space-y-1.5">
              <label
                v-for="prop in globalProperties"
                :key="prop.propertyId"
                class="flex items-center gap-2 px-2 py-1.5 rounded text-xs cursor-pointer transition-colors"
                :class="isSelected(prop.propertyId)
                  ? 'bg-blue-50 border border-blue-200'
                  : 'bg-white border border-gray-200 hover:border-gray-300'"
              >
                <input
                  type="checkbox"
                  :checked="isSelected(prop.propertyId)"
                  class="w-3.5 h-3.5 text-blue-600 border-gray-300 rounded cursor-pointer focus:ring-blue-500"
                  @change="toggleProperty(prop.propertyId)"
                />
                <span class="flex-1" :class="isSelected(prop.propertyId) ? 'text-blue-700' : 'text-gray-600'">
                  {{ prop.propertyName }}
                </span>
                <!-- 값 입력 (선택된 경우) -->
                <template v-if="isSelected(prop.propertyId)">
                  <select
                    v-if="prop.propertyType === 'SELECT'"
                    :value="propertyValues[prop.propertyId]"
                    class="px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-blue-500"
                    @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLSelectElement).value)"
                    @click.stop
                  >
                    <option value="">선택</option>
                    <option
                      v-for="opt in propertyOptions.get(prop.propertyId) || []"
                      :key="opt.optionId"
                      :value="opt.optionId"
                    >
                      {{ opt.optionName }}
                    </option>
                  </select>
                  <input
                    v-else-if="prop.propertyType === 'TEXT'"
                    type="text"
                    :value="propertyValues[prop.propertyId]"
                    class="w-24 px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-blue-500"
                    placeholder="값 입력"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <input
                    v-else-if="prop.propertyType === 'NUMBER'"
                    type="number"
                    :value="propertyValues[prop.propertyId]"
                    class="w-20 px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-blue-500"
                    placeholder="0"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <input
                    v-else-if="prop.propertyType === 'DATE'"
                    type="date"
                    :value="propertyValues[prop.propertyId]"
                    class="px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-blue-500"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <span v-else-if="prop.defaultValue" class="text-xs text-gray-400">
                    기본: {{ prop.defaultValue }}
                  </span>
                </template>
              </label>
            </div>
          </div>

          <!-- 매니저 속성 -->
          <div v-if="managerProperties.length > 0" class="property-group">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">매니저 속성</span>
              <span class="text-xs text-gray-400">
                {{ managerProperties.filter(p => isSelected(p.propertyId)).length }}/{{ managerProperties.length }}
              </span>
            </div>
            <div class="space-y-1.5">
              <label
                v-for="prop in managerProperties"
                :key="prop.propertyId"
                class="flex items-center gap-2 px-2 py-1.5 rounded text-xs cursor-pointer transition-colors"
                :class="isSelected(prop.propertyId)
                  ? 'bg-purple-50 border border-purple-200'
                  : 'bg-white border border-gray-200 hover:border-gray-300'"
              >
                <input
                  type="checkbox"
                  :checked="isSelected(prop.propertyId)"
                  class="w-3.5 h-3.5 text-purple-600 border-gray-300 rounded cursor-pointer focus:ring-purple-500"
                  @change="toggleProperty(prop.propertyId)"
                />
                <span class="flex-1" :class="isSelected(prop.propertyId) ? 'text-purple-700' : 'text-gray-600'">
                  {{ prop.propertyName }}
                </span>
                <!-- 값 입력 (선택된 경우) -->
                <template v-if="isSelected(prop.propertyId)">
                  <select
                    v-if="prop.propertyType === 'SELECT'"
                    :value="propertyValues[prop.propertyId]"
                    class="px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-purple-500"
                    @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLSelectElement).value)"
                    @click.stop
                  >
                    <option value="">선택</option>
                    <option
                      v-for="opt in propertyOptions.get(prop.propertyId) || []"
                      :key="opt.optionId"
                      :value="opt.optionId"
                    >
                      {{ opt.optionName }}
                    </option>
                  </select>
                  <input
                    v-else-if="prop.propertyType === 'TEXT'"
                    type="text"
                    :value="propertyValues[prop.propertyId]"
                    class="w-24 px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-purple-500"
                    placeholder="값 입력"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <input
                    v-else-if="prop.propertyType === 'NUMBER'"
                    type="number"
                    :value="propertyValues[prop.propertyId]"
                    class="w-20 px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-purple-500"
                    placeholder="0"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <input
                    v-else-if="prop.propertyType === 'DATE'"
                    type="date"
                    :value="propertyValues[prop.propertyId]"
                    class="px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-purple-500"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <span v-else-if="prop.defaultValue" class="text-xs text-gray-400">
                    기본: {{ prop.defaultValue }}
                  </span>
                </template>
              </label>
            </div>
          </div>

          <!-- 사용자 속성 -->
          <div v-if="userProperties.length > 0" class="property-group">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">사용자 속성</span>
              <span class="text-xs text-gray-400">
                {{ userProperties.filter(p => isSelected(p.propertyId)).length }}/{{ userProperties.length }}
              </span>
            </div>
            <div class="space-y-1.5">
              <label
                v-for="prop in userProperties"
                :key="prop.propertyId"
                class="flex items-center gap-2 px-2 py-1.5 rounded text-xs cursor-pointer transition-colors"
                :class="isSelected(prop.propertyId)
                  ? 'bg-green-50 border border-green-200'
                  : 'bg-white border border-gray-200 hover:border-gray-300'"
              >
                <input
                  type="checkbox"
                  :checked="isSelected(prop.propertyId)"
                  class="w-3.5 h-3.5 text-green-600 border-gray-300 rounded cursor-pointer focus:ring-green-500"
                  @change="toggleProperty(prop.propertyId)"
                />
                <span class="flex-1" :class="isSelected(prop.propertyId) ? 'text-green-700' : 'text-gray-600'">
                  {{ prop.propertyName }}
                </span>
                <!-- 값 입력 (선택된 경우) -->
                <template v-if="isSelected(prop.propertyId)">
                  <select
                    v-if="prop.propertyType === 'SELECT'"
                    :value="propertyValues[prop.propertyId]"
                    class="px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-green-500"
                    @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLSelectElement).value)"
                    @click.stop
                  >
                    <option value="">선택</option>
                    <option
                      v-for="opt in propertyOptions.get(prop.propertyId) || []"
                      :key="opt.optionId"
                      :value="opt.optionId"
                    >
                      {{ opt.optionName }}
                    </option>
                  </select>
                  <input
                    v-else-if="prop.propertyType === 'TEXT'"
                    type="text"
                    :value="propertyValues[prop.propertyId]"
                    class="w-24 px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-green-500"
                    placeholder="값 입력"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <input
                    v-else-if="prop.propertyType === 'NUMBER'"
                    type="number"
                    :value="propertyValues[prop.propertyId]"
                    class="w-20 px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-green-500"
                    placeholder="0"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <input
                    v-else-if="prop.propertyType === 'DATE'"
                    type="date"
                    :value="propertyValues[prop.propertyId]"
                    class="px-1.5 py-0.5 text-xs border border-gray-200 rounded focus:ring-1 focus:ring-green-500"
                    @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                    @click.stop
                  />
                  <span v-else-if="prop.defaultValue" class="text-xs text-gray-400">
                    기본: {{ prop.defaultValue }}
                  </span>
                </template>
              </label>
            </div>
          </div>

          <!-- 빈 상태 -->
          <div
            v-if="boardProperties.length === 0 && !loading"
            class="text-center py-4 text-gray-400 text-xs"
          >
            <p>이 보드에 설정된 추가 속성이 없습니다.</p>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.item-property-selector {
  @apply border border-gray-200 rounded-lg overflow-hidden;
}

.property-group {
  @apply bg-white rounded-lg p-2.5 border border-gray-100;
}
</style>
