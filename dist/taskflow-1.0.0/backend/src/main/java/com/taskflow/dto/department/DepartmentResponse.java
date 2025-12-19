package com.taskflow.dto.department;

import com.taskflow.domain.Department;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 부서 응답 DTO (단건 조회용)
 */
@Getter
@Builder
public class DepartmentResponse {

    /**
     * 부서 ID
     */
    private Long departmentId;

    /**
     * 부서 코드
     */
    private String departmentCode;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 상위 부서 ID
     */
    private Long parentId;

    /**
     * 상위 부서명
     */
    private String parentName;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부
     */
    private String useYn;

    /**
     * 계층 레벨
     */
    private Integer level;

    /**
     * 하위 부서 수
     */
    private Integer childCount;

    /**
     * 소속 사용자 수
     */
    private Integer userCount;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * Department 엔티티에서 DepartmentResponse 생성
     */
    public static DepartmentResponse from(Department department) {
        if (department == null) {
            return null;
        }

        return DepartmentResponse.builder()
                .departmentId(department.getDepartmentId())
                .departmentCode(department.getDepartmentCode())
                .departmentName(department.getDepartmentName())
                .parentId(department.getParentId())
                .parentName(department.getParentName())
                .sortOrder(department.getSortOrder())
                .useYn(department.getUseYn())
                .level(department.getLevel())
                .childCount(department.getChildCount())
                .userCount(department.getUserCount())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
