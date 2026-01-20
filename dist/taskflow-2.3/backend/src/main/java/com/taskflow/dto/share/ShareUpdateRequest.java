package com.taskflow.dto.share;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공유 권한 변경 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareUpdateRequest {

    /**
     * 변경할 권한 (VIEW/EDIT/FULL)
     */
    @NotNull(message = "권한은 필수입니다.")
    @Pattern(regexp = "^(VIEW|EDIT|FULL)$", message = "권한은 VIEW, EDIT, FULL 중 하나여야 합니다.")
    private String permission;
}
