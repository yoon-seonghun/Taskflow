<script setup lang="ts">
/**
 * 작업 템플릿 목록 컴포넌트
 * - 템플릿 목록 표시
 * - 선택/삭제 기능
 */
import { computed } from 'vue'
import Badge from '@/components/common/Badge.vue'
import type { TaskTemplate, DefaultItemStatus } from '@/types/template'

interface Props {
  templates: TaskTemplate[]
  selectedId?: number | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selectedId: null,
  loading: false
})

const emit = defineEmits<{
  (e: 'select', template: TaskTemplate): void
  (e: 'delete', template: TaskTemplate): void
}>()

// 상태별 뱃지 설정
const statusConfig: Record<DefaultItemStatus, { label: string; variant: 'default' | 'info' | 'warning' }> = {
  NOT_STARTED: { label: '시작전', variant: 'default' },
  IN_PROGRESS: { label: '진행중', variant: 'info' },
  PENDING: { label: '보류', variant: 'warning' }
}

// 상태 뱃지 정보 가져오기
function getStatusBadge(status: DefaultItemStatus) {
  return statusConfig[status] || statusConfig.NOT_STARTED
}

// 템플릿 선택
function handleSelect(template: TaskTemplate) {
  if (props.loading) return
  emit('select', template)
}

// 템플릿 삭제
function handleDelete(event: Event, template: TaskTemplate) {
  event.stopPropagation()
  if (props.loading) return
  emit('delete', template)
}

// 선택된 템플릿인지 확인
function isSelected(template: TaskTemplate): boolean {
  return props.selectedId === template.templateId
}
</script>

<template>
  <div class="template-list">
    <!-- 로딩 상태 -->
    <div v-if="loading && templates.length === 0" class="py-8 text-center">
      <svg class="animate-spin h-6 w-6 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">템플릿을 불러오는 중...</p>
    </div>

    <!-- 빈 상태 -->
    <div v-else-if="templates.length === 0" class="py-8 text-center">
      <svg class="mx-auto h-10 w-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">등록된 작업 템플릿이 없습니다.</p>
      <p class="text-xs text-gray-400">위에서 새 템플릿을 등록해보세요.</p>
    </div>

    <!-- 템플릿 목록 -->
    <div v-else class="divide-y divide-gray-100">
      <div
        v-for="template in templates"
        :key="template.templateId"
        class="template-item"
        :class="{ 'selected': isSelected(template) }"
        @click="handleSelect(template)"
      >
        <div class="flex items-start gap-3">
          <!-- 메인 콘텐츠 -->
          <div class="flex-1 min-w-0">
            <!-- 작업 내용 -->
            <div class="text-sm font-medium text-gray-900 truncate">
              {{ template.content }}
            </div>

            <!-- 부가 정보 -->
            <div class="mt-1 flex flex-wrap items-center gap-2 text-xs text-gray-500">
              <!-- 기본 담당자 -->
              <span v-if="template.defaultAssigneeName" class="flex items-center gap-1">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                {{ template.defaultAssigneeName }}
              </span>

              <!-- 기본 상태 -->
              <Badge
                :variant="getStatusBadge(template.defaultItemStatus).variant"
                size="sm"
              >
                {{ getStatusBadge(template.defaultItemStatus).label }}
              </Badge>

              <!-- 사용 횟수 -->
              <span class="flex items-center gap-1">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
                사용 {{ template.useCount }}회
              </span>
            </div>
          </div>

          <!-- 삭제 버튼 -->
          <button
            type="button"
            class="delete-btn"
            title="삭제"
            :disabled="loading"
            @click="handleDelete($event, template)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.template-list {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden;
}

.template-item {
  @apply px-4 py-3 cursor-pointer transition-colors duration-150;
}

.template-item:hover {
  @apply bg-gray-50;
}

.template-item.selected {
  @apply bg-primary-50 border-l-2 border-primary-500;
}

.delete-btn {
  @apply p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded
         transition-colors duration-150 flex-shrink-0
         disabled:opacity-50 disabled:cursor-not-allowed;
}
</style>
