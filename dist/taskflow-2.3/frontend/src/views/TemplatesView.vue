<script setup lang="ts">
/**
 * 작업 등록 메뉴 (Task Templates View)
 * - 자주 사용하는 작업 템플릿 관리
 * - 등록/수정/삭제 기능
 */
import { ref, onMounted } from 'vue'
import TemplateForm from '@/components/template/TemplateForm.vue'
import TemplateList from '@/components/template/TemplateList.vue'
import { templateApi } from '@/api/template'
import { useUiStore } from '@/stores/ui'
import type { TaskTemplate, TaskTemplateCreateRequest, TaskTemplateUpdateRequest, TemplateOwnerType } from '@/types/template'

const uiStore = useUiStore()

// 템플릿 목록
const templates = ref<TaskTemplate[]>([])
const loading = ref(false)
const submitting = ref(false)

// 선택된 템플릿 (편집 모드)
const selectedTemplate = ref<TaskTemplate | null>(null)

// 폼 컴포넌트 ref
const formRef = ref<InstanceType<typeof TemplateForm> | null>(null)

// 템플릿 목록 로드 (접근 가능한 전체 템플릿)
async function loadTemplates() {
  loading.value = true
  try {
    const response = await templateApi.getAccessibleTemplates()
    templates.value = response.data
  } catch (error) {
    console.error('Failed to load templates:', error)
  } finally {
    loading.value = false
  }
}

// 템플릿 등록/수정
async function handleSubmit(data: TaskTemplateCreateRequest | TaskTemplateUpdateRequest, ownerType: TemplateOwnerType) {
  submitting.value = true
  try {
    if (selectedTemplate.value) {
      // 수정
      await templateApi.updateTemplate(selectedTemplate.value.templateId, data as TaskTemplateUpdateRequest)
      uiStore.showSuccess('템플릿이 수정되었습니다.')
      selectedTemplate.value = null
    } else {
      // 소유 유형에 따라 적절한 API 호출
      const createRequest = data as TaskTemplateCreateRequest
      switch (ownerType) {
        case 'GLOBAL':
          await templateApi.createGlobalTemplate(createRequest)
          uiStore.showSuccess('글로벌 템플릿이 등록되었습니다.')
          break
        case 'MANAGER':
          await templateApi.createManagerTemplate(createRequest)
          uiStore.showSuccess('매니저 템플릿이 등록되었습니다.')
          break
        default:
          await templateApi.createUserTemplate(createRequest)
          uiStore.showSuccess('개인 템플릿이 등록되었습니다.')
      }
    }
    await loadTemplates()
    formRef.value?.resetForm()
  } catch (error: any) {
    console.error('Failed to save template:', error)
    // 권한 오류 처리
    if (error.response?.status === 403) {
      uiStore.showError('해당 유형의 템플릿을 등록할 권한이 없습니다.')
    } else {
      uiStore.showError('템플릿 저장에 실패했습니다.')
    }
  } finally {
    submitting.value = false
  }
}

// 템플릿 선택 (편집 모드)
function handleSelect(template: TaskTemplate) {
  if (selectedTemplate.value?.templateId === template.templateId) {
    // 이미 선택된 템플릿 클릭 시 선택 해제
    selectedTemplate.value = null
  } else {
    selectedTemplate.value = template
  }
}

// 편집 취소
function handleCancel() {
  selectedTemplate.value = null
}

// 삭제 확인 다이얼로그 표시 및 삭제 처리
async function handleDeleteClick(template: TaskTemplate) {
  const confirmed = await uiStore.confirm({
    title: '템플릿 삭제',
    message: `'${template.content}' 템플릿을 삭제하시겠습니까?`,
    confirmText: '삭제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  submitting.value = true
  try {
    await templateApi.deleteTemplate(template.templateId)

    // 삭제된 템플릿이 현재 선택된 템플릿이면 선택 해제
    if (selectedTemplate.value?.templateId === template.templateId) {
      selectedTemplate.value = null
    }

    await loadTemplates()
    uiStore.showSuccess('템플릿이 삭제되었습니다.')
  } catch (error) {
    console.error('Failed to delete template:', error)
    uiStore.showError('템플릿 삭제에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<template>
  <div class="templates-view">
    <!-- 헤더 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900 dark:text-gray-100">작업 등록 메뉴</h1>
      <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
        자주 사용하는 작업을 템플릿으로 등록하여 업무 등록 시 빠르게 입력할 수 있습니다.
      </p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 왼쪽: 등록/수정 폼 -->
      <div class="lg:col-span-1">
        <div class="sticky top-4">
          <h2 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
            {{ selectedTemplate ? '템플릿 수정' : '새 템플릿 등록' }}
          </h2>
          <TemplateForm
            ref="formRef"
            :template="selectedTemplate"
            :loading="submitting"
            @submit="handleSubmit"
            @cancel="handleCancel"
          />
        </div>
      </div>

      <!-- 오른쪽: 템플릿 목록 -->
      <div class="lg:col-span-2">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-sm font-medium text-gray-700 dark:text-gray-300">
            등록된 템플릿 ({{ templates.length }}건)
          </h2>
          <button
            v-if="selectedTemplate"
            type="button"
            class="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300"
            @click="handleCancel"
          >
            선택 해제
          </button>
        </div>
        <TemplateList
          :templates="templates"
          :selected-id="selectedTemplate?.templateId"
          :loading="loading || submitting"
          @select="handleSelect"
          @delete="handleDeleteClick"
        />
      </div>
    </div>

  </div>
</template>

<style scoped>
.templates-view {
  @apply max-w-6xl mx-auto;
}
</style>
