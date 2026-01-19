package com.taskflow.config;

import com.taskflow.service.CalendarDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 캘린더 날짜 데이터 초기화
 * 애플리케이션 시작 시 2020-2035년 데이터가 없으면 자동 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalendarDataInitializer implements CommandLineRunner {

    private final CalendarDateService calendarDateService;

    private static final int START_YEAR = 2020;
    private static final int END_YEAR = 2035;

    @Override
    public void run(String... args) {
        try {
            // 이미 데이터가 있는지 확인
            if (calendarDateService.hasYearData(START_YEAR)) {
                log.info("Calendar date data already exists, skipping initialization");
                return;
            }

            log.info("Initializing calendar date data for years {} to {}", START_YEAR, END_YEAR);

            Map<Integer, Integer> result = calendarDateService.generateMultiYearCalendar(START_YEAR, END_YEAR);

            int totalRecords = result.values().stream().mapToInt(Integer::intValue).sum();
            log.info("Calendar date initialization complete: {} years, {} total records", result.size(), totalRecords);

        } catch (Exception e) {
            log.error("Failed to initialize calendar date data", e);
        }
    }
}
