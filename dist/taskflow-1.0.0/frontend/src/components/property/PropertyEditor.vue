<script setup lang="ts">
/**
 * 속성 편집기 컴포넌트
 * - 속성 타입별 인라인 편집기
 * - TEXT, NUMBER, DATE, SELECT, MULTI_SELECT, CHECKBOX, USER 지원
 * Compact UI 적용
 */
import { computed, ref, watch } from 'vue'
import { usePropertyStore } from '@/stores/property'
import { Input, Select, DatePicker } from '@/components/common'
import type { PropertyDef, PropertyType, PropertyOption } from '@/types/property'
import type { SelectOption } from '@/components/common/Select.vue'

interface Props {
  property: PropertyDef
  modelValue?: unknown
  disabled?: boolean
  compact?: boolean
  users?: Array<{ userId: number; userName: string }>
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  disabled: false,
  compact: false,
  users: () => []
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: unknown): void
  (e: 'change', value: unknown): void
}>()

const propertyStore = usePropertyStore()

// 내부 값 (편집 중인 값)
const internalValue = ref<unknown>(props.modelValue)

// modelValue 변경 시 내부 값 동기화
watch(() => props.modelValue, (newValue) => {
  internalValue.value = newValue
}, { immediate: true })

// 옵션 목록 (SELECT, MULTI_SELECT용)
const options = computed((): SelectOption[] => {
  const propertyOptions = propertyStore.getOptionsByPropertyId(props.property.propertyId)
  return propertyOptions.map(opt => ({
    value: opt.optionId,
    label: opt.optionName,
    color: opt.color
  }))
})

// 사용자 옵션 (USER 타입용)
const userOptions = computed((): SelectOption[] => {
  return props.users.map(user => ({
    value: user.userId,
    label: user.userName
  }))
})

// 값 변경 핸들러
function handleChange(value: unknown) {
  internalValue.value = value
  emit('update:modelValue', value)
  emit('change', value)
}

// TEXT 값 변경
function handleTextChange(value: string | number) {
  handleChange(value)
}

// NUMBER 값 변경
function handleNumberChange(value: string | number) {
  const numValue = typeof value === 'string' ? parseFloat(value) || null : value
  handleChange(numValue)
}

// DATE 값 변경
function handleDateChange(value: string | null) {
  handleChange(value)
}

// SELECT 값 변경
function handleSelectChange(value: string | number | null) {
  handleChange(value)
}

// MULTI_SELECT 값 변경 (토글 방식)
function toggleMultiSelectOption(optionId: number) {
  const currentValue = Array.isArray(internalValue.value) ? [...internalValue.value] : []
  const index = currentValue.indexOf(optionId)

  if (index !== -1) {
    currentValue.splice(index, 1)
  } else {
    currentValue.push(optionId)
  }

  handleChange(currentValue)
}

// CHECKBOX 값 변경
function handleCheckboxChange(event: Event) {
  const target = event.target as HTMLInputElement
  handleChange(target.checked)
}

// MULTI_SELECT에서 선택 여부 확인
function isOptionSelected(optionId: number): boolean {
  if (!Array.isArray(internalValue.value)) return false
  return internalValue.value.includes(optionId)
}

// 옵션 색상 스타일
function getOptionStyle(option: PropertyOption) {
  if (!option.color) return {}
  return {
    backgroundColor: `${option.color}20`,
    color: option.color,
    borderColor: option.color
  }
}

// 새 옵션 생성
async function handleCreateOption(optionName: string) {
  const newOption = await propertyStore.createOption(props.property.propertyId, {
    optionCode: optionName.toLowerCase().replace(/\s+/g, '_'),
    optionName: optionName
  })

  if (newOption) {
    // 단일 선택인 경우 바로 선택
    if (props.property.propertyType === 'SELECT') {
      handleChange(newOption.optionId)
    }
  }
}
</script>

