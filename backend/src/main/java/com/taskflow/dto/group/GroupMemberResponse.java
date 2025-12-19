package com.taskflow.dto.group;

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
     * 사용자명
     */
    private String userName;

    /**
     * 로그인 ID
     */
    private String loginId;

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
     * 등록일시
     */
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
                .userName(userGroup.getUserName())
                .loginId(userGroup.getLoginId())
                .groupId(userGroup.getGroupId())
                .groupName(userGroup.getGroupName())
                .groupCode(userGroup.getGroupCode())
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
