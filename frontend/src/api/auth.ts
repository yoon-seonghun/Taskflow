import { post, get } from './client'
import type { LoginRequest, LoginResponse, User, TokenRefreshResponse } from '@/types/user'

/**
 * 인증 API
 */
export const authApi = {
  /**
   * 로그인
   * @param data 로그인 요청 (username, password)
   * @returns 액세스 토큰 및 사용자 정보
   */
  login(data: LoginRequest) {
    return post<LoginResponse>('/auth/login', data)
  },

  /**
   * 로그아웃
   * - 서버에서 Refresh Token 쿠키 삭제
   */
  logout() {
    return post<void>('/auth/logout')
  },

  /**
   * 토큰 갱신
   * - httpOnly Cookie의 Refresh Token을 사용하여 새 Access Token 발급
   * @returns 새로운 액세스 토큰
   */
  refresh() {
    return post<TokenRefreshResponse>('/auth/refresh')
  },

  /**
   * 현재 사용자 정보 조회
   * - Access Token으로 사용자 정보 조회
   * - 토큰 유효성 검증은 이 API의 응답으로 확인 (401 시 무효)
   */
  getCurrentUser() {
    return get<User>('/auth/me')
  }
}
