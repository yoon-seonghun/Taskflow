package com.taskflow.mapper;

import com.taskflow.domain.ExternalQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 외부 쿼리 Mapper
 */
@Mapper
public interface ExternalQueryMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * ID로 조회
     *
     * @param queryId 쿼리 ID
     * @return 쿼리
     */
    Optional<ExternalQuery> findById(@Param("queryId") Long queryId);

    /**
     * 코드로 조회
     *
     * @param queryCode 쿼리 코드
     * @return 쿼리
     */
    Optional<ExternalQuery> findByCode(@Param("queryCode") String queryCode);

    /**
     * 전체 목록 조회
     *
     * @return 쿼리 목록
     */
    List<ExternalQuery> findAll();

    /**
     * 활성 목록 조회
     *
     * @return 활성 쿼리 목록
     */
    List<ExternalQuery> findAllActive();

    /**
     * 데이터소스별 쿼리 목록 조회
     *
     * @param datasourceId 데이터소스 ID
     * @return 쿼리 목록
     */
    List<ExternalQuery> findByDatasourceId(@Param("datasourceId") Long datasourceId);

    /**
     * 키워드 검색
     *
     * @param keyword 검색 키워드 (코드, 이름, 설명)
     * @return 쿼리 목록
     */
    List<ExternalQuery> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 코드 중복 체크
     *
     * @param queryCode 쿼리 코드
     * @param excludeId 제외할 ID (수정 시)
     * @return 존재 여부
     */
    boolean existsByCode(@Param("queryCode") String queryCode,
                         @Param("excludeId") Long excludeId);

    // =============================================
    // 소유 유형별 조회
    // =============================================

    /**
     * 글로벌 쿼리 목록 조회
     *
     * @return 글로벌 쿼리 목록
     */
    List<ExternalQuery> findGlobalQueries();

    /**
     * 매니저 쿼리 목록 조회 (부서 계층 포함)
     *
     * @param username        소유자 USERNAME
     * @param departmentCodes 부서 코드 목록 (본인 부서 + 상위 부서)
     * @return 매니저 쿼리 목록
     */
    List<ExternalQuery> findManagerQueries(@Param("username") String username,
                                            @Param("departmentCodes") List<String> departmentCodes);

    /**
     * 개인 쿼리 목록 조회
     *
     * @param username 소유자 USERNAME
     * @return 개인 쿼리 목록
     */
    List<ExternalQuery> findUserQueries(@Param("username") String username);

    /**
     * 접근 가능한 전체 쿼리 목록 조회 (글로벌 + 매니저 + 개인)
     *
     * @param username        사용자 USERNAME
     * @param departmentCodes 부서 코드 목록 (본인 부서 + 상위 부서)
     * @return 접근 가능한 쿼리 목록
     */
    List<ExternalQuery> findAccessibleQueries(@Param("username") String username,
                                               @Param("departmentCodes") List<String> departmentCodes);

    /**
     * 소유 유형 + 코드 중복 체크
     *
     * @param queryCode     쿼리 코드
     * @param ownerType     소유 유형
     * @param ownerUsername 소유자 USERNAME (USER/MANAGER용)
     * @return 쿼리
     */
    Optional<ExternalQuery> findByCodeAndOwner(@Param("queryCode") String queryCode,
                                                @Param("ownerType") String ownerType,
                                                @Param("ownerUsername") String ownerUsername);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 등록
     *
     * @param query 쿼리
     */
    void insert(ExternalQuery query);

    /**
     * 수정
     *
     * @param query 쿼리
     */
    void update(ExternalQuery query);

    /**
     * 사용 여부 변경
     *
     * @param queryId   쿼리 ID
     * @param useYn     사용 여부
     * @param updatedBy 수정자
     */
    void updateUseYn(@Param("queryId") Long queryId,
                     @Param("useYn") String useYn,
                     @Param("updatedBy") String updatedBy);

    /**
     * 마지막 실행 정보 갱신
     *
     * @param queryId         쿼리 ID
     * @param lastExecutedAt  마지막 실행 시간
     * @param lastResultCount 마지막 결과 건수
     */
    void updateLastExecution(@Param("queryId") Long queryId,
                             @Param("lastExecutedAt") LocalDateTime lastExecutedAt,
                             @Param("lastResultCount") Integer lastResultCount);

    /**
     * 논리 삭제
     *
     * @param queryId   쿼리 ID
     * @param deletedBy 삭제자
     */
    void softDelete(@Param("queryId") Long queryId,
                    @Param("deletedBy") String deletedBy);

    /**
     * 물리 삭제
     *
     * @param queryId 쿼리 ID
     */
    void delete(@Param("queryId") Long queryId);

    /**
     * 데이터소스별 전체 삭제
     *
     * @param datasourceId 데이터소스 ID
     * @param deletedBy    삭제자
     */
    void softDeleteByDatasourceId(@Param("datasourceId") Long datasourceId,
                                   @Param("deletedBy") String deletedBy);

    // =============================================
    // 정렬 순서
    // =============================================

    /**
     * 최대 정렬 순서 조회
     *
     * @param ownerType     소유 유형
     * @param ownerUsername 소유자 USERNAME
     * @return 최대 정렬 순서
     */
    int getMaxSortOrder(@Param("ownerType") String ownerType,
                        @Param("ownerUsername") String ownerUsername);

    /**
     * 정렬 순서 변경
     *
     * @param queryId   쿼리 ID
     * @param sortOrder 정렬 순서
     * @param updatedBy 수정자
     */
    void updateSortOrder(@Param("queryId") Long queryId,
                         @Param("sortOrder") Integer sortOrder,
                         @Param("updatedBy") String updatedBy);
}
