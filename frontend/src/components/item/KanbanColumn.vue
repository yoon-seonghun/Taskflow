<script setup lang="ts">
/**
 * 칸반 컬럼 컴포넌트
 * - 아이템 카드 목록 표시
 * - 드래그 앤 드롭 대상 영역
 * - 컬럼 내 순서 변경 지원
 * - 새 아이템 추가 버튼
 */
import { ref, computed } from 'vue'
import ItemCard from './ItemCard.vue'
import type { Item } from '@/types/item'

interface Props {
  columnId: string | number
  title: string
  items: Item[]
  color?: string
  count?: number
  collapsible?: boolean
  collapsed?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  collapsible: false,
  collapsed: false
})

const emit = defineEmits<{
  (e: 'itemClick', item: Item): void
  (e: 'drop', columnId: string | number, item: Item, targetIndex?: number): void
  (e: 'reorder', columnId: string | number, itemId: number, newIndex: number): void
  (e: 'addItem', columnId: string | number): void
  (e: 'toggleCollapse', columnId: string | number): void
}>()

// 드래그 오버 상태
const isDragOver = ref(false)
const isCollapsed = ref(props.collapsed)
const dragOverIndex = ref<number | null>(null)

// 컬럼 헤더 색상 스타일
const headerStyle = computed(() => {
  if (!props.color) return {}
  return {
    borderTopColor: props.color,
    backgroundColor: `${props.color}08`
  }
})

// 아이템 수
const itemCount = computed(() => props.count ?? props.items.length)

// 드래그 오버
function handleDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  isDragOver.value = true
}

// 드래그 리브
function handleDragLeave(event: DragEvent) {
  // 자식 요소로 이동할 때는 무시
  const relatedTarget = event.relatedTarget as HTMLElement
  const currentTarget = event.currentTarget as HTMLElement
  if (currentTarget.contains(relatedTarget)) return
  isDragOver.value = false
  dragOverIndex.value = null
}

// 카드 위에 드래그 오버 (컬럼 내 순서 변경용)
function handleCardDragOver(index: number, event: DragEvent) {
  event.preventDefault()
  event.stopPropagation()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  dragOverIndex.value = index
  isDragOver.value = true
}

// 카드 드래그 리브
function handleCardDragLeave() {
  dragOverIndex.value = null
}

// 카드 위에 드롭 (컬럼 내 순서 변경)
function handleCardDrop(targetIndex: number, event: DragEvent) {
  event.preventDefault()
  event.stopPropagation()
  isDragOver.value = false
  dragOverIndex.value = null

  const itemId = event.dataTransfer?.getData('text/plain')
  if (!itemId) return

  const draggedItemId = Number(itemId)
  const draggedItem = props.items.find(i => i.itemId === draggedItemId)

  if (draggedItem) {
    // 같은 컬럼 내에서의 드롭 - 순서 변경
    const currentIndex = props.items.findIndex(i => i.itemId === draggedItemId)
    if (currentIndex !== targetIndex) {
      emit('reorder', props.columnId, draggedItemId, targetIndex)
    }
  } else {
    // 다른 컬럼에서 드롭된 경우 - 컬럼 변경 + 위치 지정
    emit('drop', props.columnId, { itemId: draggedItemId } as Item, targetIndex)
  }
}

// 컬럼 영역에 드롭 (맨 아래로 추가)
function handleDrop(event: DragEvent) {
  event.preventDefault()
  isDragOver.value = false
  dragOverIndex.value = null

  const itemId = event.dataTransfer?.getData('text/plain')
  if (itemId) {
    const draggedItemId = Number(itemId)
    const item = props.items.find(i => i.itemId === draggedItemId)
    if (item) {
      // 같은 컬럼 - 맨 아래로 이동
      const currentIndex = props.items.findIndex(i => i.itemId === draggedItemId)
      if (currentIndex !== props.items.length - 1) {
        emit('reorder', props.columnId, draggedItemId, props.items.length - 1)
      }
    } else {
      // 다른 컬럼에서 드롭된 경우
      emit('drop', props.columnId, { itemId: draggedItemId } as Item)
    }
  }
}

