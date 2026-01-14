package com.taskflow.dto.external;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 외부 쿼리 수정 요청 DTO
 */
@Getter
@Setter
public class ExternalQueryUpdateRequest {

    private Long datasourceId;

    @Size(max = 100, message = "쿼리 이름은 100자 이내여야 합니다")
    private String queryName;

    @Size(max = 500, message = "설명은 500자 이내여야 합니다")
    private String description;

    private String querySql;

    @Size(max = 100, message = "값 컬럼명은 100자 이내여야 합니다")
    private String valueColumn;

    @Size(max = 100, message = "라벨 컬럼명은 100자 이내여야 합니다")
    private String labelColumn;

    @Size(max = 100, message = "색상 컬럼명은 100자 이내여야 합니다")
    private String colorColumn;

    @Pattern(regexp = "^[YN]$", message = "캐시 사용 여부는 Y 또는 N이어야 합니다")
    private String cacheEnabledYn;

    @Min(value = 60, message = "캐시 TTL은 60초 이상이어야 합니다")
    @Max(value = 86400, message = "캐시 TTL은 86400초 이하여야 합니다")
    private Integer cacheTtlSeconds;

    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다")
    private Integer sortOrder;

    @Pattern(regexp = "^[YN]$", message = "사용 여부는 Y 또는 N이어야 합니다")
    private String useYn;
}
