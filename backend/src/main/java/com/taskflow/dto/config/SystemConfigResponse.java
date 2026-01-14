package com.taskflow.dto.config;

import lombok.Builder;
import lombok.Getter;

/**
 * 시스템 설정 응답 DTO (사용자 관리 모드 등)
 */
@Getter
@Builder
public class SystemConfigResponse {

    /**
     * 사용자 관리 모드 (internal/external)
     */
    private String userManagementMode;

    /**
     * 사용자 CRUD 활성화 여부
     */
    private boolean userCrudEnabled;

    /**
     * 부서 CRUD 활성화 여부
     */
    private boolean departmentCrudEnabled;

    /**
     * 비밀번호 변경 활성화 여부
     */
    private boolean passwordChangeEnabled;

    /**
     * 직급 CRUD 활성화 여부
     */
    private boolean positionCrudEnabled;

    /**
     * 팀장 관리 활성화 여부
     */
    private boolean headManagementEnabled;
}
