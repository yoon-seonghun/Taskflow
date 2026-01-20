package com.taskflow.dto.group;

import com.taskflow.domain.Group;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 그룹 응답 DTO
 */
@Getter
@Builder
public class GroupResponse {

    /**
     * 그룹 ID
     */
    private Long groupId;

    /**
     * 그룹 코드
     */
    private String groupCode;

    /**
     * 그룹명
     */
    private String groupName;

    /**
     * 그룹 설명
     */
    private String description;

    /**
     * 표시 색상 (#RRGGBB)
     */
    private String color;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부
     */
    private String useYn;

    /**
     * 멤버 수
     */
    private Integer memberCount;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 도메인 객체를 응답 DTO로 변환
     */
    public static GroupResponse from(Group group) {
        if (group == null) {
            return null;
        }

        return GroupResponse.builder()
                .groupId(group.getGroupId())
                .groupCode(group.getGroupCode())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .color(group.getColor())
                .sortOrder(group.getSortOrder())
                .useYn(group.getUseYn())
                .memberCount(group.getMemberCount())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 객체 리스트를 응답 DTO 리스트로 변환
     */
    public static List<GroupResponse> fromList(List<Group> groups) {
        if (groups == null) {
            return List.of();
        }

        return groups.stream()
                .map(GroupResponse::from)
                .collect(Collectors.toList());
    }
}
