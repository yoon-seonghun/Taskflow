package com.taskflow.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 아이템 이력 도메인
 *
 * 작업 처리 이력 (완료/삭제된 아이템)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemHistory {

    // =============================================
    // 작업 결과 상수
    // =============================================

    public static final String RESULT_COMPLETED = "COMPLETED";
    public static final String RESULT_DELETED = "DELETED";

    // =============================================
    // 필드
    // =============================================

    /**
     * 아이템 ID
     */
    private Long itemId;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 작업 내용 (제목)
     */
    private String title;

    /**
     * 작업 결과 (COMPLETED, DELETED)
     */
    private String result;

    /**
     * 작업자 ID (완료/삭제 처리한 사람)
     */
    private Long workerId;

    /**
     * 등록시간
     */
    private LocalDateTime createdAt;

    /**
     * 시작시간
     */
    private LocalDateTime startTime;

    /**
     * 완료시간
     */
    private LocalDateTime completedAt;

    /**
     * 수정시간
     */
    private LocalDateTime updatedAt;

    /**
     * 삭제시간
     */
    private LocalDateTime deletedAt;

    /**
     * 삭제 전 상태 (NOT_STARTED, IN_PROGRESS, PENDING)
     */
    private String previousStatus;

    // =============================================
    // 조인 필드
    // =============================================

    /**
     * 보드명
     */
    private String boardName;

    /**
     * 작업자 이름
     */
    private String workerName;

    /**
     * 등록자 이름
     */
    private String createdByName;
}
