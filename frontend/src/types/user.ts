/**
 * 사용자 타입 정의
 */

export interface User {
  userId: number
  username: string
  name?: string  // 백엔드에서 'name'으로 반환
  userName?: string  // API 클라이언트에서 name을 userName으로 복사
  email?: string  // 이메일 주소
  departmentId?: number
  departmentName?: string
  departmentCode?: string
  groupIds?: number[]
  groups?: UserGroup[]
  useYn: string
  lastLoginAt?: string  // 마지막 로그인 일시
  createdAt?: string
  createdBy?: number
  updatedAt?: string
  updatedBy?: number
}

export interface UserGroup {
  groupId: number
  groupName: string
  groupColor?: string
}

export interface UserCreateRequest {
  username: string
  password: string
  passwordConfirm: string
  userName: string
  email?: string  // 이메일 주소
  departmentId?: number
  groupIds?: number[]
}

export interface UserUpdateRequest {
  userName?: string
  email?: string  // 이메일 주소
  password?: string
  departmentId?: number
  groupIds?: number[]
  useYn?: string
}

export interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken?: string
  user: User
}

export interface TokenRefreshResponse {
  accessToken: string
}
