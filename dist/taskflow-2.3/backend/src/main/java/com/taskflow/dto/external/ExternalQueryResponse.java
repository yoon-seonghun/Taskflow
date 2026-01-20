package com.taskflow.dto.external;

import com.taskflow.domain.ExternalQuery;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 외부 쿼리 응답 DTO
 */
@Getter
@Builder
public class ExternalQueryResponse {

    private Long queryId;
    private Long datasourceId;
    private String datasourceCode;
    private String datasourceName;
    private String datasourceDbType;
    private String queryCode;
    private String queryName;
    private String description;
    private String querySql;
    private String ownerType;
    private String ownerUsername;
    private String ownerName;
    private String ownerDeptCode;
    private String valueColumn;
    private String labelColumn;
    private String colorColumn;
    private String cacheEnabledYn;
    private Integer cacheTtlSeconds;
    private Integer sortOrder;
    private LocalDateTime lastExecutedAt;
    private Integer lastResultCount;
    private String useYn;
    private LocalDateTime createdAt;
    private String createdBy;
    private String createdByName;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String updatedByName;

    /**
     * Domain에서 Response DTO로 변환
     */
    public static ExternalQueryResponse from(ExternalQuery query) {
        if (query == null) {
            return null;
        }

        return ExternalQueryResponse.builder()
                .queryId(query.getQueryId())
                .datasourceId(query.getDatasourceId())
                .datasourceCode(query.getDatasourceCode())
                .datasourceName(query.getDatasourceName())
                .datasourceDbType(query.getDatasourceDbType())
                .queryCode(query.getQueryCode())
                .queryName(query.getQueryName())
                .description(query.getDescription())
                .querySql(query.getQuerySql())
                .ownerType(query.getOwnerType())
                .ownerUsername(query.getOwnerUsername())
                .ownerName(query.getOwnerName())
                .ownerDeptCode(query.getOwnerDeptCode())
                .valueColumn(query.getValueColumn())
                .labelColumn(query.getLabelColumn())
                .colorColumn(query.getColorColumn())
                .cacheEnabledYn(query.getCacheEnabledYn())
                .cacheTtlSeconds(query.getCacheTtlSeconds())
                .sortOrder(query.getSortOrder())
                .lastExecutedAt(query.getLastExecutedAt())
                .lastResultCount(query.getLastResultCount())
                .useYn(query.getUseYn())
                .createdAt(query.getCreatedAt())
                .createdBy(query.getCreatedBy())
                .createdByName(query.getCreatedByName())
                .updatedAt(query.getUpdatedAt())
                .updatedBy(query.getUpdatedBy())
                .updatedByName(query.getUpdatedByName())
                .build();
    }

    /**
     * Domain 리스트를 Response DTO 리스트로 변환
     */
    public static List<ExternalQueryResponse> fromList(List<ExternalQuery> queries) {
        if (queries == null) {
            return Collections.emptyList();
        }

        return queries.stream()
                .map(ExternalQueryResponse::from)
                .collect(Collectors.toList());
    }
}
