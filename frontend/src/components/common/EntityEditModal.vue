<script setup lang="ts">
/**
 * 엔티티 편집 모달 (공용 컴포넌트 - cap_7.jpg 스타일)
 *
 * 사용처:
 * - 보드 관리: 보드 생성/수정
 * - 업무 페이지: 보드 속성 변경
 * - 업무 등록/수정: 업무 속성 설정
 *
 * mode에 따라 동작 변경:
 * - 'board': 카테고리 다중 선택 (체크박스)
 * - 'item': 카테고리 단일 선택 (라디오)
 */
import { ref, computed, watch, onMounted } from 'vue'
import BoardPropertySelector from '@/components/board/BoardPropertySelector.vue'
import type { Category } from '@/types/category'

interface Props {
  modelValue: boolean  // 모달 열림/닫힘
  mode: 'board' | 'item'  // 보드용/업무용
  title?: string  // 모달 제목 (기본: mode에 따라 자동 설정)

  // 초기값
  initialName?: string
  initialDescription?: string
  initialColor?: string
  initialCategoryIds?: number[]  // 보드용: 다중
  initialCategoryId?: number | null  // 업무용: 단일
  initialPropertyIds?: number[]
  initialPropertyDefaults?: Record<number, string>
  initialPropertySortOrders?: Record<number, number>  // v2.0.1: 속성 정렬 순서

  // 외부 데이터
  categories: Category[]

  // 옵션
  colorOptions?: string[]
  nameLabel?: string  // 이름 필드 라벨 (기본: 보드 이름/업무명)
  nameRequired?: boolean
  showColor?: boolean
  showDescription?: boolean
  enablePropertyDragDrop?: boolean  // v2.0.1: 속성 드래그 앤 드롭 활성화
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  initialName: '',
  initialDescription: '',
  initialColor: '#3B82F6',
  initialCategoryIds: () => [],
  initialCategoryId: null,
  initialPropertyIds: () => [],
  initialPropertyDefaults: () => ({}),
  initialPropertySortOrders: () => ({}),
  colorOptions: () => [
    '#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6',
    '#EC4899', '#6366F1', '#14B8A6', '#F97316', '#6B7280'
  ],
  nameLabel: '',
  nameRequired: true,
  showColor: true,
  showDescription: true,
  enablePropertyDragDrop: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', data: {
    name: string
    description: string
    color: string
    categoryIds: number[]  // 보드용
    categoryId: number | null  // 업무용
    propertyIds: number[]
    propertyDefaults: Record<number, string>
    propertySortOrders: Record<number, number>  // v2.0.1: 속성 정렬 순서
  }): void
  (e: 'cancel'): void
}>()

// 폼 데이터
const formData = ref({
  name: props.initialName,
  description: props.initialDescription,
  color: props.initialColor,
  categoryIds: [...props.initialCategoryIds],  // 보드용
  categoryId: props.initialCategoryId as number | null,  // 업무용
  propertyIds: [...props.initialPropertyIds],
  propertyDefaults: { ...props.initialPropertyDefaults },
  propertySortOrders: { ...props.initialPropertySortOrders }  // v2.0.1: 속성 정렬 순서
})

// 모달 제목 (props.title이 없으면 mode에 따라 자동 설정)
const modalTitle = computed(() => {
  if (props.title) return props.title
  return props.mode === 'board' ? '보드 수정' : '업무 속성 설정'
})

// 이름 필드 라벨
const nameLabelText = computed(() => {
  if (props.nameLabel) return props.nameLabel
  return props.mode === 'board' ? '보드 이름' : '업무명'
})

// 카테고리 선택용 (BoardPropertySelector에 전달)
const selectedCategoryIdsForSelector = computed(() => {
  if (props.mode === 'board') {
    return formData.value.categoryIds
  } else {
    // 업무용: 단일 선택이므로 배열로 변환
    return formData.value.categoryId ? [formData.value.categoryId] : []
  }
})

