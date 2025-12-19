<script setup lang="ts">
/**
 * 부서 트리 노드 컴포넌트
 * - 확장/축소 토글
 * - 컨텍스트 메뉴
 * - 드래그 앤 드롭 지원
 * - 재귀적 렌더링
 */
import { ref, computed, onUnmounted } from 'vue'
import type { Department } from '@/types/department'
import { useDepartmentStore } from '@/stores/department'

interface Props {
  department: Department
  depth?: number
}

const props = withDefaults(defineProps<Props>(), {
  depth: 0
})

const emit = defineEmits<{
  (e: 'select', department: Department): void
  (e: 'edit', department: Department): void
  (e: 'delete', department: Department): void
  (e: 'add-child', department: Department): void
  (e: 'drag-start', event: DragEvent, department: Department): void
  (e: 'drag-over', event: DragEvent, department: Department): void
  (e: 'drop', event: DragEvent, department: Department): void
}>()

const departmentStore = useDepartmentStore()

// 컨텍스트 메뉴
const showContextMenu = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })

// 하위 부서 존재 여부
const hasChildren = computed(() => {
  return props.department.children && props.department.children.length > 0
})

// 확장 상태
const isExpanded = computed(() => {
  return departmentStore.isExpanded(props.department.departmentId)
})

// 선택 상태
const isSelected = computed(() => {
  return departmentStore.selectedDepartmentId === props.department.departmentId
})

// 비활성 상태
const isInactive = computed(() => {
  return props.department.useYn !== 'Y'
})

// 들여쓰기 스타일
const indentStyle = computed(() => ({
  paddingLeft: `${props.depth * 24 + 8}px`
}))

// 토글
function handleToggle(event: Event) {
  event.stopPropagation()
  departmentStore.toggleNode(props.department.departmentId)
}

// 선택
function handleSelect() {
  emit('select', props.department)
}

// 컨텍스트 메뉴 닫기 핸들러
let closeMenuHandler: (() => void) | null = null

function closeContextMenu() {
  showContextMenu.value = false
  if (closeMenuHandler) {
    document.removeEventListener('click', closeMenuHandler)
    closeMenuHandler = null
  }
}

// 컨텍스트 메뉴 열기
function handleContextMenu(event: MouseEvent) {
  event.preventDefault()
  event.stopPropagation()
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  showContextMenu.value = true

  // 외부 클릭 시 닫기
  closeMenuHandler = closeContextMenu
  setTimeout(() => {
    if (closeMenuHandler) {
      document.addEventListener('click', closeMenuHandler)
    }
  }, 0)
}

// 컴포넌트 언마운트 시 정리
onUnmounted(() => {
  closeContextMenu()
})

// 수정
function handleEdit() {
  showContextMenu.value = false
  emit('edit', props.department)
}

// 삭제
function handleDelete() {
  showContextMenu.value = false
  emit('delete', props.department)
}

// 하위 부서 추가
function handleAddChild() {
  showContextMenu.value = false
  emit('add-child', props.department)
}

// 드래그 시작
function handleDragStart(event: DragEvent) {
  event.dataTransfer?.setData('departmentId', props.department.departmentId.toString())
  emit('drag-start', event, props.department)
}

// 드래그 오버
function handleDragOver(event: DragEvent) {
  event.preventDefault()
  emit('drag-over', event, props.department)
}

// 드롭
function handleDrop(event: DragEvent) {
  event.preventDefault()
  emit('drop', event, props.department)
}

// 하위 이벤트 전달
function propagateEvent(eventName: string, ...args: any[]) {
  emit(eventName as any, ...args)
}
</script>

