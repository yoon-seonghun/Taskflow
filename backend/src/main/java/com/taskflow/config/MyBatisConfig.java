package com.taskflow.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * MyBatis 설정 (Primary DataSource용)
 *
 * 설정 정책:
 * - Mapper 스캔: com.taskflow.mapper 패키지 전체
 * - Mapper XML 위치: classpath:mapper/*.xml (external 폴더 제외)
 * - Type Alias 패키지: com.taskflow.domain
 * - Underscore to CamelCase 변환 활성화
 *
 * External 모드 동작:
 * - ExternalMyBatisConfig가 @DependsOn("sqlSessionFactory")로 이 설정 이후 로드됨
 * - external 패키지의 Mapper들이 externalSqlSessionFactory로 재등록됨
 * - allow-bean-definition-overriding: true 필수
 *
 * 주의: JPA 사용 금지
 */
@Configuration
@MapperScan(
    basePackages = "com.taskflow.mapper",
    sqlSessionFactoryRef = "sqlSessionFactory"
)
@EnableTransactionManagement
public class MyBatisConfig {

    /**
     * SqlSessionFactory 빈 등록 (Primary - 메인 DataSource용)
     */
    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        // Mapper XML 위치 설정 (external 폴더 제외)
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(
                resolver.getResources("classpath:mapper/*.xml")
        );

        // Type Alias 설정
        sessionFactory.setTypeAliasesPackage("com.taskflow.domain");

        // MyBatis 설정
        org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration();

        // Underscore to CamelCase 변환
        configuration.setMapUnderscoreToCamelCase(true);

        // NULL 값 처리
        configuration.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.NULL);

        // Lazy Loading 설정
        configuration.setLazyLoadingEnabled(true);
        configuration.setAggressiveLazyLoading(false);

        // 캐시 설정
        configuration.setCacheEnabled(true);

        // 로깅 구현체 (SLF4J)
        configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);

        sessionFactory.setConfiguration(configuration);

        return sessionFactory.getObject();
    }

    /**
     * SqlSessionTemplate 빈 등록 (Primary - 메인 DataSource용)
     */
    @Bean
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
