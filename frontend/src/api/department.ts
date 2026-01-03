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

  getDepartment(departmentCode: string) {
    return get<Department>(`/departments/${departmentCode}`)
  },

  createDepartment(data: DepartmentCreateRequest) {
    return post<Department>('/departments', data)
  },

  updateDepartment(departmentCode: string, data: DepartmentUpdateRequest) {
    return put<Department>(`/departments/${departmentCode}`, data)
  },

  deleteDepartment(departmentCode: string) {
    return del<void>(`/departments/${departmentCode}`)
  },

  updateDepartmentOrder(departmentCode: string, sortOrder: number) {
    return put<void>(`/departments/${departmentCode}/order`, { sortOrder })
  },

  // 부서별 사용자 목록 (departmentCode로 조회)
  getDepartmentUsers(departmentCode: string) {
    return get<User[]>(`/departments/${departmentCode}/users`)
  }
}