<template>
  <div class="department-node">
    <!-- 노드 행 -->
    <div
      class="node-row"
      :class="{
        'selected': isSelected,
        'inactive': isInactive
      }"
      :style="indentStyle"
      draggable="true"
      @click="handleSelect"
      @contextmenu="handleContextMenu"
      @dragstart="handleDragStart"
      @dragover="handleDragOver"
      @drop="handleDrop"
    >
      <!-- 확장/축소 토글 -->
      <button
        v-if="hasChildren"
        type="button"
        class="toggle-btn"
        @click="handleToggle"
      >
        <svg
          class="w-4 h-4 transition-transform duration-150"
          :class="{ 'rotate-90': isExpanded }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </button>
      <div v-else class="toggle-placeholder" />

      <!-- 부서 아이콘 -->
      <div class="dept-icon" :class="{ 'inactive': isInactive }">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
        </svg>
      </div>

      <!-- 부서명 -->
      <span class="dept-name" :class="{ 'inactive': isInactive }">
        {{ department.departmentName }}
      </span>

      <!-- 부서 코드 -->
      <span class="dept-code">
        {{ department.departmentCode }}
      </span>

      <!-- 사용자 수 -->
      <span v-if="department.userCount !== undefined" class="user-count">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
        {{ department.userCount }}
      </span>

      <!-- 비활성 뱃지 -->
      <span v-if="isInactive" class="inactive-badge">비활성</span>

      <!-- 더보기 버튼 -->
      <button
        type="button"
        class="more-btn"
        @click.stop="handleContextMenu"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
        </svg>
      </button>
    </div>

    <!-- 하위 부서 (재귀) -->
    <div v-if="hasChildren && isExpanded" class="children">
      <DepartmentNode
        v-for="child in department.children"
        :key="child.departmentId"
        :department="child"
        :depth="depth + 1"
        @select="propagateEvent('select', $event)"
        @edit="propagateEvent('edit', $event)"
        @delete="propagateEvent('delete', $event)"
        @add-child="propagateEvent('add-child', $event)"
        @drag-start="propagateEvent('drag-start', $event, child)"
        @drag-over="propagateEvent('drag-over', $event, child)"
        @drop="propagateEvent('drop', $event, child)"
      />
    </div>

    <!-- 컨텍스트 메뉴 -->
    <Teleport to="body">
      <div
        v-if="showContextMenu"
        class="context-menu"
        :style="{
          left: contextMenuPosition.x + 'px',
          top: contextMenuPosition.y + 'px'
        }"
      >
        <button type="button" class="menu-item" @click="handleEdit">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
          수정
        </button>
        <button type="button" class="menu-item" @click="handleAddChild">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          하위 부서 추가
        </button>
        <div class="menu-divider" />
        <button type="button" class="menu-item danger" @click="handleDelete">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          삭제
        </button>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.department-node {
  @apply select-none;
}

.node-row {
  @apply flex items-center gap-2 py-2 pr-2 rounded-md cursor-pointer
         hover:bg-gray-50 transition-colors duration-150;
}

.node-row.selected {
  @apply bg-primary-50 hover:bg-primary-100;
}

.node-row.inactive {
  @apply opacity-60;
}

.toggle-btn {
  @apply p-0.5 rounded hover:bg-gray-200 text-gray-500 flex-shrink-0;
}

.toggle-placeholder {
  @apply w-5 h-5 flex-shrink-0;
}

.dept-icon {
  @apply w-6 h-6 rounded flex items-center justify-center bg-primary-100 text-primary-600 flex-shrink-0;
}

.dept-icon.inactive {
  @apply bg-gray-100 text-gray-400;
}

.dept-name {
  @apply text-sm font-medium text-gray-900 truncate;
}

.dept-name.inactive {
  @apply text-gray-500;
}

.dept-code {
  @apply text-xs text-gray-400 ml-1;
}

.user-count {
  @apply flex items-center gap-1 text-xs text-gray-400 ml-auto;
}

.inactive-badge {
  @apply px-1.5 py-0.5 text-xs font-medium bg-gray-100 text-gray-500 rounded;
}

.more-btn {
  @apply p-1 rounded hover:bg-gray-200 text-gray-400 hover:text-gray-600 opacity-0 flex-shrink-0
         transition-opacity duration-150;
}

.node-row:hover .more-btn {
  @apply opacity-100;
}

.children {
  @apply border-l border-gray-200 ml-3;
}

/* 컨텍스트 메뉴 */
.context-menu {
  @apply fixed z-50 bg-white rounded-lg shadow-lg border border-gray-200 py-1 min-w-[160px];
}

.menu-item {
  @apply w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700
         hover:bg-gray-50 transition-colors;
}

.menu-item.danger {
  @apply text-red-600 hover:bg-red-50;
}

.menu-divider {
  @apply border-t border-gray-100 my-1;
}
</style>
