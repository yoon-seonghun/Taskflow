<script setup lang="ts">
/**
 * 속성 타입 선택 컴포넌트
 * - 타입 변경 시 경고 표시
 * - 데이터 마이그레이션 안내
 * Compact UI 적용
 */
import { ref, computed } from 'vue'
import type { PropertyType } from '@/types/property'

interface Props {
  currentType: PropertyType
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'select', type: PropertyType): void
  (e: 'close'): void
}>()

// 상태
const showWarning = ref(false)
const pendingType = ref<PropertyType | null>(null)

// 타입 정의
const typeOptions: Array<{
  type: PropertyType
  label: string
  icon: string
  description: string
  compatibleWith?: PropertyType[]
}> = [
  {
    type: 'TEXT',
    label: '텍스트',
    icon: 'text',
    description: '짧은 텍스트 입력',
    compatibleWith: ['NUMBER', 'DATE']
  },
  {
    type: 'NUMBER',
    label: '숫자',
    icon: 'number',
    description: '숫자 값 입력',
    compatibleWith: ['TEXT']
  },
  {
    type: 'DATE',
    label: '날짜',
    icon: 'calendar',
    description: '날짜 선택',
    compatibleWith: ['TEXT']
  },
  {
    type: 'SELECT',
    label: '단일선택',
    icon: 'list',
    description: '옵션 중 하나 선택',
    compatibleWith: ['MULTI_SELECT', 'TEXT']
  },
  {
    type: 'MULTI_SELECT',
    label: '다중선택',
    icon: 'check-square',
    description: '여러 옵션 선택 가능',
    compatibleWith: ['SELECT', 'TEXT']
  },
  {
    type: 'CHECKBOX',
    label: '체크박스',
    icon: 'check',
    description: '예/아니오 선택',
    compatibleWith: ['TEXT']
  },
  {
    type: 'USER',
    label: '사용자',
    icon: 'user',
    description: '사용자 선택',
    compatibleWith: []
  }
]

// 호환 가능 여부 확인
function isCompatible(type: PropertyType): boolean {
  const currentOption = typeOptions.find(t => t.type === props.currentType)
  return currentOption?.compatibleWith?.includes(type) ?? false
}

// 타입 선택
function handleSelect(type: PropertyType) {
  if (type === props.currentType) {
    emit('close')
    return
  }

  // 호환되지 않는 타입이면 경고 표시
  if (!isCompatible(type)) {
    pendingType.value = type
    showWarning.value = true
    return
  }

  emit('select', type)
}

// 경고 확인 후 타입 변경
function confirmTypeChange() {
  if (pendingType.value) {
    emit('select', pendingType.value)
  }
  showWarning.value = false
  pendingType.value = null
}

// 경고 취소
function cancelTypeChange() {
  showWarning.value = false
  pendingType.value = null
}
</script>

<template>
  <div class="w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-1" @click.stop>
    <!-- 헤더 -->
    <div class="px-3 py-2 border-b border-gray-100">
      <span class="text-[13px] font-medium text-gray-700">속성 타입 선택</span>
    </div>

    <!-- 경고 화면 -->
    <div v-if="showWarning" class="p-3">
      <div class="flex items-start gap-2 mb-3">
        <svg class="w-5 h-5 text-yellow-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        <div>
          <p class="text-[13px] font-medium text-gray-900 mb-1">데이터 변환 경고</p>
          <p class="text-[12px] text-gray-600">
            타입 변경 시 기존 데이터가 손실되거나 변환될 수 있습니다.
          </p>
        </div>
      </div>

      <div class="flex justify-end gap-2">
        <button
          class="px-3 py-1.5 text-[13px] text-gray-600 hover:bg-gray-100 rounded transition-colors"
          @click="cancelTypeChange"
        >
          취소
        </button>
        <button
          class="px-3 py-1.5 text-[13px] text-white bg-yellow-500 hover:bg-yellow-600 rounded transition-colors"
          @click="confirmTypeChange"
        >
          변경
        </button>
      </div>
    </div>

    <!-- 타입 목록 -->
    <div v-else class="py-1 max-h-80 overflow-y-auto">
      <button
        v-for="option in typeOptions"
        :key="option.type"
        class="w-full flex items-start gap-3 px-3 py-2 transition-colors"
        :class="[
          option.type === currentType
            ? 'bg-primary-50 text-primary-700'
            : 'text-gray-700 hover:bg-gray-50'
        ]"
        @click="handleSelect(option.type)"
      >
        <!-- 아이콘 -->
        <span class="w-5 h-5 flex items-center justify-center flex-shrink-0 mt-0.5">
          <!-- Text Icon -->
          <svg v-if="option.icon === 'text'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
          </svg>
          <!-- Number Icon -->
          <svg v-else-if="option.icon === 'number'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
          </svg>
          <!-- Calendar Icon -->
          <svg v-else-if="option.icon === 'calendar'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <!-- List Icon -->
          <svg v-else-if="option.icon === 'list'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
          </svg>
          <!-- Check Square Icon -->
          <svg v-else-if="option.icon === 'check-square'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
          </svg>
          <!-- Check Icon -->
          <svg v-else-if="option.icon === 'check'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          <!-- User Icon -->
          <svg v-else-if="option.icon === 'user'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
        </span>

        <!-- 정보 -->
        <div class="flex-1 min-w-0 text-left">
          <div class="flex items-center gap-2">
            <span class="text-[13px] font-medium">{{ option.label }}</span>
            <svg
              v-if="option.type === currentType"
              class="w-4 h-4 text-primary-500"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <p class="text-[11px] text-gray-500">{{ option.description }}</p>
        </div>
      </button>
    </div>
  </div>
</template>
