package com.taskflow.dto.notification;

import com.taskflow.domain.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    /**
     * 알림 ID
     */
    private Long notificationId;

    /**
     * 알림 유형
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

    /**
     * 발신자 이름
     */
    private String createdByName;

    /**
     * Notification 엔티티로부터 생성
     */
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedType(notification.getRelatedType())
                .relatedId(notification.getRelatedId())
                .relatedUrl(notification.getRelatedUrl())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .createdBy(notification.getCreatedBy())
                .createdByName(notification.getCreatedByName())
                .build();
    }
}
