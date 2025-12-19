package com.taskflow.dto.user;

import com.taskflow.domain.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 응답 DTO
 */
@Getter
@Setter
@Builder
public class UserResponse {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 로그인 아이디
     */
    private String username;

    /**
     * 사용자 이름
     */
    private String name;

    /**
     * 소속 부서 ID
     */
    private Long departmentId;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 부서 코드
     */
    private String departmentCode;

    /**
     * 사용 여부
     */
    private String useYn;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 소속 그룹 ID 목록
     */
    private List<Long> groupIds;

    /**
     * 소속 그룹 정보 목록
     */
    private List<UserGroupInfo> groups;

    /**
     * User 엔티티에서 UserResponse 생성
     */
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .name(user.getName())
                .departmentId(user.getDepartmentId())
                .departmentName(user.getDepartmentName())
                .departmentCode(user.getDepartmentCode())
                .useYn(user.getUseYn())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 사용자 그룹 정보 DTO
     */
    @Getter
    @Builder
    public static class UserGroupInfo {
        private Long groupId;
        private String groupCode;
        private String groupName;
    }
}
