package com.taskflow.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 외부 DB 연결 정보 도메인
 *
 * 테이블: TB_EXTERNAL_DATASOURCE
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDatasource {

    // =============================================
    // DB 타입 상수
    // =============================================

    public static final String DB_TYPE_MYSQL = "MYSQL";
    public static final String DB_TYPE_ORACLE = "ORACLE";
    public static final String DB_TYPE_MSSQL = "MSSQL";
    public static final String DB_TYPE_TIBERO6 = "TIBERO6";
    public static final String DB_TYPE_TIBERO7 = "TIBERO7";

    // =============================================
    // 연결 타입 상수 (Oracle용)
    // =============================================

    public static final String CONNECTION_TYPE_SID = "SID";
    public static final String CONNECTION_TYPE_SERVICE_NAME = "SERVICE_NAME";

    // =============================================
    // 필드
    // =============================================

    /**
     * 데이터소스 ID (PK)
     */
    private Long datasourceId;

    /**
     * 고유 코드
     */
    private String datasourceCode;

    /**
     * 표시 이름
     */
    private String datasourceName;

    /**
     * DB 타입 (MYSQL/ORACLE/MSSQL/TIBERO6/TIBERO7)
     */
    private String dbType;

    /**
     * 연결 타입 (Oracle: SID/SERVICE_NAME)
     */
    private String connectionType;

    /**
     * 호스트 주소
     */
    private String host;

    /**
     * 포트 번호
     */
    private Integer port;

    /**
     * 데이터베이스명
     */
    private String databaseName;

    /**
     * 스키마명 (선택적, 문서화 용도)
     */
    private String schemaName;

    /**
     * 접속 계정
     */
    private String username;

    /**
     * AES-256 암호화된 비밀번호
     */
    private String passwordEncrypted;

    /**
     * 연결 타임아웃 (ms)
     */
    private Integer connectionTimeout;

    /**
     * 쿼리 타임아웃 (ms)
     */
    private Integer queryTimeout;

    /**
     * 최대 풀 크기
     */
    private Integer maxPoolSize;

    /**
     * 사용 여부
     */
    private String useYn;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 USERNAME
     */
    private String updatedBy;

    /**
     * 삭제일시 (논리삭제)
     */
    private LocalDateTime deletedAt;

    /**
     * 삭제자 USERNAME
     */
    private String deletedBy;

    // =============================================
    // 조인 필드
    // =============================================

    /**
     * 생성자 이름 (조인)
     */
    private String createdByName;

    /**
     * 수정자 이름 (조인)
     */
    private String updatedByName;

    /**
     * 연결된 쿼리 수 (조인)
     */
    private Integer queryCount;

    // =============================================
    // 헬퍼 메서드
    // =============================================

    /**
     * 사용 가능 여부
     */
    public boolean isActive() {
        return "Y".equals(this.useYn) && this.deletedAt == null;
    }

    /**
     * 삭제 여부
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * MySQL 여부
     */
    public boolean isMySql() {
        return DB_TYPE_MYSQL.equals(this.dbType);
    }

    /**
     * Oracle 여부
     */
    public boolean isOracle() {
        return DB_TYPE_ORACLE.equals(this.dbType);
    }

    /**
     * MSSQL 여부
     */
    public boolean isMsSql() {
        return DB_TYPE_MSSQL.equals(this.dbType);
    }

    /**
     * Tibero 6 여부
     */
    public boolean isTibero6() {
        return DB_TYPE_TIBERO6.equals(this.dbType);
    }

    /**
     * Tibero 7 여부
     */
    public boolean isTibero7() {
        return DB_TYPE_TIBERO7.equals(this.dbType);
    }

    /**
     * Tibero 여부 (6 또는 7)
     */
    public boolean isTibero() {
        return isTibero6() || isTibero7();
    }

    /**
     * SID 연결 방식 여부 (Oracle)
     */
    public boolean isSidConnection() {
        return CONNECTION_TYPE_SID.equals(this.connectionType);
    }
}
