package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 속성 정의 엔티티
 *
 * 테이블: TB_PROPERTY_DEF
 *
 * v2.0 변경사항:
 * - OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE 필드 추가
 * - 글로벌/매니저/사용자 속성 소유 구조
 * - USER 타입: 개인이 생성한 속성, 카테고리에 그룹화되어 보드/업무에 활용
 *
 * USERNAME 기반 FK 참조 시스템:
 * - CREATED_BY, UPDATED_BY: USERNAME 참조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDef {

    /**
     * 속성 정의 ID (PK)
     */
    private Long propertyId;

    /**
     * 보드 ID (FK) - NULL이면 보드 미귀속 속성 (글로벌/매니저)
     */
    private Long boardId;

    /**
     * 소유 유형 (GLOBAL, MANAGER, BOARD)
     */
    private String ownerType;

    /**
     * 소유자 USERNAME (GLOBAL은 NULL)
     */
    private String ownerUsername;

    /**
     * 매니저 속성의 부서 코드 (적용 범위)
     */
    private String ownerDeptCode;

    /**
     * 속성명
     */
    private String propertyName;

    /**
     * 속성 타입
     * TEXT, NUMBER, DATE, SELECT, MULTI_SELECT, CHECKBOX, USER
     */
    private String propertyType;

    /**
     * 표시 순서
     */
    private Integer sortOrder;

    /**
     * 표시 여부 (Y/N)
     */
    private String visibleYn;

    /**
     * 사용 여부 (Y/N) - 논리 삭제용
     */
    private String useYn;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 USERNAME
     */
    private String updatedBy;

    // =============================================
    // 추가 필드 (Mapper에서 설정)
    // =============================================

    /**
     * 옵션 목록 (SELECT, MULTI_SELECT 타입용)
     */
    @Builder.Default
    private List<PropertyOption> options = new ArrayList<>();

    // =============================================
    // 상수: 속성 타입
    // =============================================

    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_NUMBER = "NUMBER";
    public static final String TYPE_DATE = "DATE";
    public static final String TYPE_SELECT = "SELECT";
    public static final String TYPE_MULTI_SELECT = "MULTI_SELECT";
    public static final String TYPE_CHECKBOX = "CHECKBOX";
    public static final String TYPE_USER = "USER";

    // =============================================
    // 상수: 소유 유형
    // =============================================

    public static final String OWNER_TYPE_GLOBAL = "GLOBAL";
    public static final String OWNER_TYPE_MANAGER = "MANAGER";
    public static final String OWNER_TYPE_USER = "USER";

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 표시 속성 여부
     */
    public boolean isVisible() {
        return "Y".equals(visibleYn);
    }

    /**
     * 활성 속성 여부 (논리 삭제 여부)
     */
    public boolean isActive() {
        return "Y".equals(useYn);
    }

    /**
     * 선택형 속성 여부 (옵션이 필요한 타입)
     */
    public boolean isSelectType() {
        return TYPE_SELECT.equals(propertyType) || TYPE_MULTI_SELECT.equals(propertyType);
    }

    /**
     * 옵션 추가
     */
    public void addOption(PropertyOption option) {
        if (options == null) {
            options = new ArrayList<>();
        }
        options.add(option);
    }

    /**
     * 글로벌 속성 여부
     */
    public boolean isGlobal() {
        return OWNER_TYPE_GLOBAL.equals(ownerType);
    }

    /**
     * 매니저 속성 여부
     */
    public boolean isManager() {
        return OWNER_TYPE_MANAGER.equals(ownerType);
    }

    /**
     * 사용자 속성 여부
     */
    public boolean isUser() {
        return OWNER_TYPE_USER.equals(ownerType);
    }
}
