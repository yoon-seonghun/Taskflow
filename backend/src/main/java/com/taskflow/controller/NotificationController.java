package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.common.PageResponse;
import com.taskflow.dto.notification.NotificationResponse;
import com.taskflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 알림 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 내 알림 목록 조회
     */
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        PageResponse<NotificationResponse> notifications = notificationService.getNotifications(
                userDetails.getUsername(), unreadOnly, page, size
        );
        return ApiResponse.success(notifications);
    }

    /**
     * 읽지 않은 알림 수 조회
     */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Integer>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        int count = notificationService.getUnreadCount(userDetails.getUsername());
        return ApiResponse.success(Map.of("count", count));
    }

    /**
     * 알림 읽음 처리
     */
    @PutMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @PathVariable("id") Long notificationId
    ) {
        NotificationResponse notification = notificationService.markAsRead(notificationId);
        return ApiResponse.success(notification);
    }

    /**
     * 전체 읽음 처리
     */
    @PutMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        int updatedCount = notificationService.markAllAsRead(userDetails.getUsername());
        return ApiResponse.success(Map.of("updatedCount", updatedCount));
    }
}
