package com.taskflow.dto.external;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 외부 DB 데이터소스 수정 요청 DTO
 */
@Getter
@Setter
public class ExternalDatasourceUpdateRequest {

    @Size(max = 100, message = "데이터소스 이름은 100자 이내여야 합니다")
    private String datasourceName;

    @Pattern(regexp = "^(SID|SERVICE_NAME)?$", message = "지원하지 않는 연결 타입입니다")
    private String connectionType;

    @Size(max = 255, message = "호스트 주소는 255자 이내여야 합니다")
    private String host;

    @Min(value = 1, message = "포트 번호는 1 이상이어야 합니다")
    @Max(value = 65535, message = "포트 번호는 65535 이하여야 합니다")
    private Integer port;

    @Size(max = 100, message = "데이터베이스명은 100자 이내여야 합니다")
    private String databaseName;

    @Size(max = 100, message = "스키마명은 100자 이내여야 합니다")
    private String schemaName;

    @Size(max = 100, message = "접속 계정은 100자 이내여야 합니다")
    private String username;

    /**
     * 비밀번호 (변경할 경우에만 입력)
     */
    private String password;

    @Min(value = 1000, message = "연결 타임아웃은 1000ms 이상이어야 합니다")
    @Max(value = 60000, message = "연결 타임아웃은 60000ms 이하여야 합니다")
    private Integer connectionTimeout;

    @Min(value = 1000, message = "쿼리 타임아웃은 1000ms 이상이어야 합니다")
    @Max(value = 300000, message = "쿼리 타임아웃은 300000ms 이하여야 합니다")
    private Integer queryTimeout;

    @Min(value = 1, message = "최대 풀 크기는 1 이상이어야 합니다")
    @Max(value = 10, message = "최대 풀 크기는 10 이하여야 합니다")
    private Integer maxPoolSize;

    @Pattern(regexp = "^[YN]$", message = "사용 여부는 Y 또는 N이어야 합니다")
    private String useYn;
}
