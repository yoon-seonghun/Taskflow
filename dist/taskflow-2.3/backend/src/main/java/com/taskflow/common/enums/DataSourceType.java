package com.taskflow.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 속성의 데이터 소스 타입
 */
@Getter
@RequiredArgsConstructor
public enum DataSourceType {

    /**
     * 내부 데이터 소스 (기본값)
     * - PropertyOption 테이블의 옵션 사용
     */
    INTERNAL("내부"),

    /**
     * 외부 데이터 소스
     * - ExternalQuery를 통해 외부 DB에서 옵션 조회
     */
    EXTERNAL("외부");

    private final String displayName;

    /**
     * 문자열에서 DataSourceType 변환
     */
    public static DataSourceType fromString(String value) {
        if (value == null || value.isBlank()) {
            return INTERNAL; // 기본값
        }

        try {
            return DataSourceType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INTERNAL; // 기본값
        }
    }

    /**
     * 외부 데이터 소스 여부
     */
    public boolean isExternal() {
        return this == EXTERNAL;
    }
}