// 카드 클릭
function handleItemClick(item: Item) {
  emit('itemClick', item)
}

// 새 아이템 추가
function handleAddItem() {
  emit('addItem', props.columnId)
}

// 컬럼 접기/펼치기
function toggleCollapse() {
  if (props.collapsible) {
    isCollapsed.value = !isCollapsed.value
    emit('toggleCollapse', props.columnId)
  }
}
</script>

<template>
  <div
    class="flex flex-col bg-gray-100 rounded-lg min-w-[280px] max-w-[320px] h-full"
    :class="{ 'w-12 min-w-[48px]': isCollapsed }"
  >
    <!-- 컬럼 헤더 -->
    <div
      class="flex-shrink-0 px-3 py-2 border-t-4 rounded-t-lg"
      :style="headerStyle"
      :class="[
        isCollapsed ? 'cursor-pointer' : '',
        color ? '' : 'border-t-gray-400'
      ]"
      @click="isCollapsed ? toggleCollapse() : null"
    >
      <template v-if="isCollapsed">
        <!-- 접힌 상태 -->
        <div class="flex flex-col items-center py-4">
          <span
            class="text-[13px] font-semibold text-gray-700 writing-mode-vertical"
            style="writing-mode: vertical-rl; text-orientation: mixed;"
          >
            {{ title }}
          </span>
          <span class="mt-2 text-[12px] text-gray-500 bg-white px-1.5 py-0.5 rounded-full">
            {{ itemCount }}
          </span>
        </div>
      </template>
      <template v-else>
        <!-- 펼친 상태 -->
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <!-- 접기 버튼 -->
            <button
              v-if="collapsible"
              class="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-200 rounded transition-colors"
              @click.stop="toggleCollapse"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
              </svg>
            </button>

            <!-- 컬럼 색상 표시 -->
            <span
              v-if="color"
              class="w-3 h-3 rounded-full flex-shrink-0"
              :style="{ backgroundColor: color }"
            />

            <!-- 타이틀 -->
            <h3 class="text-[13px] font-semibold text-gray-700">{{ title }}</h3>

            <!-- 카운트 -->
            <span class="text-[12px] text-gray-500 bg-white px-1.5 py-0.5 rounded-full">
              {{ itemCount }}
            </span>
          </div>

          <!-- 메뉴 버튼 -->
          <button class="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-200 rounded transition-colors">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
            </svg>
          </button>
        </div>
      </template>
    </div>

    <!-- 컬럼 바디 (접힌 상태가 아닐 때만) -->
    <div
      v-if="!isCollapsed"
      class="flex-1 overflow-y-auto p-2 space-y-2"
      :class="{ 'bg-primary-50 ring-2 ring-primary-300 ring-inset rounded-b-lg': isDragOver && dragOverIndex === null }"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
    >
      <!-- 아이템 카드들 -->
      <div
        v-for="(item, index) in items"
        :key="item.itemId"
        class="relative"
        :class="{ 'border-t-2 border-t-primary-500': dragOverIndex === index }"
        @dragover="handleCardDragOver(index, $event)"
        @dragleave="handleCardDragLeave"
        @drop="handleCardDrop(index, $event)"
      >
        <ItemCard
          :item="item"
          @click="handleItemClick"
        />
      </div>

      <!-- 빈 상태 -->
      <div
        v-if="items.length === 0"
        class="flex items-center justify-center h-20 text-[13px] text-gray-400 border-2 border-dashed border-gray-300 rounded-lg"
      >
        카드를 여기에 놓으세요
      </div>

      <!-- 새 아이템 추가 버튼 -->
      <button
        class="w-full py-2 text-[13px] text-gray-500 hover:text-gray-700 hover:bg-gray-200 rounded-lg transition-colors flex items-center justify-center gap-1"
        @click="handleAddItem"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        새 업무 추가
      </button>
    </div>
  </div>
</template>
