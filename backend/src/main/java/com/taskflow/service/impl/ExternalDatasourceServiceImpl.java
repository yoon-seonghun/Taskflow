package com.taskflow.service.impl;

import com.taskflow.domain.ExternalDatasource;
import com.taskflow.dto.external.ConnectionTestResponse;
import com.taskflow.dto.external.ExternalDatasourceCreateRequest;
import com.taskflow.dto.external.ExternalDatasourceResponse;
import com.taskflow.dto.external.ExternalDatasourceUpdateRequest;
import com.taskflow.mapper.ExternalDatasourceMapper;
import com.taskflow.mapper.ExternalQueryMapper;
import com.taskflow.service.DynamicDatasourceManager;
import com.taskflow.service.EncryptionService;
import com.taskflow.service.ExternalDatasourceService;
import com.taskflow.service.QueryCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 외부 데이터소스 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalDatasourceServiceImpl implements ExternalDatasourceService {

    private final ExternalDatasourceMapper datasourceMapper;
    private final ExternalQueryMapper queryMapper;
    private final EncryptionService encryptionService;
    private final DynamicDatasourceManager dynamicDatasourceManager;
    private final QueryCacheService queryCacheService;

    @Override
    public ExternalDatasourceResponse findById(Long datasourceId) {
        return datasourceMapper.findById(datasourceId)
                .map(ExternalDatasourceResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + datasourceId));
    }

    @Override
    public ExternalDatasourceResponse findByCode(String datasourceCode) {
        return datasourceMapper.findByCode(datasourceCode)
                .map(ExternalDatasourceResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + datasourceCode));
    }

    @Override
    public List<ExternalDatasourceResponse> findAll() {
        return ExternalDatasourceResponse.fromList(datasourceMapper.findAll());
    }

    @Override
    public List<ExternalDatasourceResponse> findAllActive() {
        return ExternalDatasourceResponse.fromList(datasourceMapper.findAllActive());
    }

    @Override
    public List<ExternalDatasourceResponse> findByDbType(String dbType) {
        return ExternalDatasourceResponse.fromList(datasourceMapper.findByDbType(dbType));
    }

    @Override
    public List<ExternalDatasourceResponse> searchByKeyword(String keyword) {
        return ExternalDatasourceResponse.fromList(datasourceMapper.searchByKeyword(keyword));
    }

    @Override
    @Transactional
    public Long create(ExternalDatasourceCreateRequest request, String username) {
        // 코드 중복 체크
        if (datasourceMapper.existsByCode(request.getDatasourceCode(), null)) {
            throw new IllegalArgumentException("이미 존재하는 데이터소스 코드입니다: " + request.getDatasourceCode());
        }

        // 비밀번호 암호화
        String encryptedPassword = encryptionService.encrypt(request.getPassword());

        // 도메인 객체 생성
        ExternalDatasource datasource = ExternalDatasource.builder()
                .datasourceCode(request.getDatasourceCode())
                .datasourceName(request.getDatasourceName())
                .dbType(request.getDbType())
                .connectionType(request.getConnectionType())
                .host(request.getHost())
                .port(request.getPort())
                .databaseName(request.getDatabaseName())
                .schemaName(request.getSchemaName())
                .username(request.getUsername())
                .passwordEncrypted(encryptedPassword)
                .connectionTimeout(request.getConnectionTimeout())
                .queryTimeout(request.getQueryTimeout())
                .maxPoolSize(request.getMaxPoolSize())
                .useYn("Y")
                .createdBy(username)
                .build();

        datasourceMapper.insert(datasource);
        log.info("External datasource created: {} by {}", request.getDatasourceCode(), username);

        return datasource.getDatasourceId();
    }

    @Override
    @Transactional
    public void update(Long datasourceId, ExternalDatasourceUpdateRequest request, String username) {
        ExternalDatasource existing = datasourceMapper.findById(datasourceId)
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + datasourceId));

        // 기본 필드 업데이트
        existing.setDatasourceName(request.getDatasourceName() != null ? request.getDatasourceName() : existing.getDatasourceName());
        existing.setConnectionType(request.getConnectionType() != null ? request.getConnectionType() : existing.getConnectionType());
        existing.setHost(request.getHost() != null ? request.getHost() : existing.getHost());
        existing.setPort(request.getPort() != null ? request.getPort() : existing.getPort());
        existing.setDatabaseName(request.getDatabaseName() != null ? request.getDatabaseName() : existing.getDatabaseName());
        existing.setSchemaName(request.getSchemaName());
        existing.setUsername(request.getUsername() != null ? request.getUsername() : existing.getUsername());
        existing.setConnectionTimeout(request.getConnectionTimeout() != null ? request.getConnectionTimeout() : existing.getConnectionTimeout());
        existing.setQueryTimeout(request.getQueryTimeout() != null ? request.getQueryTimeout() : existing.getQueryTimeout());
        existing.setMaxPoolSize(request.getMaxPoolSize() != null ? request.getMaxPoolSize() : existing.getMaxPoolSize());
        existing.setUseYn(request.getUseYn() != null ? request.getUseYn() : existing.getUseYn());
        existing.setUpdatedBy(username);

        datasourceMapper.update(existing);

        // 비밀번호 변경 (별도 처리)
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            String encryptedPassword = encryptionService.encrypt(request.getPassword());
            datasourceMapper.updatePassword(datasourceId, encryptedPassword, username);
        }

        // DataSource 풀 재생성 (설정 변경 반영)
        if (dynamicDatasourceManager.hasDatasource(datasourceId)) {
            destroyPool(datasourceId);
            if ("Y".equals(existing.getUseYn())) {
                initializePool(datasourceId);
            }
        }

        log.info("External datasource updated: {} by {}", existing.getDatasourceCode(), username);
    }

    @Override
    @Transactional
    public void delete(Long datasourceId, String username) {
        ExternalDatasource existing = datasourceMapper.findById(datasourceId)
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + datasourceId));

        // DataSource 풀 제거
        destroyPool(datasourceId);

        // 연관 쿼리 논리 삭제
        queryMapper.softDeleteByDatasourceId(datasourceId, username);

        // 데이터소스 논리 삭제
        datasourceMapper.softDelete(datasourceId, username);

        log.info("External datasource deleted: {} by {}", existing.getDatasourceCode(), username);
    }

    @Override
    public ConnectionTestResponse testConnection(Long datasourceId) {
        ExternalDatasource datasource = datasourceMapper.findById(datasourceId)
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + datasourceId));

        String decryptedPassword = encryptionService.decrypt(datasource.getPasswordEncrypted());
        return dynamicDatasourceManager.testConnection(datasource, decryptedPassword);
    }

    @Override
    public ConnectionTestResponse testConnection(ExternalDatasourceCreateRequest request) {
        ExternalDatasource datasource = ExternalDatasource.builder()
                .datasourceCode(request.getDatasourceCode())
                .datasourceName(request.getDatasourceName())
                .dbType(request.getDbType())
                .connectionType(request.getConnectionType())
                .host(request.getHost())
                .port(request.getPort())
                .databaseName(request.getDatabaseName())
                .schemaName(request.getSchemaName())
                .username(request.getUsername())
                .connectionTimeout(request.getConnectionTimeout())
                .queryTimeout(request.getQueryTimeout())
                .maxPoolSize(request.getMaxPoolSize())
                .build();

        return dynamicDatasourceManager.testConnection(datasource, request.getPassword());
    }

    @Override
    public void initializePool(Long datasourceId) {
        ExternalDatasource datasource = datasourceMapper.findById(datasourceId)
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + datasourceId));

        if (!"Y".equals(datasource.getUseYn())) {
            log.warn("Datasource is disabled, not initializing pool: {}", datasourceId);
            return;
        }

        String decryptedPassword = encryptionService.decrypt(datasource.getPasswordEncrypted());
        dynamicDatasourceManager.createOrUpdateDatasource(datasource, decryptedPassword);
        log.info("DataSource pool initialized: {}", datasource.getDatasourceCode());
    }

    @Override
    public void destroyPool(Long datasourceId) {
        dynamicDatasourceManager.removeDatasource(datasourceId);

        // 해당 데이터소스의 모든 쿼리 캐시 무효화
        queryCacheService.evictByDatasource(datasourceId);
    }
}
