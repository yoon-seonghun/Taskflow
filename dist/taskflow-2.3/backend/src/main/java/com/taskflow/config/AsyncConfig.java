package com.taskflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 비동기 및 스케줄링 설정
 *
 * - @Async 어노테이션 활성화 (SSE 이벤트 비동기 발행)
 * - @Scheduled 어노테이션 활성화 (SSE 하트비트)
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
