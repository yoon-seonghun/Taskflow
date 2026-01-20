package com.taskflow.service.impl;

import com.taskflow.common.LogMaskUtils;
import com.taskflow.common.PageResponse;
import com.taskflow.config.UserManagementProperties;
import com.taskflow.domain.User;
import com.taskflow.domain.UserGroup;
import com.taskflow.dto.user.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.BoardMapper;
import com.taskflow.mapper.ItemMapper;
import com.taskflow.mapper.UserGroupMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 서비스 구현 (Internal 모드)
 *
 * 기본 관리 모드로 TaskFlow DB에서 사용자 데이터를 직접 관리
 * - 사용자 CRUD 가능
 * - BCrypt 비밀번호 인코딩
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "internal",
    matchIfMissing = true
)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserGroupMapper userGroupMapper;
    private final BoardMapper boardMapper;
    private final ItemMapper itemMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserManagementProperties userManagementProperties;

    // =============================================
    // 모드 확인
    // =============================================

    @Override
    public boolean isCrudEnabled() {
        return userManagementProperties.isUserCrudEnabled();
    }

    // =============================================
    // 조회
    // =============================================

    @Override
    public UserResponse getUser(Long userId) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> BusinessException.userNotFound(userId));

        UserResponse response = UserResponse.from(user);

        // 그룹 정보 조회 및 설정 (username 기준)
        List<UserGroup> userGroups = userGroupMapper.findByUsername(user.getUsername());
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
    public List<UserResponse> getUsersByDepartment(String departmentCode) {
        return userMapper.findByDepartmentCode(departmentCode).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    // =============================================
    // 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request, String createdBy) {
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
                .email(request.getEmail())
                .departmentCode(request.getDepartmentCode())
                .positionCode(request.getPositionCode())
                .role(User.normalizeRole(request.getRole()))
                .headYn("N")
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
    public UserResponse updateUser(Long userId, UserUpdateRequest request, String updatedBy) {
        log.info("Updating user: userId={}", userId);

        // 사용자 존재 확인
        User user = userMapper.findById(userId)
                .orElseThrow(() -> BusinessException.userNotFound(userId));

        // 수정 정보 설정
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setDepartmentCode(request.getDepartmentCode());
        user.setPositionCode(request.getPositionCode());
        if (request.getRole() != null) {
            user.setRole(User.normalizeRole(request.getRole()));
        }
        if (request.getHeadYn() != null) {
            user.setHeadYn(request.getHeadYn());
        }
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
            userGroupMapper.deleteByUsername(user.getUsername());
            saveUserGroups(userId, request.getGroupIds(), updatedBy);
        }

        // 수정된 사용자 조회하여 반환
        return getUser(userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request, String updatedBy) {
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
        User user = userMapper.findById(userId)
                .orElseThrow(() -> BusinessException.userNotFound(userId));

        // [v2.2.1] 소유 보드 확인 - 보드가 있으면 먼저 이관 필요
        int ownedBoardCount = boardMapper.countByOwnerUsername(user.getUsername());
        if (ownedBoardCount > 0) {
            throw BusinessException.badRequest(
                    String.format("사용자가 소유한 보드 %d개를 먼저 이관해야 합니다.", ownedBoardCount)
            );
        }

        // [v2.2.1] 소유 업무 확인 - 업무가 있으면 먼저 이관 필요
        int ownedItemCount = itemMapper.countByOwnerUsername(user.getUsername());
        if (ownedItemCount > 0) {
            throw BusinessException.badRequest(
                    String.format("사용자가 소유한 업무 %d개를 먼저 이관해야 합니다.", ownedItemCount)
            );
        }

        // 그룹 매핑 삭제 (username 기준)
        userGroupMapper.deleteByUsername(user.getUsername());

        // 삭제 (논리 삭제로 변경하려면 deactivate 사용)
        userMapper.delete(userId);
        log.info("User deleted: userId={}", userId);
    }

    // =============================================
    // 팀장 관리
    // =============================================

    @Override
    @Transactional
    public UserResponse setHead(String username, String updatedBy) {
        log.info("Setting user as head: username={}", LogMaskUtils.maskUsername(username));

        // CRUD 활성화 확인
        if (!isCrudEnabled()) {
            throw BusinessException.forbidden("External 모드에서는 팀장 지정이 불가합니다.");
        }

        // 사용자 존재 확인
        User user = userMapper.findByUsername(username)
                .orElseThrow(() -> BusinessException.userNotFound(username));

        // 부서 확인
        if (user.getDepartmentCode() == null) {
            throw BusinessException.badRequest("부서가 지정되지 않은 사용자는 팀장으로 지정할 수 없습니다.");
        }

        // 해당 부서의 기존 팀장 해제
        userMapper.clearDepartmentHead(user.getDepartmentCode(), updatedBy);

        // 팀장 지정
        userMapper.updateHeadYn(username, "Y", updatedBy);
        log.info("User set as head: username={}, departmentCode={}",
                LogMaskUtils.maskUsername(username), user.getDepartmentCode());

        return getUserByUsername(username);
    }

    @Override
    @Transactional
    public UserResponse unsetHead(String username, String updatedBy) {
        log.info("Unsetting user as head: username={}", LogMaskUtils.maskUsername(username));

        // CRUD 활성화 확인
        if (!isCrudEnabled()) {
            throw BusinessException.forbidden("External 모드에서는 팀장 해제가 불가합니다.");
        }

        // 사용자 존재 확인
        userMapper.findByUsername(username)
                .orElseThrow(() -> BusinessException.userNotFound(username));

        // 팀장 해제
        userMapper.updateHeadYn(username, "N", updatedBy);
        log.info("User unset as head: username={}", LogMaskUtils.maskUsername(username));

        return getUserByUsername(username);
    }

    // =============================================
    // 검증
    // =============================================

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }

    // =============================================
    // 인증용 메서드
    // =============================================

    @Override
    public java.util.Optional<User> findByUsernameForAuth(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public java.util.Optional<User> findByIdForAuth(Long userId) {
        return userMapper.findById(userId);
    }

    // =============================================
    // 내부 헬퍼 메서드
    // =============================================

    /**
     * 사용자-그룹 매핑 저장
     *
     * @param userId    사용자 ID
     * @param groupIds  그룹 ID 목록
     * @param createdBy 생성자 Username
     */
    private void saveUserGroups(Long userId, List<Long> groupIds, String createdBy) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }

        // userId로 username 조회
        User user = userMapper.findById(userId)
                .orElseThrow(() -> BusinessException.userNotFound(userId));

        for (Long groupId : groupIds) {
            UserGroup userGroup = UserGroup.builder()
                    .username(user.getUsername())
                    .groupId(groupId)
                    .createdBy(createdBy)
                    .build();
            userGroupMapper.insert(userGroup);
        }
        log.debug("User groups saved: userId={}, groupCount={}", userId, groupIds.size());
    }
}
