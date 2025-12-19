<script setup lang="ts">
/**
 * 로딩 스피너 컴포넌트
 */
export type SpinnerSize = 'sm' | 'md' | 'lg' | 'xl'

interface Props {
  size?: SpinnerSize
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  color: 'primary'
})

const sizeClasses: Record<SpinnerSize, string> = {
  sm: 'w-4 h-4 border-2',
  md: 'w-6 h-6 border-2',
  lg: 'w-8 h-8 border-2',
  xl: 'w-12 h-12 border-3'
}

const colorClasses: Record<string, string> = {
  primary: 'border-primary-600',
  white: 'border-white',
  gray: 'border-gray-600'
}
</script>

<template>
  <div
    class="animate-spin rounded-full border-gray-200"
    :class="[sizeClasses[size], colorClasses[color] || colorClasses.primary]"
    style="border-top-color: currentColor"
  />
</template>
