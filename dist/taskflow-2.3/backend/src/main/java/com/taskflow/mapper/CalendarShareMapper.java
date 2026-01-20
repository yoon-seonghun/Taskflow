package com.taskflow.mapper;

import com.taskflow.domain.CalendarShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 캘린더 공유 Mapper
 */
@Mapper
public interface CalendarShareMapper {

    /**
     * 공유 추가
     */
    void insert(CalendarShare share);

    /**
     * 공유 해제 (논리 삭제)
     */
    int delete(@Param("calendarId") Long calendarId, @Param("username") String username, @Param("deletedBy") String deletedBy);

    /**
     * 공유 타입 변경
     */
    int updateShareType(@Param("calendarId") Long calendarId, @Param("username") String username,
                        @Param("shareType") String shareType, @Param("updatedBy") String updatedBy);

    /**
     * 캘린더 공유 목록 조회
     */
    List<CalendarShare> selectByCalendarId(@Param("calendarId") Long calendarId);

    /**
     * 특정 사용자의 공유 정보 조회
     */
    Optional<CalendarShare> findByCalendarIdAndUsername(@Param("calendarId") Long calendarId, @Param("username") String username);

    /**
     * 공유 존재 여부 확인
     */
    boolean exists(@Param("calendarId") Long calendarId, @Param("username") String username);

    /**
     * 공유 권한 조회
     */
    String getPermission(@Param("calendarId") Long calendarId, @Param("username") String username);

    /**
     * 캘린더의 모든 공유 해제
     */
    int deleteAllByCalendarId(@Param("calendarId") Long calendarId, @Param("deletedBy") String deletedBy);
}
