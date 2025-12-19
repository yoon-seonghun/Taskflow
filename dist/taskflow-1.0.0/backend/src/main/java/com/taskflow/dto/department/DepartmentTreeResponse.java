package com.taskflow.dto.department;

import com.taskflow.domain.Department;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 부서 트리 응답 DTO (계층 구조)
 */
@Getter
@Builder
public class DepartmentTreeResponse {

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
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부
     */
    private String useYn;

    /**
     * 계층 레벨 (0 = 최상위)
     */
    private Integer level;

    /**
     * 소속 사용자 수
     */
    private Integer userCount;

    /**
     * 하위 부서 목록
     */
    @Builder.Default
    private List<DepartmentTreeResponse> children = new ArrayList<>();

    /**
     * Department 엔티티에서 DepartmentTreeResponse 생성 (재귀)
     */
    public static DepartmentTreeResponse from(Department department) {
        if (department == null) {
            return null;
        }

        List<DepartmentTreeResponse> childrenResponse = new ArrayList<>();
        if (department.getChildren() != null && !department.getChildren().isEmpty()) {
            childrenResponse = department.getChildren().stream()
                    .map(DepartmentTreeResponse::from)
                    .collect(Collectors.toList());
        }

        return DepartmentTreeResponse.builder()
                .departmentId(department.getDepartmentId())
                .departmentCode(department.getDepartmentCode())
                .departmentName(department.getDepartmentName())
                .parentId(department.getParentId())
                .sortOrder(department.getSortOrder())
                .useYn(department.getUseYn())
                .level(department.getLevel())
                .userCount(department.getUserCount())
                .children(childrenResponse)
                .build();
    }

    /**
     * Department 목록에서 트리 구조로 변환
     */
    public static List<DepartmentTreeResponse> fromList(List<Department> departments) {
        return departments.stream()
                .map(DepartmentTreeResponse::from)
                .collect(Collectors.toList());
    }
}
