package com.taskflow.mapper;

import com.taskflow.domain.ExternalDatasource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 외부 데이터소스 Mapper
 */
@Mapper
public interface ExternalDatasourceMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * ID로 조회
     *
     * @param datasourceId 데이터소스 ID
     * @return 데이터소스
     */
    Optional<ExternalDatasource> findById(@Param("datasourceId") Long datasourceId);

    /**
     * 코드로 조회
     *
     * @param datasourceCode 데이터소스 코드
     * @return 데이터소스
     */
    Optional<ExternalDatasource> findByCode(@Param("datasourceCode") String datasourceCode);

    /**
     * 전체 목록 조회
     *
     * @return 데이터소스 목록
     */
    List<ExternalDatasource> findAll();

    /**
     * 활성 목록 조회
     *
     * @return 활성 데이터소스 목록
     */
    List<ExternalDatasource> findAllActive();

    /**
     * DB 타입별 목록 조회
     *
     * @param dbType DB 타입 (MYSQL, ORACLE, MSSQL, TIBERO)
     * @return 데이터소스 목록
     */
    List<ExternalDatasource> findByDbType(@Param("dbType") String dbType);

    /**
     * 키워드 검색
     *
     * @param keyword 검색 키워드 (코드, 이름, 호스트)
     * @return 데이터소스 목록
     */
    List<ExternalDatasource> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 코드 중복 체크
     *
     * @param datasourceCode 데이터소스 코드
     * @param excludeId      제외할 ID (수정 시)
     * @return 존재 여부
     */
    boolean existsByCode(@Param("datasourceCode") String datasourceCode,
                         @Param("excludeId") Long excludeId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 등록
     *
     * @param datasource 데이터소스
     */
    void insert(ExternalDatasource datasource);

    /**
     * 수정
     *
     * @param datasource 데이터소스
     */
    void update(ExternalDatasource datasource);

    /**
     * 비밀번호 수정
     *
     * @param datasourceId      데이터소스 ID
     * @param passwordEncrypted 암호화된 비밀번호
     * @param updatedBy         수정자
     */
    void updatePassword(@Param("datasourceId") Long datasourceId,
                        @Param("passwordEncrypted") String passwordEncrypted,
                        @Param("updatedBy") String updatedBy);

    /**
     * 사용 여부 변경
     *
     * @param datasourceId 데이터소스 ID
     * @param useYn        사용 여부
     * @param updatedBy    수정자
     */
    void updateUseYn(@Param("datasourceId") Long datasourceId,
                     @Param("useYn") String useYn,
                     @Param("updatedBy") String updatedBy);

    /**
     * 논리 삭제
     *
     * @param datasourceId 데이터소스 ID
     * @param deletedBy    삭제자
     */
    void softDelete(@Param("datasourceId") Long datasourceId,
                    @Param("deletedBy") String deletedBy);

    /**
     * 물리 삭제
     *
     * @param datasourceId 데이터소스 ID
     */
    void delete(@Param("datasourceId") Long datasourceId);

    // =============================================
    // 통계
    // =============================================

    /**
     * 데이터소스별 쿼리 개수 조회
     *
     * @param datasourceId 데이터소스 ID
     * @return 쿼리 개수
     */
    int countQueries(@Param("datasourceId") Long datasourceId);
}
