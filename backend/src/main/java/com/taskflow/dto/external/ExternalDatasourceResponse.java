package com.taskflow.dto.external;

import com.taskflow.domain.ExternalDatasource;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 외부 DB 데이터소스 응답 DTO
 */
@Getter
@Builder
public class ExternalDatasourceResponse {

    private Long datasourceId;
    private String datasourceCode;
    private String datasourceName;
    private String dbType;
    private String connectionType;
    private String host;
    private Integer port;
    private String databaseName;
    private String schemaName;
    private String username;
    // 비밀번호는 응답에 포함하지 않음
    private Integer connectionTimeout;
    private Integer queryTimeout;
    private Integer maxPoolSize;
    private String useYn;
    private LocalDateTime createdAt;
    private String createdBy;
    private String createdByName;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String updatedByName;
    private Integer queryCount;

    /**
     * Domain에서 Response DTO로 변환
     */
    public static ExternalDatasourceResponse from(ExternalDatasource datasource) {
        if (datasource == null) {
            return null;
        }

        return ExternalDatasourceResponse.builder()
                .datasourceId(datasource.getDatasourceId())
                .datasourceCode(datasource.getDatasourceCode())
                .datasourceName(datasource.getDatasourceName())
                .dbType(datasource.getDbType())
                .connectionType(datasource.getConnectionType())
                .host(datasource.getHost())
                .port(datasource.getPort())
                .databaseName(datasource.getDatabaseName())
                .schemaName(datasource.getSchemaName())
                .username(datasource.getUsername())
                .connectionTimeout(datasource.getConnectionTimeout())
                .queryTimeout(datasource.getQueryTimeout())
                .maxPoolSize(datasource.getMaxPoolSize())
                .useYn(datasource.getUseYn())
                .createdAt(datasource.getCreatedAt())
                .createdBy(datasource.getCreatedBy())
                .createdByName(datasource.getCreatedByName())
                .updatedAt(datasource.getUpdatedAt())
                .updatedBy(datasource.getUpdatedBy())
                .updatedByName(datasource.getUpdatedByName())
                .queryCount(datasource.getQueryCount())
                .build();
    }

    /**
     * Domain 리스트를 Response DTO 리스트로 변환
     */
    public static List<ExternalDatasourceResponse> fromList(List<ExternalDatasource> datasources) {
        if (datasources == null) {
            return Collections.emptyList();
        }

        return datasources.stream()
                .map(ExternalDatasourceResponse::from)
                .collect(Collectors.toList());
    }
}
