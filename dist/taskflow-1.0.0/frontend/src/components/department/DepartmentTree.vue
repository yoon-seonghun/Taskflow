<script setup lang="ts">
/**
 * 부서 트리 컴포넌트
 * - 전체 트리 표시
 * - 드래그 앤 드롭 핸들링
 * - 확장/축소 컨트롤
 */
import { ref, computed } from 'vue'
import DepartmentNode from './DepartmentNode.vue'
import type { Department } from '@/types/department'
import { useDepartmentStore } from '@/stores/department'
import { useUiStore } from '@/stores/ui'

interface Props {
  showInactive?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showInactive: true
})

const emit = defineEmits<{
  (e: 'select', department: Department): void
  (e: 'edit', department: Department): void
  (e: 'delete', department: Department): void
  (e: 'add-child', department: Department): void
  (e: 'add-root'): void
}>()

const departmentStore = useDepartmentStore()
const uiStore = useUiStore()

// 드래그 상태
const draggedDepartment = ref<Department | null>(null)
const dropTargetDepartment = ref<Department | null>(null)

// 표시할 부서 목록
const displayDepartments = computed(() => {
  if (props.showInactive) {
    return departmentStore.departments
  }
  return departmentStore.activeDepartments
})

// 빈 상태 여부
const isEmpty = computed(() => displayDepartments.value.length === 0)

// 로딩 상태
const isLoading = computed(() => departmentStore.loading)

// 전체 확장
function handleExpandAll() {
  departmentStore.expandAllNodes()
}

// 전체 축소
function handleCollapseAll() {
  departmentStore.collapseAllNodes()
}

// 부서 선택
function handleSelect(department: Department) {
  departmentStore.selectDepartment(department)
  emit('select', department)
}

// 수정
function handleEdit(department: Department) {
  emit('edit', department)
}

// 삭제
function handleDelete(department: Department) {
  emit('delete', department)
}

// 하위 부서 추가
function handleAddChild(department: Department) {
  emit('add-child', department)
}

// 최상위 부서 추가
function handleAddRoot() {
  emit('add-root')
}

// 드래그 시작
function handleDragStart(event: DragEvent, department: Department) {
  draggedDepartment.value = department
}

// 드래그 오버
function handleDragOver(event: DragEvent, department: Department) {
  if (!draggedDepartment.value) return
  if (draggedDepartment.value.departmentId === department.departmentId) return

  // 자기 자신이나 자신의 하위로는 이동 불가
  if (isDescendant(draggedDepartment.value, department.departmentId)) return

  dropTargetDepartment.value = department
}

// 드롭
async function handleDrop(event: DragEvent, targetDepartment: Department) {
  if (!draggedDepartment.value) return
  if (draggedDepartment.value.departmentId === targetDepartment.departmentId) return

  // 자기 자신의 하위로는 이동 불가
  if (isDescendant(draggedDepartment.value, targetDepartment.departmentId)) {
    uiStore.showWarning('상위 부서를 하위 부서로 이동할 수 없습니다.')
    resetDragState()
    return
  }

  try {
    // 부서 이동 (상위 부서 변경)
    const success = await departmentStore.updateDepartment(draggedDepartment.value.departmentId, {
      parentId: targetDepartment.departmentId
    })

    if (success) {
      uiStore.showSuccess('부서가 이동되었습니다.')
      // 트리 새로고침
      await departmentStore.fetchDepartments()
    }
  } catch (error) {
    console.error('Failed to move department:', error)
    uiStore.showError('부서 이동에 실패했습니다.')
  } finally {
    resetDragState()
  }
}

// 드래그 상태 초기화
function resetDragState() {
  draggedDepartment.value = null
  dropTargetDepartment.value = null
}

// 자손 여부 확인 (재귀)
function isDescendant(parent: Department, targetId: number): boolean {
  if (!parent.children) return false
  for (const child of parent.children) {
    if (child.departmentId === targetId) return true
    if (isDescendant(child, targetId)) return true
  }
  return false
}
</script>

<template>
  <div class="department-tree">
    <!-- 헤더 -->
    <div class="tree-header">
      <h3 class="tree-title">부서 목록</h3>
      <div class="tree-actions">
        <button type="button" class="action-btn" title="전체 확장" @click="handleExpandAll">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
          </svg>
        </button>
        <button type="button" class="action-btn" title="전체 축소" @click="handleCollapseAll">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 9V4.5M9 9H4.5M9 9L3.75 3.75M9 15v4.5M9 15H4.5M9 15l-5.25 5.25M15 9h4.5M15 9V4.5M15 9l5.25-5.25M15 15h4.5M15 15v4.5m0-4.5l5.25 5.25" />
          </svg>
        </button>
        <button type="button" class="add-btn" @click="handleAddRoot">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          부서 추가
        </button>
      </div>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="isLoading" class="py-8 text-center">
      <svg class="animate-spin h-8 w-8 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">부서 목록을 불러오는 중...</p>
    </div>

    <!-- 빈 상태 -->
    <div v-else-if="isEmpty" class="py-8 text-center">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">등록된 부서가 없습니다.</p>
      <button type="button" class="mt-3 text-sm text-primary-600 hover:text-primary-700" @click="handleAddRoot">
        + 첫 번째 부서 추가하기
      </button>
    </div>

    <!-- 트리 노드 -->
    <div v-else class="tree-content" @dragend="resetDragState">
      <DepartmentNode
        v-for="department in displayDepartments"
        :key="department.departmentId"
        :department="department"
        :depth="0"
        @select="handleSelect"
        @edit="handleEdit"
        @delete="handleDelete"
        @add-child="handleAddChild"
        @drag-start="handleDragStart"
        @drag-over="handleDragOver"
        @drop="handleDrop"
      />
    </div>
  </div>
</template>

<style scoped>
.department-tree {
  @apply bg-white rounded-lg border border-gray-200;
}

.tree-header {
  @apply flex items-center justify-between px-4 py-3 border-b border-gray-200;
}

.tree-title {
  @apply text-sm font-medium text-gray-900;
}

.tree-actions {
  @apply flex items-center gap-2;
}

.action-btn {
  @apply p-1.5 rounded hover:bg-gray-100 text-gray-500 hover:text-gray-700 transition-colors;
}

.add-btn {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium
         text-primary-600 bg-primary-50 rounded-md
         hover:bg-primary-100 transition-colors;
}

.tree-content {
  @apply p-2 max-h-[600px] overflow-y-auto;
}
</style>
