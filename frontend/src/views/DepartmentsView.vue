<script setup lang="ts">
/**
 * 부서 관리 메뉴 (Departments View)
 * - 부서 트리 표시
 * - 부서 CRUD
 * - 부서별 사용자 목록
 */
import { ref, computed, onMounted } from 'vue'
import DepartmentTree from '@/components/department/DepartmentTree.vue'
import DepartmentForm from '@/components/department/DepartmentForm.vue'
import Badge from '@/components/common/Badge.vue'
import type { Department, DepartmentCreateRequest, DepartmentUpdateRequest } from '@/types/department'
import { useDepartmentStore } from '@/stores/department'
import { useUiStore } from '@/stores/ui'

const departmentStore = useDepartmentStore()
const uiStore = useUiStore()

// 폼 상태
type FormMode = 'none' | 'create' | 'edit' | 'create-child'
const formMode = ref<FormMode>('none')
const formLoading = ref(false)

// 편집 대상 부서
const editingDepartment = ref<Department | null>(null)

// 하위 부서 추가 시 상위 부서
const parentDepartment = ref<Department | null>(null)

// 비활성 부서 표시 여부
const showInactive = ref(true)

// 선택된 부서
const selectedDepartment = computed(() => departmentStore.selectedDepartment)

// 선택된 부서의 사용자 목록
const departmentUsers = computed(() => departmentStore.departmentUsers)

// Form ref
const formRef = ref<InstanceType<typeof DepartmentForm> | null>(null)

// 초기 로드
async function loadData() {
  await Promise.all([
    departmentStore.fetchDepartments(),
    departmentStore.fetchFlatDepartments()
  ])
}

// 부서 선택
function handleSelect(department: Department) {
  // 폼 닫기
  formMode.value = 'none'
  editingDepartment.value = null
  parentDepartment.value = null
}

// 부서 수정 시작
function handleEdit(department: Department) {
  formMode.value = 'edit'
  editingDepartment.value = department
  parentDepartment.value = null
}

// 하위 부서 추가 시작
function handleAddChild(department: Department) {
  formMode.value = 'create-child'
  editingDepartment.value = null
  parentDepartment.value = department
}

// 최상위 부서 추가 시작
function handleAddRoot() {
  formMode.value = 'create'
  editingDepartment.value = null
  parentDepartment.value = null
}

