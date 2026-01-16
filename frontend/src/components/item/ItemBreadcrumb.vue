<script setup lang="ts">
/**
 * 업무 Breadcrumb 컴포넌트 (v2.2)
 * - 하위 업무의 상위 계층 표시
 * - 클릭 시 해당 업무로 이동
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useItemStore } from '@/stores/item'
import type { Item, AncestorResponse, ItemStatus } from '@/types/item'

interface Props {
  item: Item
  boardId: number
  showCurrent?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showCurrent: true
})

const emit = defineEmits<{
  (e: 'navigate', item: AncestorResponse): void
}>()

const itemStore = useItemStore()

// State
const ancestors = ref<AncestorResponse[]>([])
const loading = ref(false)

// 하위 업무인 경우에만 Breadcrumb 표시
const isChildItem = computed(() => {
  return (props.item.itemDepth ?? 0) > 0 || !!props.item.parentItemId
})

// Breadcrumb 항목 (ancestors + 현재 아이템)
const breadcrumbItems = computed(() => {
  const items: Array<AncestorResponse & { isCurrent?: boolean }> = [...ancestors.value]

  if (props.showCurrent) {
    items.push({
      itemId: props.item.itemId,
      itemDepth: props.item.itemDepth ?? 0,
      title: props.item.title,
      status: props.item.status,
      isCurrent: true
    })
  }

  return items
})

// 상위 계층 로드
async function loadAncestors() {
  if (!isChildItem.value) return

  loading.value = true
  try {
    const result = await itemStore.fetchAncestors(props.boardId, props.item.itemId)
    // API가 현재 아이템도 포함하므로 제외 (showCurrent에서 별도 추가)
    ancestors.value = result.filter(a => a.itemId !== props.item.itemId)
  } finally {
    loading.value = false
  }
}

// 항목 클릭 핸들러
function handleClick(item: AncestorResponse & { isCurrent?: boolean }) {
  if (!item.isCurrent) {
    emit('navigate', item)
  }
}

// 상태 아이콘 색상
const statusColors: Record<ItemStatus, string> = {
  NOT_STARTED: 'text-gray-400',
  IN_PROGRESS: 'text-blue-500',
  PENDING: 'text-yellow-500',
  COMPLETED: 'text-green-500',
  DELETED: 'text-red-400'
}

// item 변경 시 ancestors 다시 로드
watch(() => props.item.itemId, () => {
  if (isChildItem.value) {
    loadAncestors()
  } else {
    ancestors.value = []
  }
})

// 초기 로드
onMounted(() => {
  if (isChildItem.value) {
    loadAncestors()
  }
})
</script>

<template>
  <nav v-if="isChildItem || breadcrumbItems.length > 1" class="item-breadcrumb flex items-center gap-1 text-[12px]">
    <!-- 홈 아이콘 -->
    <button
      class="flex items-center gap-1 px-1.5 py-0.5 text-gray-500 hover:text-primary-600 hover:bg-primary-50 rounded transition-colors"
      title="루트 업무 목록"
      @click="$emit('navigate', { itemId: 0, itemDepth: -1, title: '목록', status: 'NOT_STARTED' })"
    >
      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
      </svg>
    </button>

    <!-- 로딩 표시 -->
    <template v-if="loading">
      <span class="text-gray-400">로딩...</span>
    </template>

    <!-- Breadcrumb 항목들 -->
    <template v-else>
      <template v-for="(breadcrumb, index) in breadcrumbItems" :key="breadcrumb.itemId">
        <!-- 구분자 -->
        <svg class="w-3 h-3 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>

        <!-- Breadcrumb 항목 -->
        <button
          class="flex items-center gap-1 px-1.5 py-0.5 max-w-[150px] rounded transition-colors"
          :class="breadcrumb.isCurrent
            ? 'text-gray-900 font-medium cursor-default'
            : 'text-gray-600 hover:text-primary-600 hover:bg-primary-50'"
          :title="breadcrumb.title"
          @click="handleClick(breadcrumb)"
        >
          <!-- 상태 아이콘 -->
          <span
            class="flex-shrink-0 w-2 h-2 rounded-full"
            :class="{
              'bg-gray-300': breadcrumb.status === 'NOT_STARTED',
              'bg-blue-500': breadcrumb.status === 'IN_PROGRESS',
              'bg-yellow-500': breadcrumb.status === 'PENDING',
              'bg-green-500': breadcrumb.status === 'COMPLETED',
              'bg-red-400': breadcrumb.status === 'DELETED'
            }"
          />
          <!-- 제목 (truncate) -->
          <span class="truncate">{{ breadcrumb.title }}</span>
        </button>
      </template>
    </template>
  </nav>
</template>
