package com.taskflow.mapper;

import com.taskflow.domain.TaskTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 작업 템플릿 Mapper
 */
@Mapper
public interface TaskTemplateMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 템플릿 ID로 조회
     *
     * @param templateId 템플릿 ID
     * @return 템플릿
     */
    Optional<TaskTemplate> findById(@Param("templateId") Long templateId);

    /**
     * 전체 템플릿 목록 조회
     *
     * @return 템플릿 목록
     */
    List<TaskTemplate> findAll();

    /**
     * 활성 템플릿 목록 조회
     *
     * @return 활성 템플릿 목록
     */
    List<TaskTemplate> findAllActive();

    /**
     * 키워드로 템플릿 검색 (자동완성용)
     *
     * @param keyword 검색 키워드
     * @param limit   최대 결과 수
     * @return 검색된 템플릿 목록
     */
    List<TaskTemplate> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 작업 내용으로 조회 (중복 체크)
     *
     * @param content 작업 내용
     * @return 템플릿
     */
    Optional<TaskTemplate> findByContent(@Param("content") String content);

    /**
     * 최대 정렬 순서 조회
     *
     * @return 최대 정렬 순서
     */
    int getMaxSortOrder();

    // =============================================
    // 소유 유형별 조회
    // =============================================

    /**
     * 글로벌 템플릿 목록 조회
     *
     * @return 글로벌 템플릿 목록
     */
    List<TaskTemplate> findGlobalTemplates();

    /**
     * 매니저 템플릿 목록 조회 (부서 계층 포함)
     *
     * @param username        소유자 USERNAME
     * @param departmentCodes 부서 코드 목록 (본인 부서 + 상위 부서)
     * @return 매니저 템플릿 목록
     */
    List<TaskTemplate> findManagerTemplates(@Param("username") String username,
                                            @Param("departmentCodes") List<String> departmentCodes);

    /**
     * 개인 템플릿 목록 조회
     *
     * @param username 소유자 USERNAME
     * @return 개인 템플릿 목록
     */
    List<TaskTemplate> findUserTemplates(@Param("username") String username);

    /**
     * 접근 가능한 전체 템플릿 목록 조회 (글로벌 + 매니저 + 개인)
     *
     * @param username        사용자 USERNAME
     * @param departmentCodes 부서 코드 목록 (본인 부서 + 상위 부서)
     * @return 접근 가능한 템플릿 목록
     */
    List<TaskTemplate> findAccessibleTemplates(@Param("username") String username,
                                               @Param("departmentCodes") List<String> departmentCodes);

    /**
     * 소유 유형 + 내용 중복 체크
     *
     * @param content   작업 내용
     * @param ownerType 소유 유형
     * @param ownerUsername 소유자 USERNAME (USER/MANAGER용)
     * @return 템플릿
     */
    Optional<TaskTemplate> findByContentAndOwner(@Param("content") String content,
                                                  @Param("ownerType") String ownerType,
                                                  @Param("ownerUsername") String ownerUsername);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 템플릿 등록
     *
     * @param template 템플릿
     */
    void insert(TaskTemplate template);

    /**
     * 템플릿 수정
     *
     * @param template 템플릿
     */
    void update(TaskTemplate template);

    /**
     * 템플릿 삭제
     *
     * @param templateId 템플릿 ID
     */
    void delete(@Param("templateId") Long templateId);

    /**
     * 사용 횟수 증가
     *
     * @param templateId 템플릿 ID
     */
    void incrementUseCount(@Param("templateId") Long templateId);

    // =============================================
    // 이력 조회
    // =============================================

    /**
     * 템플릿 이력 조회 (필터/페이징)
     *
     * @param request 검색 조건
     * @return 템플릿 목록
     */
    List<TaskTemplate> findTemplateHistory(@Param("request") com.taskflow.dto.history.TemplateHistorySearchRequest request);

    /**
     * 템플릿 이력 총 개수 조회
     *
     * @param request 검색 조건
     * @return 총 개수
     */
    long countTemplateHistory(@Param("request") com.taskflow.dto.history.TemplateHistorySearchRequest request);
}
