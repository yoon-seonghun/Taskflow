package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 *
 * 테이블: TB_USER
 *
 * USERNAME 기반 FK 참조 시스템:
 * - USER_ID: JWT 토큰용 (내부 식별자)
 * - USERNAME: FK 참조 키 (불변)
 * - DEPARTMENT_CODE: 부서 참조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * 사용자 ID (PK) - JWT 토큰용
     */
    private Long userId;

    /**
     * 로그인 아이디 - FK 참조 키 (불변)
     */
    private String username;

    /**
     * 비밀번호 (BCrypt 암호화)
     * External 모드에서는 NULL 허용
     */
    private String password;

    /**
     * 사용자 이름
     */
    private String name;

    /**
     * 이메일 주소
     */
    private String email;

    /**
     * 소속 부서 코드 (FK → TB_DEPARTMENT.DEPARTMENT_CODE)
     */
    private String departmentCode;

    /**
     * 직급 코드 (FK → TB_POSITION.POSITION_CODE)
     */
    private String positionCode;

    /**
     * 권한 (ADMIN, MANAGER, USER, GUEST)
     * 대소문자 구분 없이 비교, DB 저장 시 대문자로 정규화
     */
    private String role;

    /**
     * 팀장 여부 (Y/N)
     */
    private String headYn;

    /**
     * 사용 여부 (Y/N)
     */
    private String useYn;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 USERNAME
     */
    private String updatedBy;

    /**
     * 삭제일시 (논리삭제)
     */
    private LocalDateTime deletedAt;

    /**
     * 삭제자 USERNAME
     */
    private String deletedBy;

    /**
     * 마지막 로그인 일시
     */
    private LocalDateTime lastLoginAt;

    /**
     * 마지막 동기화 일시 (External 모드)
     */
    private LocalDateTime lastSyncedAt;

    // =============================================
    // 조인 필드 (Mapper에서 설정)
    // =============================================

    /**
     * 부서명 (조인)
     */
    private String departmentName;

    /**
     * 직급명 (조인)
     */
    private String positionName;

    /**
     * 직급 정렬 순서 (조인) - 낮을수록 높은 직급
     */
    private Integer positionSortOrder;

    // =============================================
    // 권한 상수
    // =============================================

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_GUEST = "GUEST";

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 권한 정규화 (대문자 변환, 유효하지 않으면 USER)
     */
    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_USER;
        }
        String upper = role.toUpperCase().trim();
        return switch (upper) {
            case "ADMIN" -> ROLE_ADMIN;
            case "MANAGER" -> ROLE_MANAGER;
            case "GUEST" -> ROLE_GUEST;
            default -> ROLE_USER;
        };
    }

    /**
     * 권한 확인 (대소문자 구분 없음)
     */
    public boolean hasRole(String checkRole) {
        if (checkRole == null || this.role == null) {
            return false;
        }
        return this.role.equalsIgnoreCase(checkRole);
    }

    /**
     * 관리자 여부
     */
    public boolean isAdmin() {
        return hasRole(ROLE_ADMIN);
    }

    /**
     * 매니저 이상 권한 여부
     */
    public boolean isManagerOrAbove() {
        return hasRole(ROLE_ADMIN) || hasRole(ROLE_MANAGER);
    }

    /**
     * 활성 사용자 여부
     */
    public boolean isActive() {
        return "Y".equals(useYn);
    }

    /**
     * 비활성화
     */
    public void deactivate() {
        this.useYn = "N";
    }

    /**
     * 활성화
     */
    public void activate() {
        this.useYn = "Y";
    }

    /**
     * 팀장 여부
     */
    public boolean isHead() {
        return "Y".equals(headYn);
    }

    /**
     * 팀장 지정
     */
    public void setAsHead() {
        this.headYn = "Y";
    }

    /**
     * 팀장 해제
     */
    public void unsetAsHead() {
        this.headYn = "N";
    }
}
