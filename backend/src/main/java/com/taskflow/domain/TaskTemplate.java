package com.taskflow.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 작업 템플릿 도메인
 *
 * 자주 사용하는 작업을 템플릿으로 등록하여 재사용
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
     * 기본 담당자 ID
     */
    private Long defaultAssigneeId;

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

    // =============================================
    // 조인 필드
    // =============================================

    /**
     * 생성자 이름
     */
    private String createdByName;

    /**
     * 수정자 이름
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
