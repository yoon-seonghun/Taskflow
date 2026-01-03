/**
 * 카테고리 스토어 (전역 카테고리)
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { categoryApi } from '@/api/category'
import type { Category, CategoryCreateRequest, CategoryUpdateRequest } from '@/types/category'

export const useCategoryStore = defineStore('category', () => {
  // State
  const categories = ref<Category[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const activeCategories = computed(() =>
    categories.value.filter(c => c.useYn === 'Y')
  )

  const getCategoryById = computed(() => (id: number) =>
    categories.value.find(c => c.categoryId === id)
  )

  const getCategoryByName = computed(() => (name: string) =>
    categories.value.find(c => c.categoryName === name)
  )

  // Actions
  async function fetchCategories(includeDeleted: boolean = false) {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.getCategories(includeDeleted)
      categories.value = response.data
    } catch (e: any) {
      error.value = e.message || '카테고리 목록을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function createCategory(request: CategoryCreateRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.createCategory(request)
      categories.value.push(response.data)
      // 정렬
      categories.value.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
      return response.data
    } catch (e: any) {
      error.value = e.message || '카테고리 생성에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateCategory(categoryId: number, request: CategoryUpdateRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.updateCategory(categoryId, request)
      const index = categories.value.findIndex(c => c.categoryId === categoryId)
      if (index !== -1) {
        categories.value[index] = response.data
      }
      // 정렬
      categories.value.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
      return response.data
    } catch (e: any) {
      error.value = e.message || '카테고리 수정에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deleteCategory(categoryId: number) {
    loading.value = true
    error.value = null
    try {
      await categoryApi.deleteCategory(categoryId)
      // 목록에서 제거하거나 useYn = 'N'으로 변경
      const index = categories.value.findIndex(c => c.categoryId === categoryId)
      if (index !== -1) {
        categories.value[index].useYn = 'N'
      }
    } catch (e: any) {
      error.value = e.message || '카테고리 삭제에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    categories,
    loading,
    error,
    // Getters
    activeCategories,
    getCategoryById,
    getCategoryByName,
    // Actions
    fetchCategories,
    createCategory,
    updateCategory,
    deleteCategory,
    clearError
  }
})
