package com.taskflow.service;

import com.taskflow.common.PageResponse;
import com.taskflow.dto.user.*;

import java.util.List;

/**
 * 사용자 서비스 인터페이스
 */
public interface UserService {

    // =============================================
    // 조회
    // =============================================

    /**
     * 사용자 ID로 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 응답
     */
    UserResponse getUser(Long userId);

    /**
     * 로그인 아이디로 조회
     *
     * @param username 로그인 아이디
     * @return 사용자 응답
     */
    UserResponse getUserByUsername(String username);

    /**
     * 사용자 목록 조회 (페이징)
     *
     * @param request 검색 조건
     * @return 페이징 응답
     */
    PageResponse<UserResponse> getUsers(UserSearchRequest request);

    /**
     * 부서별 사용자 목록 조회
     *
     * @param departmentId 부서 ID
     * @return 사용자 목록
     */
    List<UserResponse> getUsersByDepartment(Long departmentId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 사용자 등록
     *
     * @param request 등록 요청
     * @param createdBy 생성자 ID
     * @return 생성된 사용자 응답
     */
    UserResponse createUser(UserCreateRequest request, Long createdBy);

    /**
     * 사용자 정보 수정
     *
     * @param userId 사용자 ID
     * @param request 수정 요청
     * @param updatedBy 수정자 ID
     * @return 수정된 사용자 응답
     */
    UserResponse updateUser(Long userId, UserUpdateRequest request, Long updatedBy);

    /**
     * 비밀번호 변경
     *
     * @param userId 사용자 ID
     * @param request 비밀번호 변경 요청
     * @param updatedBy 수정자 ID
     */
    void changePassword(Long userId, PasswordChangeRequest request, Long updatedBy);

    /**
     * 사용자 삭제
     *
     * @param userId 사용자 ID
     */
    void deleteUser(Long userId);

    // =============================================
    // 검증
    // =============================================

    /**
     * 아이디 중복 확인
     *
     * @param username 로그인 아이디
     * @return 중복 여부 (true: 중복)
     */
    boolean existsByUsername(String username);
}
