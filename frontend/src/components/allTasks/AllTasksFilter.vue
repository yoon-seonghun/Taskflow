<script setup lang="ts">
/**
 * 전체 업무 필터 컴포넌트
 * - 키워드 검색, 상태, 우선순위, 날짜 범위 필터
 * - 하위 업무 표시 모드 선택
 * - 정렬 옵션
 */
import { ref, computed, watch } from 'vue'
import Input from '@/components/common/Input.vue'
import Select from '@/components/common/Select.vue'
import DatePicker from '@/components/common/DatePicker.vue'
import type { AllItemsFilter, ChildDisplayMode } from '@/types/allTasks'
import type { ItemStatus, Priority } from '@/types/item'

interface Props {
  /** 현재 필터 */
  filters: AllItemsFilter
  /** 로딩 상태 */
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  (e: 'update:filters', filters: Partial<AllItemsFilter>): void
  (e: 'reset'): void
  (e: 'search', keyword: string): void
}>()

// 내부 상태
const keyword = ref(props.filters.keyword || '')
const showAdvanced = ref(false)

// 검색 디바운스
let searchTimeout: ReturnType<typeof setTimeout> | null = null

// 키워드 변경 감지
watch(keyword, (newValue) => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    emit('search', newValue)
  }, 300)
})

// props.filters 변경 시 keyword 동기화
watch(() => props.filters.keyword, (newValue) => {
  keyword.value = newValue || ''
})

// 상태 옵션
const statusOptions = [
  { label: '전체', value: '' },
  { label: '시작전', value: 'NOT_STARTED' },
  { label: '진행중', value: 'IN_PROGRESS' },
  { label: '대기중', value: 'PENDING' },
  { label: '완료', value: 'COMPLETED' }
]

// 우선순위 옵션
const priorityOptions = [
  { label: '전체', value: '' },
  { label: '긴급', value: 'URGENT' },
  { label: '높음', value: 'HIGH' },
  { label: '보통', value: 'MEDIUM' },
  { label: '낮음', value: 'LOW' }
]

// 하위 업무 표시 모드 옵션
const childDisplayModeOptions = [
  { label: '트리', value: 'tree' },
  { label: '평면', value: 'flat' },
  { label: '접기', value: 'collapse' }
]

// 정렬 필드 옵션
const sortFieldOptions = [
  { label: '생성일', value: 'createdAt' },
  { label: '요청일', value: 'requestDate' },
  { label: '마감일', value: 'dueDate' },
  { label: '수정일', value: 'updatedAt' },
  { label: '우선순위', value: 'priority' },
  { label: '제목', value: 'title' }
]

// 정렬 방향 옵션
const sortDirectionOptions = [
  { label: '내림차순', value: 'desc' },
  { label: '오름차순', value: 'asc' }
]

// 필터 변경 핸들러
function updateFilter(key: keyof AllItemsFilter, value: unknown): void {
  emit('update:filters', { [key]: value || null })
}

// 상태 변경
function onStatusChange(value: string): void {
  updateFilter('status', value ? (value as ItemStatus) : null)
}

// 우선순위 변경
function onPriorityChange(value: string): void {
  updateFilter('priority', value ? (value as Priority) : null)
}

// 하위 업무 표시 모드 변경
function onChildDisplayModeChange(value: string): void {
  updateFilter('childDisplayMode', value as ChildDisplayMode)
}

// 정렬 필드 변경
function onSortFieldChange(value: string): void {
  updateFilter('sortField', value)
}

// 정렬 방향 변경
function onSortDirectionChange(value: string): void {
  updateFilter('sortDirection', value as 'asc' | 'desc')
}

// 날짜 범위 변경
function onStartDateChange(value: string): void {
  updateFilter('startDate', value || null)
}

function onEndDateChange(value: string): void {
  updateFilter('endDate', value || null)
}

// 완료 업무 포함 토글
function toggleIncludeCompleted(): void {
  updateFilter('includeCompleted', !props.filters.includeCompleted)
}

// 필터 초기화
function resetFilters(): void {
  keyword.value = ''
  emit('reset')
}

// 활성 필터 수 계산
const activeFilterCount = computed(() => {
  let count = 0
  if (props.filters.keyword) count++
  if (props.filters.status) count++
  if (props.filters.priority) count++
  if (props.filters.startDate || props.filters.endDate) count++
  if (props.filters.includeCompleted) count++
  if (props.filters.assigneeUsername) count++
  if (props.filters.assignedByUsername) count++
  return count
})
</script>

