package com.taskflow.service.impl;

import com.taskflow.common.PageResponse;
import com.taskflow.domain.Notification;
import com.taskflow.dto.notification.NotificationResponse;
import com.taskflow.mapper.NotificationMapper;
import com.taskflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public Notification createNotification(String username, String notificationType,
                                            String title, String message,
                                            String relatedType, Long relatedId, String relatedUrl,
                                            String createdBy) {
        Notification notification = Notification.builder()
                .username(username)
                .notificationType(notificationType)
                .title(title)
                .message(message)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .relatedUrl(relatedUrl)
                .isRead(false)
                .createdBy(createdBy)
                .build();

        notificationMapper.insert(notification);
        log.info("알림 생성 완료 - 수신자: {}, 유형: {}, 제목: {}", username, notificationType, title);

        return notification;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(String username, boolean unreadOnly, int page, int size) {
        int offset = page * size;

        List<Notification> notifications = notificationMapper.selectByUsername(username, unreadOnly, offset, size);
        int total = notificationMapper.countByUsername(username, unreadOnly);

        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(String username) {
        return notificationMapper.countUnread(username);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        notificationMapper.markAsRead(notificationId);
        Notification notification = notificationMapper.selectById(notificationId);
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(String username) {
        return notificationMapper.markAllAsRead(username);
    }

    @Override
    @Transactional
    public void sendItemAssignedNotification(Long itemId, String itemTitle, String assigneeUsername,
                                              String assignedBy, String boardId) {
        String title = "새로운 업무가 배당되었습니다";
        String message = String.format("'%s' 업무가 배당되었습니다.", itemTitle);
        String relatedUrl = String.format("/boards/%s/items/%d", boardId, itemId);

        createNotification(
                assigneeUsername,
                Notification.TYPE_ITEM_ASSIGNED,
                title,
                message,
                Notification.RELATED_TYPE_ITEM,
                itemId,
                relatedUrl,
                assignedBy
        );
    }

    @Override
    @Transactional
    public void sendItemSharedNotification(Long itemId, String itemTitle, String sharedToUsername,
                                            String sharedBy, String boardId) {
        String title = "업무가 공유되었습니다";
        String message = String.format("'%s' 업무가 공유되었습니다.", itemTitle);
        String relatedUrl = String.format("/boards/%s/items/%d", boardId, itemId);

        createNotification(
                sharedToUsername,
                Notification.TYPE_ITEM_SHARED,
                title,
                message,
                Notification.RELATED_TYPE_ITEM,
                itemId,
                relatedUrl,
                sharedBy
        );
    }
}
