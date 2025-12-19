import { get, post, put, del } from './client'
import type { User, UserCreateRequest, UserUpdateRequest, PasswordChangeRequest } from '@/types/user'
import type { PageResponse } from '@/types/api'

export const userApi = {
  getUsers(params?: { page?: number; size?: number; keyword?: string; departmentId?: number; useYn?: string }) {
    return get<PageResponse<User>>('/users', params)
  },

  getUser(userId: number) {
    return get<User>(`/users/${userId}`)
  },

  createUser(data: UserCreateRequest) {
    return post<User>('/users', data)
  },

  updateUser(userId: number, data: UserUpdateRequest) {
    return put<User>(`/users/${userId}`, data)
  },

  deleteUser(userId: number) {
    return del<void>(`/users/${userId}`)
  },

  changePassword(userId: number, data: PasswordChangeRequest) {
    return put<void>(`/users/${userId}/password`, data)
  }

  // 현재 사용자 정보는 authApi.getCurrentUser() 사용 (/auth/me)
}
