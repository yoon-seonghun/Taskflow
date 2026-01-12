package com.taskflow.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 작업 템플릿 도메인
 *
 * 테이블: TB_TASK_TEMPLATE
 *
 * USERNAME 기반 FK 참조 시스템:
 * - DEFAULT_ASSIGNEE_USERNAME: 기본 담당자 USERNAME 참조
 * - CREATED_BY, UPDATED_BY: USERNAME 참조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplate {

    // =============================================
    // 상태 상수
    // =============================================

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    // =============================================
    // 필드
    // =============================================

    /**
     * 템플릿 ID (PK)
     */
    private Long templateId;

    /**
     * 작업 내용 (템플릿 제목)
     */
    private String content;

    /**
     * 기본 담당자 USERNAME (FK → TB_USER.USERNAME)
     */
    private String defaultAssigneeUsername;

    /**
     * 기본 업무 상태 (NOT_STARTED, IN_PROGRESS, PENDING)
     */
    private String defaultItemStatus;

    /**
     * 상태 (ACTIVE, INACTIVE)
     */
    private String status;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 횟수
     */
    private Integer useCount;

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

    // =============================================
    // 조인 필드
    // =============================================

    /**
     * 생성자 이름 (조인)
     */
    private String createdByName;

    /**
     * 수정자 이름 (조인)
     */
    private String updatedByName;

    /**
     * 기본 담당자 이름 (조인)
     */
    private String defaultAssigneeName;

    // =============================================
    // 헬퍼 메서드
    // =============================================

    /**
     * 활성 상태 여부
     */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(this.status);
    }
}
