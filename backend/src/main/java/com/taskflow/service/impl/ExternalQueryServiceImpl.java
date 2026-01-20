package com.taskflow.service.impl;

import com.taskflow.domain.Department;
import com.taskflow.domain.ExternalDatasource;
import com.taskflow.domain.ExternalQuery;
import com.taskflow.domain.User;
import com.taskflow.dto.external.*;
import com.taskflow.mapper.DepartmentMapper;
import com.taskflow.mapper.ExternalDatasourceMapper;
import com.taskflow.mapper.ExternalQueryMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 외부 쿼리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalQueryServiceImpl implements ExternalQueryService {

    private final ExternalQueryMapper queryMapper;
    private final ExternalDatasourceMapper datasourceMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final DynamicDatasourceManager dynamicDatasourceManager;
    private final EncryptionService encryptionService;
    private final QueryValidationService validationService;
    private final QueryCacheService cacheService;
    private final ExternalDatasourceService datasourceService;

    @Override
    public ExternalQueryResponse findById(Long queryId) {
        return queryMapper.findById(queryId)
                .map(ExternalQueryResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("쿼리를 찾을 수 없습니다: " + queryId));
    }

    @Override
    public ExternalQueryResponse findByCode(String queryCode) {
        return queryMapper.findByCode(queryCode)
                .map(ExternalQueryResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("쿼리를 찾을 수 없습니다: " + queryCode));
    }

    @Override
    public List<ExternalQueryResponse> findAll() {
        return ExternalQueryResponse.fromList(queryMapper.findAll());
    }

    @Override
    public List<ExternalQueryResponse> findAllActive() {
        return ExternalQueryResponse.fromList(queryMapper.findAllActive());
    }

    @Override
    public List<ExternalQueryResponse> findByDatasourceId(Long datasourceId) {
        return ExternalQueryResponse.fromList(queryMapper.findByDatasourceId(datasourceId));
    }

    @Override
    public List<ExternalQueryResponse> searchByKeyword(String keyword) {
        return ExternalQueryResponse.fromList(queryMapper.searchByKeyword(keyword));
    }

    @Override
    public List<ExternalQueryResponse> findGlobalQueries() {
        return ExternalQueryResponse.fromList(queryMapper.findGlobalQueries());
    }

    @Override
    public List<ExternalQueryResponse> findManagerQueries(String username) {
        List<String> departmentCodes = getHierarchyDepartmentCodes(username);
        return ExternalQueryResponse.fromList(queryMapper.findManagerQueries(username, departmentCodes));
    }

    @Override
    public List<ExternalQueryResponse> findUserQueries(String username) {
        return ExternalQueryResponse.fromList(queryMapper.findUserQueries(username));
    }

    @Override
    public List<ExternalQueryResponse> findAccessibleQueries(String username) {
        List<String> departmentCodes = getHierarchyDepartmentCodes(username);
        return ExternalQueryResponse.fromList(queryMapper.findAccessibleQueries(username, departmentCodes));
    }

    @Override
    @Transactional
    public Long createGlobalQuery(ExternalQueryCreateRequest request, String username) {
        return createQuery(request, ExternalQuery.OWNER_TYPE_GLOBAL, username, null);
    }

    @Override
    @Transactional
    public Long createManagerQuery(ExternalQueryCreateRequest request, String username) {
        String deptCode = getUserDepartmentCode(username);
        return createQuery(request, ExternalQuery.OWNER_TYPE_MANAGER, username, deptCode);
    }

    @Override
    @Transactional
    public Long createUserQuery(ExternalQueryCreateRequest request, String username) {
        return createQuery(request, ExternalQuery.OWNER_TYPE_USER, username, null);
    }

    private Long createQuery(ExternalQueryCreateRequest request, String ownerType, String username, String deptCode) {
        // 데이터소스 존재 확인
        datasourceMapper.findById(request.getDatasourceId())
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + request.getDatasourceId()));

        // 코드 중복 체크
        if (queryMapper.existsByCode(request.getQueryCode(), null)) {
            throw new IllegalArgumentException("이미 존재하는 쿼리 코드입니다: " + request.getQueryCode());
        }

        // SQL 검증
        QueryValidationService.ValidationResult validationResult = validationService.validateQuery(request.getQuerySql());
        if (!validationResult.valid()) {
            throw new IllegalArgumentException("SQL 검증 실패: " + validationResult.message());
        }

        // 정렬 순서 조회
        int sortOrder = request.getSortOrder() != null ? request.getSortOrder() :
                queryMapper.getMaxSortOrder(ownerType, username) + 1;

        // 도메인 객체 생성 (컬럼명은 대문자로 정규화)
        ExternalQuery query = ExternalQuery.builder()
                .datasourceId(request.getDatasourceId())
                .queryCode(request.getQueryCode())
                .queryName(request.getQueryName())
                .description(request.getDescription())
                .querySql(validationResult.normalizedSql())
                .ownerType(ownerType)
                .ownerUsername(ExternalQuery.OWNER_TYPE_GLOBAL.equals(ownerType) ? null : username)
                .ownerDeptCode(deptCode)
                .valueColumn(toUpperCase(request.getValueColumn()))
                .labelColumn(toUpperCase(request.getLabelColumn()))
                .colorColumn(toUpperCase(request.getColorColumn()))
                .cacheEnabledYn(request.getCacheEnabledYn())
                .cacheTtlSeconds(request.getCacheTtlSeconds())
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(username)
                .build();

        queryMapper.insert(query);
        log.info("External query created: {} ({}) by {}", request.getQueryCode(), ownerType, username);

        return query.getQueryId();
    }

    @Override
    @Transactional
    public void update(Long queryId, ExternalQueryUpdateRequest request, String username) {
        ExternalQuery existing = queryMapper.findById(queryId)
                .orElseThrow(() -> new IllegalArgumentException("쿼리를 찾을 수 없습니다: " + queryId));

        // SQL 변경 시 검증
        if (request.getQuerySql() != null && !request.getQuerySql().isBlank()) {
            QueryValidationService.ValidationResult validationResult = validationService.validateQuery(request.getQuerySql());
            if (!validationResult.valid()) {
                throw new IllegalArgumentException("SQL 검증 실패: " + validationResult.message());
            }
            existing.setQuerySql(validationResult.normalizedSql());
        }

        // 필드 업데이트 (컬럼명은 대문자로 정규화)
        if (request.getDatasourceId() != null) {
            datasourceMapper.findById(request.getDatasourceId())
                    .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + request.getDatasourceId()));
            existing.setDatasourceId(request.getDatasourceId());
        }
        if (request.getQueryName() != null) existing.setQueryName(request.getQueryName());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getValueColumn() != null) existing.setValueColumn(toUpperCase(request.getValueColumn()));
        if (request.getLabelColumn() != null) existing.setLabelColumn(toUpperCase(request.getLabelColumn()));
        existing.setColorColumn(toUpperCase(request.getColorColumn())); // null 허용
        if (request.getCacheEnabledYn() != null) existing.setCacheEnabledYn(request.getCacheEnabledYn());
        if (request.getCacheTtlSeconds() != null) existing.setCacheTtlSeconds(request.getCacheTtlSeconds());
        if (request.getSortOrder() != null) existing.setSortOrder(request.getSortOrder());
        if (request.getUseYn() != null) existing.setUseYn(request.getUseYn());
        existing.setUpdatedBy(username);

        queryMapper.update(existing);

        // 캐시 무효화
        cacheService.evict(queryId);

        log.info("External query updated: {} by {}", existing.getQueryCode(), username);
    }

    @Override
    @Transactional
    public void delete(Long queryId, String username) {
        ExternalQuery existing = queryMapper.findById(queryId)
                .orElseThrow(() -> new IllegalArgumentException("쿼리를 찾을 수 없습니다: " + queryId));

        // 캐시 무효화
        cacheService.evict(queryId);

        // 논리 삭제
        queryMapper.softDelete(queryId, username);

        log.info("External query deleted: {} by {}", existing.getQueryCode(), username);
    }

    @Override
    public QueryTestResponse testQuery(QueryTestRequest request) {
        long startTime = System.currentTimeMillis();

        // SQL 검증
        QueryValidationService.ValidationResult validationResult = validationService.validateQuery(request.getQuerySql());
        if (!validationResult.valid()) {
            return QueryTestResponse.failure("SQL 검증 실패: " + validationResult.message());
        }

        // DataSource 풀 확인/초기화
        if (!dynamicDatasourceManager.hasDatasource(request.getDatasourceId())) {
            datasourceService.initializePool(request.getDatasourceId());
        }

        try {
            // 쿼리 실행
            List<Map<String, Object>> data = dynamicDatasourceManager.executeQuery(
                    request.getDatasourceId(),
                    validationResult.normalizedSql(),
                    request.getMaxRows() != null ? request.getMaxRows() : 100
            );

            // 컬럼 목록 추출
            List<String> columns = data.isEmpty() ? Collections.emptyList() :
                    new ArrayList<>(data.get(0).keySet());

            // 옵션 매핑 (테스트 시에도 컬럼명 대문자 변환 - 아직 저장 전이므로)
            String valueCol = toUpperCase(request.getValueColumn());
            String labelCol = toUpperCase(request.getLabelColumn());
            String colorCol = toUpperCase(request.getColorColumn());

            List<QueryTestResponse.QueryOptionPreview> options = data.stream()
                    .map(row -> {
                        String value = String.valueOf(row.getOrDefault(valueCol, ""));
                        String label = String.valueOf(row.getOrDefault(labelCol, ""));
                        String color = colorCol != null ?
                                String.valueOf(row.getOrDefault(colorCol, "")) : null;
                        return QueryTestResponse.QueryOptionPreview.of(value, label, color);
                    })
                    .collect(Collectors.toList());

            long executionTime = System.currentTimeMillis() - startTime;
            return QueryTestResponse.success(columns, data, options, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Query test failed for datasource {}: {}", request.getDatasourceId(), e.getMessage());
            return QueryTestResponse.failure("쿼리 실행 실패: " + e.getMessage(), executionTime);
        }
    }

    @Override
    @Transactional
    public QueryExecuteResponse executeQuery(Long queryId) {
        ExternalQuery query = queryMapper.findById(queryId)
                .orElseThrow(() -> new IllegalArgumentException("쿼리를 찾을 수 없습니다: " + queryId));

        // 캐시 확인
        if ("Y".equals(query.getCacheEnabledYn())) {
            Optional<List<QueryExecuteResponse.QueryOption>> cached = cacheService.get(queryId);
            if (cached.isPresent()) {
                log.debug("Cache hit for query: {}", queryId);
                return QueryExecuteResponse.success(
                        query.getQueryId(),
                        query.getQueryCode(),
                        query.getQueryName(),
                        cached.get(),
                        true
                );
            }
        }

        return executeQueryInternal(query, true);
    }

    @Override
    @Transactional
    public QueryExecuteResponse executeQueryNoCache(Long queryId) {
        ExternalQuery query = queryMapper.findById(queryId)
                .orElseThrow(() -> new IllegalArgumentException("쿼리를 찾을 수 없습니다: " + queryId));

        // 캐시 무효화
        cacheService.evict(queryId);

        return executeQueryInternal(query, false);
    }

    private QueryExecuteResponse executeQueryInternal(ExternalQuery query, boolean useCache) {
        // DataSource 풀 확인/초기화
        if (!dynamicDatasourceManager.hasDatasource(query.getDatasourceId())) {
            datasourceService.initializePool(query.getDatasourceId());
        }

        try {
            // 쿼리 실행
            List<Map<String, Object>> data = dynamicDatasourceManager.executeQuery(
                    query.getDatasourceId(),
                    query.getQuerySql(),
                    1000
            );

            // 옵션 변환
            List<QueryExecuteResponse.QueryOption> options = data.stream()
                    .map(row -> {
                        String value = String.valueOf(row.getOrDefault(query.getValueColumn(), ""));
                        String label = String.valueOf(row.getOrDefault(query.getLabelColumn(), ""));
                        String color = query.getColorColumn() != null ?
                                String.valueOf(row.getOrDefault(query.getColorColumn(), "")) : null;
                        return QueryExecuteResponse.QueryOption.of(value, label, color);
                    })
                    .collect(Collectors.toList());

            // 캐시 저장
            if (useCache && "Y".equals(query.getCacheEnabledYn())) {
                int ttl = query.getCacheTtlSeconds() != null ? query.getCacheTtlSeconds() : 300;
                cacheService.put(query.getQueryId(), options, ttl);
            }

            // 마지막 실행 정보 갱신
            queryMapper.updateLastExecution(query.getQueryId(), LocalDateTime.now(), options.size());

            return QueryExecuteResponse.success(
                    query.getQueryId(),
                    query.getQueryCode(),
                    query.getQueryName(),
                    options,
                    false
            );

        } catch (Exception e) {
            log.error("Query execution failed for query {}: {}", query.getQueryId(), e.getMessage());
            return QueryExecuteResponse.failure("쿼리 실행 실패: " + e.getMessage());
        }
    }

    @Override
    public void evictCache(Long queryId) {
        cacheService.evict(queryId);
    }

    @Override
    public void evictCacheByDatasource(Long datasourceId) {
        // 해당 데이터소스의 모든 쿼리 캐시 무효화
        List<ExternalQuery> queries = queryMapper.findByDatasourceId(datasourceId);
        queries.forEach(q -> cacheService.evict(q.getQueryId()));
    }

    @Override
    public boolean canModify(Long queryId, String username) {
        ExternalQuery query = queryMapper.findById(queryId).orElse(null);
        if (query == null) {
            return false;
        }

        // ADMIN은 모든 쿼리 수정 가능 (실제로는 Controller에서 권한 체크)
        // GLOBAL 쿼리는 ADMIN만 수정 가능
        if (ExternalQuery.OWNER_TYPE_GLOBAL.equals(query.getOwnerType())) {
            return false; // Controller에서 ADMIN 권한 체크
        }

        // USER/MANAGER 쿼리는 소유자만 수정 가능
        return username.equals(query.getOwnerUsername());
    }

    // =============================================
    // Helper Methods
    // =============================================

    /**
     * 문자열을 대문자로 변환 (null-safe)
     */
    private String toUpperCase(String value) {
        return value != null ? value.toUpperCase() : null;
    }

    /**
     * 사용자의 부서 코드 조회
     */
    private String getUserDepartmentCode(String username) {
        return userMapper.findByUsername(username)
                .map(User::getDepartmentCode)
                .orElse(null);
    }

    /**
     * 사용자의 부서 계층 코드 목록 조회 (본인 부서 + 상위 부서들)
     * 매니저 쿼리 접근 규칙: 매니저가 생성한 쿼리는 해당 부서 및 하위 부서 사용자가 접근 가능
     * → 사용자 입장에서는 본인 부서와 상위 부서들의 코드가 필요
     */
    private List<String> getHierarchyDepartmentCodes(String username) {
        String deptCode = getUserDepartmentCode(username);
        if (deptCode == null) {
            return Collections.emptyList();
        }

        List<String> codes = new ArrayList<>();
        codes.add(deptCode);

        // 상위 부서 경로 조회 (루트까지)
        List<Department> ancestors = departmentMapper.findAncestors(deptCode);
        ancestors.forEach(dept -> codes.add(dept.getDepartmentCode()));

        return codes;
    }
}
