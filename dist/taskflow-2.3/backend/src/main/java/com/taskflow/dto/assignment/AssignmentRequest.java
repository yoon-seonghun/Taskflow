package com.taskflow.dto.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 업무 배정 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRequest {

    /**
     * 담당자 USERNAME (필수)
     */
    @NotBlank(message = "담당자는 필수입니다")
    private String assigneeUsername;

    /**
     * 권한 수준 (VIEW, EDIT, FULL)
     */
    @NotBlank(message = "권한 수준은 필수입니다")
    @Pattern(regexp = "VIEW|EDIT|FULL", message = "권한 수준은 VIEW, EDIT, FULL 중 하나여야 합니다")
    private String permissionLevel;

    /**
     * 이메일 발송 여부 (기본: true)
     */
    @Builder.Default
    private Boolean sendEmail = true;

    /**
     * 앱 알림 발송 여부 (기본: true)
     */
    @Builder.Default
    private Boolean sendNotification = true;
}
