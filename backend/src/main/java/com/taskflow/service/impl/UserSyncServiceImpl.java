package com.taskflow.service.impl;

import com.taskflow.domain.User;
import com.taskflow.dto.sync.SyncResult;
import com.taskflow.mapper.UserMapper;
import com.taskflow.mapper.external.ExternalUserMapper;
import com.taskflow.service.DepartmentSyncService;
import com.taskflow.service.PositionSyncService;
import com.taskflow.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 동기화 서비스 구현
 *
 * External 모드에서만 활성화
 *
 * USERNAME/DEPARTMENT_CODE 기반 FK 참조 시스템:
 * - USERNAME 기준 UPSERT 방식
 * - 외부/내부 ID 매핑 불필요 (USERNAME이 자연키)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "external"
)
public class UserSyncServiceImpl implements UserSyncService {

    private final UserMapper userMapper;
    private final ExternalUserMapper externalUserMapper;
    private final DepartmentSyncService departmentSyncService;
    private final PositionSyncService positionSyncService;

    @Override
    @Transactional
    public User syncUserOnLogin(String username) {
        log.debug("Syncing user on login: username={}", username);

        // 1. 외부 DB에서 사용자 정보 조회
        Optional<User> externalUserOpt = externalUserMapper.findByUsername(username);
        if (externalUserOpt.isEmpty()) {
            log.warn("External user not found: username={}", username);
            return null;
        }

        User externalUser = externalUserOpt.get();

        // 2. 부서 동기화 (DEPARTMENT_CODE 기준)
        if (externalUser.getDepartmentCode() != null) {
            departmentSyncService.syncDepartmentByCode(externalUser.getDepartmentCode());
        }

        // 3. 직급 동기화 (POSITION_CODE 기준)
        if (externalUser.getPositionCode() != null) {
            positionSyncService.syncPositionByCode(externalUser.getPositionCode());
        }

        // 4. UPSERT (USERNAME 기준)
        User user = User.builder()
                .username(externalUser.getUsername())
                .password(null) // External 모드에서는 비밀번호 저장 안 함
                .name(externalUser.getName())
                .email(externalUser.getEmail())
                .departmentCode(externalUser.getDepartmentCode())
                .positionCode(externalUser.getPositionCode())
                .role(externalUser.getRole()) // 외부 DB의 역할(ADMIN/USER) 동기화
                .headYn(externalUser.getHeadYn() != null ? externalUser.getHeadYn() : "N")
                .useYn(externalUser.getUseYn() != null ? externalUser.getUseYn() : "Y")
                .createdBy("SYSTEM")
                .build();

        userMapper.upsertByUsername(user);
        log.info("User synced on login: username={}, name={}, role={}", username, externalUser.getName(), externalUser.getRole());

        // 5. 동기화된 사용자 정보 반환
        return userMapper.findByUsername(username).orElse(null);
    }

    @Override
    @Transactional
    public SyncResult syncAllUsers() {
        LocalDateTime startedAt = LocalDateTime.now();
        log.info("Starting full user synchronization (UPSERT mode)...");

        int created = 0;
        int updated = 0;
        int deactivated = 0;

        try {
            // 1. 외부 DB에서 모든 활성 사용자 조회
            List<User> externalUsers = externalUserMapper.findAllActive();
            log.info("Found {} active users in external DB", externalUsers.size());

            if (externalUsers.isEmpty()) {
                return SyncResult.success("USER", 0, 0, 0, 0, startedAt);
            }

            // 2. 부서 전체 동기화 먼저 (부서 코드 참조를 위해)
            departmentSyncService.syncAllDepartments();

            // 3. 직급 전체 동기화 먼저 (직급 코드 참조를 위해)
            positionSyncService.syncAllPositions();

            // 4. 각 사용자 UPSERT
            for (User externalUser : externalUsers) {
                // 기존 데이터 존재 여부 확인 (통계용)
                boolean exists = userMapper.existsByUsername(externalUser.getUsername());

                User user = User.builder()
                        .username(externalUser.getUsername())
                        .password(null) // External 모드에서는 비밀번호 저장 안 함
                        .name(externalUser.getName())
                        .email(externalUser.getEmail())
                        .departmentCode(externalUser.getDepartmentCode())
                        .positionCode(externalUser.getPositionCode())
                        .role(externalUser.getRole()) // 외부 DB의 역할(ADMIN/USER) 동기화
                        .headYn(externalUser.getHeadYn() != null ? externalUser.getHeadYn() : "N")
                        .useYn(externalUser.getUseYn() != null ? externalUser.getUseYn() : "Y")
                        .createdBy("SYSTEM")
                        .build();

                userMapper.upsertByUsername(user);

                if (exists) {
                    updated++;
                } else {
                    created++;
                }

                log.debug("Synced user: {} ({}) role={}", externalUser.getUsername(), externalUser.getName(), externalUser.getRole());
            }

            // 5. 동기화 대상이 아닌 사용자 비활성화 (admin 제외)
            List<String> activeUsernames = externalUsers.stream()
                    .map(User::getUsername)
                    .collect(Collectors.toList());

            if (!activeUsernames.isEmpty()) {
                deactivated = userMapper.deactivateUsersNotIn(activeUsernames);
                if (deactivated > 0) {
                    log.info("Deactivated {} users not in external DB (admin excluded)", deactivated);
                }
            }

            log.info("User sync completed: created={}, updated={}, deactivated={}",
                    created, updated, deactivated);

            return SyncResult.success("USER", created, updated, deactivated, 0, startedAt);

        } catch (Exception e) {
            log.error("User sync failed", e);
            return SyncResult.failure("USER", e.getMessage(), startedAt);
        }
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.findByUsername(username).orElse(null);
    }
}