// 부서 삭제
async function handleDelete(department: Department) {
  // 하위 부서가 있는지 확인
  if (department.children && department.children.length > 0) {
    uiStore.showWarning('하위 부서가 있는 부서는 삭제할 수 없습니다.')
    return
  }

  // 소속 사용자가 있는지 확인
  if (department.userCount && department.userCount > 0) {
    uiStore.showWarning('소속 사용자가 있는 부서는 삭제할 수 없습니다.')
    return
  }

  const confirmed = await uiStore.confirm({
    title: '부서 삭제',
    message: `'${department.departmentName}' 부서를 삭제하시겠습니까?`,
    confirmText: '삭제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  try {
    const success = await departmentStore.deleteDepartment(department.departmentId)
    if (success) {
      uiStore.showSuccess('부서가 삭제되었습니다.')
    } else {
      uiStore.showError(departmentStore.error || '부서 삭제에 실패했습니다.')
    }
  } catch (error) {
    console.error('Failed to delete department:', error)
    uiStore.showError('부서 삭제에 실패했습니다.')
  }
}

// 폼 제출
async function handleFormSubmit(data: DepartmentCreateRequest | DepartmentUpdateRequest) {
  formLoading.value = true

  try {
    if (formMode.value === 'edit' && editingDepartment.value) {
      // 수정
      const success = await departmentStore.updateDepartment(
        editingDepartment.value.departmentId,
        data as DepartmentUpdateRequest
      )
      if (success) {
        uiStore.showSuccess('부서가 수정되었습니다.')
        handleFormCancel()
      } else {
        uiStore.showError(departmentStore.error || '부서 수정에 실패했습니다.')
      }
    } else {
      // 생성
      const result = await departmentStore.createDepartment(data as DepartmentCreateRequest)
      if (result) {
        uiStore.showSuccess('부서가 등록되었습니다.')
        // 생성된 부서 자동 선택
        departmentStore.selectDepartment(result)
        handleFormCancel()
      } else {
        uiStore.showError(departmentStore.error || '부서 등록에 실패했습니다.')
      }
    }
  } catch (error) {
    console.error('Failed to save department:', error)
    uiStore.showError('부서 저장에 실패했습니다.')
  } finally {
    formLoading.value = false
  }
}

// 폼 취소
function handleFormCancel() {
  formMode.value = 'none'
  editingDepartment.value = null
  parentDepartment.value = null
  formRef.value?.resetForm()
}

// 초기화
onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="departments-view">
    <!-- 헤더 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">부서 관리</h1>
      <p class="mt-1 text-sm text-gray-500">
        조직의 부서를 등록하고 관리합니다.
      </p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <!-- 왼쪽: 부서 트리 -->
      <div class="lg:col-span-5">
        <!-- 필터 옵션 -->
        <div class="flex items-center justify-between mb-3">
          <label class="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
            <input
              v-model="showInactive"
              type="checkbox"
              class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            비활성 부서 표시
          </label>
        </div>

        <DepartmentTree
          :show-inactive="showInactive"
          @select="handleSelect"
          @edit="handleEdit"
          @delete="handleDelete"
          @add-child="handleAddChild"
          @add-root="handleAddRoot"
        />
      </div>

      <!-- 오른쪽: 상세/폼 -->
      <div class="lg:col-span-7">
        <!-- 폼 모드 -->
        <template v-if="formMode !== 'none'">
          <DepartmentForm
            ref="formRef"
            :department="editingDepartment"
            :parent-department="parentDepartment"
            :loading="formLoading"
            @submit="handleFormSubmit"
            @cancel="handleFormCancel"
          />
        </template>

        <!-- 선택된 부서 상세 -->
        <template v-else-if="selectedDepartment">
          <div class="department-detail">
            <div class="detail-header">
              <div class="flex items-center gap-3">
                <div class="dept-icon">
                  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                </div>
                <div>
                  <h2 class="text-lg font-medium text-gray-900">
                    {{ selectedDepartment.departmentName }}
                  </h2>
                  <p class="text-sm text-gray-500">{{ selectedDepartment.departmentCode }}</p>
                </div>
              </div>
              <Badge
                :variant="selectedDepartment.useYn === 'Y' ? 'success' : 'default'"
                size="sm"
              >
                {{ selectedDepartment.useYn === 'Y' ? '활성' : '비활성' }}
              </Badge>
            </div>

            <!-- 부서 정보 -->
            <div class="detail-info">
              <div class="info-row">
                <span class="info-label">상위 부서</span>
                <span class="info-value">{{ selectedDepartment.parentName || '(최상위)' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">정렬 순서</span>
                <span class="info-value">{{ selectedDepartment.sortOrder }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">계층 깊이</span>
                <span class="info-value">{{ selectedDepartment.depth + 1 }}단계</span>
              </div>
            </div>

            <!-- 액션 버튼 -->
            <div class="detail-actions">
              <button type="button" class="btn-secondary" @click="handleEdit(selectedDepartment)">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
                수정
              </button>
              <button type="button" class="btn-secondary" @click="handleAddChild(selectedDepartment)">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                하위 부서 추가
              </button>
            </div>

            <!-- 소속 사용자 목록 -->
            <div class="users-section">
              <h3 class="section-title">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
                소속 사용자
                <span class="text-gray-500 font-normal">({{ departmentUsers.length }}명)</span>
              </h3>

              <div v-if="departmentUsers.length === 0" class="empty-users">
                <p>소속된 사용자가 없습니다.</p>
              </div>
              <div v-else class="users-list">
                <div
                  v-for="user in departmentUsers"
                  :key="user.userId"
                  class="user-item"
                >
                  <div class="user-avatar">
                    {{ user.userName.charAt(0) }}
                  </div>
                  <div class="user-info">
                    <span class="user-name">{{ user.userName }}</span>
                    <span class="user-id">{{ user.username }}</span>
                  </div>
                  <Badge
                    :variant="user.useYn === 'Y' ? 'success' : 'default'"
                    size="sm"
                  >
                    {{ user.useYn === 'Y' ? '활성' : '비활성' }}
                  </Badge>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 선택 안내 -->
        <template v-else>
          <div class="empty-state">
            <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
            <p class="mt-2 text-sm text-gray-500">부서를 선택하면 상세 정보를 볼 수 있습니다.</p>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.departments-view {
  @apply max-w-7xl mx-auto;
}

.department-detail {
  @apply bg-white rounded-lg border border-gray-200;
}

.detail-header {
  @apply flex items-center justify-between p-4 border-b border-gray-200;
}

.dept-icon {
  @apply w-12 h-12 rounded-lg bg-primary-100 text-primary-600 flex items-center justify-center;
}

.detail-info {
  @apply p-4 border-b border-gray-200 space-y-2;
}

.info-row {
  @apply flex items-center justify-between text-sm;
}

.info-label {
  @apply text-gray-500;
}

.info-value {
  @apply text-gray-900 font-medium;
}

.detail-actions {
  @apply flex items-center gap-2 p-4 border-b border-gray-200;
}

.btn-secondary {
  @apply inline-flex items-center gap-2 px-3 py-2 text-sm font-medium
         text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150;
}

.users-section {
  @apply p-4;
}

.section-title {
  @apply flex items-center gap-2 text-sm font-medium text-gray-900 mb-3;
}

.empty-users {
  @apply text-center py-6 text-sm text-gray-500;
}

.users-list {
  @apply space-y-2 max-h-[300px] overflow-y-auto;
}

.user-item {
  @apply flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50;
}

.user-avatar {
  @apply w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center
         text-sm font-medium text-gray-600;
}

.user-info {
  @apply flex-1 min-w-0;
}

.user-name {
  @apply block text-sm font-medium text-gray-900 truncate;
}

.user-id {
  @apply block text-xs text-gray-500;
}

.empty-state {
  @apply text-center py-12 bg-white rounded-lg border border-gray-200;
}
</style>
