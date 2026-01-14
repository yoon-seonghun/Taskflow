package com.taskflow.dto.external;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 쿼리 테스트 요청 DTO (저장 전 테스트용)
 */
@Getter
@Setter
public class QueryTestRequest {

    @NotNull(message = "데이터소스 ID는 필수입니다")
    private Long datasourceId;

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

    /**
     * 최대 결과 행 수 (기본 100, 테스트용)
     */
    @Min(value = 1, message = "최대 결과 행 수는 1 이상이어야 합니다")
    @Max(value = 1000, message = "최대 결과 행 수는 1000 이하여야 합니다")
    private Integer maxRows = 100;
}
