package com.taskflow.service.impl;

import com.taskflow.common.LogMaskUtils;
import com.taskflow.common.PageResponse;
import com.taskflow.domain.User;
import com.taskflow.domain.UserGroup;
import com.taskflow.dto.user.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.UserGroupMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserGroupMapper userGroupMapper;
    private final PasswordEncoder passwordEncoder;

    // =============================================
    // 조회
    // =============================================

    @Override
    public UserResponse getUser(Long userId) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> BusinessException.userNotFound(userId));

        UserResponse response = UserResponse.from(user);

        // 그룹 정보 조회 및 설정
        List<UserGroup> userGroups = userGroupMapper.findByUserId(userId);
        if (!userGroups.isEmpty()) {
            List<Long> groupIds = userGroups.stream()
                    .map(UserGroup::getGroupId)
                    .collect(Collectors.toList());
            List<UserResponse.UserGroupInfo> groups = userGroups.stream()
                    .map(ug -> UserResponse.UserGroupInfo.builder()
                            .groupId(ug.getGroupId())
                            .groupCode(ug.getGroupCode())
                            .groupName(ug.getGroupName())
                            .build())
                    .collect(Collectors.toList());
            response.setGroupIds(groupIds);
            response.setGroups(groups);
        }

        return response;
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userMapper.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다: " + username));
        return UserResponse.from(user);
    }

    @Override
    public PageResponse<UserResponse> getUsers(UserSearchRequest request) {
        List<User> users = userMapper.findAll(request);
        long totalCount = userMapper.countAll(request);

        List<UserResponse> content = users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(content, request.getPage(), request.getSize(), totalCount);
    }

    @Override
    public List<UserResponse> getUsersByDepartment(Long departmentId) {
        return userMapper.findByDepartmentId(departmentId).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    // =============================================
    // 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request, Long createdBy) {
        log.info("Creating user: {}", LogMaskUtils.maskUsername(request.getUsername()));

        // 비밀번호 일치 확인
        if (!request.isPasswordMatched()) {
            throw BusinessException.invalidPassword("비밀번호가 일치하지 않습니다");
        }

        // 아이디 중복 확인
        if (userMapper.existsByUsername(request.getUsername())) {
            throw BusinessException.duplicateUsername(request.getUsername());
        }

        // 사용자 엔티티 생성
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .departmentId(request.getDepartmentId())
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        // 저장
        userMapper.insert(user);
        log.info("User created: userId={}, username={}", user.getUserId(), LogMaskUtils.maskUsername(user.getUsername()));

        // 그룹 매핑 추가
        saveUserGroups(user.getUserId(), request.getGroupIds(), createdBy);

        // 생성된 사용자 조회하여 반환
        return getUser(user.getUserId());
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request, Long updatedBy) {
        log.info("Updating user: userId={}", userId);

        // 사용자 존재 확인
        User user = userMapper.findById(userId)
                .orElseThrow(() -> BusinessException.userNotFound(userId));

        // 수정 정보 설정
        user.setName(request.getName());
        user.setDepartmentId(request.getDepartmentId());
        if (request.getUseYn() != null) {
            user.setUseYn(request.getUseYn());
        }
        user.setUpdatedBy(updatedBy);

        // 비밀번호 변경 (관리자가 직접 설정하는 경우)
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            userMapper.updatePassword(userId, encodedPassword, updatedBy);
            log.info("Password updated by admin for user: userId={}", userId);
        }

        // 저장
        userMapper.update(user);
        log.info("User updated: userId={}", userId);

        // 그룹 매핑 갱신 (기존 그룹 삭제 후 새 그룹 추가)
        if (request.getGroupIds() != null) {
            userGroupMapper.deleteByUserId(userId);
            saveUserGroups(userId, request.getGroupIds(), updatedBy);
        }

        // 수정된 사용자 조회하여 반환
        return getUser(userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request, Long updatedBy) {
        log.info("Changing password for user: userId={}", userId);

        // 새 비밀번호 일치 확인
        if (!request.isNewPasswordMatched()) {
            throw BusinessException.invalidPassword("새 비밀번호가 일치하지 않습니다");
        }

        // 사용자 조회
        User user = userMapper.findById(userId)
                .orElseThrow(() -> BusinessException.userNotFound(userId));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw BusinessException.invalidPassword("현재 비밀번호가 올바르지 않습니다");
        }

        // 새 비밀번호로 변경
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(userId, encodedPassword, updatedBy);

        log.info("Password changed for user: userId={}", userId);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user: userId={}", userId);

        // 사용자 존재 확인
        if (userMapper.findById(userId).isEmpty()) {
            throw BusinessException.userNotFound(userId);
        }

        // 그룹 매핑 삭제
        userGroupMapper.deleteByUserId(userId);

        // 삭제 (논리 삭제로 변경하려면 deactivate 사용)
        userMapper.delete(userId);
        log.info("User deleted: userId={}", userId);
    }

    // =============================================
    // 검증
    // =============================================

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }

    // =============================================
    // 내부 헬퍼 메서드
    // =============================================

    /**
     * 사용자-그룹 매핑 저장
     *
     * @param userId    사용자 ID
     * @param groupIds  그룹 ID 목록
     * @param createdBy 생성자 ID
     */
    private void saveUserGroups(Long userId, List<Long> groupIds, Long createdBy) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }

        for (Long groupId : groupIds) {
            UserGroup userGroup = UserGroup.builder()
                    .userId(userId)
                    .groupId(groupId)
                    .createdBy(createdBy)
                    .build();
            userGroupMapper.insert(userGroup);
        }
        log.debug("User groups saved: userId={}, groupCount={}", userId, groupIds.size());
    }
}
