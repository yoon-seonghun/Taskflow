package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자-그룹 매핑 엔티티
 *
 * 테이블: TB_USER_GROUP
 *
 * USERNAME 기반 FK 참조 시스템:
 * - USERNAME: 사용자 USERNAME 참조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGroup {

    /**
     * 사용자-그룹 매핑 ID (PK)
     */
    private Long userGroupId;

    /**
     * 사용자 USERNAME (FK → TB_USER.USERNAME)
     */
    private String username;

    /**
     * 그룹 ID (FK)
     */
    private Long groupId;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 사용자 ID (Mapper JOIN 필드)
     */
    private Long userId;

    /**
     * 사용자명
     */
    private String userName;

    /**
     * 부서명 (Mapper JOIN 필드)
     */
    private String departmentName;

    /**
     * 그룹명
     */
    private String groupName;

    /**
     * 그룹 코드
     */
    private String groupCode;

    /**
     * 로그인 ID (Mapper JOIN 필드)
     */
    private String loginId;

    /**
     * 사용자 ID getter (Lombok 호환성)
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 사용자명 getter (Lombok 호환성)
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 부서명 getter (Lombok 호환성)
     */
    public String getDepartmentName() {
        return departmentName;
    }

    /**
     * USERNAME getter (Lombok 호환성)
     */
    public String getUsername() {
        return username;
    }

    /**
     * 사용자그룹ID getter (Lombok 호환성)
     */
    public Long getUserGroupId() {
        return userGroupId;
    }

    /**
     * 그룹ID getter (Lombok 호환성)
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * 그룹명 getter (Lombok 호환성)
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * 그룹코드 getter (Lombok 호환성)
     */
    public String getGroupCode() {
        return groupCode;
    }

    /**
     * 생성일시 getter (Lombok 호환성)
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
