<script setup lang="ts">
/**
 * 보드 속성 선택 컴포넌트 (v2.0 통합 속성 선택 패널)
 *
 * 설계서 기반:
 * - 기본 속성: 필수 (disabled)
 * - 글로벌 속성: 선택 가능
 * - 매니저 속성: 선택 가능
 * - 카테고리 속성: 선택한 카테고리에서 상속된 속성
 *
 * v2.0.1: 드래그 앤 드롭 속성 순서 변경 기능 추가
 * - owner_type별 그룹 표시 (GLOBAL → MANAGER → USER/CATEGORY)
 * - 드래그 앤 드롭으로 순서 변경
 * - 순서 변경 시 propertySortOrders emit
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { propertyApi } from '@/api/property'
import { categoryApi } from '@/api/category'
import { boardApi } from '@/api/board'
import type { PropertyDef } from '@/types/property'
import type { CategoryProperty, CategoryDetail } from '@/types/category'
import type { BoardProperty } from '@/types/board'

// 드래그 가능한 속성 인터페이스
interface DraggableProperty {
  propertyId: number
  propertyName: string
  propertyType: string
  ownerType: 'GLOBAL' | 'MANAGER' | 'USER' | 'CATEGORY'
  categoryName?: string
  defaultValue?: string
  sortOrder: number
}

interface Props {
  selectedCategoryIds: number[]
  selectedPropertyIds: number[]
  propertyDefaults?: Record<number, string>  // propertyId -> defaultValue
  initialSortOrders?: Record<number, number>  // propertyId -> sortOrder
  enableDragDrop?: boolean  // 드래그 앤 드롭 활성화 여부
  showCategoryProperties?: boolean  // v2.0.4: 카테고리 속성 표시 여부 (보드 모드에서는 false)
  boardId?: number | null  // v2.3.1: 보드 ID (업무 생성 시 보드 속성 자동 상속용)
}

const props = withDefaults(defineProps<Props>(), {
  selectedCategoryIds: () => [],
  selectedPropertyIds: () => [],
  propertyDefaults: () => ({}),
  initialSortOrders: () => ({}),
  enableDragDrop: false,
  showCategoryProperties: true,  // 기본값: 카테고리 속성 표시
  boardId: null  // v2.3.1: 보드 ID (업무 생성 시 보드 속성 자동 상속용)
})

const emit = defineEmits<{
  (e: 'update:selectedPropertyIds', value: number[]): void
  (e: 'update:propertyDefaults', value: Record<number, string>): void
  (e: 'update:propertySortOrders', value: Record<number, number>): void
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
// Vue Set 반응성을 위한 버전 카운터 (Set 변경 시 증가)
const localSelectedIdsVersion = ref(0)

// 초기 속성 ID 저장 (props에서 전달된 값)
initialPropertyIds.value = new Set(props.selectedPropertyIds)

// 드래그 앤 드롭 상태
const draggedPropertyIndex = ref<number | null>(null)
const dropTargetIndex = ref<number | null>(null)
// 정렬 순서 (propertyId -> sortOrder) - 백엔드 호환을 위해 숫자 키 사용
const propertySortOrders = ref<Map<number, number>>(new Map())

// props.initialSortOrders 초기화
Object.entries(props.initialSortOrders).forEach(([propId, sortOrder]) => {
  propertySortOrders.value.set(Number(propId), sortOrder)
})

// 카테고리 속성 목록 (flatten) - 중복 제거
// v2.0.3: 글로벌/매니저 속성과 동일 propertyId 속성도 제외 (동일 속성 중복 표시 방지)
const categoryProperties = computed(() => {
  const result: Array<{ categoryName: string; property: CategoryProperty }> = []
  const seenPropertyIds = new Set<number>()

  // 글로벌/매니저 속성의 propertyId 수집 (중복 제외용)
  const globalManagerPropertyIds = new Set<number>()
  globalProperties.value.forEach(p => globalManagerPropertyIds.add(p.propertyId))
  managerProperties.value.forEach(p => globalManagerPropertyIds.add(p.propertyId))

  categoryPropertiesMap.value.forEach((value, categoryId) => {
    value.properties.forEach(prop => {
      // 1. 글로벌/매니저 속성으로 이미 존재하는 propertyId 제외
      if (globalManagerPropertyIds.has(prop.propertyId)) {
        console.log('[categoryProperties] 글로벌/매니저 속성과 중복 - 스킵:', prop.propertyId, prop.propertyName)
        return
      }
      // 2. 같은 속성이 여러 카테고리에 있을 경우 첫 번째만 사용
      if (!seenPropertyIds.has(prop.propertyId)) {
        seenPropertyIds.add(prop.propertyId)
        result.push({
          categoryName: value.categoryName,
          property: prop
        })
      } else {
        console.warn('[categoryProperties] 중복 카테고리 속성 스킵:', prop.propertyId, prop.propertyName, '(', value.categoryName, ')')
      }
    })
  })
  console.log('[categoryProperties computed] 총', result.length, '개 속성 (중복 제거 후)')
  return result
})

// 통합 속성 목록 (드래그 앤 드롭용) - 현재 선택된 속성만 포함
// v2.0.5: computed에서 side effect 제거, 순수 계산만 수행
const flattenedProperties = computed((): DraggableProperty[] => {
  // Vue Set 반응성을 위해 버전 카운터 의존성 추가
  const _version = localSelectedIdsVersion.value

  if (!props.enableDragDrop) return []

  const result: DraggableProperty[] = []

  // 현재 선택된 속성 ID 사용 (실시간 업데이트 반영)
  const targetIds = localSelectedIds.value
  // 중복 방지: propertyId-ownerType 조합으로 체크
  const addedPropertyKeys = new Set<string>()

  console.log('[flattenedProperties] 현재 선택된 속성 (v' + _version + '):', Array.from(targetIds))

  // 글로벌 속성
  globalProperties.value.forEach(prop => {
    const propKey = `${prop.propertyId}-GLOBAL`
    if (targetIds.has(prop.propertyId) && !addedPropertyKeys.has(propKey)) {
      addedPropertyKeys.add(propKey)
      const sortOrder = propertySortOrders.value.get(prop.propertyId) ?? 9999
      result.push({
        propertyId: prop.propertyId,
        propertyName: prop.propertyName,
        propertyType: prop.propertyType,
        ownerType: 'GLOBAL',
        sortOrder
      })
    }
  })

  // 매니저 속성
  managerProperties.value.forEach(prop => {
    const propKey = `${prop.propertyId}-MANAGER`
    if (targetIds.has(prop.propertyId) && !addedPropertyKeys.has(propKey)) {
      addedPropertyKeys.add(propKey)
      const sortOrder = propertySortOrders.value.get(prop.propertyId) ?? 9999
      result.push({
        propertyId: prop.propertyId,
        propertyName: prop.propertyName,
        propertyType: prop.propertyType,
        ownerType: 'MANAGER',
        sortOrder
      })
    }
  })

  // 카테고리 속성
  categoryProperties.value.forEach(item => {
    const propKey = `${item.property.propertyId}-CATEGORY`
    if (targetIds.has(item.property.propertyId) && !addedPropertyKeys.has(propKey)) {
      addedPropertyKeys.add(propKey)
      const sortOrder = propertySortOrders.value.get(item.property.propertyId) ?? 9999
      result.push({
        propertyId: item.property.propertyId,
        propertyName: item.property.propertyName,
        propertyType: item.property.propertyType || 'TEXT',
        ownerType: 'CATEGORY',
        categoryName: item.categoryName,
        defaultValue: item.property.defaultValue as string | undefined,
        sortOrder
      })
    }
  })

  console.log('[flattenedProperties] 최종 결과:', result.length, '개')
  // sortOrder 기준으로 정렬
  return result.sort((a, b) => a.sortOrder - b.sortOrder)
})

// 선택된 속성에 대해 sortOrder가 없으면 자동 할당하고 emit
// v2.0.2: sortOrder는 1 기반으로 통일 (PropertiesContent.vue와 일관성 유지)
function ensureSortOrdersForSelectedProperties() {
  if (!props.enableDragDrop) return

  const targetIds = localSelectedIds.value
  let maxSortOrder = 0  // 1 기반이므로 0부터 시작 (다음 값이 1이 됨)
  propertySortOrders.value.forEach((order) => {
    if (order > maxSortOrder) maxSortOrder = order
  })
  let nextSortOrder = maxSortOrder + 1  // 다음 할당 값은 기존 최대값 + 1

  const allPropertyIds = new Set<number>()
  let hasNewSortOrders = false

  // 글로벌 속성
  globalProperties.value.forEach(prop => {
    if (targetIds.has(prop.propertyId)) {
      allPropertyIds.add(prop.propertyId)
      if (!propertySortOrders.value.has(prop.propertyId)) {
        propertySortOrders.value.set(prop.propertyId, nextSortOrder++)
        hasNewSortOrders = true
      }
    }
  })

  // 매니저 속성
  managerProperties.value.forEach(prop => {
    if (targetIds.has(prop.propertyId)) {
      allPropertyIds.add(prop.propertyId)
      if (!propertySortOrders.value.has(prop.propertyId)) {
        propertySortOrders.value.set(prop.propertyId, nextSortOrder++)
        hasNewSortOrders = true
      }
    }
  })

  // 카테고리 속성
  categoryProperties.value.forEach(item => {
    if (targetIds.has(item.property.propertyId)) {
      allPropertyIds.add(item.property.propertyId)
      if (!propertySortOrders.value.has(item.property.propertyId)) {
        propertySortOrders.value.set(item.property.propertyId, nextSortOrder++)
        hasNewSortOrders = true
      }
    }
  })

  // 선택 해제된 속성의 sortOrder 제거
  const keysToRemove: number[] = []
  propertySortOrders.value.forEach((_, propId) => {
    if (!allPropertyIds.has(propId)) {
      keysToRemove.push(propId)
    }
  })
  keysToRemove.forEach(propId => {
    propertySortOrders.value.delete(propId)
    hasNewSortOrders = true
  })

  // 변경이 있으면 emit
  if (hasNewSortOrders) {
    const sortOrdersObj: Record<number, number> = {}
    propertySortOrders.value.forEach((order, propId) => {
      sortOrdersObj[propId] = order
    })
    console.log('[ensureSortOrdersForSelectedProperties] sortOrders 업데이트:', sortOrdersObj)
    emit('update:propertySortOrders', sortOrdersObj)
  }
}

// ownerType 라벨
function getOwnerTypeLabel(ownerType: string): string {
  const labels: Record<string, string> = {
    GLOBAL: '글로벌',
    MANAGER: '매니저',
    USER: '사용자',
    CATEGORY: '카테고리'
  }
  return labels[ownerType] || ownerType
}

// ownerType 색상
function getOwnerTypeColor(ownerType: string): string {
  const colors: Record<string, string> = {
    GLOBAL: 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400',
    MANAGER: 'bg-purple-100 text-purple-700 dark:bg-purple-900/50 dark:text-purple-400',
    USER: 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400',
    CATEGORY: 'bg-orange-100 text-orange-700 dark:bg-orange-900/50 dark:text-orange-400'
  }
  return colors[ownerType] || 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
}

// 드래그 시작
function handlePropertyDragStart(index: number, event: DragEvent) {
  draggedPropertyIndex.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(index))
  }
}

// 드래그 오버
function handlePropertyDragOver(index: number, event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  dropTargetIndex.value = index
}

// 드래그 리브
function handlePropertyDragLeave() {
  dropTargetIndex.value = null
}

// 드래그 종료
function handlePropertyDragEnd() {
  draggedPropertyIndex.value = null
  dropTargetIndex.value = null
}

// 드롭 - 속성 순서 변경
function handlePropertyDrop(targetIndex: number, event: DragEvent) {
  event.preventDefault()

  if (draggedPropertyIndex.value === null || draggedPropertyIndex.value === targetIndex) {
    handlePropertyDragEnd()
    return
  }

  // 현재 정렬된 속성 목록을 배열로 복사
  const properties = [...flattenedProperties.value]
  const draggedItem = properties[draggedPropertyIndex.value]

  // 배열에서 드래그된 아이템 제거 후 새 위치에 삽입
  properties.splice(draggedPropertyIndex.value, 1)
  properties.splice(targetIndex, 0, draggedItem)

  // 새로운 sortOrder 계산 및 저장 (propertyId 숫자 키 사용 - 백엔드 호환)
  // v2.0.2: 1 기반 sortOrder 사용 (PropertiesContent.vue와 일관성 유지)
  const newSortOrders = new Map<number, number>()
  properties.forEach((prop, idx) => {
    newSortOrders.set(prop.propertyId, idx + 1)  // 1 기반
  })

  propertySortOrders.value = newSortOrders

  // 부모에게 emit (숫자 키 사용)
  const sortOrdersObj: Record<number, number> = {}
  newSortOrders.forEach((order, propId) => {
    sortOrdersObj[propId] = order
  })
  emit('update:propertySortOrders', sortOrdersObj)

  handlePropertyDragEnd()
}

// v2.0.3: 선택된 속성 중 동일명 속성 찾기
function findDuplicateNameProperty(propertyName: string, excludePropertyId: number): { name: string; ownerType: string } | null {
  // 현재 선택된 속성 ID 목록
  const selectedIds = localSelectedIds.value

  // 글로벌 속성에서 동일명 확인
  for (const prop of globalProperties.value) {
    if (prop.propertyId !== excludePropertyId && prop.propertyName === propertyName && selectedIds.has(prop.propertyId)) {
      return { name: prop.propertyName, ownerType: '글로벌' }
    }
  }

  // 매니저 속성에서 동일명 확인
  for (const prop of managerProperties.value) {
    if (prop.propertyId !== excludePropertyId && prop.propertyName === propertyName && selectedIds.has(prop.propertyId)) {
      return { name: prop.propertyName, ownerType: '매니저' }
    }
  }

  // 카테고리 속성에서 동일명 확인
  for (const item of categoryProperties.value) {
    if (item.property.propertyId !== excludePropertyId && item.property.propertyName === propertyName && selectedIds.has(item.property.propertyId)) {
      return { name: item.property.propertyName, ownerType: `카테고리(${item.categoryName})` }
    }
  }

  return null
}

// v2.0.3: 속성 이름 조회 (propertyId로)
function getPropertyName(propertyId: number): string {
  // 글로벌 속성에서 찾기
  const globalProp = globalProperties.value.find(p => p.propertyId === propertyId)
  if (globalProp) return globalProp.propertyName

  // 매니저 속성에서 찾기
  const managerProp = managerProperties.value.find(p => p.propertyId === propertyId)
  if (managerProp) return managerProp.propertyName

  // 카테고리 속성에서 찾기
  const categoryItem = categoryProperties.value.find(item => item.property.propertyId === propertyId)
  if (categoryItem) return categoryItem.property.propertyName

  return ''
}

// 속성 선택 토글
function toggleProperty(propertyId: number) {
  // Vue 반응성을 위해 새로운 Set 생성
  const newSet = new Set(localSelectedIds.value)

  if (newSet.has(propertyId)) {
    newSet.delete(propertyId)
    console.log('[toggleProperty] 속성 제거:', propertyId)
  } else {
    // v2.0.3: 동일명 속성 선택 경고
    const propertyName = getPropertyName(propertyId)
    const duplicate = findDuplicateNameProperty(propertyName, propertyId)
    if (duplicate) {
      alert(`동일한 이름의 속성이 이미 선택되어 있습니다.\n\n` +
            `속성명: "${duplicate.name}"\n` +
            `유형: ${duplicate.ownerType}\n\n` +
            `동일 이름의 속성을 중복 선택할 수 없습니다.`)
      // v2.0.5: @click.prevent 사용으로 체크박스 상태가 자동 변경되지 않음 - 바로 return
      return
    }

    newSet.add(propertyId)
    console.log('[toggleProperty] 속성 추가:', propertyId)
  }

  // 새로운 Set으로 교체 (반응성 보장)
  localSelectedIds.value = newSet
  // 버전 카운터 증가 (computed 재계산 트리거)
  localSelectedIdsVersion.value++

  const emitArray = Array.from(newSet)
  console.log('[toggleProperty] emit할 속성 IDs:', emitArray)
  emit('update:selectedPropertyIds', emitArray)

  // sortOrder 정리 및 emit
  ensureSortOrdersForSelectedProperties()
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

// v2.3.1: 보드 속성 로드 및 자동 선택 (업무 생성 시 보드에서 설정한 속성 자동 상속)
async function loadBoardPropertiesAndSelect(boardId: number) {
  try {
    console.log('[BoardPropertySelector] 보드 속성 로드 시작, boardId:', boardId)
    const response = await boardApi.getBoardProperties(boardId)

    if (response.success && response.data && response.data.length > 0) {
      console.log('[BoardPropertySelector] 보드 속성 로드 완료:', response.data.length, '개')

      // 보드 속성을 자동으로 선택 상태로 설정
      response.data.forEach((prop, index) => {
        // 선택 상태 추가
        localSelectedIds.value.add(prop.propertyId)

        // 기본값이 있으면 적용
        if (prop.defaultValue) {
          localDefaults.value[prop.propertyId] = prop.defaultValue
        }

        // 정렬 순서 적용 (보드에서 설정된 순서 또는 순차적 할당)
        const sortOrder = prop.sortOrder ?? (index + 1)
        if (!propertySortOrders.value.has(prop.propertyId)) {
          propertySortOrders.value.set(prop.propertyId, sortOrder)
        }
      })

      // 반응성 트리거
      localSelectedIdsVersion.value++

      console.log('[BoardPropertySelector] 보드 속성 자동 선택 완료:', Array.from(localSelectedIds.value))

      // 부모에게 변경 사항 emit
      emit('update:selectedPropertyIds', Array.from(localSelectedIds.value))
      emit('update:propertyDefaults', { ...localDefaults.value })

      // sortOrder emit
      const sortOrdersObj: Record<number, number> = {}
      propertySortOrders.value.forEach((order, propId) => {
        sortOrdersObj[propId] = order
      })
      emit('update:propertySortOrders', sortOrdersObj)
    } else {
      console.log('[BoardPropertySelector] 보드에 설정된 속성 없음')
    }
  } catch (error) {
    console.error('[BoardPropertySelector] 보드 속성 로드 실패:', error)
  }
}

// 카테고리 속성 로드
async function loadCategoryProperties(categoryIds: number[], isInitialLoad: boolean = false) {
  // 새로운 Map을 생성하여 반응성 보장
  const newMap = new Map<number, { categoryName: string; properties: CategoryProperty[] }>()

  // edit 모드 판단: initialPropertyIds가 있으면 edit 모드
  const isEditMode = initialPropertyIds.value.size > 0

  if (categoryIds.length === 0) {
    // 카테고리가 없어지면 이전 카테고리 속성 제거 (초기 로드 아닌 경우)
    if (!isInitialLoad && isEditMode && previousCategoryPropertyIds.size > 0) {
      console.log('[loadCategoryProperties] 카테고리 해제 - 이전 속성 제거:', Array.from(previousCategoryPropertyIds))
      for (const propId of previousCategoryPropertyIds) {
        localSelectedIds.value.delete(propId)
      }
      previousCategoryPropertyIds.clear()
      localSelectedIdsVersion.value++ // 반응성 트리거
      emit('update:selectedPropertyIds', Array.from(localSelectedIds.value))

      // sortOrder 정리 및 emit
      ensureSortOrdersForSelectedProperties()
    }

    categoryPropertiesMap.value = newMap
    // 카테고리가 없어도 초기 속성은 유지
    if (isInitialLoad) {
      // 초기 로드: 초기 속성 ID 복원
      for (const propId of initialPropertyIds.value) {
        localSelectedIds.value.add(propId)
      }
      localSelectedIdsVersion.value++ // 반응성 트리거
    }
    return
  }

  try {
    const promises = categoryIds.map(id => categoryApi.getCategoryDetail(id))
    const results = await Promise.all(promises)

    console.log('[loadCategoryProperties] API 결과:', results)
    console.log('[loadCategoryProperties] isEditMode:', isEditMode, 'isInitialLoad:', isInitialLoad)

    // 새 카테고리 속성 ID 수집
    const newCategoryPropertyIds = new Set<number>()

    results.forEach((res, index) => {
      if (res.success && res.data) {
        const detail = res.data as CategoryDetail
        const validProperties = (detail.properties || []).filter(p => p.propertyId != null)

        console.log(`[loadCategoryProperties] 카테고리 ${categoryIds[index]} 속성:`, validProperties.length, '개')
        validProperties.forEach(p => console.log(`  - ${p.propertyId}: ${p.propertyName} (${p.ownerType})`))

        newMap.set(categoryIds[index], {
          categoryName: detail.categoryName,
          properties: validProperties
        })

        validProperties.forEach(prop => {
          newCategoryPropertyIds.add(prop.propertyId)

          // 카테고리 기본값 적용 (create 모드에서만)
          if (!isEditMode && prop.defaultValue && typeof prop.defaultValue === 'string' && !localDefaults.value[prop.propertyId]) {
            localDefaults.value[prop.propertyId] = prop.defaultValue
          }
        })
      }
    })

    // 새 Map으로 교체 (반응성 트리거)
    categoryPropertiesMap.value = newMap
    console.log('[loadCategoryProperties] categoryPropertiesMap 크기:', categoryPropertiesMap.value.size)

    // 초기 로드 시: 초기 속성 ID 유지
    if (isInitialLoad) {
      for (const propId of initialPropertyIds.value) {
        localSelectedIds.value.add(propId)
      }
      localSelectedIdsVersion.value++ // 반응성 트리거
    }

    // 카테고리 변경 시 (초기 로드가 아닐 때)
    if (!isInitialLoad) {
      // 1. 이전 카테고리 속성 중 새 카테고리에 없는 것 제거
      if (previousCategoryPropertyIds.size > 0) {
        for (const oldPropId of previousCategoryPropertyIds) {
          if (!newCategoryPropertyIds.has(oldPropId)) {
            localSelectedIds.value.delete(oldPropId)
            console.log('[loadCategoryProperties] 이전 카테고리 속성 제거:', oldPropId)
          }
        }
      }

      // v2.0.4: 이미 선택된 글로벌/매니저 속성 이름 수집 (동일명 카테고리 속성 제외용)
      const selectedGlobalManagerNames = new Set<string>()
      for (const prop of globalProperties.value) {
        if (localSelectedIds.value.has(prop.propertyId)) {
          selectedGlobalManagerNames.add(prop.propertyName)
        }
      }
      for (const prop of managerProperties.value) {
        if (localSelectedIds.value.has(prop.propertyId)) {
          selectedGlobalManagerNames.add(prop.propertyName)
        }
      }

      // 2. 새 카테고리 속성 추가 (동일 이름의 글로벌/매니저 속성이 없는 경우에만)
      for (const newPropId of newCategoryPropertyIds) {
        // 카테고리 속성의 이름 찾기
        let propName = ''
        for (const [, catData] of newMap) {
          const foundProp = catData.properties.find(p => p.propertyId === newPropId)
          if (foundProp) {
            propName = foundProp.propertyName
            break
          }
        }

        // v2.0.4: 동일 이름의 글로벌/매니저 속성이 이미 선택되어 있으면 카테고리 속성 추가 제외
        if (selectedGlobalManagerNames.has(propName)) {
          console.log('[loadCategoryProperties] 동일명 글로벌/매니저 속성 존재 - 카테고리 속성 제외:', newPropId, propName)
          continue
        }

        localSelectedIds.value.add(newPropId)
        console.log('[loadCategoryProperties] 새 카테고리 속성 추가:', newPropId)
      }

      // 3. 반응성 트리거 및 변경 사항 emit
      localSelectedIdsVersion.value++
      console.log('[loadCategoryProperties] 카테고리 변경 후 최종 속성:', Array.from(localSelectedIds.value))
      emit('update:selectedPropertyIds', Array.from(localSelectedIds.value))
      emit('update:propertyDefaults', { ...localDefaults.value })

      // 4. sortOrder 정리 및 emit (새 속성에 순서 할당, 제거된 속성 정리)
      ensureSortOrdersForSelectedProperties()
    } else if (!isEditMode) {
      // 초기 로드 + create 모드: 카테고리 속성 자동 선택
      // v2.0.4: 이미 선택된 글로벌/매니저 속성 이름 수집 (동일명 카테고리 속성 제외용)
      const selectedGlobalManagerNames = new Set<string>()
      for (const prop of globalProperties.value) {
        if (localSelectedIds.value.has(prop.propertyId)) {
          selectedGlobalManagerNames.add(prop.propertyName)
        }
      }
      for (const prop of managerProperties.value) {
        if (localSelectedIds.value.has(prop.propertyId)) {
          selectedGlobalManagerNames.add(prop.propertyName)
        }
      }

      for (const newPropId of newCategoryPropertyIds) {
        // 카테고리 속성의 이름 찾기
        let propName = ''
        for (const [, catData] of newMap) {
          const foundProp = catData.properties.find(p => p.propertyId === newPropId)
          if (foundProp) {
            propName = foundProp.propertyName
            break
          }
        }

        // v2.0.4: 동일 이름의 글로벌/매니저 속성이 이미 선택되어 있으면 카테고리 속성 추가 제외
        if (selectedGlobalManagerNames.has(propName)) {
          console.log('[loadCategoryProperties] 초기로드 - 동일명 글로벌/매니저 속성 존재 - 카테고리 속성 제외:', newPropId, propName)
          continue
        }

        localSelectedIds.value.add(newPropId)
      }
      localSelectedIdsVersion.value++ // 반응성 트리거
    }

    // 현재 카테고리 속성을 이전 값으로 저장 (다음 변경 감지용)
    previousCategoryPropertyIds = newCategoryPropertyIds
  } catch (error) {
    console.error('Failed to load category properties:', error)
  }
}

// 이전 카테고리 ID 저장 (비교용)
let previousCategoryIds: number[] = []
// 이전 카테고리 속성 ID 저장 (카테고리 변경 시 제거용)
let previousCategoryPropertyIds: Set<number> = new Set()

// 카테고리 변경 감지
watch(() => props.selectedCategoryIds, (newIds) => {
  // 배열 내용 비교로 불필요한 API 호출 방지
  const hasChanged = JSON.stringify(newIds.slice().sort()) !== JSON.stringify(previousCategoryIds.slice().sort())
  if (hasChanged) {
    // 카테고리 변경 전 현재 카테고리 속성 ID 저장
    previousCategoryPropertyIds = new Set<number>()
    categoryPropertiesMap.value.forEach((value) => {
      value.properties.forEach(prop => {
        previousCategoryPropertyIds.add(prop.propertyId)
      })
    })
    console.log('[watch categoryIds] 이전 카테고리 속성:', Array.from(previousCategoryPropertyIds))

    previousCategoryIds = [...newIds]
    loadCategoryProperties(newIds)
  }
}, { deep: true })

// props 변경 감지 (초기화 완료 후에만 동작)
watch(() => props.selectedPropertyIds, (newIds) => {
  // 초기화 완료 전에는 무시 (onMounted에서 직접 처리)
  if (!isInitialized.value) return
  localSelectedIds.value = new Set(newIds)
  localSelectedIdsVersion.value++ // 반응성 트리거
}, { deep: true })

// v2.3.1: boardId 변경 감지 (모달 열릴 때 boardId가 전달되면 보드 속성 자동 로드)
watch(() => props.boardId, async (newBoardId, oldBoardId) => {
  // boardId가 새로 설정되었을 때만 로드 (null/undefined → 숫자)
  if (newBoardId && !oldBoardId && isInitialized.value) {
    console.log('[BoardPropertySelector] boardId 변경 감지, 보드 속성 로드:', newBoardId)
    await loadBoardPropertiesAndSelect(newBoardId)
  }
}, { immediate: false })

onMounted(async () => {
  console.log('[BoardPropertySelector] onMounted 시작, boardId:', props.boardId)
  previousCategoryIds = [...props.selectedCategoryIds]
  previousCategoryPropertyIds = new Set()

  // 초기 속성 ID 저장 (props에서 전달된 값)
  initialPropertyIds.value = new Set(props.selectedPropertyIds)
  localSelectedIds.value = new Set(props.selectedPropertyIds)
  localSelectedIdsVersion.value = 0 // 초기화

  await loadProperties()
  console.log('[BoardPropertySelector] loadProperties 완료, 글로벌:', globalProperties.value.length, '개, 매니저:', managerProperties.value.length, '개')

  // v2.3.1: boardId가 있으면 보드 속성 자동 로드 및 선택 (업무 생성 시)
  if (props.boardId) {
    console.log('[BoardPropertySelector] boardId 존재, 보드 속성 로드 시작:', props.boardId)
    await loadBoardPropertiesAndSelect(props.boardId)
  } else {
    console.log('[BoardPropertySelector] boardId 없음, 보드 속성 로드 스킵')
  }

  if (props.selectedCategoryIds.length > 0) {
    await loadCategoryProperties(props.selectedCategoryIds, true)  // 초기 로드 플래그
    // 초기 카테고리 속성 ID 저장 (다음 변경 감지용)
    categoryPropertiesMap.value.forEach((value) => {
      value.properties.forEach(prop => {
        previousCategoryPropertyIds.add(prop.propertyId)
      })
    })
    console.log('[BoardPropertySelector] 초기 카테고리 속성:', Array.from(previousCategoryPropertyIds))
  }

  // 초기화 완료 표시
  isInitialized.value = true
  localSelectedIdsVersion.value++ // 초기화 완료 후 반응성 트리거

  // 초기 로드 완료 후 최종 선택 상태를 부모에게 emit (카테고리 속성 포함)
  // (초기 props.selectedPropertyIds와 카테고리에서 상속된 속성이 병합된 상태)
  console.log('[BoardPropertySelector] 초기화 완료, 최종 선택 속성 emit:', Array.from(localSelectedIds.value))
  emit('update:selectedPropertyIds', Array.from(localSelectedIds.value))
  emit('update:propertyDefaults', { ...localDefaults.value })

  // sortOrder 정리 및 emit (초기값 + 새 속성에 순서 할당)
  ensureSortOrdersForSelectedProperties()
})

// 메모리 정리
onUnmounted(() => {
  localSelectedIds.value.clear()
  categoryPropertiesMap.value.clear()
  initialPropertyIds.value.clear()
  previousCategoryIds = []
  previousCategoryPropertyIds.clear()
  isInitialized.value = false
})
</script>

<template>
  <div class="board-property-selector">
    <div class="mb-3">
      <h4 class="text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">속성 선택</h4>
      <p class="text-xs text-gray-500 dark:text-gray-400">보드에서 사용할 속성을 선택하세요</p>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="flex items-center justify-center py-6">
      <svg class="animate-spin h-5 w-5 text-blue-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>

    <div v-else class="space-y-4 max-h-80 overflow-y-auto pr-1">
      <!-- 그룹별 속성 선택 (항상 표시) -->
        <!-- 기본 속성 (필수) -->
        <div class="property-group">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs font-medium text-gray-600 dark:text-gray-300 uppercase tracking-wide">기본 속성</span>
            <span class="text-xs text-gray-400 dark:text-gray-500">필수</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <label
              v-for="prop in basicProperties"
              :key="prop.id"
              class="inline-flex items-center gap-1.5 px-2.5 py-1.5 bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 rounded text-xs cursor-not-allowed"
            >
              <input
                type="checkbox"
                :checked="true"
                disabled
                class="w-3.5 h-3.5 text-gray-400 border-gray-300 dark:border-gray-600 rounded cursor-not-allowed"
              />
              <span>{{ prop.name }}</span>
            </label>
          </div>
        </div>

        <!-- 글로벌 속성 -->
        <div v-if="globalProperties.length > 0" class="property-group">
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-1.5">
              <span class="w-2 h-2 rounded-full bg-blue-500"></span>
              <span class="text-xs font-medium text-gray-600 dark:text-gray-300 uppercase tracking-wide">글로벌 속성</span>
            </div>
            <span class="text-xs text-gray-400 dark:text-gray-500">{{ globalProperties.filter(p => isSelected(p.propertyId)).length }}/{{ globalProperties.length }}</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <label
              v-for="prop in globalProperties"
              :key="prop.propertyId"
              class="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded text-xs cursor-pointer transition-colors group"
              :class="isSelected(prop.propertyId)
                ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400 border border-blue-200 dark:border-blue-800'
                : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-500'"
              :title="`ID: ${prop.propertyId} | 유형: 글로벌 | 타입: ${prop.propertyType}`"
              @click.prevent="toggleProperty(prop.propertyId)"
            >
              <input
                type="checkbox"
                :checked="isSelected(prop.propertyId)"
                class="w-3.5 h-3.5 text-blue-600 border-gray-300 dark:border-gray-600 rounded cursor-pointer focus:ring-blue-500 pointer-events-none"
              />
              <span class="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
              <span>{{ prop.propertyName }}</span>
            </label>
          </div>
        </div>

        <!-- 매니저 속성 -->
        <div v-if="managerProperties.length > 0" class="property-group">
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-1.5">
              <span class="w-2 h-2 rounded-full bg-purple-500"></span>
              <span class="text-xs font-medium text-gray-600 dark:text-gray-300 uppercase tracking-wide">매니저 속성</span>
            </div>
            <span class="text-xs text-gray-400 dark:text-gray-500">{{ managerProperties.filter(p => isSelected(p.propertyId)).length }}/{{ managerProperties.length }}</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <label
              v-for="prop in managerProperties"
              :key="prop.propertyId"
              class="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded text-xs cursor-pointer transition-colors group"
              :class="isSelected(prop.propertyId)
                ? 'bg-purple-50 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400 border border-purple-200 dark:border-purple-800'
                : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-500'"
              :title="`ID: ${prop.propertyId} | 유형: 매니저 | 타입: ${prop.propertyType}`"
              @click.prevent="toggleProperty(prop.propertyId)"
            >
              <input
                type="checkbox"
                :checked="isSelected(prop.propertyId)"
                class="w-3.5 h-3.5 text-purple-600 border-gray-300 dark:border-gray-600 rounded cursor-pointer focus:ring-purple-500 pointer-events-none"
              />
              <span class="w-1.5 h-1.5 rounded-full bg-purple-500"></span>
              <span>{{ prop.propertyName }}</span>
            </label>
          </div>
        </div>

        <!-- 카테고리 속성 (v2.0.4: showCategoryProperties가 true일 때만 표시) -->
        <div v-if="showCategoryProperties && categoryProperties.length > 0" class="property-group">
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-1.5">
              <span class="w-2 h-2 rounded-full bg-green-500"></span>
              <span class="text-xs font-medium text-gray-600 dark:text-gray-300 uppercase tracking-wide">카테고리 속성</span>
            </div>
            <span class="text-xs text-gray-400 dark:text-gray-500">{{ categoryProperties.filter(item => isSelected(item.property.propertyId)).length }}/{{ categoryProperties.length }}</span>
          </div>
          <div class="space-y-2">
            <div
              v-for="item in categoryProperties"
              :key="item.property.propertyId"
              class="flex items-center justify-between px-2.5 py-2 rounded text-xs cursor-pointer transition-colors"
              :class="isSelected(item.property.propertyId)
                ? 'bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800'
                : 'bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-500'"
              :title="`ID: ${item.property.propertyId} | 유형: 카테고리 (${item.categoryName}) | 타입: ${item.property.propertyType || 'TEXT'}`"
              @click="toggleProperty(item.property.propertyId)"
            >
              <div class="flex items-center gap-2">
                <input
                  type="checkbox"
                  :checked="isSelected(item.property.propertyId)"
                  class="w-3.5 h-3.5 text-green-600 border-gray-300 dark:border-gray-600 rounded cursor-pointer focus:ring-green-500 pointer-events-none"
                />
                <span class="w-1.5 h-1.5 rounded-full bg-green-500"></span>
                <span :class="isSelected(item.property.propertyId) ? 'text-green-700 dark:text-green-400' : 'text-gray-600 dark:text-gray-300'">{{ item.property.propertyName }}</span>
                <span class="text-green-500 text-[10px]">({{ item.categoryName }})</span>
              </div>
              <div v-if="item.property.defaultValue" class="flex items-center gap-1">
                <span class="text-gray-400 dark:text-gray-500 text-[10px]">기본값:</span>
                <span class="text-green-600 dark:text-green-400 text-[10px] font-medium">{{ item.property.defaultValue }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 빈 상태 (v2.0.4: showCategoryProperties 여부에 따라 조건 변경) -->
        <div v-if="globalProperties.length === 0 && managerProperties.length === 0 && (!showCategoryProperties || categoryProperties.length === 0)"
             class="text-center py-6 text-gray-400 dark:text-gray-500 text-sm">
          <p>선택 가능한 속성이 없습니다.</p>
          <p v-if="showCategoryProperties" class="text-xs mt-1">카테고리를 선택하거나 관리자에게 문의하세요.</p>
          <p v-else class="text-xs mt-1">관리자에게 글로벌 또는 매니저 속성 등록을 문의하세요.</p>
        </div>
    </div>

    <!-- 드래그 앤 드롭 정렬 섹션 (선택된 속성이 있고 enableDragDrop이 true일 때만 표시) -->
    <div v-if="enableDragDrop && flattenedProperties.length > 0" class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
      <div class="mb-3">
        <h4 class="text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">속성 순서 정렬</h4>
        <p class="text-xs text-gray-500 dark:text-gray-400">드래그하여 속성 표시 순서를 변경하세요</p>
      </div>
      <div class="space-y-1">
        <div
          v-for="(prop, index) in flattenedProperties"
          :key="`${prop.propertyId}-${prop.ownerType}`"
          draggable="true"
          class="flex items-center gap-2 px-3 py-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg cursor-grab hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors sortable-item"
          :class="{
            'sortable-dragging': draggedPropertyIndex === index,
            'sortable-drag-over': dropTargetIndex === index && draggedPropertyIndex !== index
          }"
          :title="`ID: ${prop.propertyId} | 유형: ${getOwnerTypeLabel(prop.ownerType)}${prop.categoryName ? ' (' + prop.categoryName + ')' : ''} | 타입: ${prop.propertyType}`"
          @dragstart="handlePropertyDragStart(index, $event)"
          @dragover="handlePropertyDragOver(index, $event)"
          @dragleave="handlePropertyDragLeave"
          @dragend="handlePropertyDragEnd"
          @drop="handlePropertyDrop(index, $event)"
        >
          <!-- 드래그 핸들 -->
          <svg class="w-4 h-4 text-gray-400 dark:text-gray-500 sortable-handle" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8h16M4 16h16" />
          </svg>

          <!-- 순서 번호 -->
          <span class="w-5 h-5 flex items-center justify-center bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 text-xs font-medium rounded">
            {{ index + 1 }}
          </span>

          <!-- ownerType 색상 인디케이터 -->
          <span
            class="w-2 h-2 rounded-full flex-shrink-0"
            :class="{
              'bg-blue-500': prop.ownerType === 'GLOBAL',
              'bg-purple-500': prop.ownerType === 'MANAGER',
              'bg-green-500': prop.ownerType === 'CATEGORY' || prop.ownerType === 'USER'
            }"
          ></span>

          <!-- 속성명 -->
          <span class="flex-1 text-sm text-gray-700 dark:text-gray-200">{{ prop.propertyName }}</span>

          <!-- ownerType 배지 -->
          <span
            class="px-1.5 py-0.5 text-[10px] font-medium rounded"
            :class="getOwnerTypeColor(prop.ownerType)"
          >
            {{ getOwnerTypeLabel(prop.ownerType) }}
          </span>

          <!-- 카테고리명 (카테고리 속성인 경우) -->
          <span v-if="prop.categoryName" class="text-[10px] text-gray-400 dark:text-gray-500">
            ({{ prop.categoryName }})
          </span>
        </div>
      </div>
    </div>

    <!-- 선택 요약 -->
    <div class="mt-3 pt-3 border-t border-gray-100 dark:border-gray-700">
      <div class="flex items-center justify-between text-xs">
        <span class="text-gray-500 dark:text-gray-400">
          선택된 속성: <strong class="text-gray-700 dark:text-gray-200">{{ localSelectedIds.size }}개</strong>
        </span>
        <span class="text-gray-400 dark:text-gray-500">
          기본 {{ basicProperties.length }}개 + 선택 {{ localSelectedIds.size }}개
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.board-property-selector {
  @apply bg-gray-50 dark:bg-gray-900 rounded-lg p-4;
}

.property-group {
  @apply bg-white dark:bg-gray-800 rounded-lg p-3 border border-gray-100 dark:border-gray-700;
}
</style>
