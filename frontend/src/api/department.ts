import { get, post, put, del } from './client'
import type {
  Department,
  DepartmentCreateRequest,
  DepartmentUpdateRequest
} from '@/types/department'
import type { User } from '@/types/user'

export const departmentApi = {
  // 트리 구조로 조회
  getDepartments(params?: { useYn?: string }) {
    return get<Department[]>('/departments', params)
  },

  // 평면 구조로 조회
  getDepartmentsFlat(params?: { useYn?: string }) {
    return get<Department[]>('/departments/flat', params)
  },

  getDepartment(departmentId: number) {
    return get<Department>(`/departments/${departmentId}`)
  },

  createDepartment(data: DepartmentCreateRequest) {
    return post<Department>('/departments', data)
  },

  updateDepartment(departmentId: number, data: DepartmentUpdateRequest) {
    return put<Department>(`/departments/${departmentId}`, data)
  },

  deleteDepartment(departmentId: number) {
    return del<void>(`/departments/${departmentId}`)
  },

  updateDepartmentOrder(departmentId: number, sortOrder: number) {
    return put<void>(`/departments/${departmentId}/order`, { sortOrder })
  },

  // 부서별 사용자 목록
  getDepartmentUsers(departmentId: number) {
    return get<User[]>(`/departments/${departmentId}/users`)
  }
}
