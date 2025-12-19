<script setup lang="ts">
/**
 * 공통 버튼 컴포넌트
 * Compact UI: height 32px, font 13px
 */
import { computed } from 'vue'

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'
export type ButtonSize = 'sm' | 'md' | 'lg'

interface Props {
  variant?: ButtonVariant
  size?: ButtonSize
  disabled?: boolean
  loading?: boolean
  block?: boolean
  type?: 'button' | 'submit' | 'reset'
  iconOnly?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
  loading: false,
  block: false,
  type: 'button',
  iconOnly: false
})

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
}>()

const buttonClasses = computed(() => {
  const base = [
    'inline-flex items-center justify-center gap-1.5',
    'font-medium rounded transition-all duration-150',
    'focus:outline-none focus:ring-2 focus:ring-offset-1',
    'disabled:opacity-50 disabled:cursor-not-allowed'
  ]

  // Variant styles
  const variants: Record<ButtonVariant, string> = {
    primary: 'bg-primary-600 text-white hover:bg-primary-700 active:bg-primary-800 focus:ring-primary-500',
    secondary: 'bg-gray-100 text-gray-700 hover:bg-gray-200 active:bg-gray-300 focus:ring-gray-400 border border-gray-300',
    danger: 'bg-red-600 text-white hover:bg-red-700 active:bg-red-800 focus:ring-red-500',
    ghost: 'bg-transparent text-gray-600 hover:bg-gray-100 active:bg-gray-200 focus:ring-gray-400'
  }

  // Size styles
  const sizes: Record<ButtonSize, string> = {
    sm: props.iconOnly ? 'p-1 text-xs' : 'px-2 py-1 text-xs h-7',
    md: props.iconOnly ? 'p-1.5 text-sm' : 'px-3 py-1.5 text-[13px] h-8',
    lg: props.iconOnly ? 'p-2 text-base' : 'px-4 py-2 text-sm h-9'
  }

  return [
    ...base,
    variants[props.variant],
    sizes[props.size],
    props.block ? 'w-full' : '',
    props.loading ? 'cursor-wait' : ''
  ].filter(Boolean).join(' ')
})

function handleClick(event: MouseEvent) {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}
</script>

<template>
  <button
    :type="type"
    :class="buttonClasses"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <!-- Loading Spinner -->
    <svg
      v-if="loading"
      class="animate-spin -ml-0.5 h-4 w-4"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle
        class="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        stroke-width="4"
      />
      <path
        class="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>

    <!-- Content -->
    <slot />
  </button>
</template>
