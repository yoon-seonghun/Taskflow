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
 * 사용자와 그룹의 N:M 관계를 매핑
 * - 1인이 여러 그룹에 소속 가능
 * - 1개 그룹에 여러 사용자 소속 가능
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
     * 사용자 ID (FK)
     */
    private Long userId;

    /**
     * 그룹 ID (FK)
     */
    private Long groupId;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 ID
     */
    private Long createdBy;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 사용자명
     */
    private String userName;

    /**
     * 사용자 아이디
     */
    private String loginId;

    /**
     * 그룹명
     */
    private String groupName;

    /**
     * 그룹 코드
     */
    private String groupCode;
}
