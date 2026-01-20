package com.taskflow.dto.external;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 외부 DB 데이터소스 생성 요청 DTO
 */
@Getter
@Setter
public class ExternalDatasourceCreateRequest {

    @NotBlank(message = "데이터소스 코드는 필수입니다")
    @Size(max = 50, message = "데이터소스 코드는 50자 이내여야 합니다")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "데이터소스 코드는 대문자, 숫자, 언더스코어만 가능합니다")
    private String datasourceCode;

    @NotBlank(message = "데이터소스 이름은 필수입니다")
    @Size(max = 100, message = "데이터소스 이름은 100자 이내여야 합니다")
    private String datasourceName;

    @NotBlank(message = "DB 타입은 필수입니다")
    @Pattern(regexp = "^(MYSQL|ORACLE|MSSQL|TIBERO6|TIBERO7)$", message = "지원하지 않는 DB 타입입니다")
    private String dbType;

    @Pattern(regexp = "^(SID|SERVICE_NAME)?$", message = "지원하지 않는 연결 타입입니다")
    private String connectionType;

    @NotBlank(message = "호스트 주소는 필수입니다")
    @Size(max = 255, message = "호스트 주소는 255자 이내여야 합니다")
    private String host;

    @NotNull(message = "포트 번호는 필수입니다")
    @Min(value = 1, message = "포트 번호는 1 이상이어야 합니다")
    @Max(value = 65535, message = "포트 번호는 65535 이하여야 합니다")
    private Integer port;

    @NotBlank(message = "데이터베이스명은 필수입니다")
    @Size(max = 100, message = "데이터베이스명은 100자 이내여야 합니다")
    private String databaseName;

    @Size(max = 100, message = "스키마명은 100자 이내여야 합니다")
    private String schemaName;

    @NotBlank(message = "접속 계정은 필수입니다")
    @Size(max = 100, message = "접속 계정은 100자 이내여야 합니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @Min(value = 1000, message = "연결 타임아웃은 1000ms 이상이어야 합니다")
    @Max(value = 60000, message = "연결 타임아웃은 60000ms 이하여야 합니다")
    private Integer connectionTimeout = 5000;

    @Min(value = 1000, message = "쿼리 타임아웃은 1000ms 이상이어야 합니다")
    @Max(value = 300000, message = "쿼리 타임아웃은 300000ms 이하여야 합니다")
    private Integer queryTimeout = 30000;

    @Min(value = 1, message = "최대 풀 크기는 1 이상이어야 합니다")
    @Max(value = 10, message = "최대 풀 크기는 10 이하여야 합니다")
    private Integer maxPoolSize = 3;
}
