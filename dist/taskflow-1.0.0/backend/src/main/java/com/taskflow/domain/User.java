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
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * 사용자 ID (PK)
     */
    private Long userId;

    /**
     * 로그인 아이디
     */
    private String username;

    /**
     * 비밀번호 (BCrypt 암호화)
     */
    private String password;

    /**
     * 사용자 이름
     */
    private String name;

    /**
     * 소속 부서 ID (FK)
     */
    private Long departmentId;

    /**
     * 사용 여부 (Y/N)
     */
    private String useYn;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 ID
     */
    private Long createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 ID
     */
    private Long updatedBy;

    /**
     * 마지막 로그인 일시
     */
    private LocalDateTime lastLoginAt;

    // =============================================
    // 조인 필드 (Mapper에서 설정)
    // =============================================

    /**
     * 부서명 (조인)
     */
    private String departmentName;

    /**
     * 부서 코드 (조인)
     */
    private String departmentCode;

    // =============================================
    // 편의 메서드
    // =============================================

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
}
