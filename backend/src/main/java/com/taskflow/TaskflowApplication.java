package com.taskflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Arrays;

/**
 * TaskFlow Application
 *
 * 실행 모드:
 * - Service Mode (기본): 웹 서버로 실행
 *   java -jar taskflow.jar
 *
 * - Sync Mode (CLI): 전체 동기화 후 종료
 *   java -jar taskflow.jar --sync
 */
@Slf4j
@SpringBootApplication
public class TaskflowApplication {

    public static void main(String[] args) {
        boolean isSyncMode = Arrays.asList(args).contains("--sync");

        SpringApplicationBuilder builder = new SpringApplicationBuilder(TaskflowApplication.class);

        if (isSyncMode) {
            // CLI 모드: 웹 서버 없이 실행
            log.info("Starting TaskFlow in SYNC MODE (CLI)");
            builder.web(WebApplicationType.NONE);
        } else {
            // Service 모드: 웹 서버 실행
            log.info("Starting TaskFlow in SERVICE MODE (Web)");
            builder.web(WebApplicationType.SERVLET);
        }

        builder.run(args);
    }
}
