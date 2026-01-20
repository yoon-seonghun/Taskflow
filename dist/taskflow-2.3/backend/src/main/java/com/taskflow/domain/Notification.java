package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 알림 엔티티
 *
 * 테이블: TB_NOTIFICATION
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    /**
     * 알림 ID (PK)
     */
    private Long notificationId;

    /**
     * 수신자 USERNAME
     */
    private String username;

    /**
     * 알림 유형 (ITEM_ASSIGNED, ITEM_SHARED, ITEM_UPDATED, ITEM_COMPLETED)
     */
    private String notificationType;

    /**
     * 알림 제목
     */
    private String title;

    /**
     * 알림 내용
     */
    private String message;

    /**
     * 관련 대상 유형 (ITEM, BOARD 등)
     */
    private String relatedType;

    /**
     * 관련 대상 ID
     */
    private Long relatedId;

    /**
     * 관련 링크 URL
     */
    private String relatedUrl;

    /**
     * 읽음 여부
     */
    private Boolean isRead;

    /**
     * 읽은 시간
     */
    private LocalDateTime readAt;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 발신자 USERNAME
     */
    private String createdBy;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 발신자 이름
     */
    private String createdByName;

    // =============================================
    // 상수
    // =============================================

    public static final String TYPE_ITEM_ASSIGNED = "ITEM_ASSIGNED";
    public static final String TYPE_ITEM_SHARED = "ITEM_SHARED";
    public static final String TYPE_ITEM_UPDATED = "ITEM_UPDATED";
    public static final String TYPE_ITEM_COMPLETED = "ITEM_COMPLETED";

    public static final String RELATED_TYPE_ITEM = "ITEM";
    public static final String RELATED_TYPE_BOARD = "BOARD";

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * 읽지 않음 여부
     */
    public boolean isUnread() {
        return !Boolean.TRUE.equals(isRead);
    }
}