// 카테고리 단일 선택 핸들러 (업무용)
function handleCategoryRadioChange(categoryId: number | null) {
  formData.value.categoryId = categoryId
}

// 속성 ID 업데이트 핸들러
function handlePropertyIdsUpdate(ids: number[]) {
  console.log('[EntityEditModal] handlePropertyIdsUpdate 수신:', ids)
  formData.value.propertyIds = [...ids]  // 새 배열로 복사하여 반응성 보장
  console.log('[EntityEditModal] formData.propertyIds 업데이트됨:', formData.value.propertyIds)
}

// 속성 기본값 업데이트 핸들러
function handlePropertyDefaultsUpdate(defaults: Record<number, string>) {
  formData.value.propertyDefaults = defaults
}

// v2.0.1: 속성 정렬 순서 업데이트 핸들러
function handlePropertySortOrdersUpdate(sortOrders: Record<number, number>) {
  formData.value.propertySortOrders = sortOrders
}

// 저장
function handleSave() {
  if (props.nameRequired && !formData.value.name.trim()) {
    return
  }

  emit('save', {
    name: formData.value.name.trim(),
    description: formData.value.description,
    color: formData.value.color,
    categoryIds: formData.value.categoryIds,
    categoryId: formData.value.categoryId,
    propertyIds: formData.value.propertyIds,
    propertyDefaults: formData.value.propertyDefaults,
    propertySortOrders: formData.value.propertySortOrders
  })
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

// 모달 열릴 때 초기값 복원
watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    formData.value = {
      name: props.initialName,
      description: props.initialDescription,
      color: props.initialColor,
      categoryIds: [...props.initialCategoryIds],
      categoryId: props.initialCategoryId,
      propertyIds: [...props.initialPropertyIds],
      propertyDefaults: { ...props.initialPropertyDefaults },
      propertySortOrders: { ...props.initialPropertySortOrders }
    }
  }
})
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center">
      <!-- 배경 오버레이 -->
      <div class="fixed inset-0 bg-black/50" @click="handleClose" />

      <!-- 모달 컨테이너 -->
      <div class="relative bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-hidden flex flex-col dark:bg-gray-800">
        <!-- 헤더 -->
        <div class="px-6 py-4 border-b dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">{{ modalTitle }}</h3>
        </div>

        <!-- 본문 -->
        <div class="flex-1 overflow-y-auto px-6 py-4">
          <div class="space-y-4">
            <!-- 이름 필드 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1 dark:text-gray-300">
                {{ nameLabelText }}
                <span v-if="nameRequired" class="text-red-500">*</span>
              </label>
              <input
                v-model="formData.name"
                type="text"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100 dark:focus:ring-blue-400"
              />
            </div>

            <!-- 설명 필드 -->
            <div v-if="showDescription">
              <label class="block text-sm font-medium text-gray-700 mb-1 dark:text-gray-300">설명</label>
              <textarea
                v-model="formData.description"
                rows="3"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100 dark:focus:ring-blue-400"
              />
            </div>

            <!-- 색상 선택 -->
            <div v-if="showColor">
              <label class="block text-sm font-medium text-gray-700 mb-2 dark:text-gray-300">색상</label>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="color in colorOptions"
                  :key="color"
                  type="button"
                  @click="formData.color = color"
                  class="w-8 h-8 rounded-full border-2 transition-transform hover:scale-110"
                  :class="formData.color === color ? 'border-gray-900 dark:border-white scale-110' : 'border-transparent'"
                  :style="{ backgroundColor: color }"
                />
              </div>
            </div>

            <!-- 카테고리 선택 -->
            <div v-if="categories.length > 0">
              <label class="block text-sm font-medium text-gray-700 mb-2 dark:text-gray-300">카테고리 (선택)</label>
              <div class="max-h-40 overflow-y-auto border border-gray-200 rounded-lg p-2 space-y-1 dark:border-gray-700">
                <!-- 보드용: 다중 선택 (체크박스) -->
                <template v-if="mode === 'board'">
                  <label
                    v-for="cat in categories"
                    :key="cat.categoryId"
                    class="flex items-center gap-2 p-2 rounded hover:bg-gray-50 cursor-pointer dark:hover:bg-gray-700"
                  >
                    <input
                      type="checkbox"
                      :value="cat.categoryId"
                      v-model="formData.categoryIds"
                      class="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700"
                    />
                    <div
                      class="w-3 h-3 rounded"
                      :style="{ backgroundColor: cat.categoryColor || '#6B7280' }"
                    />
                    <span class="text-sm text-gray-700 dark:text-gray-200">{{ cat.categoryName }}</span>
                  </label>
                </template>

                <!-- 업무용: 단일 선택 (라디오) -->
                <template v-else>
                  <!-- 선택 안함 옵션 -->
                  <label
                    class="flex items-center gap-2 p-2 rounded hover:bg-gray-50 cursor-pointer dark:hover:bg-gray-700"
                    :class="{ 'bg-blue-50 dark:bg-blue-900/30': formData.categoryId === null }"
                  >
                    <input
                      type="radio"
                      name="category-radio"
                      :checked="formData.categoryId === null"
                      class="w-4 h-4 text-blue-600 border-gray-300 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700"
                      @change="handleCategoryRadioChange(null)"
                    />
                    <span class="text-sm text-gray-500 dark:text-gray-400">선택 안함</span>
                  </label>
                  <label
                    v-for="cat in categories"
                    :key="cat.categoryId"
                    class="flex items-center gap-2 p-2 rounded hover:bg-gray-50 cursor-pointer dark:hover:bg-gray-700"
                    :class="{ 'bg-blue-50 dark:bg-blue-900/30': formData.categoryId === cat.categoryId }"
                  >
                    <input
                      type="radio"
                      name="category-radio"
                      :checked="formData.categoryId === cat.categoryId"
                      class="w-4 h-4 text-blue-600 border-gray-300 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700"
                      @change="handleCategoryRadioChange(cat.categoryId)"
                    />
                    <div
                      class="w-3 h-3 rounded"
                      :style="{ backgroundColor: cat.categoryColor || '#6B7280' }"
                    />
                    <span class="text-sm text-gray-700 dark:text-gray-200">{{ cat.categoryName }}</span>
                  </label>
                </template>
              </div>
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                {{ mode === 'board' ? '보드에서 사용할 카테고리를 선택하세요' : '업무에 적용할 카테고리를 선택하세요' }}
              </p>
            </div>

            <!-- 속성 선택 패널 (v2.0.4: 보드 모드에서는 카테고리 속성 숨김 및 드래그앤드롭 비활성) -->
            <BoardPropertySelector
              :selected-category-ids="selectedCategoryIdsForSelector"
              :selected-property-ids="formData.propertyIds"
              :property-defaults="formData.propertyDefaults"
              :initial-sort-orders="formData.propertySortOrders"
              :enable-drag-drop="mode !== 'board' && enablePropertyDragDrop"
              :show-category-properties="mode !== 'board'"
              @update:selected-property-ids="handlePropertyIdsUpdate"
              @update:property-defaults="handlePropertyDefaultsUpdate"
              @update:property-sort-orders="handlePropertySortOrdersUpdate"
            />
          </div>
        </div>

        <!-- 푸터 -->
        <div class="flex justify-end gap-3 px-6 py-4 border-t bg-gray-50 dark:bg-gray-700/50 dark:border-gray-700">
          <button
            type="button"
            @click="handleCancel"
            class="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-lg dark:text-gray-300 dark:hover:bg-gray-600"
          >
            취소
          </button>
          <button
            type="button"
            @click="handleSave"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg"
            :disabled="nameRequired && !formData.name.trim()"
            :class="{ 'opacity-50 cursor-not-allowed': nameRequired && !formData.name.trim() }"
          >
            저장
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
