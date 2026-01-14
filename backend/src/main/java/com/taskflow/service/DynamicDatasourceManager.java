package com.taskflow.service;

import com.taskflow.domain.ExternalDatasource;
import com.taskflow.dto.external.ConnectionTestResponse;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 외부 DB 동적 DataSource 관리 서비스
 *
 * HikariCP 기반으로 외부 DB 연결 풀을 동적으로 생성/관리합니다.
 */
public interface DynamicDatasourceManager {

    /**
     * DataSource 생성 또는 갱신
     *
     * @param datasource 데이터소스 설정
     * @param decryptedPassword 복호화된 비밀번호
     * @return 생성된 DataSource
     */
    DataSource createOrUpdateDatasource(ExternalDatasource datasource, String decryptedPassword);

    /**
     * DataSource 조회
     *
     * @param datasourceId 데이터소스 ID
     * @return DataSource (없으면 null)
     */
    DataSource getDatasource(Long datasourceId);

    /**
     * DataSource 존재 여부 확인
     *
     * @param datasourceId 데이터소스 ID
     * @return 존재 여부
     */
    boolean hasDatasource(Long datasourceId);

    /**
     * DataSource 제거 (풀 종료)
     *
     * @param datasourceId 데이터소스 ID
     */
    void removeDatasource(Long datasourceId);

    /**
     * 모든 DataSource 제거 (애플리케이션 종료 시)
     */
    void removeAllDatasources();

    /**
     * 연결 테스트
     *
     * @param datasource 데이터소스 설정
     * @param decryptedPassword 복호화된 비밀번호
     * @return 테스트 결과
     */
    ConnectionTestResponse testConnection(ExternalDatasource datasource, String decryptedPassword);

    /**
     * 쿼리 실행 (읽기 전용)
     *
     * @param datasourceId 데이터소스 ID
     * @param sql SQL 쿼리
     * @param maxRows 최대 결과 행 수
     * @return 쿼리 결과 (컬럼명:값 Map 리스트)
     * @throws IllegalStateException 데이터소스가 없는 경우
     */
    List<Map<String, Object>> executeQuery(Long datasourceId, String sql, int maxRows);

    /**
     * JDBC URL 생성
     *
     * @param datasource 데이터소스 설정
     * @return JDBC URL
     */
    String buildJdbcUrl(ExternalDatasource datasource);

    /**
     * Driver 클래스명 조회
     *
     * @param dbType DB 타입
     * @return Driver 클래스명
     */
    String getDriverClassName(String dbType);

    /**
     * 연결 검증 쿼리 조회
     *
     * @param dbType DB 타입
     * @return 검증 쿼리
     */
    String getValidationQuery(String dbType);
}
