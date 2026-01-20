package com.taskflow.service;

import com.taskflow.dto.external.ConnectionTestResponse;
import com.taskflow.dto.external.ExternalDatasourceCreateRequest;
import com.taskflow.dto.external.ExternalDatasourceResponse;
import com.taskflow.dto.external.ExternalDatasourceUpdateRequest;

import java.util.List;

/**
 * 외부 데이터소스 서비스
 */
public interface ExternalDatasourceService {

    // =============================================
    // 조회
    // =============================================

    /**
     * ID로 조회
     *
     * @param datasourceId 데이터소스 ID
     * @return 데이터소스 응답
     */
    ExternalDatasourceResponse findById(Long datasourceId);

    /**
     * 코드로 조회
     *
     * @param datasourceCode 데이터소스 코드
     * @return 데이터소스 응답
     */
    ExternalDatasourceResponse findByCode(String datasourceCode);

    /**
     * 전체 목록 조회
     *
     * @return 데이터소스 목록
     */
    List<ExternalDatasourceResponse> findAll();

    /**
     * 활성 목록 조회
     *
     * @return 활성 데이터소스 목록
     */
    List<ExternalDatasourceResponse> findAllActive();

    /**
     * DB 타입별 목록 조회
     *
     * @param dbType DB 타입
     * @return 데이터소스 목록
     */
    List<ExternalDatasourceResponse> findByDbType(String dbType);

    /**
     * 키워드 검색
     *
     * @param keyword 검색 키워드
     * @return 데이터소스 목록
     */
    List<ExternalDatasourceResponse> searchByKeyword(String keyword);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 데이터소스 생성
     *
     * @param request  생성 요청
     * @param username 생성자 USERNAME
     * @return 생성된 데이터소스 ID
     */
    Long create(ExternalDatasourceCreateRequest request, String username);

    /**
     * 데이터소스 수정
     *
     * @param datasourceId 데이터소스 ID
     * @param request      수정 요청
     * @param username     수정자 USERNAME
     */
    void update(Long datasourceId, ExternalDatasourceUpdateRequest request, String username);

    /**
     * 데이터소스 삭제
     *
     * @param datasourceId 데이터소스 ID
     * @param username     삭제자 USERNAME
     */
    void delete(Long datasourceId, String username);

    // =============================================
    // 연결 테스트
    // =============================================

    /**
     * 연결 테스트 (기존 데이터소스)
     *
     * @param datasourceId 데이터소스 ID
     * @return 테스트 결과
     */
    ConnectionTestResponse testConnection(Long datasourceId);

    /**
     * 연결 테스트 (새 설정)
     *
     * @param request 생성 요청 (비밀번호 포함)
     * @return 테스트 결과
     */
    ConnectionTestResponse testConnection(ExternalDatasourceCreateRequest request);

    // =============================================
    // DataSource 풀 관리
    // =============================================

    /**
     * DataSource 풀 초기화 (설정 변경 후 호출)
     *
     * @param datasourceId 데이터소스 ID
     */
    void initializePool(Long datasourceId);

    /**
     * DataSource 풀 제거
     *
     * @param datasourceId 데이터소스 ID
     */
    void destroyPool(Long datasourceId);
}
