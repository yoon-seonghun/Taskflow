<script setup lang="ts">
/**
 * 뱃지/태그 컴포넌트
 * Compact UI: font 12px
 */

export type BadgeVariant = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info'
export type BadgeSize = 'sm' | 'md'

interface Props {
  variant?: BadgeVariant
  size?: BadgeSize
  color?: string
  removable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  size: 'md',
  removable: false
})

const emit = defineEmits<{
  (e: 'remove'): void
}>()

const variantClasses: Record<BadgeVariant, string> = {
  default: 'bg-gray-100 text-gray-700',
  primary: 'bg-primary-100 text-primary-700',
  success: 'bg-green-100 text-green-700',
  warning: 'bg-yellow-100 text-yellow-700',
  danger: 'bg-red-100 text-red-700',
  info: 'bg-blue-100 text-blue-700'
}

const sizeClasses: Record<BadgeSize, string> = {
  sm: 'px-1.5 py-0.5 text-[11px]',
  md: 'px-2 py-0.5 text-[12px]'
}

function getCustomStyle() {
  if (!props.color) return {}
  return {
    backgroundColor: `${props.color}20`,
    color: props.color
  }
}
</script>

<template>
  <span
    class="inline-flex items-center gap-1 rounded font-medium"
    :class="[
      color ? '' : variantClasses[variant],
      sizeClasses[size]
    ]"
    :style="getCustomStyle()"
  >
    <!-- Color Dot -->
    <span
      v-if="color"
      class="w-2 h-2 rounded-full flex-shrink-0"
      :style="{ backgroundColor: color }"
    />

    <!-- Content -->
    <slot />

    <!-- Remove Button -->
    <button
      v-if="removable"
      type="button"
      class="flex-shrink-0 hover:bg-black/10 rounded-full p-0.5 -mr-0.5"
      @click.stop="emit('remove')"
    >
      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
      </svg>
    </button>
  </span>
</template>
