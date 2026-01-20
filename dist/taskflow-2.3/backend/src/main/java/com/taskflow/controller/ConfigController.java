package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.config.UserManagementProperties;
import com.taskflow.dto.config.SystemConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시스템 설정 컨트롤러
 *
 * API:
 * - GET /api/config/system - 시스템 설정 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final UserManagementProperties userManagementProperties;

    /**
     * 시스템 설정 조회
     *
     * 프론트엔드에서 UI를 조건부로 렌더링하기 위한 설정 정보
     * - 사용자 관리 모드 (internal/external)
     * - CRUD 활성화 여부
     */
    @GetMapping("/system")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getSystemConfig() {
        log.debug("Get system config");

        SystemConfigResponse response = SystemConfigResponse.builder()
                .userManagementMode(userManagementProperties.getMode())
                .userCrudEnabled(userManagementProperties.isUserCrudEnabled())
                .departmentCrudEnabled(userManagementProperties.isDepartmentCrudEnabled())
                .passwordChangeEnabled(userManagementProperties.isInternalMode())
                .positionCrudEnabled(userManagementProperties.isPositionCrudEnabled())
                .headManagementEnabled(userManagementProperties.isInternalMode())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
