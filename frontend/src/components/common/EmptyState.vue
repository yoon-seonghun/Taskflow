<script setup lang="ts">
/**
 * 빈 상태 표시 컴포넌트
 */
interface Props {
  title?: string
  description?: string
  icon?: 'inbox' | 'search' | 'document' | 'folder' | 'users'
}

const props = withDefaults(defineProps<Props>(), {
  title: '데이터가 없습니다',
  description: '',
  icon: 'inbox'
})

const iconPaths: Record<string, string> = {
  inbox: 'M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4',
  search: 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z',
  document: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
  folder: 'M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z',
  users: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z'
}
</script>

<template>
  <div class="flex flex-col items-center justify-center py-12 px-4 text-center">
    <!-- Icon -->
    <div class="w-16 h-16 mb-4 text-gray-300">
      <svg class="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="1.5"
          :d="iconPaths[icon]"
        />
      </svg>
    </div>

    <!-- Title -->
    <h3 class="text-[14px] font-medium text-gray-600 mb-1">
      {{ title }}
    </h3>

    <!-- Description -->
    <p v-if="description" class="text-[13px] text-gray-400 max-w-sm">
      {{ description }}
    </p>

    <!-- Action Slot -->
    <div v-if="$slots.action" class="mt-4">
      <slot name="action" />
    </div>
  </div>
</template>
