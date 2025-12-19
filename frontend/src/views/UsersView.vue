<script setup lang="ts">
/**
 * 사용자 등록 메뉴 (Users View)
 * - 사용자 목록 표시
 * - 사용자 등록/수정/삭제
 */
import { ref, onMounted } from 'vue'
import UserForm from '@/components/user/UserForm.vue'
import UserList from '@/components/user/UserList.vue'
import Pagination from '@/components/common/Pagination.vue'
import Input from '@/components/common/Input.vue'
import Select from '@/components/common/Select.vue'
import type { SelectOption } from '@/components/common/Select.vue'
import type { User, UserCreateRequest, UserUpdateRequest } from '@/types/user'
import { userApi } from '@/api/user'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()

// 사용자 목록
const users = ref<User[]>([])
const loading = ref(false)
const formLoading = ref(false)

// 페이지네이션
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = ref(20)

// 선택된 사용자 (편집용)
const selectedUser = ref<User | null>(null)

// 검색 필터
const searchKeyword = ref('')
const statusFilter = ref<string>('')

// 상태 필터 옵션
const statusOptions: SelectOption[] = [
  { value: '', label: '전체' },
  { value: 'Y', label: '활성' },
  { value: 'N', label: '비활성' }
]

// Form ref
const formRef = ref<InstanceType<typeof UserForm> | null>(null)

// 사용자 목록 조회
async function loadUsers() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      useYn: statusFilter.value || undefined
    }

    const response = await userApi.getUsers(params)
    users.value = response.data.content
    totalPages.value = response.data.totalPages
    totalElements.value = response.data.totalElements
  } catch (error) {
    console.error('Failed to load users:', error)
    uiStore.showError('사용자 목록을 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

// 사용자 등록
async function handleCreate(data: UserCreateRequest) {
  formLoading.value = true
  try {
    await userApi.createUser(data)
    uiStore.showSuccess('사용자가 등록되었습니다.')
    formRef.value?.resetForm()
    await loadUsers()
  } catch (error: any) {
    console.error('Failed to create user:', error)
    const message = error.response?.data?.message || '사용자 등록에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    formLoading.value = false
  }
}

// 사용자 수정
async function handleUpdate(data: UserUpdateRequest) {
  if (!selectedUser.value) return

  formLoading.value = true
  try {
    await userApi.updateUser(selectedUser.value.userId, data)
    uiStore.showSuccess('사용자 정보가 수정되었습니다.')
    selectedUser.value = null
    formRef.value?.resetForm()
    await loadUsers()
  } catch (error: any) {
    console.error('Failed to update user:', error)
    const message = error.response?.data?.message || '사용자 수정에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    formLoading.value = false
  }
}

// 폼 제출 핸들러
function handleSubmit(data: UserCreateRequest | UserUpdateRequest) {
  if (selectedUser.value) {
    handleUpdate(data as UserUpdateRequest)
  } else {
    handleCreate(data as UserCreateRequest)
  }
}

// 폼 취소
function handleCancel() {
  selectedUser.value = null
}

// 사용자 선택 (편집)
function handleSelect(user: User) {
  selectedUser.value = user
}

// 상태 토글
async function handleToggleStatus(user: User) {
  const newStatus = user.useYn === 'Y' ? 'N' : 'Y'
  const action = newStatus === 'Y' ? '활성화' : '비활성화'

  const confirmed = await uiStore.confirm({
    title: `사용자 ${action}`,
    message: `'${user.userName}' 사용자를 ${action}하시겠습니까?`,
    confirmText: action,
    cancelText: '취소'
  })

  if (!confirmed) return

  try {
    await userApi.updateUser(user.userId, { useYn: newStatus })
    uiStore.showSuccess(`사용자가 ${action}되었습니다.`)
    await loadUsers()
  } catch (error: any) {
    console.error('Failed to toggle user status:', error)
    const message = error.response?.data?.message || `사용자 ${action}에 실패했습니다.`
    uiStore.showError(message)
  }
}

// 사용자 삭제
async function handleDelete(user: User) {
  const confirmed = await uiStore.confirm({
    title: '사용자 삭제',
    message: `'${user.userName}' 사용자를 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.`,
    confirmText: '삭제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  try {
    await userApi.deleteUser(user.userId)
    uiStore.showSuccess('사용자가 삭제되었습니다.')

    // 선택된 사용자가 삭제된 경우 선택 해제
    if (selectedUser.value?.userId === user.userId) {
      selectedUser.value = null
      formRef.value?.resetForm()
    }

    await loadUsers()
  } catch (error: any) {
    console.error('Failed to delete user:', error)
    const message = error.response?.data?.message || '사용자 삭제에 실패했습니다.'
    uiStore.showError(message)
  }
}

// 검색
function handleSearch() {
  currentPage.value = 0
  loadUsers()
}

// 페이지 변경
function handlePageChange(page: number) {
  currentPage.value = page
  loadUsers()
}

// 초기 로드
onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="users-view">
    <!-- 헤더 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">사용자 관리</h1>
      <p class="mt-1 text-sm text-gray-500">
        사용자를 등록하고 관리합니다.
      </p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 왼쪽: 사용자 폼 -->
      <div class="lg:col-span-1">
        <div class="sticky top-4">
          <h2 class="text-base font-medium text-gray-900 mb-3">
            {{ selectedUser ? '사용자 수정' : '사용자 등록' }}
          </h2>
          <UserForm
            ref="formRef"
            :user="selectedUser"
            :loading="formLoading"
            @submit="handleSubmit"
            @cancel="handleCancel"
          />
        </div>
      </div>

      <!-- 오른쪽: 사용자 목록 -->
      <div class="lg:col-span-2">
        <!-- 검색 필터 -->
        <div class="filter-section mb-4">
          <div class="flex items-center gap-4">
            <div class="flex-1">
              <Input
                v-model="searchKeyword"
                placeholder="이름 또는 아이디로 검색..."
                :clearable="true"
                @enter="handleSearch"
              />
            </div>
            <div class="w-32">
              <Select
                v-model="statusFilter"
                :options="statusOptions"
                placeholder="상태"
                @change="handleSearch"
              />
            </div>
            <button
              type="button"
              class="btn-primary h-8"
              @click="handleSearch"
            >
              <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              검색
            </button>
          </div>
        </div>

        <!-- 결과 정보 -->
        <div class="flex items-center justify-between mb-3">
          <p class="text-sm text-gray-600">
            총 <span class="font-medium text-gray-900">{{ totalElements }}</span>명
          </p>
        </div>

        <!-- 사용자 목록 -->
        <div class="table-container">
          <UserList
            :users="users"
            :selected-user-id="selectedUser?.userId"
            :loading="loading"
            @select="handleSelect"
            @toggle-status="handleToggleStatus"
            @delete="handleDelete"
          />
        </div>

        <!-- 페이지네이션 -->
        <div v-if="totalPages > 1" class="mt-4 flex justify-center">
          <Pagination
            :current-page="currentPage"
            :total-pages="totalPages"
            @change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.users-view {
  @apply max-w-7xl mx-auto;
}

.filter-section {
  @apply p-3 bg-white rounded-lg border border-gray-200;
}

.table-container {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden;
}

.btn-primary {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150;
}
</style>
