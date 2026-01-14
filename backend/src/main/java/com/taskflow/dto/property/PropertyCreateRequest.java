package com.taskflow.dto.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 속성 정의 생성 요청 DTO
 *
 * v2.0 변경사항:
 * - requiredYn 필드 제거 (CategoryProperty/BoardProperty로 이동)
 * - ownerType, ownerUsername, ownerDeptCode 필드 추가
 */
@Getter
@Setter
public class PropertyCreateRequest {

    /**
     * 속성명
     */
    @NotBlank(message = "속성명은 필수입니다")
    @Size(max = 100, message = "속성명은 100자 이내여야 합니다")
    private String propertyName;

    /**
     * 속성 타입 (TEXT, NUMBER, DATE, SELECT, MULTI_SELECT, CHECKBOX, USER)
     */
    @NotBlank(message = "속성 타입은 필수입니다")
    @Pattern(regexp = "^(TEXT|NUMBER|DATE|SELECT|MULTI_SELECT|CHECKBOX|USER)$",
            message = "유효하지 않은 속성 타입입니다")
    private String propertyType;

    /**
     * 소유 유형 (GLOBAL, MANAGER, USER)
     * - GLOBAL: 전역 속성 (관리자만 생성 가능)
     * - MANAGER: 매니저 속성 (생성자 본인/하위 부서에서 사용)
     * - USER: 사용자 속성 (기본값, 카테고리에 그룹화되어 보드/업무에 활용)
     */
    @Pattern(regexp = "^(GLOBAL|MANAGER|USER)$",
            message = "소유 유형은 GLOBAL, MANAGER, USER 중 하나여야 합니다")
    private String ownerType = "USER";

    /**
     * 소유자 USERNAME (MANAGER 타입 시 자동 설정)
     */
    @Size(max = 50, message = "소유자 USERNAME은 50자 이내여야 합니다")
    private String ownerUsername;

    /**
     * 매니저 속성의 부서 코드 (적용 범위)
     */
    @Size(max = 20, message = "부서 코드는 20자 이내여야 합니다")
    private String ownerDeptCode;

    /**
     * 표시 순서
     */
    private Integer sortOrder;

    /**
     * 표시 여부 (Y/N)
     */
    @Pattern(regexp = "^[YN]$", message = "표시 여부는 Y 또는 N이어야 합니다")
    private String visibleYn = "Y";

    /**
     * 외부 쿼리 ID (SELECT/MULTI_SELECT/CHECKBOX 타입에서 외부 데이터 소스 사용 시)
     */
    private Long externalQueryId;

    /**
     * 데이터 소스 타입 (INTERNAL/EXTERNAL)
     * - INTERNAL: 내부 옵션 테이블 사용 (기본값)
     * - EXTERNAL: 외부 쿼리 결과 사용
     */
    @Pattern(regexp = "^(INTERNAL|EXTERNAL)$", message = "데이터 소스 타입은 INTERNAL 또는 EXTERNAL이어야 합니다")
    private String dataSourceType = "INTERNAL";
}
