/**
 * 부서 타입 정의
 */

export interface Department {
  departmentId: number
  departmentCode: string
  departmentName: string
  parentCode?: string
  parentName?: string
  sortOrder: number
  useYn: string
  depth: number
  userCount?: number
  children?: Department[]
  createdAt: string
  createdBy: string
  updatedAt?: string
  updatedBy?: string
}

export interface DepartmentCreateRequest {
  departmentCode: string
  departmentName: string
  parentCode?: string
  sortOrder?: number
}

export interface DepartmentUpdateRequest {
  departmentName?: string
  parentCode?: string
  sortOrder?: number
  useYn?: string
}

export interface DepartmentOrderRequest {
  sortOrder: number
}
