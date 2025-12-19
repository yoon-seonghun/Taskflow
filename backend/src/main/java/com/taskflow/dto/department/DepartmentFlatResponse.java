package com.taskflow.dto.department;

import com.taskflow.domain.Department;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 부서 평면 응답 DTO (SELECT 박스용)
 *
 * 계층 구조를 유지하면서 평면 목록으로 반환
 * 들여쓰기용 레벨 정보 포함
 */
@Getter
@Builder
public class DepartmentFlatResponse {

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
     * 표시용 부서명 (레벨에 따른 들여쓰기 포함)
     * 예: "├─ 개발1팀", "│  └─ 프론트엔드"
     */
    private String displayName;

    /**
     * 상위 부서 ID
     */
    private Long parentId;

    /**
     * 계층 레벨 (0 = 최상위)
     */
    private Integer level;

    /**
     * 경로 (예: "1/2/3")
     */
    private String path;

    /**
     * 사용 여부
     */
    private String useYn;

    /**
     * Department 엔티티에서 DepartmentFlatResponse 생성
     */
    public static DepartmentFlatResponse from(Department department) {
        if (department == null) {
            return null;
        }

        String displayName = buildDisplayName(department.getDepartmentName(), department.getLevel());

        return DepartmentFlatResponse.builder()
                .departmentId(department.getDepartmentId())
                .departmentCode(department.getDepartmentCode())
                .departmentName(department.getDepartmentName())
                .displayName(displayName)
                .parentId(department.getParentId())
                .level(department.getLevel())
                .path(department.getPath())
                .useYn(department.getUseYn())
                .build();
    }

    /**
     * Department 목록에서 DepartmentFlatResponse 목록 생성
     */
    public static List<DepartmentFlatResponse> fromList(List<Department> departments) {
        return departments.stream()
                .map(DepartmentFlatResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 들여쓰기가 포함된 표시용 부서명 생성
     */
    private static String buildDisplayName(String name, Integer level) {
        if (level == null || level == 0) {
            return name;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        sb.append(name);
        return sb.toString();
    }
}
