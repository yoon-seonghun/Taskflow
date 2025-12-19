<script setup lang="ts">
/**
 * 그룹 목록 컴포넌트 (카드 UI)
 * - 그룹 카드 표시
 * - 드래그 앤 드롭 순서 변경
 * - 그룹 선택/수정/삭제
 */
import { ref, computed } from 'vue'
import type { Group } from '@/types/group'
import { useGroupStore } from '@/stores/group'

interface Props {
  showInactive?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showInactive: true
})

const emit = defineEmits<{
  (e: 'select', group: Group): void
  (e: 'edit', group: Group): void
  (e: 'delete', group: Group): void
  (e: 'add'): void
  (e: 'toggle-active', group: Group): void
}>()

const groupStore = useGroupStore()

// 드래그 상태
const draggedGroup = ref<Group | null>(null)
const dragOverGroupId = ref<number | null>(null)

// 표시할 그룹 목록
const displayGroups = computed(() => {
  if (props.showInactive) {
    return groupStore.groups
  }
  return groupStore.activeGroups
})

// 로딩 상태
const isLoading = computed(() => groupStore.loading)

// 빈 상태 여부
const isEmpty = computed(() => displayGroups.value.length === 0)

// 선택된 그룹 ID
const selectedGroupId = computed(() => groupStore.selectedGroupId)

// 그룹 선택
function handleSelect(group: Group) {
  groupStore.selectGroup(group)
  emit('select', group)
}

// 수정
function handleEdit(event: Event, group: Group) {
  event.stopPropagation()
  emit('edit', group)
}

// 삭제
function handleDelete(event: Event, group: Group) {
  event.stopPropagation()
  emit('delete', group)
}

// 활성/비활성 토글
function handleToggleActive(event: Event, group: Group) {
  event.stopPropagation()
  emit('toggle-active', group)
}

// 추가
function handleAdd() {
  emit('add')
}

// 드래그 시작
function handleDragStart(event: DragEvent, group: Group) {
  draggedGroup.value = group
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('groupId', group.groupId.toString())
  }
}

// 드래그 오버
function handleDragOver(event: DragEvent, group: Group) {
  event.preventDefault()
  if (!draggedGroup.value) return
  if (draggedGroup.value.groupId === group.groupId) return
  dragOverGroupId.value = group.groupId
}

// 드래그 리브
function handleDragLeave() {
  dragOverGroupId.value = null
}

// 드롭
async function handleDrop(event: DragEvent, targetGroup: Group) {
  event.preventDefault()
  if (!draggedGroup.value) return
  if (draggedGroup.value.groupId === targetGroup.groupId) return

  // 순서 변경
  const success = await groupStore.updateGroupOrder(
    draggedGroup.value.groupId,
    targetGroup.sortOrder
  )

  if (success) {
    // 새로고침은 store에서 처리됨
  }

  resetDragState()
}

// 드래그 종료
function handleDragEnd() {
  resetDragState()
}

// 드래그 상태 초기화
function resetDragState() {
  draggedGroup.value = null
  dragOverGroupId.value = null
}

// 그룹 색상 스타일
function getColorStyle(color?: string) {
  if (!color) return {}
  return {
    borderLeftColor: color,
    backgroundColor: `${color}08`
  }
}

// 멤버 수 표시
function getMemberCountText(count?: number) {
  if (count === undefined || count === 0) return '멤버 없음'
  return `${count}명`
}
</script>

