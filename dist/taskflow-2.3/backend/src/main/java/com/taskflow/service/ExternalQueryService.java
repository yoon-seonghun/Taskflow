package com.taskflow.service;

import com.taskflow.dto.external.*;

import java.util.List;

/**
 * 외부 쿼리 서비스
 */
public interface ExternalQueryService {

    // =============================================
    // 조회
    // =============================================

    /**
     * ID로 조회
     *
     * @param queryId 쿼리 ID
     * @return 쿼리 응답
     */
    ExternalQueryResponse findById(Long queryId);

    /**
     * 코드로 조회
     *
     * @param queryCode 쿼리 코드
     * @return 쿼리 응답
     */
    ExternalQueryResponse findByCode(String queryCode);

    /**
     * 전체 목록 조회
     *
     * @return 쿼리 목록
     */
    List<ExternalQueryResponse> findAll();

    /**
     * 활성 목록 조회
     *
     * @return 활성 쿼리 목록
     */
    List<ExternalQueryResponse> findAllActive();

    /**
     * 데이터소스별 쿼리 목록 조회
     *
     * @param datasourceId 데이터소스 ID
     * @return 쿼리 목록
     */
    List<ExternalQueryResponse> findByDatasourceId(Long datasourceId);

    /**
     * 키워드 검색
     *
     * @param keyword 검색 키워드
     * @return 쿼리 목록
     */
    List<ExternalQueryResponse> searchByKeyword(String keyword);

    // =============================================
    // 소유 유형별 조회
    // =============================================

    /**
     * 글로벌 쿼리 목록 조회
     *
     * @return 글로벌 쿼리 목록
     */
    List<ExternalQueryResponse> findGlobalQueries();

    /**
     * 매니저 쿼리 목록 조회
     *
     * @param username 매니저 USERNAME
     * @return 매니저 쿼리 목록
     */
    List<ExternalQueryResponse> findManagerQueries(String username);

    /**
     * 사용자 쿼리 목록 조회
     *
     * @param username 사용자 USERNAME
     * @return 사용자 쿼리 목록
     */
    List<ExternalQueryResponse> findUserQueries(String username);

    /**
     * 접근 가능한 전체 쿼리 목록 조회
     *
     * @param username 사용자 USERNAME
     * @return 접근 가능한 쿼리 목록
     */
    List<ExternalQueryResponse> findAccessibleQueries(String username);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 글로벌 쿼리 생성 (ADMIN)
     *
     * @param request  생성 요청
     * @param username 생성자 USERNAME
     * @return 생성된 쿼리 ID
     */
    Long createGlobalQuery(ExternalQueryCreateRequest request, String username);

    /**
     * 매니저 쿼리 생성
     *
     * @param request  생성 요청
     * @param username 생성자 USERNAME
     * @return 생성된 쿼리 ID
     */
    Long createManagerQuery(ExternalQueryCreateRequest request, String username);

    /**
     * 사용자 쿼리 생성
     *
     * @param request  생성 요청
     * @param username 생성자 USERNAME
     * @return 생성된 쿼리 ID
     */
    Long createUserQuery(ExternalQueryCreateRequest request, String username);

    /**
     * 쿼리 수정
     *
     * @param queryId  쿼리 ID
     * @param request  수정 요청
     * @param username 수정자 USERNAME
     */
    void update(Long queryId, ExternalQueryUpdateRequest request, String username);

    /**
     * 쿼리 삭제
     *
     * @param queryId  쿼리 ID
     * @param username 삭제자 USERNAME
     */
    void delete(Long queryId, String username);

    // =============================================
    // 쿼리 테스트/실행
    // =============================================

    /**
     * 쿼리 테스트 (저장 전)
     *
     * @param request 테스트 요청
     * @return 테스트 결과
     */
    QueryTestResponse testQuery(QueryTestRequest request);

    /**
     * 쿼리 실행 (옵션 조회)
     *
     * @param queryId 쿼리 ID
     * @return 실행 결과
     */
    QueryExecuteResponse executeQuery(Long queryId);

    /**
     * 쿼리 실행 (캐시 무시)
     *
     * @param queryId 쿼리 ID
     * @return 실행 결과
     */
    QueryExecuteResponse executeQueryNoCache(Long queryId);

    // =============================================
    // 캐시 관리
    // =============================================

    /**
     * 쿼리 캐시 무효화
     *
     * @param queryId 쿼리 ID
     */
    void evictCache(Long queryId);

    /**
     * 데이터소스별 캐시 무효화
     *
     * @param datasourceId 데이터소스 ID
     */
    void evictCacheByDatasource(Long datasourceId);

    // =============================================
    // 권한 확인
    // =============================================

    /**
     * 쿼리 수정 권한 확인
     *
     * @param queryId  쿼리 ID
     * @param username 사용자 USERNAME
     * @return 권한 여부
     */
    boolean canModify(Long queryId, String username);
}
