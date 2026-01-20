package com.taskflow.config;

import com.taskflow.service.DynamicDatasourceManager;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 외부 DataSource 설정
 *
 * 애플리케이션 종료 시 동적으로 생성된 DataSource 풀을 정리합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ExternalDatasourceConfig {

    private final DynamicDatasourceManager dynamicDatasourceManager;

    /**
     * 애플리케이션 종료 시 모든 외부 DataSource 종료
     */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down external DataSource pools...");
        dynamicDatasourceManager.removeAllDatasources();
        log.info("External DataSource pools shutdown completed");
    }
}
