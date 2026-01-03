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
     * 이메일 주소
     */
    private String email;

    /**
     * 부서 코드
     */
    private String departmentCode;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 직급 코드
     */
    private String positionCode;

    /**
     * 직급명
     */
    private String positionName;

    /**
     * 직급 정렬 순서 (낮을수록 높은 직급)
     */
    private Integer positionSortOrder;

    /**
     * 권한 (ADMIN, MANAGER, USER, GUEST)
     */
    private String role;

    /**
     * 팀장 여부
     */
    private String headYn;

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
                .email(user.getEmail())
                .departmentCode(user.getDepartmentCode())
                .departmentName(user.getDepartmentName())
                .positionCode(user.getPositionCode())
                .positionName(user.getPositionName())
                .positionSortOrder(user.getPositionSortOrder())
                .role(user.getRole())
                .headYn(user.getHeadYn())
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
