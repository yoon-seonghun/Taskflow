package com.taskflow.dto.share;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공유 추가 요청 DTO
 * 보드 공유, 업무 공유에 공통 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareRequest {

    /**
     * 공유받을 사용자 ID
     */
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    /**
     * 권한 (VIEW/EDIT/FULL)
     */
    @NotNull(message = "권한은 필수입니다.")
    @Pattern(regexp = "^(VIEW|EDIT|FULL)$", message = "권한은 VIEW, EDIT, FULL 중 하나여야 합니다.")
    private String permission;
}
