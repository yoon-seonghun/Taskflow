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
 * USERNAME 기반 FK 참조 시스템:
 * - ACTOR_USERNAME: 수행자 USERNAME 참조
 * - RELATED_USERNAME: 관련 사용자 USERNAME 참조
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
     * 대상 이름 (보드명, 업무명 등)
     */
    private String targetName;

    /**
     * 작업 유형 (CREATE/UPDATE/DELETE/TRANSFER/SHARE/UNSHARE)
     */
    private String action;

    /**
     * 수행자 USERNAME (FK → TB_USER.USERNAME)
     */
    private String actorUsername;

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
     * 관련 사용자 USERNAME (이관/공유 대상)
     */
    private String relatedUsername;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 수행자명
     */
    private String actorName;

    /**
     * 관련 사용자명
     */
    private String relatedUserName;

    /**
     * 관련 사용자명 getter (Lombok 호환성)
     */
    public String getRelatedUserName() {
        return relatedUserName;
    }

    /**
     * 관련 사용자 USERNAME getter (Lombok 호환성)
     */
    public String getRelatedUsername() {
        return relatedUsername;
    }

    /**
     * 수행자 USERNAME getter (Lombok 호환성)
     */
    public String getActorUsername() {
        return actorUsername;
    }

    /**
     * 수행자명 getter (Lombok 호환성)
     */
    public String getActorName() {
        return actorName;
    }

    /**
     * 로그ID getter (Lombok 호환성)
     */
    public Long getLogId() {
        return logId;
    }

    /**
     * 대상유형 getter (Lombok 호환성)
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 대상ID getter (Lombok 호환성)
     */
    public Long getTargetId() {
        return targetId;
    }

    /**
     * 대상명 getter (Lombok 호환성)
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * 액션 getter (Lombok 호환성)
     */
    public String getAction() {
        return action;
    }

    /**
     * 설명 getter (Lombok 호환성)
     */
    public String getDescription() {
        return description;
    }

    /**
     * 생성일시 getter (Lombok 호환성)
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

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
    public static AuditLog boardCreated(Long boardId, String actorUsername, String description) {
        return AuditLog.builder()
                .targetType(TARGET_BOARD)
                .targetId(boardId)
                .action(ACTION_CREATE)
                .actorUsername(actorUsername)
                .description(description)
                .build();
    }

    /**
     * 보드 수정 로그
     */
    public static AuditLog boardUpdated(Long boardId, String actorUsername, String description,
                                         String beforeData, String afterData) {
        return AuditLog.builder()
                .targetType(TARGET_BOARD)
                .targetId(boardId)
                .action(ACTION_UPDATE)
                .actorUsername(actorUsername)
                .description(description)
                .beforeData(beforeData)
                .afterData(afterData)
                .build();
    }

    /**
     * 보드 삭제 로그
     */
    public static AuditLog boardDeleted(Long boardId, String actorUsername, String description,
                                         String beforeData) {
        return AuditLog.builder()
                .targetType(TARGET_BOARD)
                .targetId(boardId)
                .action(ACTION_DELETE)
                .actorUsername(actorUsername)
                .description(description)
                .beforeData(beforeData)
                .build();
    }

    /**
     * 업무 이관 로그
     */
    public static AuditLog itemTransferred(Long itemId, String actorUsername, String relatedUsername,
                                            String description, String beforeData, String afterData) {
        return AuditLog.builder()
                .targetType(TARGET_ITEM)
                .targetId(itemId)
                .action(ACTION_TRANSFER)
                .actorUsername(actorUsername)
                .relatedUsername(relatedUsername)
                .description(description)
                .beforeData(beforeData)
                .afterData(afterData)
                .build();
    }

    /**
     * 공유 추가 로그
     */
    public static AuditLog shareAdded(String targetType, Long targetId, String actorUsername,
                                       String relatedUsername, String description) {
        return AuditLog.builder()
                .targetType(targetType)
                .targetId(targetId)
                .action(ACTION_SHARE)
                .actorUsername(actorUsername)
                .relatedUsername(relatedUsername)
                .description(description)
                .build();
    }

    /**
     * 공유 제거 로그
     */
    public static AuditLog shareRemoved(String targetType, Long targetId, String actorUsername,
                                         String relatedUsername, String description) {
        return AuditLog.builder()
                .targetType(targetType)
                .targetId(targetId)
                .action(ACTION_UNSHARE)
                .actorUsername(actorUsername)
                .relatedUsername(relatedUsername)
                .description(description)
                .build();
    }
}
