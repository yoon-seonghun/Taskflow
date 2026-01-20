package com.taskflow.config;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 외부 DB Mapper 스캔 설정
 *
 * taskflow.user-management.mode=external 일 때만 활성화
 * 외부 DB용 Mapper는 com.taskflow.mapper.external 패키지에 위치
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "external"
)
@MapperScan(
    basePackages = "com.taskflow.mapper.external",
    sqlSessionFactoryRef = "externalSqlSessionFactory"
)
public class ExternalMapperConfig {

    public ExternalMapperConfig() {
        log.info("External Mapper Configuration initialized");
    }
}
