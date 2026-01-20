package com.taskflow.dto.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskflow.domain.UserGroup;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 그룹 멤버 응답 DTO
 */
@Getter
@Builder
public class GroupMemberResponse {

    /**
     * 사용자-그룹 매핑 ID
     */
    private Long userGroupId;

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 사용자 USERNAME
     */
    private String username;

    /**
     * 사용자명
     */
    @JsonProperty("user_name")
    private String userName;

    /**
     * 그룹 ID
     */
    private Long groupId;

    /**
     * 그룹명
     */
    private String groupName;

    /**
     * 그룹 코드
     */
    private String groupCode;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 등록일시 (joinedAt으로 프론트엔드에 전달)
     */
    @JsonProperty("joined_at")
    private LocalDateTime createdAt;

    /**
     * UserGroup 도메인에서 변환
     */
    public static GroupMemberResponse from(UserGroup userGroup) {
        if (userGroup == null) {
            return null;
        }

        return GroupMemberResponse.builder()
                .userGroupId(userGroup.getUserGroupId())
                .userId(userGroup.getUserId())
                .username(userGroup.getUsername())
                .userName(userGroup.getUserName())
                .groupId(userGroup.getGroupId())
                .groupName(userGroup.getGroupName())
                .groupCode(userGroup.getGroupCode())
                .departmentName(userGroup.getDepartmentName())
                .createdAt(userGroup.getCreatedAt())
                .build();
    }

    /**
     * UserGroup 리스트에서 변환
     */
    public static List<GroupMemberResponse> fromList(List<UserGroup> userGroups) {
        if (userGroups == null) {
            return List.of();
        }

        return userGroups.stream()
                .map(GroupMemberResponse::from)
                .collect(Collectors.toList());
    }
}
