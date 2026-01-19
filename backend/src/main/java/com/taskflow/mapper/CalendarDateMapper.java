package com.taskflow.mapper;

import com.taskflow.domain.CalendarDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 날짜 기준 테이블 Mapper
 */
@Mapper
public interface CalendarDateMapper {

    /**
     * 날짜 조회
     */
    CalendarDate findByDate(@Param("calDate") LocalDate calDate);

    /**
     * 기간 조회
     */
    List<CalendarDate> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 월간 조회
     */
    List<CalendarDate> findByMonth(
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * 연간 조회
     */
    List<CalendarDate> findByYear(@Param("year") int year);

    /**
     * 음력 날짜로 조회
     */
    List<CalendarDate> findByLunarDate(
            @Param("lunarYear") int lunarYear,
            @Param("lunarMonth") int lunarMonth,
            @Param("lunarDay") int lunarDay,
            @Param("isLeapMonth") Boolean isLeapMonth
    );

    /**
     * 공휴일 목록 조회
     */
    List<CalendarDate> findHolidays(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 근무일수 카운트
     */
    int countWorkdays(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 데이터 존재 여부 확인
     */
    boolean existsByDate(@Param("calDate") LocalDate calDate);

    /**
     * 연도 데이터 존재 여부 확인
     */
    int countByYear(@Param("year") int year);

    /**
     * 단건 저장
     */
    int insert(CalendarDate calendarDate);

    /**
     * 벌크 저장
     */
    int insertBatch(@Param("list") List<CalendarDate> calendarDates);

    /**
     * 공휴일 정보 업데이트
     */
    int updateHoliday(
            @Param("calDate") LocalDate calDate,
            @Param("isHoliday") boolean isHoliday,
            @Param("holidayName") String holidayName,
            @Param("holidayType") String holidayType,
            @Param("isWorkday") boolean isWorkday
    );

    /**
     * 연도별 삭제 (재생성용)
     */
    int deleteByYear(@Param("year") int year);

    /**
     * 최소/최대 연도 조회
     */
    Integer findMinYear();
    Integer findMaxYear();
}
