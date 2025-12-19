package com.taskflow.dto.group;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 그룹 멤버 추가 요청 DTO
 */
@Getter
@Setter
public class GroupMemberRequest {

    /**
     * 사용자 ID
     */
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;
}
