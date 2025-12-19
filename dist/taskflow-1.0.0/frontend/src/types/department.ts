/**
 * 부서 타입 정의
 */

export interface Department {
  departmentId: number
  departmentCode: string
  departmentName: string
  parentId?: number
  parentName?: string
  sortOrder: number
  useYn: string
  depth: number
  userCount?: number
  children?: Department[]
  createdAt: string
  createdBy: number
  updatedAt?: string
  updatedBy?: number
}

export interface DepartmentCreateRequest {
  departmentCode: string
  departmentName: string
  parentId?: number
  sortOrder?: number
}

export interface DepartmentUpdateRequest {
  departmentCode?: string
  departmentName?: string
  parentId?: number
  sortOrder?: number
  useYn?: string
}

export interface DepartmentOrderRequest {
  departmentId: number
  sortOrder: number
}
