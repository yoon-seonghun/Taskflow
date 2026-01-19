package com.taskflow.mapper;

import com.taskflow.domain.Item;
import com.taskflow.domain.PropertyDef;
import com.taskflow.domain.Todo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Calendar/Gantt Mapper
 */
@Mapper
public interface CalendarMapper {

    // ============================================
    // Calendar 조회
    // ============================================

    /**
     * 캘린더용 Todo 목록 조회 (마감일 기준)
     * @param ownerUserId 소유자 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return Todo 목록
     */
    List<Todo> selectTodosForCalendar(
            @Param("ownerUserId") Long ownerUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 캘린더용 Item 목록 조회 (마감일 기준)
     * - 소유 보드 + 공유 보드의 업무
     * @param username 사용자명
     * @param startDate 시작일
     * @param endDate 종료일
     * @param boardId 보드 ID (optional)
     * @return Item 목록
     */
    List<Item> selectItemsForCalendar(
            @Param("username") String username,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("boardId") Long boardId
    );

    // ============================================
    // Gantt 조회
    // ============================================

    /**
     * Gantt용 Item 목록 조회 (공유/배당 정보 포함)
     * - 소유 보드 + 공유 보드의 업무
     * - 하위 업무 포함 (depth순 정렬)
     * - 공유/배당 여부, 공유자/배당자 정보 포함
     * @param username 사용자명
     * @param boardId 보드 ID (optional)
     * @param startDate 시작일 (optional - CUSTOM 범위용)
     * @param endDate 종료일 (optional - CUSTOM 범위용)
     * @param includeCompleted 완료 포함 여부
     * @return Item 정보 Map 목록
     */
    List<Map<String, Object>> selectItemsForGantt(
            @Param("username") String username,
            @Param("boardId") Long boardId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("includeCompleted") Boolean includeCompleted
    );

    /**
     * 전체 업무의 날짜 범위 조회 (FULL 범위 계산용)
     * @param username 사용자명
     * @param boardId 보드 ID (optional)
     * @return 최소 요청일, 최대 마감일 (minDate, maxDate)
     */
    Map<String, Object> selectDateRangeForGantt(
            @Param("username") String username,
            @Param("boardId") Long boardId
    );

    // ============================================
    // 동적 속성 조회 (시작일/완료일)
    // ============================================

    /**
     * "시작일" 속성 정의 조회
     * @return 시작일 PropertyDef (없으면 null)
     */
    PropertyDef selectStartDateProperty();

    /**
     * "완료일" 속성 정의 조회
     * @return 완료일 PropertyDef (없으면 null)
     */
    PropertyDef selectCompletionDateProperty();

    /**
     * 업무별 시작일/완료일 속성값 조회
     * @param itemIds 업무 ID 목록
     * @param startDatePropertyId 시작일 속성 ID
     * @param completionDatePropertyId 완료일 속성 ID
     * @return 업무별 속성값 목록 (itemId, propertyId, valueDate)
     */
    List<Map<String, Object>> selectItemPropertyDates(
            @Param("itemIds") List<Long> itemIds,
            @Param("startDatePropertyId") Long startDatePropertyId,
            @Param("completionDatePropertyId") Long completionDatePropertyId
    );
}
