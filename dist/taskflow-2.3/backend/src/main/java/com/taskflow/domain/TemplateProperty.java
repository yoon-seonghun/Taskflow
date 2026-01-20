package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 템플릿 속성 매핑 엔티티
 *
 * 테이블: TB_TEMPLATE_PROPERTY
 *
 * 작업 템플릿에 미리 지정된 속성과 기본값을 저장합니다.
 * 템플릿 기반 업무 생성 시 해당 속성값이 자동으로 적용됩니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateProperty {

    /**
     * 매핑 ID (PK)
     */
    private Long id;

    /**
     * 템플릿 ID (FK)
     */
    private Long templateId;

    /**
     * 속성 ID (FK)
     */
    private Long propertyId;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 기본값
     */
    private String defaultValue;

    /**
     * 필수 여부 (Y/N)
     */
    private String requiredYn;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 삭제일시 (논리삭제)
     */
    private LocalDateTime deletedAt;

    /**
     * 삭제자 USERNAME
     */
    private String deletedBy;

    // =============================================
    // 조인 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 속성명
     */
    private String propertyName;

    /**
     * 속성 타입
     */
    private String propertyType;

    /**
     * 속성 소유자 타입 (GLOBAL, MANAGER, USER)
     */
    private String ownerType;
}
