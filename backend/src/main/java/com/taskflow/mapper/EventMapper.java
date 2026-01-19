package com.taskflow.mapper;

import com.taskflow.domain.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 이벤트 Mapper
 */
@Mapper
public interface EventMapper {

    /**
     * 이벤트 생성
     */
    void insert(Event event);

    /**
     * 이벤트 수정
     */
    int update(Event event);

    /**
     * 이벤트 삭제 (논리 삭제 - STATUS 변경)
     */
    int delete(@Param("eventId") Long eventId, @Param("username") String username);

    /**
     * 이벤트 조회
     */
    Optional<Event> findById(@Param("eventId") Long eventId);

    /**
     * 이벤트 조회 (공유 정보 포함)
     */
    Optional<Event> findByIdWithShare(@Param("eventId") Long eventId, @Param("username") String username);

    /**
     * 기간별 이벤트 조회 (내 이벤트 + 공유받은 이벤트)
     */
    List<Event> selectByDateRange(
            @Param("username") String username,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("calendarId") Long calendarId,
            @Param("includeShared") Boolean includeShared
    );

    /**
     * 특정 캘린더의 이벤트 목록
     */
    List<Event> selectByCalendarId(@Param("calendarId") Long calendarId);

    /**
     * 반복 이벤트의 자식 이벤트 조회
     */
    List<Event> selectByParentEventId(@Param("parentEventId") Long parentEventId);

    /**
     * 이벤트 소유자 변경 (이관)
     */
    int transferOwner(@Param("eventId") Long eventId, @Param("newOwnerUsername") String newOwnerUsername,
                      @Param("newCalendarId") Long newCalendarId, @Param("updatedBy") String updatedBy);

    /**
     * 이벤트 접근 권한 확인
     */
    boolean hasAccess(@Param("eventId") Long eventId, @Param("username") String username);

    /**
     * 이벤트 권한 조회 (OWNER, VIEW, EDIT)
     */
    String getPermission(@Param("eventId") Long eventId, @Param("username") String username);

    /**
     * 이벤트 상태 변경
     */
    int updateStatus(@Param("eventId") Long eventId, @Param("status") String status, @Param("updatedBy") String updatedBy);

    /**
     * 반복 예외 날짜 추가
     */
    int addExceptionDate(@Param("eventId") Long eventId, @Param("exceptionDate") String exceptionDate, @Param("updatedBy") String updatedBy);

    /**
     * 캘린더 내 이벤트 수
     */
    int countByCalendarId(@Param("calendarId") Long calendarId);
}
