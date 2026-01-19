package com.taskflow.service.impl;

import com.taskflow.domain.Item;
import com.taskflow.domain.PropertyDef;
import com.taskflow.domain.Todo;
import com.taskflow.dto.calendar.*;
import com.taskflow.mapper.CalendarMapper;
import com.taskflow.service.CalendarDateService;
import com.taskflow.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calendar/Gantt 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarServiceImpl implements CalendarService {

    private final CalendarMapper calendarMapper;
    private final CalendarDateService calendarDateService;

    @Override
    public CalendarResponse getCalendarData(
            String username,
            Long userId,
            int year,
            int month,
            boolean includeTodo,
            boolean includeItem,
            Long boardId
    ) {
        // 해당 월의 시작일과 종료일 계산
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 날짜별 이벤트 맵
        Map<String, List<CalendarEventResponse>> events = new HashMap<>();

        // Todo 조회
        if (includeTodo && userId != null) {
            List<Todo> todos = calendarMapper.selectTodosForCalendar(userId, startDate, endDate);
            log.debug("Calendar todos fetched: count={}, userId={}", todos.size(), userId);
            for (Todo todo : todos) {
                String dateKey = todo.getDueDate().toString();
                events.computeIfAbsent(dateKey, k -> new ArrayList<>())
                        .add(CalendarEventResponse.builder()
                                .type("TODO")
                                .id(todo.getTodoId())
                                .title(todo.getTitle())
                                .priority(todo.getPriority())
                                .isCompleted(todo.getIsCompleted())
                                .date(todo.getDueDate())
                                .build());
            }
        }

        // Item 조회
        if (includeItem) {
            List<Item> items = calendarMapper.selectItemsForCalendar(username, startDate, endDate, boardId);
            log.debug("Calendar items fetched: count={}, username={}, boardId={}", items.size(), username, boardId);
            for (Item item : items) {
                // 마감일 또는 요청일 사용 (마감일 우선)
                LocalDate displayDate = item.getDueDate() != null ? item.getDueDate() : item.getRequestDate();
                if (displayDate == null) continue;

                String dateKey = displayDate.toString();
                events.computeIfAbsent(dateKey, k -> new ArrayList<>())
                        .add(CalendarEventResponse.builder()
                                .type("ITEM")
                                .id(item.getItemId())
                                .title(item.getTitle())
                                .priority(item.getPriority())
                                .status(item.getStatus())
                                .boardId(item.getBoardId())
                                .boardName(item.getBoardName())
                                .date(displayDate)
                                .parentItemId(item.getParentItemId())
                                .itemDepth(item.getItemDepth())
                                .build());
            }
        }

        // 날짜 기준 정보 조회 (공휴일, 음력 등)
        List<CalendarDateResponse> calendarDates = calendarDateService.getByMonth(year, month);
        Map<String, CalendarDateResponse> dateInfo = new HashMap<>();
        for (CalendarDateResponse cd : calendarDates) {
            dateInfo.put(cd.getCalDate().toString(), cd);
        }

        return CalendarResponse.builder()
                .events(events)
                .dateInfo(dateInfo)
                .year(year)
                .month(month)
                .build();
    }

    @Override
    public GanttResponse getGanttData(
            String username,
            Long boardId,
            String range,
            String startDateStr,
            String endDateStr,
            boolean includeCompleted
    ) {
        // 날짜 범위 계산
        GanttDateRange dateRange = calculateDateRange(username, boardId, range, startDateStr, endDateStr);

        // 시작일/완료일 속성 정보 조회
        GanttPropertyInfo propertyInfo = getPropertyInfo();

        // 업무 목록 조회 (전체 범위 시 날짜 필터 없이 조회) - HashMap 반환
        boolean isFullRange = "FULL".equalsIgnoreCase(range);
        List<Map<String, Object>> itemMaps = calendarMapper.selectItemsForGantt(
                username, boardId,
                isFullRange ? null : dateRange.getStart(),
                isFullRange ? null : dateRange.getEnd(),
                includeCompleted
        );

        log.debug("Gantt items fetched: count={}, range={}, username={}, boardId={}",
                itemMaps.size(), range, username, boardId);

        // 시작일/완료일 동적 속성 값 조회
        Map<Long, LocalDate> startDates = new HashMap<>();
        Map<Long, LocalDate> completionDates = new HashMap<>();

        if (!itemMaps.isEmpty() && (propertyInfo.getHasStartDateProperty() || propertyInfo.getHasCompletionDateProperty())) {
            List<Long> itemIds = itemMaps.stream()
                    .map(m -> ((Number) m.get("itemId")).longValue())
                    .collect(Collectors.toList());

            Long startPropertyId = propertyInfo.getStartDatePropertyId();
            Long completionPropertyId = propertyInfo.getCompletionDatePropertyId();

            if (startPropertyId != null || completionPropertyId != null) {
                List<Map<String, Object>> propertyDates = calendarMapper.selectItemPropertyDates(
                        itemIds,
                        startPropertyId != null ? startPropertyId : -1L,
                        completionPropertyId != null ? completionPropertyId : -1L
                );

                for (Map<String, Object> row : propertyDates) {
                    Long itemId = ((Number) row.get("itemId")).longValue();
                    Long propertyId = ((Number) row.get("propertyId")).longValue();
                    Object valueDateObj = row.get("valueDate");

                    LocalDate valueDate = null;
                    if (valueDateObj instanceof LocalDate) {
                        valueDate = (LocalDate) valueDateObj;
                    } else if (valueDateObj instanceof java.sql.Date) {
                        valueDate = ((java.sql.Date) valueDateObj).toLocalDate();
                    }

                    if (valueDate != null) {
                        if (startPropertyId != null && propertyId.equals(startPropertyId)) {
                            startDates.put(itemId, valueDate);
                        } else if (completionPropertyId != null && propertyId.equals(completionPropertyId)) {
                            completionDates.put(itemId, valueDate);
                        }
                    }
                }
            }
        }

        // GanttItemResponse 변환
        List<GanttItemResponse> ganttItems = itemMaps.stream()
                .map(row -> {
                    Long itemId = ((Number) row.get("itemId")).longValue();
                    Long parentItemIdObj = row.get("parentItemId") != null ? ((Number) row.get("parentItemId")).longValue() : null;
                    Integer itemDepth = row.get("itemDepth") != null ? ((Number) row.get("itemDepth")).intValue() : 0;
                    Long boardIdVal = row.get("boardId") != null ? ((Number) row.get("boardId")).longValue() : null;

                    // 날짜 변환 헬퍼
                    LocalDate requestDate = toLocalDate(row.get("requestDate"));
                    LocalDate dueDate = toLocalDate(row.get("dueDate"));

                    // 공유/배당 플래그 변환
                    boolean isBoardShared = toBoolean(row.get("isBoardShared"));
                    boolean isAssignedToMe = toBoolean(row.get("isAssignedToMe"));

                    return GanttItemResponse.builder()
                            .itemId(itemId)
                            .title((String) row.get("title"))
                            .description((String) row.get("description"))
                            .parentItemId(parentItemIdObj)
                            .parentTitle((String) row.get("parentTitle"))
                            .itemDepth(itemDepth)
                            .requestDate(requestDate)
                            .dueDate(dueDate)
                            .startDate(startDates.get(itemId))
                            .completionDate(completionDates.get(itemId))
                            .hasStartDateProperty(propertyInfo.getHasStartDateProperty() && startDates.containsKey(itemId))
                            .hasCompletionDateProperty(propertyInfo.getHasCompletionDateProperty() && completionDates.containsKey(itemId))
                            .status((String) row.get("status"))
                            .priority((String) row.get("priority"))
                            .assigneeUserName((String) row.get("assigneeName"))
                            .boardId(boardIdVal)
                            .boardName((String) row.get("boardName"))
                            .boardOwnerName((String) row.get("boardOwnerName"))
                            .createdAt(toLocalDateTime(row.get("createdAt")))
                            // 공유/배당 정보
                            .isBoardShared(isBoardShared)
                            .isSharedToMe(isBoardShared)  // 보드 공유 = 공유받은 업무
                            .sharedByUserName((String) row.get("sharedByUserName"))
                            .isAssignedToMe(isAssignedToMe)
                            .assignedByUserName((String) row.get("assignedByUserName"))
                            .isItemShared(false)  // 업무 단위 공유는 별도 조회 필요 (현재 미지원)
                            .build();
                })
                .collect(Collectors.toList());

        return GanttResponse.builder()
                .dateRange(dateRange)
                .propertyInfo(propertyInfo)
                .items(ganttItems)
                .build();
    }

    /**
     * Object를 LocalDate로 변환
     */
    private LocalDate toLocalDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof LocalDate) return (LocalDate) obj;
        if (obj instanceof java.sql.Date) return ((java.sql.Date) obj).toLocalDate();
        return null;
    }

    /**
     * Object를 LocalDateTime으로 변환
     */
    private java.time.LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof java.time.LocalDateTime) return (java.time.LocalDateTime) obj;
        if (obj instanceof java.sql.Timestamp) return ((java.sql.Timestamp) obj).toLocalDateTime();
        return null;
    }

    /**
     * Object를 boolean으로 변환
     */
    private boolean toBoolean(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof Number) return ((Number) obj).intValue() == 1;
        return false;
    }

    /**
     * 시작일/완료일 속성 정보 조회
     */
    private GanttPropertyInfo getPropertyInfo() {
        PropertyDef startDateProp = calendarMapper.selectStartDateProperty();
        PropertyDef completionDateProp = calendarMapper.selectCompletionDateProperty();

        return GanttPropertyInfo.builder()
                .startDatePropertyId(startDateProp != null ? startDateProp.getPropertyId() : null)
                .completionDatePropertyId(completionDateProp != null ? completionDateProp.getPropertyId() : null)
                .hasStartDateProperty(startDateProp != null)
                .hasCompletionDateProperty(completionDateProp != null)
                .build();
    }

    /**
     * 날짜 범위 계산
     */
    private GanttDateRange calculateDateRange(
            String username,
            Long boardId,
            String range,
            String startDateStr,
            String endDateStr
    ) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;

        switch (range.toUpperCase()) {
            case "WEEK":
                start = today.minusWeeks(1);
                end = today.plusWeeks(1);
                break;
            case "MONTH":
                start = today.minusMonths(1);
                end = today.plusMonths(1);
                break;
            case "CUSTOM":
                start = LocalDate.parse(startDateStr);
                end = LocalDate.parse(endDateStr);
                break;
            case "FULL":
            default:
                // 전체 업무의 날짜 범위 조회
                Map<String, Object> dateRangeMap = calendarMapper.selectDateRangeForGantt(username, boardId);

                // null 체크 추가
                if (dateRangeMap == null) {
                    log.debug("No date range data found, using default range");
                    start = today.minusMonths(1);
                    end = today.plusMonths(1);
                    break;
                }

                Object minDateObj = dateRangeMap.get("minDate");
                Object maxDateObj = dateRangeMap.get("maxDate");

                if (minDateObj != null && maxDateObj != null) {
                    if (minDateObj instanceof LocalDate) {
                        start = (LocalDate) minDateObj;
                    } else if (minDateObj instanceof java.sql.Date) {
                        start = ((java.sql.Date) minDateObj).toLocalDate();
                    } else {
                        start = today.minusMonths(1);
                    }

                    if (maxDateObj instanceof LocalDate) {
                        end = (LocalDate) maxDateObj;
                    } else if (maxDateObj instanceof java.sql.Date) {
                        end = ((java.sql.Date) maxDateObj).toLocalDate();
                    } else {
                        end = today.plusMonths(1);
                    }
                } else {
                    // 데이터가 없으면 현재 월 기준
                    log.debug("Min/Max date is null, using default range");
                    start = today.minusMonths(1);
                    end = today.plusMonths(1);
                }
                break;
        }

        // 최소 범위 보장 (시작일이 종료일보다 뒤면 조정)
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        return GanttDateRange.builder()
                .start(start)
                .end(end)
                .build();
    }
}
