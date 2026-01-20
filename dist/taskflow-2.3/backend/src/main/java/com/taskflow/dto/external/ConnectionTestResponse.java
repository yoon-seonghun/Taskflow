package com.taskflow.dto.external;

import lombok.Builder;
import lombok.Getter;

/**
 * 외부 DB 연결 테스트 응답 DTO
 */
@Getter
@Builder
public class ConnectionTestResponse {

    /**
     * 연결 성공 여부
     */
    private boolean success;

    /**
     * 메시지 (성공/실패 상세)
     */
    private String message;

    /**
     * 연결 소요 시간 (ms)
     */
    private Long connectionTimeMs;

    /**
     * DB 버전 정보
     */
    private String dbVersion;

    /**
     * 드라이버 정보
     */
    private String driverInfo;

    /**
     * 성공 응답 생성
     */
    public static ConnectionTestResponse success(String dbVersion, String driverInfo, long connectionTimeMs) {
        return ConnectionTestResponse.builder()
                .success(true)
                .message("연결 성공")
                .dbVersion(dbVersion)
                .driverInfo(driverInfo)
                .connectionTimeMs(connectionTimeMs)
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static ConnectionTestResponse failure(String errorMessage) {
        return ConnectionTestResponse.builder()
                .success(false)
                .message(errorMessage)
                .build();
    }

    /**
     * 실패 응답 생성 (소요시간 포함)
     */
    public static ConnectionTestResponse failure(String errorMessage, long connectionTimeMs) {
        return ConnectionTestResponse.builder()
                .success(false)
                .message(errorMessage)
                .connectionTimeMs(connectionTimeMs)
                .build();
    }
}
