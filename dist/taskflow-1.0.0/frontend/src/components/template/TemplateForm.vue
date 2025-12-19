<script setup lang="ts">
/**
 * 작업 템플릿 등록/수정 폼 컴포넌트
 * - 작업 내용, 기본 담당자, 기본 상태 설정
 * - 등록/변경 버튼 토글
 */
import { ref, computed, watch, onMounted } from 'vue'
import Input from '@/components/common/Input.vue'
import Select from '@/components/common/Select.vue'
import type { SelectOption } from '@/components/common/Select.vue'
import type { TaskTemplate, TaskTemplateCreateRequest, TaskTemplateUpdateRequest, DefaultItemStatus } from '@/types/template'
import type { User } from '@/types/user'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

interface Props {
  template?: TaskTemplate | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  template: null,
  loading: false
})

const emit = defineEmits<{
  (e: 'submit', data: TaskTemplateCreateRequest | TaskTemplateUpdateRequest): void
  (e: 'cancel'): void
}>()

const authStore = useAuthStore()
const uiStore = useUiStore()

// Form state
const content = ref('')
const defaultAssigneeId = ref<number | null>(null)
const defaultItemStatus = ref<DefaultItemStatus>('NOT_STARTED')

// Users for assignee dropdown
const users = ref<User[]>([])
const loadingUsers = ref(false)

// 상태 옵션 (완료/삭제 제외)
const statusOptions: SelectOption[] = [
  { value: 'NOT_STARTED', label: '시작전', color: '#6B7280' },
  { value: 'IN_PROGRESS', label: '진행중', color: '#3B82F6' },
  { value: 'PENDING', label: '보류', color: '#F59E0B' }
]

// 담당자 옵션
const userOptions = computed<SelectOption[]>(() => {
  return users.value.map(user => ({
    value: user.userId,
    label: user.userName
  }))
})

// 편집 모드 여부
const isEditMode = computed(() => !!props.template)

// 버튼 텍스트
const submitButtonText = computed(() => isEditMode.value ? '변경' : '등록')

// 폼 유효성
const isValid = computed(() => {
  return content.value.trim().length > 0
})

// 사용자 목록 로드
async function loadUsers() {
  loadingUsers.value = true
  try {
    const response = await userApi.getUsers({ useYn: 'Y', size: 100 })
    users.value = response.data.content
  } catch (error) {
    console.error('Failed to load users:', error)
    uiStore.showError('담당자 목록을 불러오는데 실패했습니다.')
  } finally {
    loadingUsers.value = false
  }
}

// 폼 초기화
function resetForm() {
  content.value = ''
  // 기본 담당자는 현재 사용자로 설정
  defaultAssigneeId.value = authStore.currentUserId
  defaultItemStatus.value = 'NOT_STARTED'
}

// 템플릿 데이터로 폼 채우기
function fillForm(template: TaskTemplate) {
  content.value = template.content
  defaultAssigneeId.value = template.defaultAssigneeId ?? null
  defaultItemStatus.value = template.defaultItemStatus ?? 'NOT_STARTED'
}

// 제출
function handleSubmit() {
  if (!isValid.value || props.loading) return

  if (isEditMode.value) {
    const data: TaskTemplateUpdateRequest = {
      content: content.value.trim(),
      defaultAssigneeId: defaultAssigneeId.value ?? undefined,
      defaultItemStatus: defaultItemStatus.value
    }
    emit('submit', data)
  } else {
    const data: TaskTemplateCreateRequest = {
      content: content.value.trim(),
      defaultAssigneeId: defaultAssigneeId.value ?? undefined,
      defaultItemStatus: defaultItemStatus.value
    }
    emit('submit', data)
  }
}

// 취소
function handleCancel() {
  resetForm()
  emit('cancel')
}

// 템플릿 변경 감지
watch(() => props.template, (newTemplate) => {
  if (newTemplate) {
    fillForm(newTemplate)
  } else {
    resetForm()
  }
}, { immediate: true })

onMounted(() => {
  loadUsers()
  if (!props.template) {
    // 새 템플릿 등록 시 기본 담당자를 현재 사용자로 설정
    defaultAssigneeId.value = authStore.currentUserId
  }
})

// 외부에서 폼 초기화 가능하도록 expose
defineExpose({ resetForm })
</script>

<template>
  <div class="template-form">
    <div class="space-y-4">
      <!-- 작업 내용 -->
      <Input
        v-model="content"
        label="작업 내용"
        placeholder="자주 사용하는 작업 내용을 입력하세요"
        :required="true"
        :maxlength="500"
        :disabled="loading"
      />

      <!-- 기본 담당자 -->
      <Select
        v-model="defaultAssigneeId"
        :options="userOptions"
        label="기본 담당자"
        placeholder="담당자 선택"
        :searchable="true"
        :clearable="true"
        :disabled="loading || loadingUsers"
      />

      <!-- 기본 상태 -->
      <Select
        v-model="defaultItemStatus"
        :options="statusOptions"
        label="기본 상태"
        placeholder="상태 선택"
        :disabled="loading"
      />

      <!-- 버튼 영역 -->
      <div class="flex items-center gap-2 pt-2">
        <button
          type="button"
          class="btn-primary flex-1"
          :disabled="!isValid || loading"
          @click="handleSubmit"
        >
          <span v-if="loading" class="flex items-center justify-center gap-2">
            <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            처리중...
          </span>
          <span v-else>{{ submitButtonText }}</span>
        </button>

        <button
          v-if="isEditMode"
          type="button"
          class="btn-secondary"
          :disabled="loading"
          @click="handleCancel"
        >
          취소
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.template-form {
  @apply p-4 bg-white rounded-lg border border-gray-200;
}

.btn-primary {
  @apply px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed
         transition-colors duration-150;
}

.btn-secondary {
  @apply px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed
         transition-colors duration-150;
}
</style>
