package com.taskflow.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 외부 쿼리 정의 도메인
 *
 * 테이블: TB_EXTERNAL_QUERY
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalQuery {

    // =============================================
    // 소유 유형 상수
    // =============================================

    public static final String OWNER_TYPE_GLOBAL = "GLOBAL";
    public static final String OWNER_TYPE_MANAGER = "MANAGER";
    public static final String OWNER_TYPE_USER = "USER";

    // =============================================
    // 필드
    // =============================================

    /**
     * 쿼리 ID (PK)
     */
    private Long queryId;

    /**
     * 데이터소스 ID (FK → TB_EXTERNAL_DATASOURCE)
     */
    private Long datasourceId;

    /**
     * 쿼리 고유 코드
     */
    private String queryCode;

    /**
     * 쿼리 표시명
     */
    private String queryName;

    /**
     * 쿼리 설명
     */
    private String description;

    /**
     * SELECT 쿼리문
     */
    private String querySql;

    /**
     * 소유 유형 (GLOBAL/MANAGER/USER)
     */
    private String ownerType;

    /**
     * 소유자 USERNAME (FK → TB_USER.USERNAME)
     */
    private String ownerUsername;

    /**
     * 소유자 부서 코드
     */
    private String ownerDeptCode;

    /**
     * 값 컬럼명
     */
    private String valueColumn;

    /**
     * 라벨 컬럼명
     */
    private String labelColumn;

    /**
     * 색상 컬럼명
     */
    private String colorColumn;

    /**
     * 캐시 사용 여부
     */
    private String cacheEnabledYn;

    /**
     * 캐시 TTL (초)
     */
    private Integer cacheTtlSeconds;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 마지막 실행 시간
     */
    private LocalDateTime lastExecutedAt;

    /**
     * 마지막 결과 건수
     */
    private Integer lastResultCount;

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
     * 소유자 이름 (조인)
     */
    private String ownerName;

    /**
     * 데이터소스 코드 (조인)
     */
    private String datasourceCode;

    /**
     * 데이터소스 이름 (조인)
     */
    private String datasourceName;

    /**
     * 데이터소스 DB 타입 (조인)
     */
    private String datasourceDbType;

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
     * 캐시 사용 여부
     */
    public boolean isCacheEnabled() {
        return "Y".equals(this.cacheEnabledYn);
    }

    /**
     * 글로벌 쿼리 여부
     */
    public boolean isGlobal() {
        return OWNER_TYPE_GLOBAL.equals(this.ownerType);
    }

    /**
     * 매니저 쿼리 여부
     */
    public boolean isManager() {
        return OWNER_TYPE_MANAGER.equals(this.ownerType);
    }

    /**
     * 사용자 쿼리 여부
     */
    public boolean isUser() {
        return OWNER_TYPE_USER.equals(this.ownerType);
    }
}
