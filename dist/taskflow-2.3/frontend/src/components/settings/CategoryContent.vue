<script setup lang="ts">
/**
 * 카테고리 관리 컴포넌트 (설정 페이지 내 탭용)
 * - 전역 카테고리 관리 (TB_CATEGORY)
 * - 카테고리 추가/수정/삭제 (논리 삭제)
 * - 카테고리 공유/속성 관리 (상세 모달)
 */
import { ref, onMounted } from 'vue'
import { categoryApi } from '@/api/category'
import type { Category, CategoryCreateRequest, CategoryUpdateRequest } from '@/types/category'
import { useUiStore } from '@/stores/ui'
import CategoryDetailModal from './CategoryDetailModal.vue'

const uiStore = useUiStore()

// 상태
const loading = ref(false)
const saving = ref(false)
const categories = ref<Category[]>([])

// 상세 모달 상태
const showDetailModal = ref(false)
const selectedCategoryId = ref<number | null>(null)

// 편집 상태
const editingCategory = ref<Category | null>(null)
const showAddForm = ref(false)
const newCategory = ref<CategoryCreateRequest>({
  categoryCode: '',
  categoryName: '',
  categoryColor: '#3B82F6',
  sortOrder: 0
})

// 색상 옵션
const colorOptions = [
  '#3B82F6', // blue
  '#10B981', // green
  '#8B5CF6', // purple
  '#EC4899', // pink
  '#F59E0B', // amber
  '#EF4444', // red
  '#6B7280', // gray
  '#06B6D4', // cyan
]

