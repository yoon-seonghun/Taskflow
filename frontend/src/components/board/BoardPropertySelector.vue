<script setup lang="ts">
/**
 * 보드 속성 선택 컴포넌트 (v2.0 통합 속성 선택 패널)
 *
 * 설계서 기반:
 * - 기본 속성: 필수 (disabled)
 * - 글로벌 속성: 선택 가능
 * - 매니저 속성: 선택 가능
 * - 카테고리 속성: 선택한 카테고리에서 상속된 속성
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { propertyApi } from '@/api/property'
import { categoryApi } from '@/api/category'
import type { PropertyDef } from '@/types/property'
import type { CategoryProperty, CategoryDetail } from '@/types/category'

interface Props {
  selectedCategoryIds: number[]
  selectedPropertyIds: number[]
  propertyDefaults?: Record<number, string>  // propertyId -> defaultValue
}

const props = withDefaults(defineProps<Props>(), {
  selectedCategoryIds: () => [],
  selectedPropertyIds: () => [],
  propertyDefaults: () => ({})
})

const emit = defineEmits<{
  (e: 'update:selectedPropertyIds', value: number[]): void
  (e: 'update:propertyDefaults', value: Record<number, string>): void
}>()

// 상태
const loading = ref(false)
const globalProperties = ref<PropertyDef[]>([])
const managerProperties = ref<PropertyDef[]>([])
const categoryPropertiesMap = ref<Map<number, { categoryName: string; properties: CategoryProperty[] }>>(new Map())

// 초기화 완료 플래그 (초기 로드 중 emit 방지)
const isInitialized = ref(false)
// 초기 속성 ID 저장 (카테고리 로드 시 병합용)
const initialPropertyIds = ref<Set<number>>(new Set())

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

// 선택된 속성 ID 로컬 상태
const localSelectedIds = ref<Set<number>>(new Set(props.selectedPropertyIds))
const localDefaults = ref<Record<number, string>>({ ...props.propertyDefaults })

// 초기 속성 ID 저장 (props에서 전달된 값)
initialPropertyIds.value = new Set(props.selectedPropertyIds)

// 카테고리 속성 목록 (flatten)
const categoryProperties = computed(() => {
  const result: Array<{ categoryName: string; property: CategoryProperty }> = []
  categoryPropertiesMap.value.forEach((value, categoryId) => {
    value.properties.forEach(prop => {
      result.push({
        categoryName: value.categoryName,
        property: prop
      })
    })
  })
  return result
})

// 속성 선택 토글
function toggleProperty(propertyId: number) {
  // Vue 반응성을 위해 새로운 Set 생성
  const newSet = new Set(localSelectedIds.value)

  if (newSet.has(propertyId)) {
    newSet.delete(propertyId)
    console.log('[toggleProperty] 속성 제거:', propertyId)
  } else {
    newSet.add(propertyId)
    console.log('[toggleProperty] 속성 추가:', propertyId)
  }

  // 새로운 Set으로 교체 (반응성 보장)
  localSelectedIds.value = newSet

  const emitArray = Array.from(newSet)
  console.log('[toggleProperty] emit할 속성 IDs:', emitArray)
  emit('update:selectedPropertyIds', emitArray)
}

// 기본값 변경
function updateDefault(propertyId: number, value: string) {
  localDefaults.value[propertyId] = value
  emit('update:propertyDefaults', { ...localDefaults.value })
}

// 속성이 선택되었는지 확인
function isSelected(propertyId: number): boolean {
  return localSelectedIds.value.has(propertyId)
}

// 카테고리에서 상속된 속성인지 확인
function isInheritedFromCategory(propertyId: number): boolean {
  for (const [_, value] of categoryPropertiesMap.value) {
    if (value.properties.some(p => p.propertyId === propertyId)) {
      return true
    }
  }
  return false
}

// 데이터 로드
async function loadProperties() {
  loading.value = true
  try {
    // 글로벌 속성과 매니저 속성 로드
    const [globalRes, managerRes] = await Promise.all([
      propertyApi.getGlobalProperties(),
      propertyApi.getManagerProperties()
    ])

    if (globalRes.success && globalRes.data) {
      globalProperties.value = globalRes.data
    }
    if (managerRes.success && managerRes.data) {
      managerProperties.value = managerRes.data
    }
  } catch (error) {
    console.error('Failed to load properties:', error)
  } finally {
    loading.value = false
  }
}

// 카테고리 속성 로드
async function loadCategoryProperties(categoryIds: number[], isInitialLoad: boolean = false) {
  categoryPropertiesMap.value.clear()

  if (categoryIds.length === 0) {
    // 카테고리가 없어도 초기 속성은 유지
    if (isInitialLoad) {
      // 초기 로드: 초기 속성 ID 복원
      for (const propId of initialPropertyIds.value) {
        localSelectedIds.value.add(propId)
      }
    }
    return
  }

  try {
    const promises = categoryIds.map(id => categoryApi.getCategoryDetail(id))
    const results = await Promise.all(promises)

    results.forEach((res, index) => {
      if (res.success && res.data) {
        const detail = res.data as CategoryDetail
        const validProperties = (detail.properties || []).filter(p => p.propertyId != null)

        categoryPropertiesMap.value.set(categoryIds[index], {
          categoryName: detail.categoryName,
          properties: validProperties
        })

        // 카테고리 속성은 자동 선택
        validProperties.forEach(prop => {
          localSelectedIds.value.add(prop.propertyId)
          // 카테고리 기본값 적용
          if (prop.defaultValue && typeof prop.defaultValue === 'string' && !localDefaults.value[prop.propertyId]) {
            localDefaults.value[prop.propertyId] = prop.defaultValue
          }
        })
      }
    })

    // 초기 로드 시: 초기 속성 ID도 유지 (카테고리 속성과 병합)
    if (isInitialLoad) {
      for (const propId of initialPropertyIds.value) {
        localSelectedIds.value.add(propId)
      }
    }

    // 초기 로드가 아닐 때만 emit (사용자가 카테고리를 변경한 경우)
    if (!isInitialLoad) {
      emit('update:selectedPropertyIds', Array.from(localSelectedIds.value))
      emit('update:propertyDefaults', { ...localDefaults.value })
    }
  } catch (error) {
    console.error('Failed to load category properties:', error)
  }
}

// 이전 카테고리 ID 저장 (비교용)
let previousCategoryIds: number[] = []

// 카테고리 변경 감지
watch(() => props.selectedCategoryIds, (newIds) => {
  // 배열 내용 비교로 불필요한 API 호출 방지
  const hasChanged = JSON.stringify(newIds.slice().sort()) !== JSON.stringify(previousCategoryIds.slice().sort())
  if (hasChanged) {
    previousCategoryIds = [...newIds]
    loadCategoryProperties(newIds)
  }
}, { deep: true })

// props 변경 감지 (초기화 완료 후에만 동작)
watch(() => props.selectedPropertyIds, (newIds) => {
  // 초기화 완료 전에는 무시 (onMounted에서 직접 처리)
  if (!isInitialized.value) return
  localSelectedIds.value = new Set(newIds)
}, { deep: true })

onMounted(async () => {
  previousCategoryIds = [...props.selectedCategoryIds]

  // 초기 속성 ID 저장 (props에서 전달된 값)
  initialPropertyIds.value = new Set(props.selectedPropertyIds)
  localSelectedIds.value = new Set(props.selectedPropertyIds)

  await loadProperties()
  if (props.selectedCategoryIds.length > 0) {
    await loadCategoryProperties(props.selectedCategoryIds, true)  // 초기 로드 플래그
  }

  // 초기화 완료 표시
  isInitialized.value = true
})

// 메모리 정리
onUnmounted(() => {
  localSelectedIds.value.clear()
  categoryPropertiesMap.value.clear()
  initialPropertyIds.value.clear()
  previousCategoryIds = []
  isInitialized.value = false
})
</script>

<template>
  <div class="board-property-selector">
    <div class="mb-3">
      <h4 class="text-sm font-medium text-gray-700 mb-1">속성 선택</h4>
      <p class="text-xs text-gray-500">보드에서 사용할 속성을 선택하세요</p>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="flex items-center justify-center py-6">
      <svg class="animate-spin h-5 w-5 text-blue-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>

    <div v-else class="space-y-4 max-h-80 overflow-y-auto pr-1">
      <!-- 기본 속성 (필수) -->
      <div class="property-group">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">기본 속성</span>
          <span class="text-xs text-gray-400">필수</span>
        </div>
        <div class="flex flex-wrap gap-2">
          <label
            v-for="prop in basicProperties"
            :key="prop.id"
            class="inline-flex items-center gap-1.5 px-2.5 py-1.5 bg-gray-100 text-gray-500 rounded text-xs cursor-not-allowed"
          >
            <input
              type="checkbox"
              :checked="true"
              disabled
              class="w-3.5 h-3.5 text-gray-400 border-gray-300 rounded cursor-not-allowed"
            />
            <span>{{ prop.name }}</span>
          </label>
        </div>
      </div>

      <!-- 글로벌 속성 -->
      <div v-if="globalProperties.length > 0" class="property-group">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">글로벌 속성</span>
          <span class="text-xs text-gray-400">{{ globalProperties.filter(p => isSelected(p.propertyId)).length }}/{{ globalProperties.length }}</span>
        </div>
        <div class="flex flex-wrap gap-2">
          <label
            v-for="prop in globalProperties"
            :key="prop.propertyId"
            class="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded text-xs cursor-pointer transition-colors"
            :class="isSelected(prop.propertyId)
              ? 'bg-blue-50 text-blue-700 border border-blue-200'
              : 'bg-white text-gray-600 border border-gray-200 hover:border-gray-300'"
          >
            <input
              type="checkbox"
              :checked="isSelected(prop.propertyId)"
              @change="toggleProperty(prop.propertyId)"
              class="w-3.5 h-3.5 text-blue-600 border-gray-300 rounded cursor-pointer focus:ring-blue-500"
            />
            <span>{{ prop.propertyName }}</span>
          </label>
        </div>
      </div>

      <!-- 매니저 속성 -->
      <div v-if="managerProperties.length > 0" class="property-group">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">매니저 속성</span>
          <span class="text-xs text-gray-400">{{ managerProperties.filter(p => isSelected(p.propertyId)).length }}/{{ managerProperties.length }}</span>
        </div>
        <div class="flex flex-wrap gap-2">
          <label
            v-for="prop in managerProperties"
            :key="prop.propertyId"
            class="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded text-xs cursor-pointer transition-colors"
            :class="isSelected(prop.propertyId)
              ? 'bg-purple-50 text-purple-700 border border-purple-200'
              : 'bg-white text-gray-600 border border-gray-200 hover:border-gray-300'"
          >
            <input
              type="checkbox"
              :checked="isSelected(prop.propertyId)"
              @change="toggleProperty(prop.propertyId)"
              class="w-3.5 h-3.5 text-purple-600 border-gray-300 rounded cursor-pointer focus:ring-purple-500"
            />
            <span>{{ prop.propertyName }}</span>
          </label>
        </div>
      </div>

      <!-- 카테고리 속성 -->
      <div v-if="categoryProperties.length > 0" class="property-group">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">카테고리 속성</span>
          <span class="text-xs text-gray-400">선택한 카테고리에서 상속</span>
        </div>
        <div class="space-y-2">
          <div
            v-for="item in categoryProperties"
            :key="item.property.propertyId"
            class="flex items-center justify-between px-2.5 py-2 bg-green-50 border border-green-200 rounded text-xs"
          >
            <div class="flex items-center gap-2">
              <input
                type="checkbox"
                :checked="isSelected(item.property.propertyId)"
                @change="toggleProperty(item.property.propertyId)"
                class="w-3.5 h-3.5 text-green-600 border-gray-300 rounded cursor-pointer focus:ring-green-500"
              />
              <span class="text-green-700">{{ item.property.propertyName }}</span>
              <span class="text-green-500 text-[10px]">({{ item.categoryName }})</span>
            </div>
            <div v-if="item.property.defaultValue" class="flex items-center gap-1">
              <span class="text-gray-400 text-[10px]">기본값:</span>
              <span class="text-green-600 text-[10px] font-medium">{{ item.property.defaultValue }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 빈 상태 -->
      <div v-if="globalProperties.length === 0 && managerProperties.length === 0 && categoryProperties.length === 0"
           class="text-center py-6 text-gray-400 text-sm">
        <p>선택 가능한 속성이 없습니다.</p>
        <p class="text-xs mt-1">카테고리를 선택하거나 관리자에게 문의하세요.</p>
      </div>
    </div>

    <!-- 선택 요약 -->
    <div class="mt-3 pt-3 border-t border-gray-100">
      <div class="flex items-center justify-between text-xs">
        <span class="text-gray-500">
          선택된 속성: <strong class="text-gray-700">{{ localSelectedIds.size }}개</strong>
        </span>
        <span class="text-gray-400">
          기본 {{ basicProperties.length }}개 + 선택 {{ localSelectedIds.size }}개
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.board-property-selector {
  @apply bg-gray-50 rounded-lg p-4;
}

.property-group {
  @apply bg-white rounded-lg p-3 border border-gray-100;
}
</style>
