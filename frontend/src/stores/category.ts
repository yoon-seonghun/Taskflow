/**
 * 카테고리 스토어 (v2.0 - 소유자 기반 카테고리)
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { categoryApi } from '@/api/category'
import type {
  Category,
  CategoryDetail,
  CategoryCreateRequest,
  CategoryUpdateRequest,
  CategoryShareRequest,
  CategoryPropertyRequest,
  CategoryShare
} from '@/types/category'

export const useCategoryStore = defineStore('category', () => {
  // =============================================
  // State
  // =============================================

  // 전체 카테고리 목록 (관리자용)
  const categories = ref<Category[]>([])

  // 접근 가능한 카테고리 목록 (내 것 + 공유받은 것)
  const accessibleCategories = ref<Category[]>([])

  // 내 카테고리 목록
  const myCategories = ref<Category[]>([])

  // 공유받은 카테고리 목록
  const sharedCategories = ref<Category[]>([])

  // 현재 선택된 카테고리 상세
  const currentCategory = ref<CategoryDetail | null>(null)

  // 로딩 상태
  const loading = ref(false)
  const error = ref<string | null>(null)

  // =============================================
  // Getters
  // =============================================

  // 활성 카테고리만 필터
  const activeCategories = computed(() =>
    categories.value.filter(c => c.useYn === 'Y')
  )

  const activeAccessibleCategories = computed(() =>
    accessibleCategories.value.filter(c => c.useYn === 'Y')
  )

  // ID로 카테고리 찾기
  const getCategoryById = computed(() => (id: number) =>
    categories.value.find(c => c.categoryId === id) ||
    accessibleCategories.value.find(c => c.categoryId === id)
  )

  // 코드로 카테고리 찾기
  const getCategoryByCode = computed(() => (code: string) =>
    categories.value.find(c => c.categoryCode === code) ||
    accessibleCategories.value.find(c => c.categoryCode === code)
  )

  // 이름으로 카테고리 찾기
  const getCategoryByName = computed(() => (name: string) =>
    categories.value.find(c => c.categoryName === name) ||
    accessibleCategories.value.find(c => c.categoryName === name)
  )

  // 내 소유 카테고리인지 확인
  const isMyCategory = computed(() => (categoryId: number) =>
    myCategories.value.some(c => c.categoryId === categoryId)
  )

  // =============================================
  // Actions - 조회
  // =============================================

  /**
   * 전체 카테고리 목록 조회 (관리자용)
   */
  async function fetchCategories(includeDeleted: boolean = false) {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.getCategories(includeDeleted)
      categories.value = response.data
      sortCategories(categories.value)
    } catch (e: any) {
      error.value = e.message || '카테고리 목록을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 접근 가능한 카테고리 목록 조회
   */
  async function fetchAccessibleCategories() {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.getAccessibleCategories()
      accessibleCategories.value = response.data
      sortCategories(accessibleCategories.value)
    } catch (e: any) {
      error.value = e.message || '카테고리 목록을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 내 카테고리 목록 조회
   */
  async function fetchMyCategories() {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.getMyCategories()
      myCategories.value = response.data
      sortCategories(myCategories.value)
    } catch (e: any) {
      error.value = e.message || '내 카테고리 목록을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 공유받은 카테고리 목록 조회
   */
  async function fetchSharedCategories() {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.getSharedWithMeCategories()
      sharedCategories.value = response.data
      sortCategories(sharedCategories.value)
    } catch (e: any) {
      error.value = e.message || '공유받은 카테고리 목록을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 카테고리 상세 조회
   */
  async function fetchCategoryDetail(categoryId: number) {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.getCategoryDetail(categoryId)
      currentCategory.value = response.data
      return response.data
    } catch (e: any) {
      error.value = e.message || '카테고리 상세를 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // =============================================
  // Actions - CRUD
  // =============================================

  /**
   * 카테고리 생성
   */
  async function createCategory(request: CategoryCreateRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.createCategory(request)
      const newCategory = response.data

      // 목록에 추가
      categories.value.push(newCategory)
      myCategories.value.push(newCategory)
      accessibleCategories.value.push(newCategory)

      sortCategories(categories.value)
      sortCategories(myCategories.value)
      sortCategories(accessibleCategories.value)

      return newCategory
    } catch (e: any) {
      error.value = e.message || '카테고리 생성에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 카테고리 수정
   */
  async function updateCategory(categoryId: number, request: CategoryUpdateRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.updateCategory(categoryId, request)
      const updatedCategory = response.data

      // 목록 업데이트
      updateCategoryInList(categories.value, updatedCategory)
      updateCategoryInList(myCategories.value, updatedCategory)
      updateCategoryInList(accessibleCategories.value, updatedCategory)

      // 현재 상세 업데이트
      if (currentCategory.value?.categoryId === categoryId) {
        currentCategory.value = { ...currentCategory.value, ...updatedCategory }
      }

      return updatedCategory
    } catch (e: any) {
      error.value = e.message || '카테고리 수정에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 카테고리 삭제 (논리 삭제)
   */
  async function deleteCategory(categoryId: number) {
    loading.value = true
    error.value = null
    try {
      await categoryApi.deleteCategory(categoryId)

      // 목록에서 논리 삭제
      markCategoryDeleted(categories.value, categoryId)
      markCategoryDeleted(myCategories.value, categoryId)
      markCategoryDeleted(accessibleCategories.value, categoryId)

      // 현재 선택된 카테고리가 삭제된 경우 초기화
      if (currentCategory.value?.categoryId === categoryId) {
        currentCategory.value = null
      }
    } catch (e: any) {
      error.value = e.message || '카테고리 삭제에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // =============================================
  // Actions - 공유 관리
  // =============================================

  /**
   * 카테고리 공유 추가
   */
  async function addShare(categoryId: number, request: CategoryShareRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.addShare(categoryId, request)

      // 현재 상세에 공유 추가
      if (currentCategory.value?.categoryId === categoryId) {
        currentCategory.value.shares.push(response.data)
      }

      return response.data
    } catch (e: any) {
      error.value = e.message || '공유 추가에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 카테고리 공유 다건 추가
   */
  async function addShares(categoryId: number, requests: CategoryShareRequest[]) {
    loading.value = true
    error.value = null
    try {
      const response = await categoryApi.addShares(categoryId, requests)

      // 현재 상세에 공유 추가
      if (currentCategory.value?.categoryId === categoryId) {
        currentCategory.value.shares.push(...response.data)
      }

      return response.data
    } catch (e: any) {
      error.value = e.message || '공유 추가에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 카테고리 공유 권한 변경
   */
  async function updateSharePermission(categoryId: number, shareId: number, permission: 'READ' | 'EDIT') {
    loading.value = true
    error.value = null
    try {
      await categoryApi.updateSharePermission(categoryId, shareId, permission)

      // 현재 상세에서 공유 권한 업데이트
      if (currentCategory.value?.categoryId === categoryId) {
        const share = currentCategory.value.shares.find(s => s.shareId === shareId)
        if (share) {
          share.permission = permission
        }
      }
    } catch (e: any) {
      error.value = e.message || '공유 권한 변경에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 카테고리 공유 해제
   */
  async function removeShare(categoryId: number, shareId: number) {
    loading.value = true
    error.value = null
    try {
      await categoryApi.removeShare(categoryId, shareId)

      // 현재 상세에서 공유 제거
      if (currentCategory.value?.categoryId === categoryId) {
        currentCategory.value.shares = currentCategory.value.shares.filter(s => s.shareId !== shareId)
      }
    } catch (e: any) {
      error.value = e.message || '공유 해제에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // =============================================
  // Actions - 속성 관리
  // =============================================

  /**
   * 카테고리에 속성 추가
   */
  async function addProperty(categoryId: number, request: CategoryPropertyRequest) {
    loading.value = true
    error.value = null
    try {
      await categoryApi.addProperty(categoryId, request)

      // 상세 다시 로드
      if (currentCategory.value?.categoryId === categoryId) {
        await fetchCategoryDetail(categoryId)
      }
    } catch (e: any) {
      error.value = e.message || '속성 추가에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 카테고리에서 속성 제거
   */
  async function removeProperty(categoryId: number, propertyId: number) {
    loading.value = true
    error.value = null
    try {
      await categoryApi.removeProperty(categoryId, propertyId)

      // 현재 상세에서 속성 제거
      if (currentCategory.value?.categoryId === categoryId) {
        currentCategory.value.properties = currentCategory.value.properties.filter(p => p.propertyId !== propertyId)
      }
    } catch (e: any) {
      error.value = e.message || '속성 제거에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 카테고리 속성 순서 변경 (일괄)
   */
  async function reorderProperties(categoryId: number, orders: { propertyId: number; sortOrder: number }[]) {
    loading.value = true
    error.value = null
    try {
      await categoryApi.reorderProperties(categoryId, orders)

      // 현재 상세에서 속성 순서 업데이트
      if (currentCategory.value?.categoryId === categoryId) {
        for (const order of orders) {
          const property = currentCategory.value.properties.find(p => p.propertyId === order.propertyId)
          if (property) {
            property.sortOrder = order.sortOrder
          }
        }
        // 재정렬
        currentCategory.value.properties.sort((a, b) => a.sortOrder - b.sortOrder)
      }
    } catch (e: any) {
      error.value = e.message || '속성 순서 변경에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 카테고리 속성 기본값 변경
   */
  async function updatePropertyDefaultValue(categoryId: number, propertyId: number, defaultValue: string) {
    loading.value = true
    error.value = null
    try {
      await categoryApi.updatePropertyDefaultValue(categoryId, propertyId, defaultValue)

      // 현재 상세에서 속성 기본값 업데이트
      if (currentCategory.value?.categoryId === categoryId) {
        const property = currentCategory.value.properties.find(p => p.propertyId === propertyId)
        if (property) {
          property.defaultValue = defaultValue
        }
      }
    } catch (e: any) {
      error.value = e.message || '속성 기본값 변경에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // =============================================
  // Helper Functions
  // =============================================

  function sortCategories(list: Category[]) {
    list.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  }

  function updateCategoryInList(list: Category[], updated: Category) {
    const index = list.findIndex(c => c.categoryId === updated.categoryId)
    if (index !== -1) {
      list[index] = { ...list[index], ...updated }
    }
    sortCategories(list)
  }

  function markCategoryDeleted(list: Category[], categoryId: number) {
    const index = list.findIndex(c => c.categoryId === categoryId)
    if (index !== -1) {
      list[index].useYn = 'N'
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentCategory() {
    currentCategory.value = null
  }

  /**
   * 모든 카테고리 상태 초기화 (로그아웃 시 사용)
   */
  function reset() {
    categories.value = []
    accessibleCategories.value = []
    myCategories.value = []
    sharedCategories.value = []
    currentCategory.value = null
    error.value = null
  }

  // =============================================
  // Return
  // =============================================

  return {
    // State
    categories,
    accessibleCategories,
    myCategories,
    sharedCategories,
    currentCategory,
    loading,
    error,

    // Getters
    activeCategories,
    activeAccessibleCategories,
    getCategoryById,
    getCategoryByCode,
    getCategoryByName,
    isMyCategory,

    // Actions - 조회
    fetchCategories,
    fetchAccessibleCategories,
    fetchMyCategories,
    fetchSharedCategories,
    fetchCategoryDetail,

    // Actions - CRUD
    createCategory,
    updateCategory,
    deleteCategory,

    // Actions - 공유
    addShare,
    addShares,
    updateSharePermission,
    removeShare,

    // Actions - 속성
    addProperty,
    removeProperty,
    reorderProperties,
    updatePropertyDefaultValue,

    // Helpers
    clearError,
    clearCurrentCategory,
    reset
  }
})
