package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 통합 감사 로그 엔티티
 *
 * 테이블: TB_AUDIT_LOG
 *
 * 보드, 업무, 공유 등 모든 변경 이력 기록
 * - 생성, 수정, 삭제, 이관, 공유/해제 작업 추적
 * - 변경 전/후 데이터 JSON으로 저장
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    /**
     * 로그 ID (PK)
     */
    private Long logId;

    /**
     * 대상 유형 (BOARD/ITEM/BOARD_SHARE/ITEM_SHARE)
     */
    private String targetType;

    /**
     * 대상 ID
     */
    private Long targetId;

    /**
     * 작업 유형 (CREATE/UPDATE/DELETE/TRANSFER/SHARE/UNSHARE)
     */
    private String action;

    /**
     * 수행자 ID (FK)
     */
    private Long actorId;

    /**
     * 변경 내용 설명
     */
    private String description;

    /**
     * 변경 전 데이터 (JSON)
     */
    private String beforeData;

    /**
     * 변경 후 데이터 (JSON)
     */
    private String afterData;

    /**
     * 관련 사용자 ID (이관/공유 대상)
     */
    private Long relatedUserId;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 대상 이름 (보드명/업무 제목 등)
     */
    private String targetName;

    /**
     * 수행자명
     */
    private String actorName;

    /**
     * 관련 사용자명
     */
    private String relatedUserName;

    // =============================================
    // 대상 유형 상수
    // =============================================

    public static final String TARGET_BOARD = "BOARD";
    public static final String TARGET_ITEM = "ITEM";
    public static final String TARGET_BOARD_SHARE = "BOARD_SHARE";
    public static final String TARGET_ITEM_SHARE = "ITEM_SHARE";

    // =============================================
    // 작업 유형 상수
    // =============================================

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_TRANSFER = "TRANSFER";
    public static final String ACTION_SHARE = "SHARE";
    public static final String ACTION_UNSHARE = "UNSHARE";

    // =============================================
    // 빌더 헬퍼 메서드
    // =============================================

    /**
     * 보드 생성 로그
     */
    public static AuditLog boardCreated(Long boardId, Long actorId, String description) {
        return AuditLog.builder()
                .targetType(TARGET_BOARD)
                .targetId(boardId)
                .action(ACTION_CREATE)
                .actorId(actorId)
                .description(description)
                .build();
    }

    /**
     * 보드 수정 로그
     */
    public static AuditLog boardUpdated(Long boardId, Long actorId, String description,
                                         String beforeData, String afterData) {
        return AuditLog.builder()
                .targetType(TARGET_BOARD)
                .targetId(boardId)
                .action(ACTION_UPDATE)
                .actorId(actorId)
                .description(description)
                .beforeData(beforeData)
                .afterData(afterData)
                .build();
    }

    /**
     * 보드 삭제 로그
     */
    public static AuditLog boardDeleted(Long boardId, Long actorId, String description,
                                         String beforeData) {
        return AuditLog.builder()
                .targetType(TARGET_BOARD)
                .targetId(boardId)
                .action(ACTION_DELETE)
                .actorId(actorId)
                .description(description)
                .beforeData(beforeData)
                .build();
    }

    /**
     * 업무 이관 로그
     */
    public static AuditLog itemTransferred(Long itemId, Long actorId, Long relatedUserId,
                                            String description, String beforeData, String afterData) {
        return AuditLog.builder()
                .targetType(TARGET_ITEM)
                .targetId(itemId)
                .action(ACTION_TRANSFER)
                .actorId(actorId)
                .relatedUserId(relatedUserId)
                .description(description)
                .beforeData(beforeData)
                .afterData(afterData)
                .build();
    }

    /**
     * 공유 추가 로그
     */
    public static AuditLog shareAdded(String targetType, Long targetId, Long actorId,
                                       Long relatedUserId, String description) {
        return AuditLog.builder()
                .targetType(targetType)
                .targetId(targetId)
                .action(ACTION_SHARE)
                .actorId(actorId)
                .relatedUserId(relatedUserId)
                .description(description)
                .build();
    }

    /**
     * 공유 제거 로그
     */
    public static AuditLog shareRemoved(String targetType, Long targetId, Long actorId,
                                         Long relatedUserId, String description) {
        return AuditLog.builder()
                .targetType(targetType)
                .targetId(targetId)
                .action(ACTION_UNSHARE)
                .actorId(actorId)
                .relatedUserId(relatedUserId)
                .description(description)
                .build();
    }
}
