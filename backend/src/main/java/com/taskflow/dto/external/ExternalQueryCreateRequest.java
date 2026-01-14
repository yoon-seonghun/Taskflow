package com.taskflow.dto.external;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 외부 쿼리 생성 요청 DTO
 */
@Getter
@Setter
public class ExternalQueryCreateRequest {

    @NotNull(message = "데이터소스 ID는 필수입니다")
    private Long datasourceId;

    @NotBlank(message = "쿼리 코드는 필수입니다")
    @Size(max = 50, message = "쿼리 코드는 50자 이내여야 합니다")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "쿼리 코드는 대문자, 숫자, 언더스코어만 가능합니다")
    private String queryCode;

    @NotBlank(message = "쿼리 이름은 필수입니다")
    @Size(max = 100, message = "쿼리 이름은 100자 이내여야 합니다")
    private String queryName;

    @Size(max = 500, message = "설명은 500자 이내여야 합니다")
    private String description;

    @NotBlank(message = "SQL 쿼리는 필수입니다")
    private String querySql;

    @NotBlank(message = "값 컬럼명은 필수입니다")
    @Size(max = 100, message = "값 컬럼명은 100자 이내여야 합니다")
    private String valueColumn = "value";

    @NotBlank(message = "라벨 컬럼명은 필수입니다")
    @Size(max = 100, message = "라벨 컬럼명은 100자 이내여야 합니다")
    private String labelColumn = "label";

    @Size(max = 100, message = "색상 컬럼명은 100자 이내여야 합니다")
    private String colorColumn;

    @Pattern(regexp = "^[YN]$", message = "캐시 사용 여부는 Y 또는 N이어야 합니다")
    private String cacheEnabledYn = "N";

    @Min(value = 60, message = "캐시 TTL은 60초 이상이어야 합니다")
    @Max(value = 86400, message = "캐시 TTL은 86400초 이하여야 합니다")
    private Integer cacheTtlSeconds = 300;

    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다")
    private Integer sortOrder = 0;
}
