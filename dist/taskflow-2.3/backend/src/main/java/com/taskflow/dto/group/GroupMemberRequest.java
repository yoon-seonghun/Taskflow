package com.taskflow.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 그룹 멤버 추가 요청 DTO
 */
@Getter
@Setter
public class GroupMemberRequest {

    /**
     * 사용자 아이디
     */
    @NotBlank(message = "사용자 아이디는 필수입니다")
    private String username;
}
