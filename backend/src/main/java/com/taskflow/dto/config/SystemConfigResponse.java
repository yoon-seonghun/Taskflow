package com.taskflow.dto.config;

import lombok.Builder;
import lombok.Getter;

/**
 * 시스템 설정 응답 DTO
 *
 * 프론트엔드에서 UI를 조건부로 렌더링하기 위한 설정 정보
 */
@Getter
@Builder
public class SystemConfigResponse {

    /**
     * 사용자 관리 모드
     * - internal: 기본 관리 모드
     * - external: 외부 DB 연동 모드
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
     * 비밀번호 변경 가능 여부
     */
    private boolean passwordChangeEnabled;

    /**
     * 직급 CRUD 활성화 여부
     */
    private boolean positionCrudEnabled;

    /**
     * 팀장 지정/해제 가능 여부
     * - internal 모드에서만 true
     */
    private boolean headManagementEnabled;
}