<template>
  <div class="group-list">
    <!-- 헤더 -->
    <div class="list-header">
      <h3 class="list-title">그룹 목록</h3>
      <button type="button" class="add-btn" @click="handleAdd">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        그룹 추가
      </button>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="isLoading" class="py-8 text-center">
      <svg class="animate-spin h-8 w-8 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">그룹 목록을 불러오는 중...</p>
    </div>

    <!-- 빈 상태 -->
    <div v-else-if="isEmpty" class="py-8 text-center">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">등록된 그룹이 없습니다.</p>
      <button type="button" class="mt-3 text-sm text-primary-600 hover:text-primary-700" @click="handleAdd">
        + 첫 번째 그룹 추가하기
      </button>
    </div>

    <!-- 그룹 카드 목록 -->
    <div v-else class="cards-container" @dragend="handleDragEnd">
      <div
        v-for="group in displayGroups"
        :key="group.groupId"
        class="group-card"
        :class="{
          'selected': selectedGroupId === group.groupId,
          'inactive': group.useYn !== 'Y',
          'drag-over': dragOverGroupId === group.groupId
        }"
        :style="getColorStyle(group.groupColor)"
        draggable="true"
        @click="handleSelect(group)"
        @dragstart="handleDragStart($event, group)"
        @dragover="handleDragOver($event, group)"
        @dragleave="handleDragLeave"
        @drop="handleDrop($event, group)"
      >
        <!-- 색상 바 -->
        <div class="color-bar" :style="{ backgroundColor: group.groupColor || '#9CA3AF' }" />

        <!-- 카드 내용 -->
        <div class="card-content">
          <div class="card-header">
            <div class="group-info">
              <h4 class="group-name" :class="{ 'inactive': group.useYn !== 'Y' }">
                {{ group.groupName }}
              </h4>
              <span class="group-code">{{ group.groupCode }}</span>
            </div>

            <!-- 액션 버튼 -->
            <div class="card-actions">
              <button type="button" class="action-btn" title="수정" @click="handleEdit($event, group)">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
              <button type="button" class="action-btn danger" title="삭제" @click="handleDelete($event, group)">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </div>

          <!-- 설명 -->
          <p v-if="group.groupDescription" class="group-description">
            {{ group.groupDescription }}
          </p>

          <!-- 멤버 수 -->
          <div class="card-footer">
            <div class="member-count">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
              {{ getMemberCountText(group.memberCount) }}
            </div>

            <!-- 활성/비활성 토글 버튼 -->
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'active': group.useYn === 'Y' }"
              :title="group.useYn === 'Y' ? '비활성화' : '활성화'"
              @click="handleToggleActive($event, group)"
            >
              <span class="toggle-track">
                <span class="toggle-thumb" />
              </span>
              <span class="toggle-label">{{ group.useYn === 'Y' ? '활성' : '비활성' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.group-list {
  @apply bg-white rounded-lg border border-gray-200;
}

.list-header {
  @apply flex items-center justify-between px-4 py-3 border-b border-gray-200;
}

.list-title {
  @apply text-sm font-medium text-gray-900;
}

.add-btn {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium
         text-primary-600 bg-primary-50 rounded-md
         hover:bg-primary-100 transition-colors;
}

.cards-container {
  @apply p-4 grid gap-4 max-h-[600px] overflow-y-auto;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
}

.group-card {
  @apply relative bg-white rounded-lg border border-gray-200 cursor-pointer
         hover:shadow-md transition-all duration-150 overflow-hidden;
  border-left-width: 4px;
  border-left-color: #9CA3AF;
}

.group-card.selected {
  @apply ring-2 ring-primary-500 border-gray-200;
}

.group-card.inactive {
  @apply opacity-60;
}

.group-card.drag-over {
  @apply ring-2 ring-primary-300 bg-primary-50;
}

.color-bar {
  @apply absolute left-0 top-0 bottom-0 w-1;
}

.card-content {
  @apply p-4 pl-5;
}

.card-header {
  @apply flex items-start justify-between gap-2;
}

.group-info {
  @apply flex-1 min-w-0;
}

.group-name {
  @apply text-sm font-medium text-gray-900 truncate;
}

.group-name.inactive {
  @apply text-gray-500;
}

.group-code {
  @apply text-xs text-gray-400;
}

.card-actions {
  @apply flex items-center gap-1 opacity-0 transition-opacity duration-150;
}

.group-card:hover .card-actions {
  @apply opacity-100;
}

.action-btn {
  @apply p-1 rounded hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors;
}

.action-btn.danger {
  @apply hover:bg-red-50 hover:text-red-600;
}

.group-description {
  @apply mt-2 text-xs text-gray-500 line-clamp-2;
}

.card-footer {
  @apply mt-3 flex items-center justify-between;
}

.member-count {
  @apply flex items-center gap-1 text-xs text-gray-500;
}

.inactive-badge {
  @apply px-1.5 py-0.5 text-xs font-medium bg-gray-100 text-gray-500 rounded;
}

.toggle-btn {
  @apply flex items-center gap-1.5 px-2 py-1 rounded-md text-xs
         hover:bg-gray-100 transition-colors;
}

.toggle-track {
  @apply relative w-7 h-4 bg-gray-300 rounded-full transition-colors;
}

.toggle-btn.active .toggle-track {
  @apply bg-primary-500;
}

.toggle-thumb {
  @apply absolute top-0.5 left-0.5 w-3 h-3 bg-white rounded-full shadow transition-transform;
}

.toggle-btn.active .toggle-thumb {
  @apply translate-x-3;
}

.toggle-label {
  @apply text-gray-500;
}

.toggle-btn.active .toggle-label {
  @apply text-primary-600;
}
</style>