<template>
  <div class="space-y-3">
    <!-- 기본 필터 (항상 표시) - 한 줄로 고정 -->
    <div class="flex items-center gap-2">
      <!-- 키워드 검색 -->
      <div class="w-64 flex-shrink-0">
        <Input
          v-model="keyword"
          type="text"
          placeholder="업무 검색..."
          :disabled="loading"
          class="w-full"
        >
          <template #prefix>
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </template>
        </Input>
      </div>

      <!-- 하위 업무 표시 모드 (검색창 바로 다음) -->
      <div class="w-20 flex-shrink-0">
        <Select
          :model-value="filters.childDisplayMode"
          :options="childDisplayModeOptions"
          :disabled="loading"
          @update:model-value="onChildDisplayModeChange"
        />
      </div>

      <!-- 정렬 -->
      <div class="flex items-center gap-1 flex-shrink-0">
        <div class="w-24">
          <Select
            :model-value="filters.sortField"
            :options="sortFieldOptions"
            :disabled="loading"
            @update:model-value="onSortFieldChange"
          />
        </div>
        <button
          type="button"
          class="p-2 rounded hover:bg-gray-100 dark:hover:bg-gray-700"
          :title="filters.sortDirection === 'desc' ? '내림차순' : '오름차순'"
          :disabled="loading"
          @click="onSortDirectionChange(filters.sortDirection === 'desc' ? 'asc' : 'desc')"
        >
          <svg
            class="w-4 h-4 text-gray-500 dark:text-gray-400 transition-transform"
            :class="{ 'rotate-180': filters.sortDirection === 'asc' }"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>

      <!-- 고급 필터 토글 -->
      <button
        type="button"
        class="flex items-center gap-1 px-3 py-2 text-sm rounded-lg transition-colors whitespace-nowrap flex-shrink-0"
        :class="showAdvanced || activeFilterCount > 0
          ? 'bg-blue-50 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400'
          : 'text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700'"
        @click="showAdvanced = !showAdvanced"
      >
        <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
        </svg>
        <span>필터</span>
        <span
          v-if="activeFilterCount > 0"
          class="px-1.5 py-0.5 text-xs font-medium bg-blue-100 text-blue-600 dark:bg-blue-900 dark:text-blue-400 rounded-full"
        >
          {{ activeFilterCount }}
        </span>
      </button>

      <!-- 필터 초기화 -->
      <button
        v-if="activeFilterCount > 0"
        type="button"
        class="flex items-center gap-1 px-3 py-2 text-sm text-gray-600 hover:text-red-600 dark:text-gray-400 dark:hover:text-red-400 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
        @click="resetFilters"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
        <span>초기화</span>
      </button>
    </div>

    <!-- 고급 필터 (토글) -->
    <div v-if="showAdvanced" class="flex flex-wrap items-center gap-3 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
      <!-- 상태 -->
      <div class="flex items-center gap-2">
        <span class="text-xs text-gray-500 dark:text-gray-400">상태</span>
        <Select
          :model-value="filters.status || ''"
          :options="statusOptions"
          :disabled="loading"
          class="w-24"
          @update:model-value="onStatusChange"
        />
      </div>

      <!-- 우선순위 -->
      <div class="flex items-center gap-2">
        <span class="text-xs text-gray-500 dark:text-gray-400">우선순위</span>
        <Select
          :model-value="filters.priority || ''"
          :options="priorityOptions"
          :disabled="loading"
          class="w-24"
          @update:model-value="onPriorityChange"
        />
      </div>

      <!-- 날짜 범위 -->
      <div class="flex items-center gap-2">
        <span class="text-xs text-gray-500 dark:text-gray-400">기간</span>
        <DatePicker
          :model-value="filters.startDate || ''"
          placeholder="시작일"
          :disabled="loading"
          class="w-32"
          @update:model-value="onStartDateChange"
        />
        <span class="text-gray-400">~</span>
        <DatePicker
          :model-value="filters.endDate || ''"
          placeholder="종료일"
          :disabled="loading"
          class="w-32"
          @update:model-value="onEndDateChange"
        />
      </div>

      <!-- 완료 업무 포함 -->
      <label class="flex items-center gap-2 cursor-pointer">
        <input
          type="checkbox"
          :checked="filters.includeCompleted"
          :disabled="loading"
          class="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700"
          @change="toggleIncludeCompleted"
        />
        <span class="text-sm text-gray-600 dark:text-gray-400">완료 업무 포함</span>
      </label>
    </div>
  </div>
</template>
