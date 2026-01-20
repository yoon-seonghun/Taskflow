package com.taskflow.service;

import com.taskflow.domain.CalendarDate;
import com.taskflow.dto.calendar.CalendarDateResponse;
import com.taskflow.mapper.CalendarDateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * 날짜 기준 테이블 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarDateService {

    private final CalendarDateMapper calendarDateMapper;
    private final LunarCalendarService lunarCalendarService;

    // 양력 고정 공휴일
    private static final Map<String, String> SOLAR_FIXED_HOLIDAYS = Map.of(
            "01-01", "신정",
            "03-01", "삼일절",
            "05-05", "어린이날",
            "06-06", "현충일",
            "08-15", "광복절",
            "10-03", "개천절",
            "10-09", "한글날",
            "12-25", "크리스마스"
    );

    // 음력 고정 공휴일 (음력월-일 -> 이름)
    private static final Map<String, String> LUNAR_FIXED_HOLIDAYS = Map.of(
            "01-01", "설날",
            "01-02", "설날 연휴",
            "04-08", "부처님오신날",
            "08-15", "추석"
    );

    // 설날/추석 전날도 연휴
    private static final Map<String, String> LUNAR_HOLIDAYS_WITH_EVE = Map.of(
            "12-30", "설날 연휴",  // 음력 12월 30일 (설 전날, 작은달인 경우 12월 29일)
            "08-14", "추석 연휴"   // 추석 전날
    );

    // 추석 다음날도 연휴
    private static final Map<String, String> LUNAR_HOLIDAYS_NEXT_DAY = Map.of(
            "08-16", "추석 연휴"
    );

    /**
     * 연도별 캘린더 생성
     */
    @Transactional
    public int generateYearCalendar(int year) {
        log.info("Generating calendar for year: {}", year);

        // 기존 데이터 확인
        int existingCount = calendarDateMapper.countByYear(year);
        if (existingCount > 0) {
            log.info("Year {} already has {} records, deleting...", year, existingCount);
            calendarDateMapper.deleteByYear(year);
        }

        List<CalendarDate> calendarDates = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 각 날짜별 데이터 생성
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            CalendarDate calendarDate = createCalendarDate(currentDate);
            calendarDates.add(calendarDate);
            currentDate = currentDate.plusDays(1);
        }

        // 공휴일 적용
        applyHolidays(calendarDates, year);

        // 대체공휴일 적용
        applySubstituteHolidays(calendarDates, year);

        // 근무일 설정
        applyWorkdays(calendarDates);

        // 배치 저장 (1000건씩)
        int totalInserted = 0;
        int batchSize = 1000;
        for (int i = 0; i < calendarDates.size(); i += batchSize) {
            int end = Math.min(i + batchSize, calendarDates.size());
            List<CalendarDate> batch = calendarDates.subList(i, end);
            calendarDateMapper.insertBatch(batch);
            totalInserted += batch.size();
        }

        log.info("Generated {} calendar dates for year {}", totalInserted, year);
        return totalInserted;
    }

    /**
     * 다년도 캘린더 생성 (초기 데이터)
     */
    @Transactional
    public Map<Integer, Integer> generateMultiYearCalendar(int startYear, int endYear) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int year = startYear; year <= endYear; year++) {
            int count = generateYearCalendar(year);
            result.put(year, count);
        }
        return result;
    }

    /**
     * 단일 날짜 데이터 생성
     */
    private CalendarDate createCalendarDate(LocalDate date) {
        // 양력 정보
        int solarYear = date.getYear();
        int solarMonth = date.getMonthValue();
        int solarDay = date.getDayOfMonth();

        // 요일 (1=일, 2=월, ..., 7=토)
        int dayOfWeek = date.getDayOfWeek().getValue() % 7 + 1;
        String dayOfWeekName = CalendarDate.getDayOfWeekName(dayOfWeek);

        // 주차 계산 (ISO 표준)
        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int weekOfYear = date.get(weekFields.weekOfYear());
        int weekOfMonth = date.get(weekFields.weekOfMonth());

        // 분기
        int quarter = (solarMonth - 1) / 3 + 1;

        // 음력 변환
        var lunarResult = lunarCalendarService.solarToLunar(date);
        int lunarYear = lunarResult.getLunarYear();
        int lunarMonth = lunarResult.getLunarMonth();
        int lunarDay = lunarResult.getLunarDay();
        boolean isLeapMonth = Boolean.TRUE.equals(lunarResult.getIsLeapMonth());

        // 음력 표시
        String lunarDisplay = (isLeapMonth ? "윤" : "") + lunarMonth + "." + lunarDay;

        return CalendarDate.builder()
                .calDate(date)
                .solarYear(solarYear)
                .solarMonth(solarMonth)
                .solarDay(solarDay)
                .dayOfWeek(dayOfWeek)
                .dayOfWeekName(dayOfWeekName)
                .weekOfYear(weekOfYear)
                .weekOfMonth(weekOfMonth)
                .quarter(quarter)
                .lunarYear(lunarYear)
                .lunarMonth(lunarMonth)
                .lunarDay(lunarDay)
                .isLeapMonth(isLeapMonth)
                .lunarDisplay(lunarDisplay)
                .isHoliday(false)
                .holidayName(null)
                .holidayType(null)
                .isWorkday(true)
                .build();
    }

    /**
     * 공휴일 적용
     */
    private void applyHolidays(List<CalendarDate> calendarDates, int year) {
        Map<LocalDate, CalendarDate> dateMap = new HashMap<>();
        for (CalendarDate cd : calendarDates) {
            dateMap.put(cd.getCalDate(), cd);
        }

        // 양력 고정 공휴일
        for (Map.Entry<String, String> entry : SOLAR_FIXED_HOLIDAYS.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            LocalDate holidayDate = LocalDate.of(year, month, day);

            CalendarDate cd = dateMap.get(holidayDate);
            if (cd != null) {
                cd.setIsHoliday(true);
                cd.setHolidayName(entry.getValue());
                cd.setHolidayType(CalendarDate.HOLIDAY_SOLAR_FIXED);
            }
        }

        // 음력 고정 공휴일
        applyLunarHolidays(dateMap, year, LUNAR_FIXED_HOLIDAYS, CalendarDate.HOLIDAY_LUNAR_FIXED);
        applyLunarHolidays(dateMap, year, LUNAR_HOLIDAYS_WITH_EVE, CalendarDate.HOLIDAY_LUNAR_FIXED);
        applyLunarHolidays(dateMap, year, LUNAR_HOLIDAYS_NEXT_DAY, CalendarDate.HOLIDAY_LUNAR_FIXED);

        // 설날 연휴 (음력 12/30 또는 12/29 - 작은달인 경우)
        applySeollalEve(dateMap, year);
    }

    /**
     * 음력 공휴일 적용
     */
    private void applyLunarHolidays(Map<LocalDate, CalendarDate> dateMap, int year,
                                     Map<String, String> holidays, String holidayType) {
        for (Map.Entry<String, String> entry : holidays.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int lunarMonth = Integer.parseInt(parts[0]);
            int lunarDay = Integer.parseInt(parts[1]);

            try {
                // 음력 -> 양력 변환
                var solarResult = lunarCalendarService.lunarToSolar(year, lunarMonth, lunarDay, false);
                LocalDate solarDate = solarResult.getSolarDate();

                // 변환된 양력 날짜가 해당 연도인 경우만 적용
                if (solarDate != null && solarDate.getYear() == year) {
                    CalendarDate cd = dateMap.get(solarDate);
                    if (cd != null && !cd.getIsHoliday()) {
                        cd.setIsHoliday(true);
                        cd.setHolidayName(entry.getValue());
                        cd.setHolidayType(holidayType);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to convert lunar date {}/{}/{}: {}", year, lunarMonth, lunarDay, e.getMessage());
            }
        }
    }

    /**
     * 설날 전날 (음력 12/30 또는 12/29) 적용
     */
    private void applySeollalEve(Map<LocalDate, CalendarDate> dateMap, int year) {
        try {
            // 설날은 음력 1월 1일, 전날은 그 전년도 음력 12월의 마지막 날
            // 해당 연도에서는 이전 해 음력 12월이 양력으로 떨어지는 날짜를 찾아야 함
            var seollal = lunarCalendarService.lunarToSolar(year, 1, 1, false);
            if (seollal != null && seollal.getSolarDate() != null) {
                LocalDate seollalDate = seollal.getSolarDate();
                LocalDate eveDate = seollalDate.minusDays(1);

                if (eveDate.getYear() == year) {
                    CalendarDate cd = dateMap.get(eveDate);
                    if (cd != null && !cd.getIsHoliday()) {
                        cd.setIsHoliday(true);
                        cd.setHolidayName("설날 연휴");
                        cd.setHolidayType(CalendarDate.HOLIDAY_LUNAR_FIXED);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to apply Seollal eve for year {}: {}", year, e.getMessage());
        }
    }

    /**
     * 대체공휴일 적용
     * - 설날/추석 연휴가 일요일과 겹치는 경우
     * - 어린이날이 토/일요일과 겹치는 경우
     */
    private void applySubstituteHolidays(List<CalendarDate> calendarDates, int year) {
        Map<LocalDate, CalendarDate> dateMap = new HashMap<>();
        for (CalendarDate cd : calendarDates) {
            dateMap.put(cd.getCalDate(), cd);
        }

        // 설날 연휴 대체공휴일
        applySeollalSubstitute(dateMap, year);

        // 추석 연휴 대체공휴일
        applyChuseokSubstitute(dateMap, year);

        // 어린이날 대체공휴일
        applyChildrensDaySubstitute(dateMap, year);

        // 광복절, 개천절, 한글날 대체공휴일 (2021년부터)
        if (year >= 2021) {
            applyNationalHolidaySubstitute(dateMap, year, 8, 15, "광복절");
            applyNationalHolidaySubstitute(dateMap, year, 10, 3, "개천절");
            applyNationalHolidaySubstitute(dateMap, year, 10, 9, "한글날");
        }
    }

    /**
     * 설날 대체공휴일
     */
    private void applySeollalSubstitute(Map<LocalDate, CalendarDate> dateMap, int year) {
        try {
            var seollal = lunarCalendarService.lunarToSolar(year, 1, 1, false);
            if (seollal == null || seollal.getSolarDate() == null) return;

            LocalDate seollalDate = seollal.getSolarDate();
            List<LocalDate> seollalDays = Arrays.asList(
                    seollalDate.minusDays(1),  // 전날
                    seollalDate,                // 설날
                    seollalDate.plusDays(1)     // 다음날
            );

            // 연휴 중 일요일과 겹치는 날이 있으면 다음 평일에 대체공휴일
            for (LocalDate day : seollalDays) {
                if (day.getDayOfWeek() == DayOfWeek.SUNDAY && day.getYear() == year) {
                    LocalDate substitute = findNextWorkday(dateMap, seollalDays.get(2).plusDays(1));
                    if (substitute != null && substitute.getYear() == year) {
                        CalendarDate cd = dateMap.get(substitute);
                        if (cd != null && !cd.getIsHoliday()) {
                            cd.setIsHoliday(true);
                            cd.setHolidayName("대체공휴일(설날)");
                            cd.setHolidayType(CalendarDate.HOLIDAY_SUBSTITUTE);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to apply Seollal substitute for year {}: {}", year, e.getMessage());
        }
    }

    /**
     * 추석 대체공휴일
     */
    private void applyChuseokSubstitute(Map<LocalDate, CalendarDate> dateMap, int year) {
        try {
            var chuseok = lunarCalendarService.lunarToSolar(year, 8, 15, false);
            if (chuseok == null || chuseok.getSolarDate() == null) return;

            LocalDate chuseokDate = chuseok.getSolarDate();
            List<LocalDate> chuseokDays = Arrays.asList(
                    chuseokDate.minusDays(1),  // 전날
                    chuseokDate,                // 추석
                    chuseokDate.plusDays(1)     // 다음날
            );

            // 연휴 중 일요일과 겹치는 날이 있으면 다음 평일에 대체공휴일
            for (LocalDate day : chuseokDays) {
                if (day.getDayOfWeek() == DayOfWeek.SUNDAY && day.getYear() == year) {
                    LocalDate substitute = findNextWorkday(dateMap, chuseokDays.get(2).plusDays(1));
                    if (substitute != null && substitute.getYear() == year) {
                        CalendarDate cd = dateMap.get(substitute);
                        if (cd != null && !cd.getIsHoliday()) {
                            cd.setIsHoliday(true);
                            cd.setHolidayName("대체공휴일(추석)");
                            cd.setHolidayType(CalendarDate.HOLIDAY_SUBSTITUTE);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to apply Chuseok substitute for year {}: {}", year, e.getMessage());
        }
    }

    /**
     * 어린이날 대체공휴일
     */
    private void applyChildrensDaySubstitute(Map<LocalDate, CalendarDate> dateMap, int year) {
        LocalDate childrensDay = LocalDate.of(year, 5, 5);
        DayOfWeek dow = childrensDay.getDayOfWeek();

        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            LocalDate substitute = findNextWorkday(dateMap, childrensDay.plusDays(1));
            if (substitute != null) {
                CalendarDate cd = dateMap.get(substitute);
                if (cd != null && !cd.getIsHoliday()) {
                    cd.setIsHoliday(true);
                    cd.setHolidayName("대체공휴일(어린이날)");
                    cd.setHolidayType(CalendarDate.HOLIDAY_SUBSTITUTE);
                }
            }
        }
    }

    /**
     * 국경일 대체공휴일 (광복절, 개천절, 한글날)
     */
    private void applyNationalHolidaySubstitute(Map<LocalDate, CalendarDate> dateMap, int year,
                                                 int month, int day, String holidayName) {
        LocalDate holiday = LocalDate.of(year, month, day);
        DayOfWeek dow = holiday.getDayOfWeek();

        if (dow == DayOfWeek.SUNDAY) {
            LocalDate substitute = findNextWorkday(dateMap, holiday.plusDays(1));
            if (substitute != null) {
                CalendarDate cd = dateMap.get(substitute);
                if (cd != null && !cd.getIsHoliday()) {
                    cd.setIsHoliday(true);
                    cd.setHolidayName("대체공휴일(" + holidayName + ")");
                    cd.setHolidayType(CalendarDate.HOLIDAY_SUBSTITUTE);
                }
            }
        }
    }

    /**
     * 다음 평일 찾기 (공휴일/주말 제외)
     */
    private LocalDate findNextWorkday(Map<LocalDate, CalendarDate> dateMap, LocalDate startDate) {
        LocalDate current = startDate;
        int maxDays = 10; // 무한루프 방지

        for (int i = 0; i < maxDays; i++) {
            CalendarDate cd = dateMap.get(current);
            if (cd != null) {
                boolean isWeekend = cd.getDayOfWeek() == CalendarDate.SUNDAY ||
                                   cd.getDayOfWeek() == CalendarDate.SATURDAY;
                if (!isWeekend && !cd.getIsHoliday()) {
                    return current;
                }
            }
            current = current.plusDays(1);
        }
        return null;
    }

    /**
     * 근무일 설정 (주말, 공휴일 제외)
     */
    private void applyWorkdays(List<CalendarDate> calendarDates) {
        for (CalendarDate cd : calendarDates) {
            boolean isWeekend = cd.getDayOfWeek() == CalendarDate.SUNDAY ||
                               cd.getDayOfWeek() == CalendarDate.SATURDAY;
            cd.setIsWorkday(!isWeekend && !cd.getIsHoliday());
        }
    }

    // ==================== 조회 API ====================

    /**
     * 날짜 조회
     */
    public CalendarDateResponse getByDate(LocalDate date) {
        CalendarDate calendarDate = calendarDateMapper.findByDate(date);
        if (calendarDate == null) {
            // 데이터가 없으면 동적 생성
            calendarDate = createCalendarDate(date);
        }
        return CalendarDateResponse.from(calendarDate);
    }

    /**
     * 기간 조회
     */
    public List<CalendarDateResponse> getByDateRange(LocalDate startDate, LocalDate endDate) {
        List<CalendarDate> calendarDates = calendarDateMapper.findByDateRange(startDate, endDate);
        return CalendarDateResponse.fromList(calendarDates);
    }

    /**
     * 월간 조회
     */
    public List<CalendarDateResponse> getByMonth(int year, int month) {
        List<CalendarDate> calendarDates = calendarDateMapper.findByMonth(year, month);
        return CalendarDateResponse.fromList(calendarDates);
    }

    /**
     * 연간 조회
     */
    public List<CalendarDateResponse> getByYear(int year) {
        List<CalendarDate> calendarDates = calendarDateMapper.findByYear(year);
        return CalendarDateResponse.fromList(calendarDates);
    }

    /**
     * 공휴일 목록 조회
     */
    public List<CalendarDateResponse> getHolidays(LocalDate startDate, LocalDate endDate) {
        List<CalendarDate> holidays = calendarDateMapper.findHolidays(startDate, endDate);
        return CalendarDateResponse.fromList(holidays);
    }

    /**
     * 근무일수 카운트
     */
    public int countWorkdays(LocalDate startDate, LocalDate endDate) {
        return calendarDateMapper.countWorkdays(startDate, endDate);
    }

    /**
     * 음력 날짜로 양력 조회
     */
    public List<CalendarDateResponse> getByLunarDate(int lunarYear, int lunarMonth, int lunarDay, Boolean isLeapMonth) {
        List<CalendarDate> calendarDates = calendarDateMapper.findByLunarDate(lunarYear, lunarMonth, lunarDay, isLeapMonth);
        return CalendarDateResponse.fromList(calendarDates);
    }

    /**
     * 데이터 존재 연도 범위 조회
     */
    public Map<String, Integer> getYearRange() {
        Map<String, Integer> range = new HashMap<>();
        range.put("minYear", calendarDateMapper.findMinYear());
        range.put("maxYear", calendarDateMapper.findMaxYear());
        return range;
    }

    /**
     * 연도 데이터 존재 여부
     */
    public boolean hasYearData(int year) {
        return calendarDateMapper.countByYear(year) > 0;
    }

    /**
     * 임시 공휴일 추가
     */
    @Transactional
    public void addTemporaryHoliday(LocalDate date, String holidayName) {
        calendarDateMapper.updateHoliday(date, true, holidayName, CalendarDate.HOLIDAY_TEMPORARY, false);
    }
}
