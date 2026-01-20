package com.taskflow.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 멀티 DataSource 설정
 *
 * 구성:
 * - Primary DataSource: TaskFlow 기본 DB (항상 사용)
 * - External DataSource: 외부 사용자/부서 DB (external 모드에서만 사용)
 *
 * 모드별 동작:
 * - internal: Primary DataSource만 사용
 * - external: Primary + External DataSource 모두 사용
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final UserManagementProperties userManagementProperties;

    // ==================== Primary DataSource (항상 활성화) ====================

    /**
     * Primary DataSource - TaskFlow 메인 DB
     * Spring Boot auto-configuration 대신 명시적으로 정의
     */
    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource primaryDataSource() {
        log.info("Initializing Primary DataSource for TaskFlow main database");
        return new HikariDataSource();
    }

    // ==================== External DataSource (조건부 활성화) ====================

    /**
     * 외부 DB DataSource
     * - taskflow.user-management.mode=external 일 때만 활성화
     */
    @Bean(name = "externalDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.external")
    @ConditionalOnProperty(
        name = "taskflow.user-management.mode",
        havingValue = "external"
    )
    public DataSource externalDataSource() {
        log.info("Initializing External DataSource for user/department data");
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setReadOnly(true); // 읽기 전용
        return dataSource;
    }

    /**
     * 외부 DB SqlSessionFactory
     */
    @Bean(name = "externalSqlSessionFactory")
    @ConditionalOnProperty(
        name = "taskflow.user-management.mode",
        havingValue = "external"
    )
    public SqlSessionFactory externalSqlSessionFactory(
            @Qualifier("externalDataSource") DataSource dataSource
    ) throws Exception {
        log.info("Initializing External SqlSessionFactory");

        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);

        // 외부 Mapper XML 위치
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        bean.setMapperLocations(
            resolver.getResources("classpath:mapper/external/**/*.xml")
        );

        // Type Alias 설정
        bean.setTypeAliasesPackage("com.taskflow.domain");

        // MyBatis 설정
        org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.NULL);
        configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);

        // 테이블/뷰 이름을 MyBatis 변수로 설정 (application.yml에서 설정 가능)
        java.util.Properties variables = new java.util.Properties();
        variables.setProperty("userTable", userManagementProperties.getExternal().getUserTable());
        variables.setProperty("departmentTable", userManagementProperties.getExternal().getDepartmentTable());
        variables.setProperty("positionTable", userManagementProperties.getExternal().getPositionTable());
        configuration.setVariables(variables);

        log.info("External DB tables configured: userTable={}, departmentTable={}, positionTable={}",
                userManagementProperties.getExternal().getUserTable(),
                userManagementProperties.getExternal().getDepartmentTable(),
                userManagementProperties.getExternal().getPositionTable());

        bean.setConfiguration(configuration);

        return bean.getObject();
    }

    /**
     * 외부 DB SqlSessionTemplate
     */
    @Bean(name = "externalSqlSessionTemplate")
    @ConditionalOnProperty(
        name = "taskflow.user-management.mode",
        havingValue = "external"
    )
    public SqlSessionTemplate externalSqlSessionTemplate(
            @Qualifier("externalSqlSessionFactory") SqlSessionFactory sqlSessionFactory
    ) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 외부 DB Transaction Manager (읽기 전용)
     */
    @Bean(name = "externalTransactionManager")
    @ConditionalOnProperty(
        name = "taskflow.user-management.mode",
        havingValue = "external"
    )
    public DataSourceTransactionManager externalTransactionManager(
            @Qualifier("externalDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }
}
