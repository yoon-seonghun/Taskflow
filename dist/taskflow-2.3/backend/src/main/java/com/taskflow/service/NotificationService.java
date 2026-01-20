package com.taskflow.service;

import com.taskflow.common.PageResponse;
import com.taskflow.domain.Notification;
import com.taskflow.dto.notification.NotificationResponse;

/**
 * 알림 서비스 인터페이스
 */
public interface NotificationService {

    /**
     * 알림 생성
     */
    Notification createNotification(String username, String notificationType,
                                     String title, String message,
                                     String relatedType, Long relatedId, String relatedUrl,
                                     String createdBy);

    /**
     * 알림 목록 조회 (페이징)
     */
    PageResponse<NotificationResponse> getNotifications(String username, boolean unreadOnly, int page, int size);

    /**
     * 읽지 않은 알림 수 조회
     */
    int getUnreadCount(String username);

    /**
     * 알림 읽음 처리
     */
    NotificationResponse markAsRead(Long notificationId);

    /**
     * 전체 읽음 처리
     */
    int markAllAsRead(String username);

    /**
     * 업무 배당 알림 생성
     */
    void sendItemAssignedNotification(Long itemId, String itemTitle, String assigneeUsername,
                                       String assignedBy, String boardId);

    /**
     * 업무 공유 알림 생성
     */
    void sendItemSharedNotification(Long itemId, String itemTitle, String sharedToUsername,
                                     String sharedBy, String boardId);
}
