package com.taskflow.mapper;

import com.taskflow.domain.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 알림 Mapper
 */
@Mapper
public interface NotificationMapper {

    /**
     * 알림 생성
     */
    int insert(Notification notification);

    /**
     * 알림 조회 (ID)
     */
    Notification selectById(@Param("notificationId") Long notificationId);

    /**
     * 사용자별 알림 목록 조회 (페이징)
     */
    List<Notification> selectByUsername(
            @Param("username") String username,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 사용자별 알림 총 개수
     */
    int countByUsername(
            @Param("username") String username,
            @Param("unreadOnly") boolean unreadOnly
    );

    /**
     * 읽지 않은 알림 수
     */
    int countUnread(@Param("username") String username);

    /**
     * 알림 읽음 처리
     */
    int markAsRead(@Param("notificationId") Long notificationId);

    /**
     * 사용자의 모든 알림 읽음 처리
     */
    int markAllAsRead(@Param("username") String username);

    /**
     * 알림 삭제
     */
    int delete(@Param("notificationId") Long notificationId);

    /**
     * 오래된 알림 삭제 (30일 이상)
     */
    int deleteOldNotifications(@Param("daysOld") int daysOld);
}
