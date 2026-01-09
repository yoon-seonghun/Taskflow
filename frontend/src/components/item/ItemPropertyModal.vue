<script setup lang="ts">
/**
 * 업무 속성 편집 모달 (v2.0 - cap_7.jpg 스타일)
 * - 보드 편집 모달과 동일한 통합 스타일
 * - 업무명, 설명, 카테고리, 속성값 편집
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { boardApi } from '@/api/board'
import { categoryApi } from '@/api/category'
import { propertyApi } from '@/api/property'
import { useToast } from '@/composables/useToast'
import type { BoardProperty, BoardCategory } from '@/types/board'
import type { CategoryProperty } from '@/types/category'
import type { PropertyOption } from '@/types/property'

interface Props {
  modelValue: boolean  // 모달 열림/닫힘
  boardId: number
  itemId?: number | null  // 기존 업무 수정 시
  initialTitle?: string
  initialDescription?: string
  initialCategoryId?: number | null
  initialProperties?: Record<number, unknown>
  mode?: 'create' | 'edit'  // 생성 모드 또는 편집 모드
}

const props = withDefaults(defineProps<Props>(), {
  itemId: null,
  initialTitle: '',
  initialDescription: '',
  initialCategoryId: null,
  initialProperties: () => ({}),
  mode: 'create'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'apply', data: {
    title: string
    description: string
    categoryId: number | null
    properties: Record<number, unknown>
  }): void
  (e: 'cancel'): void
}>()

const toast = useToast()

// 상태
const loading = ref(false)
const boardCategories = ref<BoardCategory[]>([])
const boardProperties = ref<BoardProperty[]>([])
const categoryProperties = ref<CategoryProperty[]>([])
const categoryLoading = ref(false)
const propertyOptions = ref<Map<number, PropertyOption[]>>(new Map())

let isComponentMounted = false

// 폼 데이터
const formData = ref({
  title: props.initialTitle,
  description: props.initialDescription,
  categoryId: props.initialCategoryId as number | null,
  propertyValues: { ...props.initialProperties } as Record<number, unknown>
})

// 기본 카테고리
const defaultCategory = computed(() => {
  return boardCategories.value.find(c => c.isDefault)
})

// 모든 속성 (보드 속성 + 카테고리 속성)
const allProperties = computed(() => {
  const boardPropIds = new Set(boardProperties.value.map(p => p.propertyId))
  const catProps = categoryProperties.value.filter(p => !boardPropIds.has(p.propertyId))

  // 속성을 ownerType별로 그룹화하여 반환
  return {
    category: catProps,
    global: boardProperties.value.filter(p => p.ownerType === 'GLOBAL'),
    manager: boardProperties.value.filter(p => p.ownerType === 'MANAGER'),
    user: boardProperties.value.filter(p => p.ownerType === 'USER')
  }
})

// 데이터 로드
async function loadData() {
  if (!props.boardId) return

  loading.value = true
  try {
    const [categoriesRes, propertiesRes] = await Promise.all([
      boardApi.getBoardCategories(props.boardId),
      boardApi.getBoardProperties(props.boardId)
    ])

    if (!isComponentMounted) return

    if (categoriesRes.success && categoriesRes.data) {
      boardCategories.value = categoriesRes.data

      // 초기 카테고리 설정
      if (props.initialCategoryId) {
        formData.value.categoryId = props.initialCategoryId
      } else {
        const defaultCat = categoriesRes.data.find(c => c.isDefault)
        if (defaultCat) {
          formData.value.categoryId = defaultCat.categoryId
        }
      }

      // 카테고리 속성 로드
      if (formData.value.categoryId) {
        await loadCategoryProperties(formData.value.categoryId)
      }
    }

    if (propertiesRes.success && propertiesRes.data) {
      boardProperties.value = propertiesRes.data

      // 옵션 로드
      const optionPromises: Promise<void>[] = []
      for (const prop of propertiesRes.data) {
        if (prop.propertyType === 'SELECT' || prop.propertyType === 'MULTI_SELECT') {
          optionPromises.push(loadPropertyOptions(prop.propertyId))
        }

        // 새 업무 생성 시 기본값 적용
        if (props.mode === 'create' && prop.defaultValue && !formData.value.propertyValues[prop.propertyId]) {
          formData.value.propertyValues[prop.propertyId] = prop.defaultValue
        }
      }
      await Promise.allSettled(optionPromises)
    }
  } catch (error) {
    console.error('Failed to load board data:', error)
    toast.error('보드 정보를 불러오는데 실패했습니다.')
  } finally {
    if (isComponentMounted) {
      loading.value = false
    }
  }
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

      // 옵션 로드 및 기본값 적용
      const optionPromises: Promise<void>[] = []
      for (const prop of response.data) {
        if (prop.propertyType === 'SELECT' || prop.propertyType === 'MULTI_SELECT') {
          optionPromises.push(loadPropertyOptions(prop.propertyId))
        }

        // 새 업무 생성 시 기본값 적용
        if (props.mode === 'create' && prop.defaultValue && !formData.value.propertyValues[prop.propertyId]) {
          formData.value.propertyValues[prop.propertyId] = prop.defaultValue
        }
      }
      await Promise.allSettled(optionPromises)
    }
  } catch (error) {
    console.error('Failed to load category properties:', error)
  } finally {
    if (isComponentMounted) {
      categoryLoading.value = false
    }
  }
}

// 속성 옵션 로드
async function loadPropertyOptions(propertyId: number) {
  if (propertyOptions.value.has(propertyId)) return

  try {
    const response = await propertyApi.getOptions(propertyId)
    if (!isComponentMounted) return

    if (response.success && response.data) {
      propertyOptions.value.set(propertyId, response.data)
    }
  } catch (error) {
    console.error('Failed to load property options:', error)
  }
}

// 카테고리 변경
function handleCategoryChange(categoryId: number | null) {
  formData.value.categoryId = categoryId
  loadCategoryProperties(categoryId)
}

// 속성값 변경
function updatePropertyValue(propertyId: number, value: unknown) {
  formData.value.propertyValues[propertyId] = value
}

// 적용
function handleApply() {
  if (props.mode === 'create' && !formData.value.title.trim()) {
    toast.error('업무명을 입력해주세요.')
    return
  }

  emit('apply', {
    title: formData.value.title,
    description: formData.value.description,
    categoryId: formData.value.categoryId,
    properties: formData.value.propertyValues
  })
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
    // 초기값 복원
    formData.value = {
      title: props.initialTitle,
      description: props.initialDescription,
      categoryId: props.initialCategoryId,
      propertyValues: { ...props.initialProperties }
    }
    loadData()
  }
})

onMounted(() => {
  isComponentMounted = true
  if (props.modelValue) {
    loadData()
  }
})

onUnmounted(() => {
  isComponentMounted = false
})

// 속성 타입별 색상
const propertyColors = {
  category: { bg: 'bg-orange-50', border: 'border-orange-200', text: 'text-orange-700', ring: 'focus:ring-orange-500' },
  global: { bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-700', ring: 'focus:ring-blue-500' },
  manager: { bg: 'bg-purple-50', border: 'border-purple-200', text: 'text-purple-700', ring: 'focus:ring-purple-500' },
  user: { bg: 'bg-green-50', border: 'border-green-200', text: 'text-green-700', ring: 'focus:ring-green-500' }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center">
      <!-- 배경 오버레이 -->
      <div class="fixed inset-0 bg-black/50" @click="handleClose" />

      <!-- 모달 컨테이너 -->
      <div class="relative bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-hidden flex flex-col">
        <!-- 헤더 -->
        <div class="px-6 py-4 border-b">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ mode === 'create' ? '새 업무 등록' : '업무 속성 수정' }}
          </h3>
        </div>

        <!-- 본문 -->
        <div class="flex-1 overflow-y-auto px-6 py-4">
          <!-- 로딩 -->
          <div v-if="loading" class="flex items-center justify-center py-8">
            <svg class="animate-spin h-8 w-8 text-blue-600" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </div>

          <div v-else class="space-y-4">
            <!-- 업무명 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">업무명 *</label>
              <input
                v-model="formData.title"
                type="text"
                placeholder="업무 제목을 입력하세요"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <!-- 설명 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
              <textarea
                v-model="formData.description"
                rows="3"
                placeholder="업무 설명을 입력하세요 (선택)"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <!-- 카테고리 선택 -->
            <div v-if="boardCategories.length > 0">
              <label class="block text-sm font-medium text-gray-700 mb-2">카테고리</label>
              <div class="max-h-32 overflow-y-auto border border-gray-200 rounded-lg p-2 space-y-1">
                <!-- 선택 안함 옵션 -->
                <label
                  class="flex items-center gap-2 p-2 rounded hover:bg-gray-50 cursor-pointer"
                  :class="{ 'bg-blue-50': formData.categoryId === null }"
                >
                  <input
                    type="radio"
                    name="category"
                    :checked="formData.categoryId === null"
                    class="w-4 h-4 text-blue-600 border-gray-300 focus:ring-blue-500"
                    @change="handleCategoryChange(null)"
                  />
                  <span class="text-sm text-gray-500">선택 안함</span>
                </label>
                <!-- 카테고리 목록 -->
                <label
                  v-for="cat in boardCategories"
                  :key="cat.categoryId"
                  class="flex items-center gap-2 p-2 rounded hover:bg-gray-50 cursor-pointer"
                  :class="{ 'bg-blue-50': formData.categoryId === cat.categoryId }"
                >
                  <input
                    type="radio"
                    name="category"
                    :checked="formData.categoryId === cat.categoryId"
                    class="w-4 h-4 text-blue-600 border-gray-300 focus:ring-blue-500"
                    @change="handleCategoryChange(cat.categoryId)"
                  />
                  <div
                    class="w-3 h-3 rounded flex-shrink-0"
                    :style="{ backgroundColor: cat.categoryColor || '#6B7280' }"
                  />
                  <span class="text-sm text-gray-700">
                    {{ cat.categoryName }}
                    <span v-if="cat.isDefault" class="text-xs text-gray-400">(기본)</span>
                  </span>
                </label>
              </div>
              <p class="mt-1 text-xs text-gray-500">업무에 적용할 카테고리를 선택하세요</p>
            </div>

            <!-- 속성 편집 영역 -->
            <div class="space-y-4">
              <!-- 카테고리 속성 -->
              <div v-if="categoryLoading" class="flex items-center gap-2 text-sm text-gray-500 py-2">
                <svg class="animate-spin h-4 w-4 text-orange-500" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                카테고리 속성 로딩 중...
              </div>

              <div v-else-if="allProperties.category.length > 0">
                <label class="block text-sm font-medium text-orange-600 mb-2">
                  카테고리 속성 ({{ allProperties.category.length }})
                </label>
                <div class="space-y-2">
                  <div
                    v-for="prop in allProperties.category"
                    :key="prop.propertyId"
                    class="flex items-center gap-3 p-3 rounded-lg border bg-orange-50 border-orange-200"
                  >
                    <span class="text-sm font-medium text-orange-700 min-w-[100px]">
                      {{ prop.propertyName }}
                      <span v-if="prop.requiredYn === 'Y'" class="text-red-500">*</span>
                    </span>
                    <div class="flex-1">
                      <!-- SELECT 타입 -->
                      <select
                        v-if="prop.propertyType === 'SELECT'"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-orange-200 rounded focus:ring-2 focus:ring-orange-500"
                        @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLSelectElement).value || null)"
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
                      <!-- TEXT 타입 -->
                      <input
                        v-else-if="prop.propertyType === 'TEXT'"
                        type="text"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-orange-200 rounded focus:ring-2 focus:ring-orange-500"
                        placeholder="텍스트 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <!-- NUMBER 타입 -->
                      <input
                        v-else-if="prop.propertyType === 'NUMBER'"
                        type="number"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-orange-200 rounded focus:ring-2 focus:ring-orange-500"
                        placeholder="숫자 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <!-- DATE 타입 -->
                      <input
                        v-else-if="prop.propertyType === 'DATE'"
                        type="date"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-orange-200 rounded focus:ring-2 focus:ring-orange-500"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <!-- 글로벌 속성 -->
              <div v-if="allProperties.global.length > 0">
                <label class="block text-sm font-medium text-blue-600 mb-2">
                  글로벌 속성 ({{ allProperties.global.length }})
                </label>
                <div class="space-y-2">
                  <div
                    v-for="prop in allProperties.global"
                    :key="prop.propertyId"
                    class="flex items-center gap-3 p-3 rounded-lg border bg-blue-50 border-blue-200"
                  >
                    <span class="text-sm font-medium text-blue-700 min-w-[100px]">
                      {{ prop.propertyName }}
                      <span v-if="prop.requiredYn === 'Y'" class="text-red-500">*</span>
                    </span>
                    <div class="flex-1">
                      <select
                        v-if="prop.propertyType === 'SELECT'"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-blue-200 rounded focus:ring-2 focus:ring-blue-500"
                        @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLSelectElement).value || null)"
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
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-blue-200 rounded focus:ring-2 focus:ring-blue-500"
                        placeholder="텍스트 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <input
                        v-else-if="prop.propertyType === 'NUMBER'"
                        type="number"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-blue-200 rounded focus:ring-2 focus:ring-blue-500"
                        placeholder="숫자 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <input
                        v-else-if="prop.propertyType === 'DATE'"
                        type="date"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-blue-200 rounded focus:ring-2 focus:ring-blue-500"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <!-- 매니저 속성 -->
              <div v-if="allProperties.manager.length > 0">
                <label class="block text-sm font-medium text-purple-600 mb-2">
                  매니저 속성 ({{ allProperties.manager.length }})
                </label>
                <div class="space-y-2">
                  <div
                    v-for="prop in allProperties.manager"
                    :key="prop.propertyId"
                    class="flex items-center gap-3 p-3 rounded-lg border bg-purple-50 border-purple-200"
                  >
                    <span class="text-sm font-medium text-purple-700 min-w-[100px]">
                      {{ prop.propertyName }}
                      <span v-if="prop.requiredYn === 'Y'" class="text-red-500">*</span>
                    </span>
                    <div class="flex-1">
                      <select
                        v-if="prop.propertyType === 'SELECT'"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-purple-200 rounded focus:ring-2 focus:ring-purple-500"
                        @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLSelectElement).value || null)"
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
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-purple-200 rounded focus:ring-2 focus:ring-purple-500"
                        placeholder="텍스트 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <input
                        v-else-if="prop.propertyType === 'NUMBER'"
                        type="number"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-purple-200 rounded focus:ring-2 focus:ring-purple-500"
                        placeholder="숫자 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <input
                        v-else-if="prop.propertyType === 'DATE'"
                        type="date"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-purple-200 rounded focus:ring-2 focus:ring-purple-500"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <!-- 사용자 속성 -->
              <div v-if="allProperties.user.length > 0">
                <label class="block text-sm font-medium text-green-600 mb-2">
                  사용자 속성 ({{ allProperties.user.length }})
                </label>
                <div class="space-y-2">
                  <div
                    v-for="prop in allProperties.user"
                    :key="prop.propertyId"
                    class="flex items-center gap-3 p-3 rounded-lg border bg-green-50 border-green-200"
                  >
                    <span class="text-sm font-medium text-green-700 min-w-[100px]">
                      {{ prop.propertyName }}
                      <span v-if="prop.requiredYn === 'Y'" class="text-red-500">*</span>
                    </span>
                    <div class="flex-1">
                      <select
                        v-if="prop.propertyType === 'SELECT'"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-green-200 rounded focus:ring-2 focus:ring-green-500"
                        @change="updatePropertyValue(prop.propertyId, ($event.target as HTMLSelectElement).value || null)"
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
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-green-200 rounded focus:ring-2 focus:ring-green-500"
                        placeholder="텍스트 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <input
                        v-else-if="prop.propertyType === 'NUMBER'"
                        type="number"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-green-200 rounded focus:ring-2 focus:ring-green-500"
                        placeholder="숫자 입력"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                      <input
                        v-else-if="prop.propertyType === 'DATE'"
                        type="date"
                        :value="formData.propertyValues[prop.propertyId]"
                        class="w-full px-2 py-1.5 text-sm border border-green-200 rounded focus:ring-2 focus:ring-green-500"
                        @input="updatePropertyValue(prop.propertyId, ($event.target as HTMLInputElement).value)"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <!-- 빈 상태 -->
              <div
                v-if="!loading && boardCategories.length === 0 && boardProperties.length === 0 && categoryProperties.length === 0"
                class="text-center py-8 text-gray-400"
              >
                <p>이 보드에 설정된 카테고리나 속성이 없습니다.</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="flex justify-end gap-3 px-6 py-4 border-t bg-gray-50">
          <button
            type="button"
            class="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-lg"
            @click="handleCancel"
          >
            취소
          </button>
          <button
            type="button"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg"
            @click="handleApply"
          >
            {{ mode === 'create' ? '등록' : '저장' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
