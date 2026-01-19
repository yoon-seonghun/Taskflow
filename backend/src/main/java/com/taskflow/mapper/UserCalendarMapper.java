package com.taskflow.mapper;

import com.taskflow.domain.Calendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 캘린더 Mapper
 */
@Mapper
public interface UserCalendarMapper {

    /**
     * 캘린더 생성
     */
    void insert(Calendar calendar);

    /**
     * 캘린더 수정
     */
    int update(Calendar calendar);

    /**
     * 캘린더 삭제 (논리 삭제)
     */
    int delete(@Param("calendarId") Long calendarId, @Param("username") String username);

    /**
     * 캘린더 조회
     */
    Optional<Calendar> findById(@Param("calendarId") Long calendarId);

    /**
     * 캘린더 조회 (공유 정보 포함)
     */
    Optional<Calendar> findByIdWithShare(@Param("calendarId") Long calendarId, @Param("username") String username);

    /**
     * 내 캘린더 목록 조회 (공유받은 캘린더 포함)
     */
    List<Calendar> selectMyCalendars(@Param("username") String username);

    /**
     * 내 소유 캘린더 목록만 조회
     */
    List<Calendar> selectOwnedCalendars(@Param("username") String username);

    /**
     * 기본 캘린더 조회
     */
    Optional<Calendar> findDefaultCalendar(@Param("username") String username);

    /**
     * 기본 캘린더 설정
     */
    int setDefaultCalendar(@Param("calendarId") Long calendarId, @Param("username") String username);

    /**
     * 기존 기본 캘린더 해제
     */
    int clearDefaultCalendar(@Param("username") String username);

    /**
     * 캘린더 내 이벤트 수 조회
     */
    int countEvents(@Param("calendarId") Long calendarId);

    /**
     * 최대 정렬 순서 조회
     */
    Integer getMaxSortOrder(@Param("username") String username);

    /**
     * 캘린더 접근 권한 확인
     */
    boolean hasAccess(@Param("calendarId") Long calendarId, @Param("username") String username);

    /**
     * 캘린더 권한 조회 (OWNER, VIEW, EDIT)
     */
    String getPermission(@Param("calendarId") Long calendarId, @Param("username") String username);
}
