import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { departmentApi } from '@/api/department'
import type {
  Department,
  DepartmentCreateRequest,
  DepartmentUpdateRequest
} from '@/types/department'
import type { User } from '@/types/user'

export const useDepartmentStore = defineStore('department', () => {
  // ==================== State ====================
  const departments = ref<Department[]>([])
  const flatDepartments = ref<Department[]>([])
  const selectedDepartment = ref<Department | null>(null)
  const departmentUsers = ref<User[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 트리 확장 상태 (departmentId -> expanded)
  const expandedNodes = ref<Set<number>>(new Set())

  // ==================== Getters ====================
  // 활성 부서만 (트리)
  const activeDepartments = computed(() => {
    return filterActiveDepartments(departments.value)
  })

  // 활성 부서만 (평면)
  const activeFlatDepartments = computed(() => {
    return flatDepartments.value.filter(d => d.useYn === 'Y')
  })

  // 선택된 부서 ID
  const selectedDepartmentId = computed(() => selectedDepartment.value?.departmentId || null)

  // ==================== Helper Functions ====================
  // 활성 부서만 필터링 (재귀)
  function filterActiveDepartments(depts: Department[]): Department[] {
    return depts
      .filter(d => d.useYn === 'Y')
      .map(d => ({
        ...d,
        children: d.children ? filterActiveDepartments(d.children) : undefined
      }))
  }

  // 부서 찾기 (트리에서)
  function findDepartmentInTree(depts: Department[], id: number): Department | null {
    for (const dept of depts) {
      if (dept.departmentId === id) return dept
      if (dept.children) {
        const found = findDepartmentInTree(dept.children, id)
        if (found) return found
      }
    }
    return null
  }

  // 부서 업데이트 (트리에서)
  function updateDepartmentInTree(depts: Department[], id: number, data: Partial<Department>): boolean {
    for (let i = 0; i < depts.length; i++) {
      if (depts[i].departmentId === id) {
        depts[i] = { ...depts[i], ...data }
        return true
      }
      if (depts[i].children) {
        if (updateDepartmentInTree(depts[i].children, id, data)) return true
      }
    }
    return false
  }

  // 부서 삭제 (트리에서)
  function removeDepartmentFromTree(depts: Department[], id: number): Department[] {
    return depts.filter(d => {
      if (d.departmentId === id) return false
      if (d.children) {
        d.children = removeDepartmentFromTree(d.children, id)
      }
      return true
    })
  }

  // ==================== Actions ====================

  /**
   * 부서 목록 조회 (트리)
   */
  async function fetchDepartments(params?: { useYn?: string }): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await departmentApi.getDepartments(params)
      if (response.success && response.data) {
        departments.value = response.data
        // 모든 노드 확장
        expandAllNodes()
        return true
      }
      error.value = response.message || '부서 목록을 불러오는데 실패했습니다.'
      return false
    } catch (e) {
      error.value = '부서 목록을 불러오는데 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 부서 목록 조회 (평면)
   */
  async function fetchFlatDepartments(params?: { useYn?: string }): Promise<boolean> {
    try {
      const response = await departmentApi.getDepartmentsFlat(params)
      if (response.success && response.data) {
        flatDepartments.value = response.data
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 부서 생성
   */
  async function createDepartment(data: DepartmentCreateRequest): Promise<Department | null> {
    loading.value = true
    error.value = null

    try {
      const response = await departmentApi.createDepartment(data)
      if (response.success && response.data) {
        // 목록 새로고침
        await fetchDepartments()
        await fetchFlatDepartments()
        return response.data
      }
      error.value = response.message || '부서 생성에 실패했습니다.'
      return null
    } catch (e) {
      error.value = '부서 생성에 실패했습니다.'
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 부서 수정
   */
  async function updateDepartment(departmentId: number, data: DepartmentUpdateRequest): Promise<boolean> {
    error.value = null

    try {
      // 현재 부서의 상위 부서 ID 저장 (변경 감지용)
      const currentDept = findDepartmentInTree(departments.value, departmentId)
      const previousParentId = currentDept?.parentId

      const response = await departmentApi.updateDepartment(departmentId, data)
      if (response.success && response.data) {
        // 상위 부서가 변경되면 전체 트리 새로고침
        if (data.parentId !== previousParentId) {
          await fetchDepartments()
        } else {
          // 트리 업데이트
          updateDepartmentInTree(departments.value, departmentId, response.data)
        }
        // 선택된 부서 업데이트
        if (selectedDepartment.value?.departmentId === departmentId) {
          selectedDepartment.value = response.data
        }
        // 평면 목록도 새로고침
        await fetchFlatDepartments()
        return true
      }
      error.value = response.message || '부서 수정에 실패했습니다.'
      return false
    } catch (e) {
      error.value = '부서 수정에 실패했습니다.'
      return false
    }
  }

  /**
   * 부서 삭제
   */
  async function deleteDepartment(departmentId: number): Promise<boolean> {
    error.value = null

    try {
      const response = await departmentApi.deleteDepartment(departmentId)
      if (response.success) {
        // 트리에서 제거
        departments.value = removeDepartmentFromTree(departments.value, departmentId)
        // 선택 해제
        if (selectedDepartment.value?.departmentId === departmentId) {
          selectedDepartment.value = null
          departmentUsers.value = []
        }
        // 평면 목록도 새로고침
        await fetchFlatDepartments()
        return true
      }
      error.value = response.message || '부서 삭제에 실패했습니다.'
      return false
    } catch (e) {
      error.value = '부서 삭제에 실패했습니다.'
      return false
    }
  }

  /**
   * 부서 순서 변경
   */
  async function updateDepartmentOrder(departmentId: number, sortOrder: number): Promise<boolean> {
    try {
      const response = await departmentApi.updateDepartmentOrder(departmentId, sortOrder)
      if (response.success) {
        // 목록 새로고침
        await fetchDepartments()
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 부서별 사용자 목록 조회
   */
  async function fetchDepartmentUsers(departmentId: number): Promise<boolean> {
    try {
      const response = await departmentApi.getDepartmentUsers(departmentId)
      if (response.success && response.data) {
        departmentUsers.value = response.data
        return true
      }
      return false
    } catch (e) {
      return false
    }
  }

  // ==================== Tree Node Functions ====================
  function toggleNode(departmentId: number) {
    if (expandedNodes.value.has(departmentId)) {
      expandedNodes.value.delete(departmentId)
    } else {
      expandedNodes.value.add(departmentId)
    }
  }

  function expandNode(departmentId: number) {
    expandedNodes.value.add(departmentId)
  }

  function collapseNode(departmentId: number) {
    expandedNodes.value.delete(departmentId)
  }

  function expandAllNodes() {
    const collectIds = (depts: Department[]) => {
      for (const dept of depts) {
        expandedNodes.value.add(dept.departmentId)
        if (dept.children?.length) {
          collectIds(dept.children)
        }
      }
    }
    collectIds(departments.value)
  }

  function collapseAllNodes() {
    expandedNodes.value.clear()
  }

  function isExpanded(departmentId: number): boolean {
    return expandedNodes.value.has(departmentId)
  }

  // ==================== Selection Functions ====================
  function selectDepartment(department: Department | null) {
    selectedDepartment.value = department
    if (department) {
      fetchDepartmentUsers(department.departmentId)
    } else {
      departmentUsers.value = []
    }
  }

  function getDepartmentById(departmentId: number): Department | null {
    return findDepartmentInTree(departments.value, departmentId)
  }

  function clearSelection() {
    selectedDepartment.value = null
    departmentUsers.value = []
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    departments,
    flatDepartments,
    selectedDepartment,
    departmentUsers,
    loading,
    error,
    expandedNodes,
    // Getters
    activeDepartments,
    activeFlatDepartments,
    selectedDepartmentId,
    // Actions
    fetchDepartments,
    fetchFlatDepartments,
    createDepartment,
    updateDepartment,
    deleteDepartment,
    updateDepartmentOrder,
    fetchDepartmentUsers,
    // Tree
    toggleNode,
    expandNode,
    collapseNode,
    expandAllNodes,
    collapseAllNodes,
    isExpanded,
    // Selection
    selectDepartment,
    getDepartmentById,
    clearSelection,
    clearError
  }
})
