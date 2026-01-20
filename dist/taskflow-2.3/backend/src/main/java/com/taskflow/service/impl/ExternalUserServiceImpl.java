package com.taskflow.service.impl;

import com.taskflow.common.PageResponse;
import com.taskflow.config.UserManagementProperties;
import com.taskflow.domain.User;
import com.taskflow.dto.user.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.UserMapper;
import com.taskflow.mapper.external.ExternalUserMapper;
import com.taskflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 서비스 구현 (External 모드)
 *
 * 외부 DB 연동 모드로 읽기 전용
 * - 사용자 조회: 외부 DB에서 조회 (VIEW)
 * - CRUD 불가 (외부 시스템에서 관리)
 * - SHA256 비밀번호 인코딩 (MySQL sha2() 호환)
 * - 인증 후 Shadow User 생성/갱신 (AuthServiceImpl에서 처리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "external"
)
public class ExternalUserServiceImpl implements UserService {

    private final ExternalUserMapper externalUserMapper;
    private final UserMapper userMapper; // Shadow User 조회용
    private final UserManagementProperties userManagementProperties;

    // =============================================
    // 모드 확인
    // =============================================

    @Override
    public boolean isCrudEnabled() {
        return false; // 외부 모드는 항상 읽기 전용
    }

    // =============================================
    // 조회
    // =============================================

    @Override
    public UserResponse getUser(Long userId) {
        User user = externalUserMapper.findById(userId)
                .orElseThrow(() -> BusinessException.userNotFound(userId));
        return UserResponse.from(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = externalUserMapper.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다: " + username));
        return UserResponse.from(user);
    }

    @Override
    public PageResponse<UserResponse> getUsers(UserSearchRequest request) {
        List<User> users = externalUserMapper.findAll(request);
        long totalCount = externalUserMapper.countAll(request);

        List<UserResponse> content = users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(content, request.getPage(), request.getSize(), totalCount);
    }

    @Override
    public List<UserResponse> getUsersByDepartment(String departmentCode) {
        return externalUserMapper.findByDepartmentCode(departmentCode).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    // =============================================
    // 등록/수정/삭제 (읽기 전용으로 모두 예외 발생)
    // =============================================

    @Override
    public UserResponse createUser(UserCreateRequest request, String createdBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 사용자 등록이 불가능합니다.");
    }

    @Override
    public UserResponse updateUser(Long userId, UserUpdateRequest request, String updatedBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 사용자 수정이 불가능합니다.");
    }

    @Override
    public void changePassword(Long userId, PasswordChangeRequest request, String updatedBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 비밀번호 변경이 불가능합니다.");
    }

    @Override
    public void deleteUser(Long userId) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 사용자 삭제가 불가능합니다.");
    }

    // =============================================
    // 팀장 지정/해제 (읽기 전용으로 예외 발생)
    // =============================================

    @Override
    public UserResponse setHead(String username, String updatedBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 팀장 지정이 불가능합니다.");
    }

    @Override
    public UserResponse unsetHead(String username, String updatedBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 팀장 해제가 불가능합니다.");
    }

    // =============================================
    // 검증
    // =============================================

    @Override
    public boolean existsByUsername(String username) {
        return externalUserMapper.existsByUsername(username);
    }

    // =============================================
    // 인증용 메서드
    // =============================================

    /**
     * 로그인 아이디로 사용자 조회 (인증용)
     *
     * 외부 DB에서 조회하여 비밀번호 검증에 사용
     * 인증 성공 후 Shadow User 동기화는 AuthServiceImpl에서 처리
     */
    @Override
    public java.util.Optional<User> findByUsernameForAuth(String username) {
        return externalUserMapper.findByUsername(username);
    }

    /**
     * 사용자 ID로 조회 (인증용 - 토큰 갱신)
     *
     * JWT 토큰에 저장된 ID는 내부 Shadow User ID이므로
     * 내부 DB에서 조회
     */
    @Override
    public java.util.Optional<User> findByIdForAuth(Long userId) {
        // JWT에 저장된 userId는 내부 Shadow User의 ID
        return userMapper.findById(userId);
    }
}
