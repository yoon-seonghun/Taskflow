<script setup lang="ts">
/**
 * 보드 속성 패널 컴포넌트 (v2.0)
 * - 업무 페이지에 항상 표시되는 보드 카테고리/속성 정보 패널
 * - 현재 보드의 카테고리와 속성 목록을 표시
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { boardApi } from '@/api/board'
import { categoryApi } from '@/api/category'
import type { BoardCategory, BoardProperty } from '@/types/board'
import type { CategoryProperty } from '@/types/category'

interface Props {
  boardId: number | null
}

const props = defineProps<Props>()

// 상태
const loading = ref(false)
const boardCategories = ref<BoardCategory[]>([])
const boardProperties = ref<BoardProperty[]>([])
const selectedCategoryId = ref<number | null>(null)
const categoryProperties = ref<CategoryProperty[]>([])
const categoryLoading = ref(false)
let isComponentMounted = false

// 기본 카테고리
const defaultCategory = computed(() => {
  return boardCategories.value.find(c => c.isDefault)
})

// 현재 선택된 카테고리명
const currentCategoryName = computed(() => {
  if (selectedCategoryId.value) {
    const cat = boardCategories.value.find(c => c.categoryId === selectedCategoryId.value)
    return cat?.categoryName || '선택 안함'
  }
  return defaultCategory.value?.categoryName || '선택 안함'
})

// 보드 속성 그룹핑
const globalProperties = computed(() =>
  boardProperties.value.filter(p => p.ownerType === 'GLOBAL')
)

const managerProperties = computed(() =>
  boardProperties.value.filter(p => p.ownerType === 'MANAGER')
)

const userProperties = computed(() =>
  boardProperties.value.filter(p => p.ownerType === 'USER')
)

// 데이터 로드
async function loadData() {
  if (!props.boardId) {
    boardCategories.value = []
    boardProperties.value = []
    return
  }

  loading.value = true
  try {
    const [categoriesRes, propertiesRes] = await Promise.all([
      boardApi.getBoardCategories(props.boardId),
      boardApi.getBoardProperties(props.boardId)
    ])

    if (!isComponentMounted) return

    if (categoriesRes.success && categoriesRes.data) {
      boardCategories.value = categoriesRes.data
      // 기본 카테고리 자동 선택
      const defaultCat = categoriesRes.data.find(c => c.isDefault)
      if (defaultCat) {
        selectedCategoryId.value = defaultCat.categoryId
        await loadCategoryProperties(defaultCat.categoryId)
      }
    }

    if (propertiesRes.success && propertiesRes.data) {
      boardProperties.value = propertiesRes.data
    }
  } catch (error) {
    console.error('Failed to load board data:', error)
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
    }
  } catch (error) {
    console.error('Failed to load category properties:', error)
  } finally {
    if (isComponentMounted) {
      categoryLoading.value = false
    }
  }
}

// 카테고리 변경
function handleCategoryChange(categoryId: number | null) {
  selectedCategoryId.value = categoryId
  loadCategoryProperties(categoryId)
}

// 속성 타입 아이콘
function getPropertyTypeIcon(type: string): string {
  switch (type) {
    case 'TEXT': return 'T'
    case 'NUMBER': return '#'
    case 'DATE': return '📅'
    case 'SELECT': return '▼'
    case 'MULTI_SELECT': return '☑'
    case 'CHECKBOX': return '✓'
    case 'USER': return '👤'
    default: return '•'
  }
}

// boardId 변경 감지
watch(() => props.boardId, () => {
  loadData()
}, { immediate: true })

onMounted(() => {
  isComponentMounted = true
})

onUnmounted(() => {
  isComponentMounted = false
})

defineExpose({
  currentCategoryName,
  selectedCategoryId
})
</script>

<template>
  <div class="board-property-panel bg-white border border-gray-200 rounded-lg">
    <!-- 헤더 -->
    <div class="px-3 py-2 bg-gray-50 border-b border-gray-200 rounded-t-lg">
      <div class="flex items-center justify-between">
        <h3 class="text-sm font-medium text-gray-700 flex items-center gap-2">
          <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          보드 속성 정보
        </h3>
        <span v-if="loading" class="text-xs text-gray-400">로딩 중...</span>
      </div>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="p-4 flex items-center justify-center">
      <svg class="animate-spin h-5 w-5 text-blue-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>

    <!-- 콘텐츠 -->
    <div v-else class="p-3 space-y-3 max-h-80 overflow-y-auto">
      <!-- 카테고리 섹션 -->
      <div v-if="boardCategories.length > 0" class="space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">카테고리</span>
          <span class="text-xs text-gray-400">{{ boardCategories.length }}개</span>
        </div>
        <select
          :value="selectedCategoryId"
          class="w-full px-2.5 py-1.5 text-xs border border-gray-200 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
          @change="handleCategoryChange(($event.target as HTMLSelectElement).value ? Number(($event.target as HTMLSelectElement).value) : null)"
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

      <!-- 카테고리 속성 -->
      <div v-if="categoryLoading" class="flex items-center gap-2 text-xs text-gray-500">
        <svg class="animate-spin h-4 w-4 text-orange-500" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
        카테고리 속성 로딩 중...
      </div>
      <div v-else-if="categoryProperties.length > 0" class="space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-orange-600 uppercase tracking-wide">카테고리 속성</span>
          <span class="text-xs text-gray-400">{{ categoryProperties.length }}개</span>
        </div>
        <div class="flex flex-wrap gap-1">
          <span
            v-for="prop in categoryProperties"
            :key="prop.propertyId"
            class="inline-flex items-center gap-1 px-2 py-1 bg-orange-50 text-orange-700 rounded text-xs border border-orange-200"
          >
            <span class="w-4 h-4 flex items-center justify-center text-[10px]">{{ getPropertyTypeIcon(prop.propertyType) }}</span>
            {{ prop.propertyName }}
            <span v-if="prop.requiredYn === 'Y'" class="text-red-500">*</span>
          </span>
        </div>
      </div>

      <!-- 글로벌 속성 -->
      <div v-if="globalProperties.length > 0" class="space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-blue-600 uppercase tracking-wide">글로벌 속성</span>
          <span class="text-xs text-gray-400">{{ globalProperties.length }}개</span>
        </div>
        <div class="flex flex-wrap gap-1">
          <span
            v-for="prop in globalProperties"
            :key="prop.propertyId"
            class="inline-flex items-center gap-1 px-2 py-1 bg-blue-50 text-blue-700 rounded text-xs border border-blue-200"
          >
            <span class="w-4 h-4 flex items-center justify-center text-[10px]">{{ getPropertyTypeIcon(prop.propertyType) }}</span>
            {{ prop.propertyName }}
            <span v-if="prop.requiredYn === 'Y'" class="text-red-500">*</span>
          </span>
        </div>
      </div>

      <!-- 매니저 속성 -->
      <div v-if="managerProperties.length > 0" class="space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-purple-600 uppercase tracking-wide">매니저 속성</span>
          <span class="text-xs text-gray-400">{{ managerProperties.length }}개</span>
        </div>
        <div class="flex flex-wrap gap-1">
          <span
            v-for="prop in managerProperties"
            :key="prop.propertyId"
            class="inline-flex items-center gap-1 px-2 py-1 bg-purple-50 text-purple-700 rounded text-xs border border-purple-200"
          >
            <span class="w-4 h-4 flex items-center justify-center text-[10px]">{{ getPropertyTypeIcon(prop.propertyType) }}</span>
            {{ prop.propertyName }}
            <span v-if="prop.requiredYn === 'Y'" class="text-red-500">*</span>
          </span>
        </div>
      </div>

      <!-- 사용자 속성 -->
      <div v-if="userProperties.length > 0" class="space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-green-600 uppercase tracking-wide">사용자 속성</span>
          <span class="text-xs text-gray-400">{{ userProperties.length }}개</span>
        </div>
        <div class="flex flex-wrap gap-1">
          <span
            v-for="prop in userProperties"
            :key="prop.propertyId"
            class="inline-flex items-center gap-1 px-2 py-1 bg-green-50 text-green-700 rounded text-xs border border-green-200"
          >
            <span class="w-4 h-4 flex items-center justify-center text-[10px]">{{ getPropertyTypeIcon(prop.propertyType) }}</span>
            {{ prop.propertyName }}
            <span v-if="prop.requiredYn === 'Y'" class="text-red-500">*</span>
          </span>
        </div>
      </div>

      <!-- 빈 상태 -->
      <div
        v-if="!loading && boardCategories.length === 0 && boardProperties.length === 0"
        class="text-center py-4 text-gray-400 text-xs"
      >
        이 보드에 설정된 카테고리나 속성이 없습니다.
      </div>
    </div>
  </div>
</template>

<style scoped>
.board-property-panel {
  min-width: 200px;
}
</style>