// 카테고리 목록 조회 (본인 소유 카테고리만)
async function loadCategories() {
  loading.value = true
  try {
    // 본인 소유 카테고리만 조회 (공유받은 카테고리는 제외)
    const res = await categoryApi.getMyCategories()
    categories.value = res.data
  } catch (error) {
    console.error('Failed to load categories:', error)
    uiStore.showError('카테고리 정보를 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

// 카테고리 코드 자동 생성
function generateCategoryCode(): string {
  const timestamp = Date.now().toString(36).toUpperCase()
  return `CAT_${timestamp}`
}

// 추가 폼 표시
function handleShowAddForm() {
  showAddForm.value = true
  const maxSort = categories.value.length > 0
    ? Math.max(...categories.value.map(c => c.sortOrder || 0))
    : 0
  newCategory.value = {
    categoryCode: generateCategoryCode(),
    categoryName: '',
    categoryColor: '#3B82F6',
    sortOrder: maxSort + 1
  }
}

// 추가 취소
function handleCancelAdd() {
  showAddForm.value = false
  newCategory.value = { categoryCode: '', categoryName: '', categoryColor: '#3B82F6', sortOrder: 0 }
}

// 카테고리 추가
async function handleAddCategory() {
  if (!newCategory.value.categoryName.trim()) {
    uiStore.showWarning('카테고리명을 입력해주세요.')
    return
  }

  saving.value = true
  try {
    await categoryApi.createCategory(newCategory.value)
    uiStore.showSuccess('카테고리가 추가되었습니다.')
    handleCancelAdd()
    await loadCategories()
  } catch (error: any) {
    console.error('Failed to add category:', error)
    const message = error.response?.data?.message || '카테고리 추가에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    saving.value = false
  }
}

// 편집 시작
function handleEditCategory(category: Category) {
  editingCategory.value = { ...category }
}

// 편집 취소
function handleCancelEdit() {
  editingCategory.value = null
}

// 카테고리 저장
async function handleSaveCategory() {
  if (!editingCategory.value) return
  if (!editingCategory.value.categoryName.trim()) {
    uiStore.showWarning('카테고리명을 입력해주세요.')
    return
  }

  saving.value = true
  try {
    const updateData: CategoryUpdateRequest = {
      categoryName: editingCategory.value.categoryName.trim(),
      categoryColor: editingCategory.value.categoryColor,
      sortOrder: editingCategory.value.sortOrder,
      useYn: editingCategory.value.useYn
    }
    await categoryApi.updateCategory(editingCategory.value.categoryId, updateData)
    uiStore.showSuccess('카테고리가 수정되었습니다.')
    editingCategory.value = null
    await loadCategories()
  } catch (error: any) {
    console.error('Failed to update category:', error)
    const message = error.response?.data?.message || '카테고리 수정에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    saving.value = false
  }
}

// 카테고리 삭제 (논리 삭제)
async function handleDeleteCategory(category: Category) {
  const confirmed = await uiStore.confirm({
    title: '카테고리 삭제',
    message: `'${category.categoryName}' 카테고리를 삭제하시겠습니까?\n이 카테고리를 사용 중인 업무가 있으면 해당 업무의 카테고리가 표시되지 않습니다.`,
    confirmText: '삭제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  saving.value = true
  try {
    await categoryApi.deleteCategory(category.categoryId)
    uiStore.showSuccess('카테고리가 삭제되었습니다.')
    await loadCategories()
  } catch (error: any) {
    console.error('Failed to delete category:', error)
    const message = error.response?.data?.message || '카테고리 삭제에 실패했습니다.'
    uiStore.showError(message)
  } finally {
    saving.value = false
  }
}

// 순서 위로
async function handleMoveUp(category: Category, index: number) {
  if (index === 0) return
  const prevCategory = categories.value[index - 1]

  saving.value = true
  try {
    await categoryApi.updateCategory(category.categoryId, { sortOrder: prevCategory.sortOrder })
    await categoryApi.updateCategory(prevCategory.categoryId, { sortOrder: category.sortOrder })
    await loadCategories()
  } catch (error) {
    console.error('Failed to reorder:', error)
  } finally {
    saving.value = false
  }
}

// 순서 아래로
async function handleMoveDown(category: Category, index: number) {
  if (index === categories.value.length - 1) return
  const nextCategory = categories.value[index + 1]

  saving.value = true
  try {
    await categoryApi.updateCategory(category.categoryId, { sortOrder: nextCategory.sortOrder })
    await categoryApi.updateCategory(nextCategory.categoryId, { sortOrder: category.sortOrder })
    await loadCategories()
  } catch (error) {
    console.error('Failed to reorder:', error)
  } finally {
    saving.value = false
  }
}

// 상세 모달 열기
function handleOpenDetail(category: Category) {
  selectedCategoryId.value = category.categoryId
  showDetailModal.value = true
}

// 상세 모달에서 업데이트 시 목록 갱신
function handleDetailUpdated() {
  loadCategories()
}

// 초기 로드
onMounted(() => {
  loadCategories()
})
</script>

<template>
  <div class="category-content">
    <!-- 설명 -->
    <div class="mb-4">
      <p class="text-sm text-gray-500">
        업무에 사용되는 카테고리를 관리합니다. 카테고리는 모든 보드에서 공통으로 사용됩니다.
      </p>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="py-12 text-center">
      <svg class="animate-spin h-8 w-8 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">카테고리를 불러오는 중...</p>
    </div>

    <!-- 메인 콘텐츠 -->
    <div v-else class="settings-card">
      <div class="settings-card-title flex items-center justify-between">
        <span>카테고리 목록</span>
        <button
          v-if="!showAddForm"
          type="button"
          class="btn-add"
          @click="handleShowAddForm"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          카테고리 추가
        </button>
      </div>

      <div class="settings-card-content">
        <!-- 추가 폼 -->
        <div v-if="showAddForm" class="mb-4 p-4 bg-gray-50 dark:bg-gray-700 rounded-lg border border-gray-200 dark:border-gray-600">
          <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">새 카테고리 추가</h4>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-500 mb-1">카테고리명</label>
              <input
                v-model="newCategory.categoryName"
                type="text"
                class="form-input"
                placeholder="예: 개발"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-500 mb-1">색상</label>
              <div class="flex items-center gap-2">
                <div
                  class="w-8 h-8 rounded-lg border border-gray-200"
                  :style="{ backgroundColor: newCategory.categoryColor }"
                ></div>
                <div class="flex flex-wrap gap-1">
                  <button
                    v-for="color in colorOptions"
                    :key="color"
                    type="button"
                    class="w-5 h-5 rounded border border-gray-200 hover:scale-110 transition-transform"
                    :class="{ 'ring-2 ring-primary-500': newCategory.categoryColor === color }"
                    :style="{ backgroundColor: color }"
                    @click="newCategory.categoryColor = color"
                  ></button>
                </div>
              </div>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-500 mb-1">정렬 순서</label>
              <input
                v-model.number="newCategory.sortOrder"
                type="number"
                min="0"
                class="form-input"
              />
            </div>
          </div>
          <div class="mt-3 flex justify-end gap-2">
            <button type="button" class="btn-cancel" @click="handleCancelAdd" :disabled="saving">
              취소
            </button>
            <button type="button" class="btn-primary" @click="handleAddCategory" :disabled="saving">
              {{ saving ? '저장 중...' : '추가' }}
            </button>
          </div>
        </div>

        <!-- 카테고리 목록 테이블 -->
        <table v-if="categories.length > 0" class="option-table">
          <thead>
            <tr>
              <th class="w-[80px]">순서</th>
              <th class="w-[60px]">색상</th>
              <th>카테고리명</th>
              <th class="w-[80px]">상태</th>
              <th class="w-[180px]">관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(category, index) in categories" :key="category.categoryId">
              <!-- 편집 모드 -->
              <template v-if="editingCategory?.categoryId === category.categoryId">
                <td>
                  <input
                    v-model.number="editingCategory.sortOrder"
                    type="number"
                    min="0"
                    class="form-input w-16"
                  />
                </td>
                <td>
                  <div class="flex items-center gap-1">
                    <div
                      class="w-6 h-6 rounded border border-gray-200"
                      :style="{ backgroundColor: editingCategory.categoryColor }"
                    ></div>
                    <select v-model="editingCategory.categoryColor" class="form-input w-20 text-xs">
                      <option v-for="color in colorOptions" :key="color" :value="color">
                        {{ color }}
                      </option>
                    </select>
                  </div>
                </td>
                <td>
                  <input
                    v-model="editingCategory.categoryName"
                    type="text"
                    class="form-input"
                  />
                </td>
                <td>
                  <select v-model="editingCategory.useYn" class="form-input">
                    <option value="Y">활성</option>
                    <option value="N">비활성</option>
                  </select>
                </td>
                <td>
                  <div class="flex items-center gap-1">
                    <button type="button" class="icon-btn text-green-600" title="저장" @click="handleSaveCategory" :disabled="saving">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </button>
                    <button type="button" class="icon-btn text-gray-400" title="취소" @click="handleCancelEdit" :disabled="saving">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                </td>
              </template>

              <!-- 조회 모드 -->
              <template v-else>
                <td>
                  <div class="flex items-center gap-1">
                    <span class="text-gray-600 dark:text-gray-300">{{ category.sortOrder }}</span>
                    <div class="flex flex-col">
                      <button
                        type="button"
                        class="order-btn"
                        :disabled="index === 0 || saving"
                        @click="handleMoveUp(category, index)"
                      >
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7" />
                        </svg>
                      </button>
                      <button
                        type="button"
                        class="order-btn"
                        :disabled="index === categories.length - 1 || saving"
                        @click="handleMoveDown(category, index)"
                      >
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                        </svg>
                      </button>
                    </div>
                  </div>
                </td>
                <td>
                  <div
                    class="w-6 h-6 rounded-lg border border-gray-200"
                    :style="{ backgroundColor: category.categoryColor || '#6B7280' }"
                  ></div>
                </td>
                <td class="font-medium text-gray-900 dark:text-white">
                  {{ category.categoryName }}
                </td>
                <td>
                  <span
                    class="status-badge"
                    :class="category.useYn === 'Y' ? 'active' : 'inactive'"
                  >
                    {{ category.useYn === 'Y' ? '활성' : '비활성' }}
                  </span>
                </td>
                <td>
                  <div class="flex items-center gap-1">
                    <button type="button" class="icon-btn text-primary-600" title="공유/속성 관리" @click="handleOpenDetail(category)">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
                      </svg>
                    </button>
                    <button type="button" class="icon-btn" title="수정" @click="handleEditCategory(category)">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button type="button" class="icon-btn text-red-500" title="삭제" @click="handleDeleteCategory(category)">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </td>
              </template>
            </tr>
          </tbody>
        </table>

        <!-- 빈 상태 -->
        <div v-else class="py-8 text-center">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
              d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
          </svg>
          <p class="mt-2 text-sm text-gray-500">등록된 카테고리가 없습니다.</p>
          <button
            type="button"
            class="mt-3 text-sm text-primary-600 hover:text-primary-700"
            @click="handleShowAddForm"
          >
            + 첫 번째 카테고리 추가하기
          </button>
        </div>
      </div>
    </div>

    <!-- 카테고리 상세 모달 (공유/속성 관리) -->
    <CategoryDetailModal
      v-model="showDetailModal"
      :category-id="selectedCategoryId"
      @updated="handleDetailUpdated"
    />
  </div>
</template>

<style scoped>
.category-content {
  @apply w-full max-w-4xl;
}

.settings-card {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden
         dark:bg-gray-800 dark:border-gray-700;
}

.settings-card-title {
  @apply px-4 py-3 text-base font-medium text-gray-900 bg-gray-50 border-b border-gray-200
         dark:text-white dark:bg-gray-700 dark:border-gray-600;
}

.settings-card-content {
  @apply p-4;
}

.btn-add {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium
         text-primary-600 bg-primary-50 rounded-md
         hover:bg-primary-100 transition-colors
         dark:text-primary-400 dark:bg-primary-900/50 dark:hover:bg-primary-900/70;
}

.btn-primary {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150 disabled:opacity-50 disabled:cursor-not-allowed
         dark:focus:ring-offset-gray-900;
}

.btn-cancel {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150 disabled:opacity-50
         dark:text-gray-200 dark:bg-gray-700 dark:border-gray-600 dark:hover:bg-gray-600
         dark:focus:ring-offset-gray-900;
}

.form-input {
  @apply w-full px-3 py-1.5 text-sm border border-gray-300 rounded-md
         focus:ring-primary-500 focus:border-primary-500
         dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400;
}

.option-table {
  @apply w-full text-sm;
}

.option-table thead {
  @apply bg-gray-50 border-y border-gray-200
         dark:bg-gray-700 dark:border-gray-600;
}

.option-table th {
  @apply px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider
         dark:text-gray-400;
}

.option-table tbody {
  @apply divide-y divide-gray-100
         dark:divide-gray-700;
}

.option-table td {
  @apply px-4 py-3 whitespace-nowrap dark:text-gray-300;
}

.option-table tbody tr {
  @apply hover:bg-gray-50 transition-colors
         dark:hover:bg-gray-700;
}

.status-badge {
  @apply inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full;
}

.status-badge.active {
  @apply bg-green-100 text-green-700
         dark:bg-green-900/50 dark:text-green-400;
}

.status-badge.inactive {
  @apply bg-gray-100 text-gray-600
         dark:bg-gray-700 dark:text-gray-400;
}

.icon-btn {
  @apply p-1.5 rounded hover:bg-gray-100 text-gray-500 transition-colors disabled:opacity-50
         dark:hover:bg-gray-700 dark:text-gray-400;
}

.order-btn {
  @apply p-0.5 text-gray-400 hover:text-gray-600 disabled:opacity-30 disabled:cursor-not-allowed
         dark:hover:text-gray-300;
}
</style>
