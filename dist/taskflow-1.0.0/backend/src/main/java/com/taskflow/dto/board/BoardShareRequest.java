package com.taskflow.dto.board;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 보드 공유 추가 요청 DTO
 */
@Getter
@Setter
public class BoardShareRequest {

    /**
     * 공유할 사용자 ID
     */
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    /**
     * 권한 레벨 (MEMBER: 편집, VIEWER: 조회만)
     */
    @Pattern(regexp = "^(MEMBER|VIEWER)$", message = "권한은 MEMBER 또는 VIEWER이어야 합니다")
    private String permission = "MEMBER";  // 기본값: 편집 권한
}