<template>
  <div class="property-editor" :class="{ 'compact': compact }">
    <!-- TEXT 타입 -->
    <template v-if="property.propertyType === 'TEXT'">
      <Input
        :model-value="String(internalValue || '')"
        :disabled="disabled"
        :size="compact ? 'sm' : 'md'"
        :placeholder="property.propertyName"
        @update:model-value="handleTextChange"
      />
    </template>

    <!-- NUMBER 타입 -->
    <template v-else-if="property.propertyType === 'NUMBER'">
      <Input
        :model-value="internalValue as number"
        type="number"
        :disabled="disabled"
        :size="compact ? 'sm' : 'md'"
        :placeholder="property.propertyName"
        @update:model-value="handleNumberChange"
      />
    </template>

    <!-- DATE 타입 -->
    <template v-else-if="property.propertyType === 'DATE'">
      <DatePicker
        :model-value="internalValue as string"
        :disabled="disabled"
        :size="compact ? 'sm' : 'md'"
        :placeholder="property.propertyName"
        mode="date"
        clearable
        @update:model-value="handleDateChange"
      />
    </template>

    <!-- SELECT 타입 -->
    <template v-else-if="property.propertyType === 'SELECT'">
      <Select
        :model-value="internalValue as number"
        :options="options"
        :disabled="disabled"
        :size="compact ? 'sm' : 'md'"
        :placeholder="property.propertyName"
        searchable
        clearable
        allow-create
        @update:model-value="handleSelectChange"
        @create="handleCreateOption"
      />
    </template>

    <!-- MULTI_SELECT 타입 -->
    <template v-else-if="property.propertyType === 'MULTI_SELECT'">
      <div class="flex flex-wrap gap-1.5">
        <!-- 선택된 옵션들 -->
        <button
          v-for="option in propertyStore.getOptionsByPropertyId(property.propertyId)"
          :key="option.optionId"
          type="button"
          class="inline-flex items-center gap-1 px-2 py-0.5 text-[12px] rounded-full border transition-colors"
          :class="[
            isOptionSelected(option.optionId)
              ? 'border-current'
              : 'border-gray-200 text-gray-500 hover:border-gray-300'
          ]"
          :style="isOptionSelected(option.optionId) ? getOptionStyle(option) : {}"
          :disabled="disabled"
          @click="toggleMultiSelectOption(option.optionId)"
        >
          <!-- 체크 아이콘 -->
          <svg
            v-if="isOptionSelected(option.optionId)"
            class="w-3 h-3"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          {{ option.optionName }}
        </button>

        <!-- 옵션이 없는 경우 -->
        <span v-if="options.length === 0" class="text-[12px] text-gray-400">
          옵션 없음
        </span>
      </div>
    </template>

    <!-- CHECKBOX 타입 -->
    <template v-else-if="property.propertyType === 'CHECKBOX'">
      <label class="flex items-center gap-2 cursor-pointer">
        <input
          type="checkbox"
          :checked="Boolean(internalValue)"
          :disabled="disabled"
          class="w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500 focus:ring-2 cursor-pointer disabled:cursor-not-allowed"
          @change="handleCheckboxChange"
        />
        <span class="text-[13px] text-gray-700">{{ property.propertyName }}</span>
      </label>
    </template>

    <!-- USER 타입 -->
    <template v-else-if="property.propertyType === 'USER'">
      <Select
        :model-value="internalValue as number"
        :options="userOptions"
        :disabled="disabled"
        :size="compact ? 'sm' : 'md'"
        :placeholder="property.propertyName"
        searchable
        clearable
        @update:model-value="handleSelectChange"
      />
    </template>

    <!-- 알 수 없는 타입 -->
    <template v-else>
      <div class="text-[12px] text-gray-400">
        지원하지 않는 속성 타입: {{ property.propertyType }}
      </div>
    </template>
  </div>
</template>

<style scoped>
.property-editor.compact {
  @apply text-[12px];
}
</style>
