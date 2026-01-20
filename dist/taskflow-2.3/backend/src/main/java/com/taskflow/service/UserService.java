package com.taskflow.service;

import com.taskflow.common.PageResponse;
import com.taskflow.domain.User;
import com.taskflow.dto.user.*;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 서비스 인터페이스
 *
 * 두 가지 모드 지원:
 * - internal: 기본 관리 모드 (CRUD 가능)
 * - external: 외부 DB 연동 모드 (조회만 가능)
 */
public interface UserService {

    // =============================================
    // 모드 확인
    // =============================================

    /**
     * CRUD 활성화 여부 확인
     * - internal 모드: true (기본)
     * - external 모드: false
     *
     * @return CRUD 활성화 여부
     */
    boolean isCrudEnabled();

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
     * @param departmentCode 부서 코드
     * @return 사용자 목록
     */
    List<UserResponse> getUsersByDepartment(String departmentCode);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 사용자 등록
     *
     * @param request 등록 요청
     * @param createdBy 생성자 Username
     * @return 생성된 사용자 응답
     */
    UserResponse createUser(UserCreateRequest request, String createdBy);

    /**
     * 사용자 정보 수정
     *
     * @param userId 사용자 ID
     * @param request 수정 요청
     * @param updatedBy 수정자 Username
     * @return 수정된 사용자 응답
     */
    UserResponse updateUser(Long userId, UserUpdateRequest request, String updatedBy);

    /**
     * 비밀번호 변경
     *
     * @param userId 사용자 ID
     * @param request 비밀번호 변경 요청
     * @param updatedBy 수정자 Username
     */
    void changePassword(Long userId, PasswordChangeRequest request, String updatedBy);

    /**
     * 사용자 삭제
     *
     * @param userId 사용자 ID
     */
    void deleteUser(Long userId);

    // =============================================
    // 팀장 관리
    // =============================================

    /**
     * 팀장 지정
     * - 해당 부서의 기존 팀장은 자동으로 해제됨
     *
     * @param username 사용자 Username
     * @param updatedBy 수정자 Username
     * @return 수정된 사용자 응답
     */
    UserResponse setHead(String username, String updatedBy);

    /**
     * 팀장 해제
     *
     * @param username 사용자 Username
     * @param updatedBy 수정자 Username
     * @return 수정된 사용자 응답
     */
    UserResponse unsetHead(String username, String updatedBy);

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

    // =============================================
    // 인증용 메서드
    // =============================================

    /**
     * 로그인 아이디로 사용자 조회 (인증용)
     * - 비밀번호 검증을 위해 User 도메인 객체 반환
     *
     * @param username 로그인 아이디
     * @return 사용자 (Optional)
     */
    Optional<User> findByUsernameForAuth(String username);

    /**
     * 사용자 ID로 조회 (인증용)
     * - 토큰 갱신 등에서 사용
     *
     * @param userId 사용자 ID
     * @return 사용자 (Optional)
     */
    Optional<User> findByIdForAuth(Long userId);
}
